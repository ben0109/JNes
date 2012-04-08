package org.jnes.mapper.impl;

import org.jnes.mapper.AbstractMapper;

public class MMC1 extends AbstractMapper {
	
	protected class PRGRomChunk extends RomChunk {
		
		public PRGRomChunk(byte[] array) {
			super(array);
		}

		@Override
		public void write(int address, int value) {
			registerWrite(address, value);
		}
	}
	
	private int bitNumber = 0;
	private int shiftRegister = 0;
	private int[] register;

	protected int nbRomBanks;
	protected Chunk[] romChunks;

	protected int nbRamBanks;
	protected Chunk[] ramChunks;
	
	protected int nbVRomBanks;
	protected Chunk[] vromChunks;
	
	public MMC1(byte[][] romBanks, int nbRamBanks, byte[][] vromBanks) {
		super(nbRamBanks);

		this.nbRomBanks = romBanks.length;
		this.romChunks = new Chunk[nbRomBanks];
		for (int i=0; i<nbRomBanks; i++) {
			this.romChunks[i] = new PRGRomChunk(romBanks[i]);
		}

		this.nbRamBanks = nbRamBanks;
		this.ramChunks = new RamChunk[nbRamBanks];
		for (int i=0; i<nbRamBanks; i++) {
			ramChunks[i] = new RamChunk(0x1000);
		}

		this.nbVRomBanks = 2*vromBanks.length;
		this.vromChunks = new Chunk[2*nbVRomBanks];
		for (int i=0; i<nbVRomBanks; i++) {
			this.vromChunks[i] = new RomChunk(vromBanks[i/2],(i%2)*0x1000,0x1000);
		}
		
		register = new int[] { 0x1c,0,0,0 };
		updateChunks();
	}
	
	private void registerWrite(int address, int value) {

		logger.debug("mapper registerWrite %04x <- %02x", address, value);
		// bit 7 resets the shift register
		if ((value&0x80)!=0) {
			shiftRegister = 0;
			bitNumber = 0;
			return;
		}

		// update bits
		shiftRegister |= (value&1)<<bitNumber;
		bitNumber++;
	
		if (bitNumber==5) {
			updateRegister((address&0x7fff)>>13, shiftRegister);
			shiftRegister = 0;
			bitNumber = 0;
		}
	}
	
	private void updateRegister(int index, int value) {
		logger.debug("mapper register %d is now %02x", index, value);
		register[index] = value;		
		updateChunks();
	}

	private void updateChunks() {
		// cpu
		int romMap = (register[0]>>2)&3;
		switch (romMap){
		case 0:
		case 1:
			switchRomPage(0, 2*register[3]);
			switchRomPage(1, 2*register[3]+1);
			break;
		case 2:
			switchRomPage(0, 0);
			switchRomPage(1, register[3]);
			break;
		case 3:
			switchRomPage(0, register[3]);
			switchRomPage(1, nbRomBanks-1);
			break;
		}
		
		// ppu
		int nameTableType = register[0]&3;
		switch (nameTableType) {
		case 0:
		case 1:
			setNoMirroring();
			break;
		case 2:
			setVerticalMirroring();
			break;
		case 3:
			setHorizontalMirroring();
			break;
		}
		
		int vromType = (register[0]>>4)&1;
		if (vromType==1) {
			switchVRomPage(0, register[1]);
			switchVRomPage(1, register[2]);
		} else {
			switchVRomPage(0, 2*register[1]);
			switchVRomPage(1, 2*register[1]+1);
		}
	}

	private void switchRomPage(int area, int bank) {
		bank %= nbRomBanks;
		mapCPU(0x4000*(area+2),0x4000,romChunks[bank]);
	}

	private void switchVRomPage(int area, int bank) {
		bank &= nbVRomBanks-1;
		mapPPU(0x1000*area,0x1000,vromChunks[bank]);
	}
}
