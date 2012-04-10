package org.jnes;

import org.jnes.component.CPU;
import org.jnes.component.PPU;
import org.jnes.mapper.Mapper;

public class Emulator {
	
	public static int SCREEN_LINES= 242;
	
	public static int TOTAL_LINES = 262;
	
	public static int CYCLES_HBLANK = 1773447/(50*TOTAL_LINES);

	private Mapper mapper;
	private CPU cpu;
	private PPU ppu;
	private long clock=0;
	
	public void runForOneLine() {
//		System.out.println("line");
		clock += CYCLES_HBLANK+cpu.run(3*CYCLES_HBLANK/3);
		if (mapper.endLine(ppu.getLineNumber())) {
			cpu.irq();
		}
		ppu.endLine();
	}
	
	public void runForOneScreen() {
		for (int i=0; i<TOTAL_LINES; i++) {
			runForOneLine();
		}
	}

	public Emulator(Mapper mapper, CPU cpu, PPU ppu) {
		super();
		this.mapper = mapper;
		this.cpu = cpu;
		this.ppu = ppu;
		cpu.reset();
	}
}
