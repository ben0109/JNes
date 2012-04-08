package org.jnes;

import java.util.ArrayList;
import java.util.List;

import org.jnes.mapper.Mapper;
public class Disassembler {

	private String[] table;
	
	private int offset;
	
	private Mapper system;
	
	public Disassembler(Mapper system) {
		this.system = system;
		
		table = new String[0x100];

		table[0x00] = "BRK";
		table[0x20] = "JSR %s";
		table[0x40] = "RTI";
		table[0x60] = "RTS";
		table[0xa0] = "LDY #%b";
		table[0xc0] = "CPY #%b";
		table[0xe0] = "CPX #%b";
		
		table[0x10] = "BPL %r";
		table[0x30] = "BMI %r";
		table[0x50] = "BVC %r";
		table[0x70] = "BVS %r";
		table[0x90] = "BCC %r";
		table[0xb0] = "BCS %r";
		table[0xd0] = "BNE %r";
		table[0xf0] = "BEQ %r";
		
		table[0x01] = "ORA (%b,X)";
		table[0x21] = "AND (%b,X)";
		table[0x41] = "EOR (%b,X)";
		table[0x61] = "ADC (%b,X)";
		table[0x81] = "STA (%b,X)";
		table[0xa1] = "LDA (%b,X)";
		table[0xc1] = "CMP (%b,X)";
		table[0xe1] = "SBC (%b,X)";
		table[0x11] = "ORA (%b),Y";
		table[0x31] = "AND (%b),Y";
		table[0x51] = "EOR (%b),Y";
		table[0x71] = "ADC (%b),Y";
		table[0x91] = "STA (%b),Y";
		table[0xb1] = "LDA (%b),Y";
		table[0xd1] = "CMP (%b),Y";
		table[0xf1] = "SBC (%b),Y";
		
		table[0xa2] = "LDX #%b";

		table[0x24] = "BIT %b";
		table[0x84] = "STY %b";
		table[0x94] = "STY %b,X";
		table[0xa4] = "LDY %b";
		table[0xb4] = "LDY %b,X";
		table[0xc4] = "CPY %b";
		table[0xe4] = "CPY %b,X";
		
		table[0x05] = "ORA %b";
		table[0x15] = "ORA %b,X";
		table[0x25] = "AND %b";
		table[0x35] = "AND %b,X";
		table[0x45] = "EOR %b";
		table[0x55] = "EOR %b,X";
		table[0x65] = "ADC %b";
		table[0x75] = "ADC %b,X";
		table[0x85] = "STA %b";
		table[0x95] = "STA %b,X";
		table[0xa5] = "LDA %b";
		table[0xb5] = "LDA %b,X";
		table[0xc5] = "CMP %b";
		table[0xd5] = "CMP %b,X";
		table[0xe5] = "SBC %b";
		table[0xf5] = "SBC %b,X";
		
		table[0x06] = "ASL %b";
		table[0x16] = "ASL %b,X";
		table[0x26] = "ROL %b";
		table[0x36] = "ROL %b,X";
		table[0x46] = "LSR %b";
		table[0x56] = "LSR %b,X";
		table[0x66] = "ROR %b";
		table[0x76] = "ROR %b,X";
		table[0x86] = "STX %b";
		table[0x96] = "STX %b,Y";
		table[0xa6] = "LDX %b";
		table[0xb6] = "LDX %b,Y";
		table[0xc6] = "DEC %b";
		table[0xd6] = "DEC %b,X";
		table[0xe6] = "INC %b";
		table[0xf6] = "INC %b,X";
		
		table[0x08] = "PHP";
		table[0x18] = "CLC";
		table[0x28] = "PLP";
		table[0x38] = "SEC";
		table[0x48] = "PHA";
		table[0x58] = "CLI";
		table[0x68] = "PLA";
		table[0x78] = "SEI";
		table[0x88] = "DEY";
		table[0x98] = "TYA";
		table[0xa8] = "TAY";
		table[0xb8] = "CLV";
		table[0xc8] = "INY";
		table[0xd8] = "CLD";
		table[0xe8] = "INX";
		table[0xf8] = "SED";

		table[0x09] = "ORA #%b";
		table[0x19] = "ORA %s,Y";
		table[0x29] = "AND #%b";
		table[0x39] = "AND %s,Y";
		table[0x49] = "EOR #%b";
		table[0x59] = "EOR %s,Y";
		table[0x69] = "ADC #%b";
		table[0x79] = "ADC %s,Y";
		table[0x99] = "STA %s,Y";
		table[0xa9] = "LDA #%b";
		table[0xb9] = "LDA %s,Y";
		table[0xc9] = "CMP #%b";
		table[0xd9] = "CMP %s,Y";
		table[0xe9] = "SBC #%b";
		table[0xf9] = "SBC %s,Y";
		
		table[0x0a] = "ASL A";
		table[0x2a] = "ROL A";
		table[0x4a] = "LSR A";
		table[0x6a] = "ROR A";
		table[0x8a] = "TXA";
		table[0x9a] = "TXS";
		table[0xaa] = "TAX";
		table[0xba] = "TSX";
		table[0xca] = "DEX";
		table[0xea] = "NOP";

		table[0x2c] = "BIT %s";
		table[0x4c] = "JMP %s";
		table[0x6c] = "JMP (%s)";
		table[0x8c] = "STY %s";
		table[0xac] = "LDY %s";
		table[0xbc] = "LDY %s,X";
		table[0xcc] = "CPY %s";
		table[0xec] = "CPY %s";
		
		table[0x0d] = "ORA %s";
		table[0x1d] = "ORA %s,X";
		table[0x2d] = "AND %s";
		table[0x3d] = "AND %s,X";
		table[0x4d] = "EOR %s";
		table[0x5d] = "EOR %s,X";
		table[0x6d] = "ADC %s";
		table[0x7d] = "ADC %s,X";
		table[0x8d] = "STA %s";
		table[0x9d] = "STA %s,X";
		table[0xad] = "LDA %s";
		table[0xbd] = "LDA %s,X";
		table[0xcd] = "CMP %s";
		table[0xdd] = "CMP %s,X";
		table[0xed] = "SBC %s";
		table[0xfd] = "SBC %s,X";
		
		table[0x0e] = "ASL %s";
		table[0x1e] = "ASL %s,X";
		table[0x2e] = "ROL %s";
		table[0x3e] = "ROL %s,X";
		table[0x4e] = "LSR %s";
		table[0x5e] = "LSR %s,X";
		table[0x6e] = "ROR %s";
		table[0x7e] = "ROR %s,X";
		table[0x8e] = "STX %s";
		table[0xae] = "LDX %s";
		table[0xbe] = "LDX %s,X";
		table[0xce] = "DEC %s";
		table[0xde] = "DEC %s,X";
		table[0xee] = "INC %s";
		table[0xfe] = "INC %s,X";
	}
	
	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public DecodedInstruction decode() {
		DecodedInstruction r = new DecodedInstruction(offset);
		int b = r.nextByte();
		String template = table[b];
		if (template==null) {
			r.instruction = "???";
		} else {
			int l,h;
			StringBuilder instr = new StringBuilder();
			for (int i=0; i<template.length(); ) {
				char c = template.charAt(i++);
				if (c!='%') {
					instr.append(c);
				} else {
					switch (template.charAt(i++)) {
					case 'b':
						l = r.nextByte();
						appendHex(instr,2,l);
						break;
					case 's':
						l = r.nextByte();
						h = r.nextByte();
						appendHex(instr,2,h);
						appendHex(instr,2,l);
						break;
					case 'r':
						l = r.nextByte();
						if (l>127) {
							l |= 0xffffff00;
						}
						l += offset;
						appendHex(instr,4,l);
						break;
					}
				}
			}
			r.instruction = instr.toString();
		}
		return r;
	}

	private void appendHex(StringBuilder sb, int n, int value) {
		for (int i=n-1; i>=0; i--) {
			int d = (value>>(4*i)) & 0xf;
			sb.append((char)((d<10) ? '0'+d : 'a'+(d-10)));
		}
	}

	
	public class DecodedInstruction {

		private String instruction;
		private int pc;
		private List<Integer> bytes = new ArrayList<Integer>();

		public DecodedInstruction(int offset) {
			super();
			this.pc = offset;
		}
		
		public String getInstruction() {
			return instruction;
		}
		public void setInstruction(String instr) {
			this.instruction = instr;
		}
		public int getPc() {
			return pc;
		}
		public void setPc(int offset) {
			this.pc = offset;
		}
		public List<Integer> getBytes() {
			return bytes;
		}
		public boolean addByte(int o) {
			return bytes.add(o);
		}

		private int nextByte() {
			int b = system.cpuRead(offset++);
			bytes.add(b);
			return b;
		}
	}
}
