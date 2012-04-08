package org.jnes.component.impl;

import java.util.HashSet;

import org.jnes.Log;
import org.jnes.NESSystem;
import org.jnes.NESSystemImpl;
import org.jnes.component.PPU;

public class PPUImpl implements PPU {
	
	public static int SCREEN_LINES= 232;
	
	public static int TOTAL_LINES = 262;
	
	public static int CYCLES_HBLANK = 1773447/(50*TOTAL_LINES);
	
	private NESSystemImpl system;
	private Log logger = new Log();
	
	private int physicalLineNumber = 0;

	private boolean addressLatch = false;
	private int sprRamAddress = 0;
	private int[] sprRam = new int[0x100];
	private int vramAddress = 0;	
	private int hScroll;
	private int vScroll;
	private int logicalLineNumber;
	private int dataLatch = 0;

	public byte[] palettes = new byte[0x20];

	private int addressIncrement = 1;
	private int control1,control2;
	private boolean vblankFlag;
	private boolean collisionFlag;
	
	public PPUImpl() {
		mix = new int[0x100];
		for (int i=0; i<0x100; i++) {
			int a = 0;
			for (int j=0; j<8; j++) {
				int k = ((i>>j)&1)<<(7-j);
				a += k*k;
			}
			mix[i] = a;
		}
	}

	
	public void setSystem(NESSystem system) {
		this.system = (NESSystemImpl)system;
	}

	public int getLineNumber() {
		return physicalLineNumber;
	}

	public void endLine() {

		if (physicalLineNumber<SCREEN_LINES) {
			drawLine(target, physicalLineNumber<<8);
			logicalLineNumber++;
			if ((logicalLineNumber&0xff)==0xf0) {
				logicalLineNumber ^= 0x100;
				logicalLineNumber &= 0x100;
			}

		} else if (physicalLineNumber==SCREEN_LINES) {
			logger.debug("vblank");
			vblankFlag = true;
			collisionFlag = false;
			if ((control1&0x80)!=0) {
				system.nmi();
			}
			
		} else if (physicalLineNumber==TOTAL_LINES) {
			logger.debug("end of vblank");
			vblankFlag = false;
			physicalLineNumber = 0;
			logicalLineNumber = vScroll + (control1&2)<<0x400;
			/*
			for (int i=0; i<0x100; i+=4) {
				int y = sprRam[i];
				if (y<0xef || y>0xfe) {
					y = (y+1) & 0xff;
					int tile = sprRam[i+1];
					int attributes = sprRam[i+2];
					int x = sprRam[i+3];
					System.out.printf("%02x : %02x %02x %02x %02x\n",i,x,y,tile,attributes);
				}
			}
			System.out.println();
			*/
		}

		physicalLineNumber++;
	}
	
	HashSet<Integer> addresses = new HashSet<Integer>();

	private int readStatus() {/*
		int pc = system.cpu.pc;
		if (!addresses.contains(pc)) {
			System.out.printf("%04x\n",system.cpu.pc);
			addresses.add(pc);
		}*/
		
		int value = 0;
		// bit 7 = in vblank
		value |= vblankFlag ? 0x80 : 0;
		
		// sprite 0 collision
		/*if ((control2&0x18)==0x18) {
			value |= 0x40; // TODO
		}*/
		value |= collisionFlag ? 0x40 : 0;
		return value;
	}
	
	private int readSprRam() {
		return sprRam[sprRamAddress];
	}
	
	private void writeSprRam(int value) {
		sprRam[sprRamAddress] = value&0xff;
		sprRamAddress = (sprRamAddress+1) & 0xFF;
	}
	
	private int readVRam() {
		int r;
		if ((vramAddress&0x3f00)==0x3f00) {
			r = palettes[vramAddress&0x1f];
		} else {
			r = dataLatch;
			dataLatch = system.ppuRead(vramAddress);
		}
		incrementVRamAddress();
		return r;
	}
	private void writeVRam(int value) {
		if ((control2&0x8)==0x8 && !vblankFlag) return;
		if ((vramAddress&0x3f00)==0x3f00) {
			paletteWrite(vramAddress, value);
		} else {
			system.ppuWrite(vramAddress, value);
		}
		incrementVRamAddress();
		logger.debug("vramWrite(%02x) - address is now %04x",value,vramAddress);
	}

	private void incrementVRamAddress() {
		vramAddress = (vramAddress+addressIncrement) & 0xffff;
	}

