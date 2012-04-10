package org.jnes.mapper;

import org.jnes.Log;
import org.jnes.NESSystem;

/**
 * CPU and PPU memories are mapped in chunks of 0x400 bytes (1kB).
 * access is via an array of arrays : the main array (32 entries) maps the high
 * order bits to chunks (array of 0x800 ints). Switching is implemented by
 * updating the array of chunks.
 */
public abstract class AbstractMapper implements Mapper {
	
	protected Log logger = new Log();
	
	protected interface Chunk {
		int read(int address);
		void write(int address, int value);
	}
	
	protected class RamChunk implements Chunk {
		int[] array;
		int mask;
		public RamChunk(int size) {
			array = new int[size];
			mask = array.length-1;
		}
		
		public int read(int address) {
			return array[address&mask];
		}
		
		public void write(int address, int value) {
			array[address&mask] = value;
		}
	}
	
	protected class RomChunk implements Chunk {
		int[] array;
		int mask;

		public RomChunk(int[] array) {
			this.array = array;
			mask = array.length-1;
		}
		
		public RomChunk(byte[] array, int offset, int length) {
			this.array = new int[length];
			for (int i=0; i<length; i++) {
				this.array[i] = array[offset+i]&0xff;
			}
			mask = length-1;
		}
		
		public RomChunk(byte[] array) {
			this(array,0,array.length);
		}

		public int read(int address) {
			return array[address&mask];
		}
		
		public void write(int address, int value) {
			// nop
		}
	}
	
	protected class IOChunk implements Chunk {
		public int read(int address) {
			return system.ioRead(address);
		}
		public void write(int address, int value) {
			system.ioWrite(address, value);
		}
	}
	
	protected class NoOpChunk implements Chunk {
		public int read(int address) {
			return 0;
		}
		public void write(int address, int value) {
			// nop
		}
	}
	
	protected NESSystem system;
	
	protected Chunk[] cpuChunks = new Chunk[0x40];

	//protected Chunk nameTable0 = new RamChunk(0x400);
	//protected Chunk nameTable1 = new RamChunk(0x400);
	protected Chunk[] ppuChunks = new Chunk[0x40];
	
	protected int mirroringSource;
	
	public AbstractMapper(int nbRamBanks)
	{		
		// cpu memory map
		mapCPU(0,0x10000,new NoOpChunk());
		// ppu memory map
		mapPPU(0, 0x4000,new NoOpChunk());
		
		setHorizontalMirroring();
	}
	
	@Override
	public void setMirroring(Mirroring mirroring) {
		switch (mirroring) {
		case Vertical: setVerticalMirroring(); break;
		case Horizontal: setHorizontalMirroring(); break;
		case FourScreen: setNoMirroring(); break;
		}		
	}
	
	protected void setNoMirroring() {
		mirroringSource = 0x000;
	}
	
	protected void setHorizontalMirroring() {
		mirroringSource = 0x800;
	}
	
	protected void setVerticalMirroring() {
		mirroringSource = 0x400;
	}

	public NESSystem getSystem() {
		return system;
	}

	public void setSystem(NESSystem system) {
		this.system = system;
	}

	public int cpuRead(int address) {
		return cpuChunks[address>>10].read(address);
	}
	
	public void cpuWrite(int address, int value) {
		cpuChunks[address>>10].write(address,value);
	}

	public int ppuRead(int address) {
		return (address<0x2000 ? ppuChunks[address>>10].read(address) : 0) | getVRamCE(address) | getVRamA10(address);
	}
	
	public int ppuWrite(int address, int value) {
		ppuChunks[address>>10].write(address,value); 
		return getVRamCE(address)|getVRamA10(address);
	}

	private int getVRamCE(int address) {
		return (address&0x2000)==0 ? Mapper.CIRAM_NCE : 0;
	}
	
	protected int getVRamA10(int address) {
		return (address&mirroringSource)!=0 ? Mapper.CIRAM_A10 : 0;
	}
	
	public boolean endLine(int lineNumber) {
		// nothing
		return false;
	}
	
	protected void mapCPU(int address, int length, Chunk chunk) {
		address >>= 10;
		length >>= 10;
		for (int i=0; i<length; i++) {
			cpuChunks[address+i] = chunk;
		}
	}
	
	protected void mapPPU(int address, int length, Chunk chunk) {
		address >>= 10;
		length >>= 10;
		for (int i=0; i<length; i++) {
			ppuChunks[address+i] = chunk;
		}
	}
}
