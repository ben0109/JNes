package org.jnes;

import org.jnes.component.CPU;
import org.jnes.component.Controller;
import org.jnes.component.PPU;
import org.jnes.mapper.Mapper;

public class NESSystemImpl implements NESSystem {
	
	private Log logger = new Log();
	
	private Mapper mapper;
	
	public CPU cpu;
	
	private PPU ppu;
	
	private Controller controller;
	
	private byte ram[] = new byte[0x800];
	
	private byte vram[] = new byte[0x800];
	
	public NESSystemImpl(CPU cpu, PPU ppu, Controller controller) {
		super();
		this.cpu = cpu;
		this.ppu = ppu;
		this.controller = controller;
		this.cpu.setSystem(this);
		this.ppu.setSystem(this);
		this.controller.setSystem(this);
	}


	public void setMapper(Mapper mapper) {
		this.mapper = mapper;
	}


	public int cpuRead(int address)
	{
		switch (address&0xe000) {
		case 0x0000: return ram[address&0x7ff]&0xff;
		case 0x2000: return ppu.readRegister(address);
		case 0x4000:
		case 0x6000: return ioRead(address);
		default: return mapper.cpuRead(address);
		}		
	}


	public void cpuWrite(int address, int value)
	{
		switch (address&0xe000) {
		case 0x0000: ram[address&0x7ff] = (byte)value; break;
		case 0x2000: ppu.writeRegister(address, value); break;
		case 0x4000:
		case 0x6000: ioWrite(address, value); break;
		default: mapper.cpuWrite(address, value); break;
		}
	}


	public int ppuRead(int address)
	{
		int r = mapper.ppuRead(address);
		if ((r&Mapper.CIRAM_NCE)==0) {
			address &= 0x3ff;
			address |= ((r&Mapper.CIRAM_A10)!=0) ? 0x400 : 0;
			return vram[address]&0xff;
		} else {
			return r &0xff;
		}
	}


	public void ppuWrite(int address, int value)
	{
		int r = mapper.ppuWrite(address, value);
		if ((r&Mapper.CIRAM_NCE)==0) {
			address &= 0x3ff;
			address |= ((r&Mapper.CIRAM_A10)!=0) ? 0x400 : 0;
			vram[address] = (byte)value;
		}
	}


	public void nmi()
	{
		logger.debug("nmi");
		cpu.nmi();
	}
	
	public void irq()
	{
		logger.debug("irq");
		cpu.irq();
	}
	
	public int ioRead(int address)
	{
		switch (address) {
		case 0x4016:
			return controller.read4016();
		case 0x4017:
			return controller.read4017();
		default:
			return 0;
		}
	}
	
	public void ioWrite(int address, int value)
	{
		switch (address) {
		case 0x2000:
		case 0x2001:
		case 0x2002:
		case 0x2003:
		case 0x2004:
		case 0x2005:
		case 0x2006:
		case 0x2007:
			ppu.writeRegister(address,value);
			break;
		case 0x4014:
			address = value*0x100;
			for (int i=0; i<0x100; i++) {
				ppu.writeRegister(0x2004, cpuRead(address++));
			}
			break;
		case 0x4016:
//			System.out.printf("%04x : %04x<-%02x\n",cpu.pc,address,value);
			controller.write4016(value);
			break;
		default:
			break;
		}
	}
}