	private void writeControl1(int value) {
		logger.debug("ppu control1=%02x", value);
		addressIncrement = (value&4)!=0 ? 0x20 : 1;
		control1 = value;
	}

	private void writeControl2(int value) {
		logger.debug("ppu control2=%02x", value);
		control2 = value;
	}

	private void updateVRamAddress(int value) {
		addressLatch = !addressLatch;
		if (!addressLatch) {
			hScroll = value;
			vramAddress = (vramAddress&0xff00) | value;
		} else {
			//vScroll = value;
			vramAddress = (vramAddress&0x00ff) | (value<<8);
			int screen = (vramAddress>>11)&1;
			int y = (vramAddress>>5) & 0x1f;
			logicalLineNumber = (screen*0x100) + (y<<3);
		}
		logger.debug("updateVRamAddress(%02x) - address is now %04x",value,vramAddress);
	}


	public int readRegister(int address) {
		switch (address & 7) {
		case 2:
			addressLatch = false;
			return readStatus();
		case 4:
			return readSprRam();
		case 7:
			return readVRam();
		default:
			return 0;
		}
	}

	public void writeRegister(int address, int value) {
		switch (address & 7) {
		case 0:
			writeControl1(value);
			break;
		case 1:
			writeControl2(value);
			break;
		case 3:
			sprRamAddress = value;
			break;
		case 4:
			writeSprRam(value);
			break;
			
		case 5:
			addressLatch = !addressLatch;
			if (addressLatch) {
				hScroll = value;
			} else {
				vScroll = value;
			}
			break;
			
		case 6:
			updateVRamAddress(value);
			break;
		case 7:
			writeVRam(value);
			break;
			
		default:
			// nop
			break;
		}
	}
	
	private void paletteWrite(int address, int value) {
		byte b = (byte)(value &= 0x3f);
		address &= 0x1f;
		
		palettes[address] = b;
		// mirrorring of 00, 04, 08 and 0c to 10, 14, 18 and 1c
		if ((address&3)==0) {
			palettes[address^0x10] = b;
		}
	}

	private byte[] target = new byte[0x10000];

	public byte[] getTargetArray() {
		return target;
	}
	
	
	public void showTiles(byte[] target, int offset, int scanline, int palette) {
		palette <<= 2;
		for (int i=0; i<0x200; i++) {
			for (int j=0; j<8; j++) {
				int x0 = (i&0xf)<<3;
				int y = ((i>>4)<<3) + j;
				int off = offset + y*scanline + x0;

				int pointer = (i<<4) + j;
				int p0 = system.ppuRead(pointer);
				int p1 = system.ppuRead(pointer+8);
				int colors = mix[p0] + (mix[p1]<<1);
				for (int k=0; k<8; k++) {
					int c = colors&3;
					colors >>= 2;
					byte color = palettes[c + palette];
					target[off+k] = color;
				}
			}
		}
	}
	
	public void showScreens(byte[] target, int offset, int scanline) {
		int[] buffer = new int[0x100];
		for (int i=0; i<0x1e0; i++) {
			int y = (i>0xef) ? i+0x10 : i;
			drawBackground(buffer, y, 0);
			for (int j=0; j<0x100; j++) {
				target[offset + scanline*i + j] = palettes[buffer[j]];
			}
			drawBackground(buffer, y, 0x100);
			for (int j=0; j<0x100; j++) {
				target[offset + scanline*i + j + 0x100] = palettes[buffer[j]];
			}
		}
	}
	
	private int[] mix;
	
	private int[] bgBuffer = new int[0x100];
	private int[] spriteNumberBuffer = new int[0x100];
	private int[] backSprBuffer = new int[0x100];
	private int[] frontSprBuffer = new int[0x100];
	
	private void drawLine(byte[] array, int offset) {
		for (int i=0; i<0x100; i++) {
			bgBuffer[i] = 0;
			frontSprBuffer[i] = 0;
			backSprBuffer[i] = 0;
		}
		if ((control2&8)!=0) {
			int x = hScroll + 0x100*(control1&1);			
			drawBackground(bgBuffer, logicalLineNumber, x);
		}
		if ((control2&0x10)!=0) {
			drawSprites(frontSprBuffer, backSprBuffer, physicalLineNumber);
		}
		for (int i=0; i<0x100; i++) {
			int color = 0;
			if (frontSprBuffer[i]>0) {
				color = frontSprBuffer[i];
			} else if ((bgBuffer[i]&3)>0) {
				color = bgBuffer[i];
			} else {
				color = backSprBuffer[i];
			}
			array[offset+i] = palettes[color];
		}
		for (int i=0; i<0x100; i++) {
			if ((bgBuffer[i]&3)>0
			&& (frontSprBuffer[i]>0 || backSprBuffer[i]>0)
			&& spriteNumberBuffer[i]==0) {
				collisionFlag = true;
				break;
			}
		}
	}
	
