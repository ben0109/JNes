package org.jnes;

import org.jnes.mapper.Mapper;

public interface NESSystem {
	
	void setMapper(Mapper mapper);

	int cpuRead(int address);
	
	void cpuWrite(int address, int value);	

	int ppuRead(int address);
	
	void ppuWrite(int address, int value);
	
	void nmi();

	void irq();
	
	int ioRead(int address);
	
	void ioWrite(int address, int value);
}
