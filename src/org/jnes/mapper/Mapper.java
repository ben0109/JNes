package org.jnes.mapper;

import org.jnes.NESSystem;
import org.jnes.component.CPU;
import org.jnes.component.PPU;

public interface Mapper {
	
	public static final int CIRAM_A10 = 0x100;
	public static final int CIRAM_NCE = 0x200;
	
	void endLine(int lineNumber);
	
	void setCPU(CPU ppu);
	
	void setPPU(PPU ppu);
	
	void setSystem(NESSystem system);

	void cpuWrite(int address,int value);
	
	int cpuRead(int address);
	
	int ppuWrite(int address,int value);
	
	int ppuRead(int address);
}