	private int getYFromAddress(int address) {
		int screen = (address>>11) & 1;
		int tiles = (address>>5) & 0x1f;
		int line = (address>>12) & 7;
		return screen*0xf0 + (tiles<<3) + line;
	}
	
	public void drawBackground(int[] array, int y, int x) {
		
		int pointer = 0x2000
					+ ((y&0x100)<<3)
					+ ((y&0x0f8)<<2)
					+ ((x&0x100)<<2)
					+ ((x&0x0ff)>>3);
		int tileLine = y&7;

		int tilePage = (control1>>4)&1;
		int colors = 0;
		int palette = 0;
		int yTile = (pointer>>5) & 0x1f;
		for (int i=0; i<0x100; i++) {
			if (i==0 || (x&7)==0) {
				// read attributes
				int attributePointer = (pointer&0xfc00) | 0x3c0;
				attributePointer |= ((pointer>>2)&7) | (((pointer>>7)&7)<<3);
				int attributes = tilePage*0x100 + system.ppuRead(attributePointer);
				int dAttribute = (((x>>4)&1)<<1) + (((yTile>>1)&1)<<2);
				palette = ((attributes>>dAttribute)&3) << 2;

				// read colors
				int tileIndex = tilePage*0x100 + system.ppuRead(pointer);
				int tilePointer = (tileIndex<<4) + tileLine;
				int p0 = system.ppuRead(tilePointer);
				int p1 = system.ppuRead(tilePointer+8);
				colors = mix[p0] | (mix[p1]<<1);
								
				// increment pointer
				int tile = pointer&0x1f;
				tile = (tile+1) & 0x1f;
				pointer = (pointer&~0x1f) | tile;
				if (tile==0) {
					pointer ^= 0x400;
				}
			}
			array[i] = ((colors>>(2*(x&7)))&3) + palette;
			x++;
		}
	}

	private void drawSprites(int[] frontBuffer, int[] backBuffer, int y0) {
		boolean doubleHeight = (control1&0x20)!=0;
		int height = doubleHeight ? 16 : 8;
		int nbSprites = 0;
		for (int i=0; i<0x100; i+=4) {
			int y = sprRam[i];
			if (y>=0xef && y<=0xfe) {
				continue;
			}
			y = (y+1)&0xff;
			int line = y0-y;
			if (line>=0 && line<height) {
				int tileIndex = sprRam[i+1];
				int attributes = sprRam[i+2];
				int x = sprRam[i+3];
				
				if (doubleHeight) {
					tileIndex = ((tileIndex&1)<<8) + (tileIndex&0xfe);
				} else if ((control1>>3)!=0) {
					tileIndex += ((control1>>5)&1)<<8;
				}

				int palette = attributes&3;
				int[] buffer = ((attributes&0x20)!=0) ? backBuffer : frontBuffer;
				boolean hFlip = (attributes&0x40)!=0;
				boolean vFlip = (attributes&0x80)!=0;
				if (vFlip) {
					line = (height-1) - line;
				}
				
				drawSprite(buffer, i, x, tileIndex+(line>>3), line&7, palette, hFlip);
				nbSprites++;
				if (nbSprites>7) {
					break;
				}
			}
		}
	}

	private void drawSprite(int[] array, int number, int x0, int tileIndex, int line, int palette, boolean flip) {
		
		int tilePointer = (tileIndex<<4) + (line&7);
		int p0 = system.ppuRead(tilePointer);
		int p1 = system.ppuRead(tilePointer+8);
		int colors = mix[p0] | (mix[p1]<<1);

		int colorOffset = (palette<<2) + 16;
		int offset = flip ? 7 : 0;
		int d = flip ? -1 : 1;

		for (int i=0; i<8; i++) {
			int x = (x0 + offset+d*i) &0xff;
			int color = colors&3;
			colors >>= 2;
			if (color>0 && array[x]==0) {
				array[x] = colorOffset + color;
				spriteNumberBuffer[x] = number;
			}
		}
	}
}
