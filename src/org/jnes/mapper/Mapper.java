package org.jnes.mapper;

import org.jnes.NESSystem;

public interface Mapper {

	public static enum Mirroring {
		Horizontal, Vertical, FourScreen
	};

	public static final int CIRAM_A10 = 0x100;
	public static final int CIRAM_NCE = 0x200;

	void setSystem(NESSystem system);

	void setMirroring(Mirroring mirroring);

	void cpuWrite(int address, int value);

	int cpuRead(int address);

	int ppuWrite(int address, int value);

	int ppuRead(int address);

	boolean endLine(int lineNumber);
}
