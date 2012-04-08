package org.jnes.mapper;

import java.util.HashSet;
import java.util.Set;

import org.jnes.Log;
import org.jnes.NESSystem;
import org.jnes.component.CPU;
import org.jnes.component.PPU;

public class DebugMapper implements Mapper {
	
	private Log logger = new Log();
	private Mapper mapper;
	private Set<Integer> tracedAddresses = new HashSet<Integer>();

	public DebugMapper(Mapper mapper) {
		super();
		this.mapper = mapper;
		//tracedAddresses.add(0x2007);
		//tracedAddresses.add(0xfc);
		//tracedAddresses.add(0xc000);
		//tracedAddresses.add(0xc001);
		/*
		tracedAddresses.add(0x723);
		tracedAddresses.add(0x725);
		tracedAddresses.add(0x726);
		tracedAddresses.add(0x727);
		tracedAddresses.add(0x728);
		tracedAddresses.add(0x729);
		*/
	}

	public int cpuRead(int address) {
		int r = mapper.cpuRead(address);
		if (tracedAddresses.contains(address)) {
			logger.info("cpuRead(%04x)=%02x",address,r);
		}
		return r;
	}

	public void cpuWrite(int address, int value) {
		if ((value&~0xff) != 0) {
			value &= 0xff;
		}
		if (tracedAddresses.contains(address)) {
			logger.info("cpuWrite(%04x,%02x)",address,value);
		}
		mapper.cpuWrite(address, value);
	}

	public int ppuRead(int address) {
//		logger.debug("ppuRead(%04x)",address);
		return mapper.ppuRead(address);
	}

	public int ppuWrite(int address, int value) {
		if ((value&~0xff) != 0) {
			value &= 0xff;
		}
//		logger.debug("ppuWrite(%04x,%02x)",address,value);
		return mapper.ppuWrite(address, value&0xff);
	}

	public void setCPU(CPU cpu) {
		mapper.setCPU(cpu);
	}

	public void setPPU(PPU ppu) {
		mapper.setPPU(ppu);
	}

	public void endLine(int lineNumber) {
		mapper.endLine(lineNumber);
	}

	public void setSystem(NESSystem system) {
		mapper.setSystem(system);
	}
}
