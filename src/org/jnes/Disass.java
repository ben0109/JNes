package org.jnes;

import java.io.FileInputStream;
import java.io.InputStream;

import org.jnes.Disassembler.DecodedInstruction;
import org.jnes.mapper.Mapper;
import org.jnes.mapper.NESFileLoader;

public class Disass {

	private static int start = 0x8100;
	private static int end   = 0x8300;

	public static void main(String[] args) throws Exception {
		
		InputStream is = new FileInputStream(RomName.ROM);
		Mapper m = new NESFileLoader().load(is);
		is.close();

		m.cpuWrite(0x8000, 6);

		System.out.printf("NMI : %04x\n",readShort(m,0xFFFA));
		System.out.printf("RST : %04x\n",readShort(m,0xFFFC));
		System.out.printf("IRQ : %04x\n",readShort(m,0xFFFE));

		Disassembler d = new Disassembler(m);
		d.setOffset(start);
		System.out.println();
		System.out.println();
		System.out.println();

		while ((d.getOffset()&0xffff)<end) {
			DecodedInstruction instr = d.decode();

			System.out.printf("%04x | ",instr.getPc());
			for (int i=0; i<3; i++) {
				if (i<instr.getBytes().size()) {
					System.out.printf("%02x ",instr.getBytes().get(i));
				} else {
					System.out.print("   ");
				}
			}
			System.out.print("| ");
			System.out.println(instr.getInstruction());
		}
	}
	
	static int readShort(Mapper m, int address) {
		return m.cpuRead(address) | (m.cpuRead(address+1)<<8);
	}
}
