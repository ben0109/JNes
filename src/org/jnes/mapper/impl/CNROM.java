package org.jnes.mapper.impl;

import org.jnes.mapper.AbstractMapper;

public class CNROM extends AbstractMapper {
	
	protected class PRGRomChunk extends RomChunk {
		
		public PRGRomChunk(byte[] array) {
			super(array);
		}

		@Override
		public void write(int address, int value) {
			registerWrite(value);
		}
	}
	
	private Chunk[] vromBanks;
	
	public CNROM(byte[][] romBanks, int nbRamBanks, byte[][] vromBanks) {
		super(nbRamBanks);

		Chunk chunk;
		
		// two 16kb rom banks
		chunk = new PRGRomChunk(romBanks[0]);
		mapCPU(0x8000,0x4000,chunk);

		chunk = new PRGRomChunk(romBanks[1]);
		mapCPU(0xC000,0x4000,chunk);

		// vrom banks
		this.vromBanks = new Chunk[vromBanks.length];
		for (int i=0; i<vromBanks.length; i++) {
			this.vromBanks[i] = new RomChunk(vromBanks[i]);
		}

		mapPPU(0x0000,0x2000,this.vromBanks[0]);
		setHorizontalMirroring();
	}
	
	private void registerWrite(int value) {
		int vromBank = value & 3;
		mapPPU(0x0000,0x2000,this.vromBanks[vromBank]);
	}
}
