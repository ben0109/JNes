package org.jnes.mapper.impl;

import org.jnes.mapper.AbstractMapper;

public class UNROM extends AbstractMapper {
	
	protected class PRGRomChunk extends RomChunk {
		
		public PRGRomChunk(byte[] array) {
			super(array);
		}

		@Override
		public void write(int address, int value) {
			registerWrite(value);
		}
	}
	
	protected Chunk[] romBanks;
	
	public UNROM(byte[][] romBanks, int nbRamBanks, byte[][] vromBanks) {
		super(nbRamBanks);

		this.romBanks = new Chunk[romBanks.length];
		for (int i=0; i<romBanks.length; i++) {
			this.romBanks[i] = new PRGRomChunk(romBanks[i]);
		}
		
		// two 16kb rom banks
		mapCPU(0x8000,0x4000,this.romBanks[0]);
		mapCPU(0xc000,0x4000,this.romBanks[romBanks.length-1]);
		
		// 8kb ram @ 0x0000
		mapPPU(0x0000, 0x2000, new RamChunk(0x2000));
		
		setVerticalMirroring();
	}
	
	private void registerWrite(int value) {
		//System.out.println("switch to "+value);
		int bank = value & (romBanks.length-1);
		mapCPU(0x8000,0x4000,this.romBanks[bank]);
	}
}
