package org.jnes.mapper.impl;

import org.jnes.mapper.AbstractMapper;

public class MMC3 extends AbstractMapper {
	
	protected class PRGRomChunk extends RomChunk {
		
		public PRGRomChunk(byte[] array,int offset,int length) {
			super(array,offset,length);
		}

		@Override
		public void write(int address, int value) {
			registerWrite(address, value);
		}
	}

	protected int command;
	protected int chrAddress;
	protected int prgAddress;
	protected int[] chrBankNumber = new int[6];
	protected int[] prgBankNumber = new int[2];

	protected boolean disableIrq;
	
	protected boolean counterLatched;
	protected int irqCounter;
	
	protected boolean irqLatched;
	protected int irqLatch;

	protected int nbRomBanks;
	protected Chunk[] romChunks;

	protected int nbRamBanks;
	protected Chunk[] ramChunks;
	
	protected int nbVRomBanks;
	protected Chunk[] vromChunks;
	
	public MMC3(byte[][] romBanks, int nbRamBanks, byte[][] vromBanks) {
		super(nbRamBanks);

		this.nbRomBanks = 2*romBanks.length;
		this.romChunks = new Chunk[nbRomBanks];
		for (int i=0; i<nbRomBanks; i++) {
			this.romChunks[i] = new PRGRomChunk(romBanks[i/2],(i%2)*0x2000,0x2000);
		}

		this.nbVRomBanks = 8*vromBanks.length;
		this.vromChunks = new Chunk[8*nbVRomBanks];
		for (int i=0; i<nbVRomBanks; i++) {
			this.vromChunks[i] = new RomChunk(vromBanks[i/8],(i%8)*0x400,0x400);
		}

		this.nbRamBanks = nbRamBanks;
		this.ramChunks = new RamChunk[nbRamBanks];
		for (int i=0; i<nbRamBanks; i++) {
			ramChunks[i] = new RamChunk(0x1000);
		}
		
		// sram at 6000-7fff
		Chunk sram = new RamChunk(0x1000);
		for (int i=0x18; i<0x20; i++) {
			cpuChunks[i] = sram;
		}
		
		// vrom
		for (int i=0; i<4; i++) {
			ppuChunks[i] = vromChunks[i];
		}
		
		// rom
		mapCPU(0x8000,0x2000,romChunks[0]);
		mapCPU(0xa000,0x2000,romChunks[1]);
		mapCPU(0xc000,0x2000,romChunks[nbRomBanks-2]);
		mapCPU(0xe000,0x2000,romChunks[nbRomBanks-1]);
	}
	
	private void registerWrite(int address, int value) {
		
		logger.debug("mapper registerWrite %04x <- %02x", address, value);
		switch (address & 0xe001) {
		case 0x8000:
			chrAddress = ((value>>7)&1)*4;
			prgAddress = ((value>>6)&1)*0x4000;
			command = value&7;
			break;
			
		case 0x8001:
			value &= nbVRomBanks-1;
            switch (command) {
            // VROM swapping commands
            case 0:
            	chrBankNumber[0] = value;
            	break;
            case 1:
            	chrBankNumber[1] = value;
            	break;
            case 2:
            case 3:
            case 4:
            case 5:
            	chrBankNumber[command] = value;
            	break;
 
            // ROM swapping commands
            case 6:
                // 1 8k page at $8000 or $C000
            	prgBankNumber[0] = value;
                break;
            case 7:
                // 1 8k page at $A000
            	prgBankNumber[1] = value;
                break;
            }
			break;
			
/*	
		case 0xa000:
            // Mirroring
            if (! (global_rom.control_byte_1 & ROM_CTRL_FOUR_SCREEN))
            {
                ppu_set_mirroring (((value & 1) ?
                    MIRRORING_HORIZONTAL : MIRRORING_VERTICAL));
            }
			break;

		case 0xa001:
			break;
*/
		case 0xc000:
            irqLatch = value;
            if (!counterLatched) {
            	irqCounter = irqLatch;
            }
			break;
			
		case 0xc001:
            counterLatched = false;
            irqCounter = irqLatch;
			break;
			
		case 0xe000:
            disableIrq = true;
            if (!counterLatched) {
            	irqCounter = irqLatch;
            }
			break;
			
		case 0xe001:
            disableIrq = false;
            if (!counterLatched) {
            	irqCounter = irqLatch;
            }
			break;

		default:
			break;
		}
		mapPPU((0^chrAddress)*0x400,0x400,vromChunks[chrBankNumber[0] & ~1]);
		mapPPU((1^chrAddress)*0x400,0x400,vromChunks[chrBankNumber[0] |  1]);
		mapPPU((2^chrAddress)*0x400,0x400,vromChunks[chrBankNumber[1] & ~1]);
		mapPPU((3^chrAddress)*0x400,0x400,vromChunks[chrBankNumber[1] |  1]);
    	for (int i=2; i<6; i++) {
    		mapPPU(((i+2)^chrAddress)*0x400,0x400,vromChunks[chrBankNumber[i]]);
    	}

    	mapCPU(0x8000^prgAddress,0x2000,romChunks[prgBankNumber[0]&(nbRomBanks-1)]);
    	mapCPU(0xa000,           0x2000,romChunks[prgBankNumber[1]&(nbRomBanks-1)]);
    	mapCPU(0xc000^prgAddress,0x2000,romChunks[nbRomBanks-2]);
	}

	@Override
	public void endLine(int lineNumber) {
		if (lineNumber<240) {
			irqCounter--;
			if (irqCounter<0) {
				irqCounter = irqLatch;
				irqLatched = false;
				if (!disableIrq) {
					cpu.irq();
				}
				counterLatched = false;
			}
		}
	}
}
