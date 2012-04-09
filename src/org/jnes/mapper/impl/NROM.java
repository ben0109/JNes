package org.jnes.mapper.impl;

import org.jnes.mapper.AbstractMapper;

public class NROM extends AbstractMapper {
	
	public NROM(byte[][] romBanks, int nbRamBanks, byte[][] vromBanks) {
		super(nbRamBanks);

		// two 16kb rom banks
		mapCPU(0x8000,0x4000,new RomChunk(romBanks[0]));
		mapCPU(0xc000,0x4000,new RomChunk(romBanks[1]));

		// one 8kb vrom bank
		mapPPU(0x0000,0x2000,new RomChunk(vromBanks[0]));
		
		setVerticalMirroring();
	}
}
