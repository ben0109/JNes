package org.jnes.component.impl;

import org.jnes.NESSystem;
import org.jnes.component.CPU;

public class Emu6502 implements CPU
{
    private NESSystem system;
    
    public void setSystem(NESSystem system) {
        this.system = system;
    }

    public void reset() {
		PC = memoryRead(0xfffc);
		PC |= (memoryRead(0xfffd) << 8);
		S = (S - 3) & 0xff;
		setI(true);
		setD(false); // not on NMOS 6502
		setFC(getC());
		setFNZ(getN(), getZ());
		clock += 7;
    }

    public void nmi() {
		push(PC >> 8);
		push(PC);
		setN(getFN());
		setZ(getFZ());
		setC(getFC());
		push(P);
		PC = memoryRead(0xfffa);
		PC |= memoryRead(0xfffb) << 8;
		clock += 7;
    }

    public void irq() {
		if (getI()) {
			//pendingIRQ++;
		} else {
			push(PC >> 8);
			push(PC);
			setN(getFN());
			setZ(getFZ());
			setC(getFC());
			setB(false);
			push(P);
			setI(true);
			PC = memoryRead(0xfffe);
			PC |= memoryRead(0xffff) << 8;
			clock += 7;
		}
    }
    
	protected int memoryRead(int address) {
		return system.cpuRead(address);
	}
	
	protected void memoryWrite(int address,int value) {
		system.cpuWrite(address, value);
	}

	/*
	 * Zero page read & write
	 */
	private final int zeroPageRead(int addr)
	{
		return memoryRead(addr);
	}
	private final void zeroPageWrite(int addr, int value)
	{
		memoryWrite(addr, value);
	}
	
	/**
	 * Userspace interrupts
	 */
	public final void assertReset() {
		exceptionRegister |= SIG_6502_RESET;
	}
	public final void assertNMI() {
		exceptionRegister |= SIG_6502_NMI;
	}
	public final void assertIRQ() {
		exceptionRegister |= SIG_6502_IRQ;
	}
	
	/**
	 * Userspace interrupt handlers
	 */
	protected void onReset() {
	};

	protected void onNMI() {
	};

	protected void onIRQ() {
	};

	/**
	 * CPU Registers
	 */
	public int A, X, Y, P, S, PC;

	/**
	 * CPU Clock
	 */
	protected long clock;

	/**
	 * CPU Flags
	 */
	public static final int FLAG_C = (1 << 0);
	public static final int FLAG_Z = (1 << 1);
	public static final int FLAG_I = (1 << 2);
	public static final int FLAG_D = (1 << 3);
	public static final int FLAG_B = (1 << 4);
	public static final int FLAG_V = (1 << 6);
	public static final int FLAG_N = (1 << 7);
	/*
	 *	owing to a bug in 6502, the bit 5 must be always 1;
	 *	otherwise, programs like DOS 3.3 will break down
	 *	see instructions in $9FF4-$9FF5 of DOS 3.3
	 */
	
	/**
	 * CPU Signals
     */
	private int exceptionRegister = 0;

 	public static final int SIG_6502_RESET = (1 << 0);
	public static final int SIG_6502_NMI = (1 << 1);
	public static final int SIG_6502_IRQ = (1 << 2);

	/**
	 * CPU IRQ State
	 */ 
	private int pendingIRQ;
	
	/**
	 * Emulator registers
	 */
	private int easp1, easp2;
	private int operandAddress;
	private int opcode;
	private int operand;
	private int result;
	private int NZFlags;
	
	/**
	 * ALU look up tables
	 */
	private int BCDTableAdd[];	// addition correction
	private int BCDTableSub[];	// subtraction correction

	/**
	 * Constructor
	 */
	public Emu6502() {
		// Init BCD tables
		BCDTableAdd = new int[512];
		BCDTableSub = new int[512];
			
		for (int i = 0; i < 512; i++) {
			BCDTableAdd[i] = ((i & 0x0f) <= 0x09) ? i : (i + 0x06);
			BCDTableAdd[i] += ((BCDTableAdd[i] & 0xf0) <= 0x90) ? 0 : 0x60;
			if (BCDTableAdd[i] > 0x1ff)
				BCDTableAdd[i] -= 0x100;
			  
			BCDTableSub[i] = ((i & 0x0f) <= 0x09) ? i : (i - 0x06);
			BCDTableSub[i] -= ((BCDTableSub[i] & 0xf0) <= 0x90) ? 0 : 0x60;
		}
	}
	
	/*
	 * Stack macros
	 */
	private final int pop() {
		S++;
		S &= 0xff;
		return memoryRead(S | 0x100);
	}
	private final void push(int value) {
		memoryWrite(S | 0x100, value);
		S--;
		S &= 0xff;
	}

	/*
	 * Macros for P flags
	 */
	private final void setN(boolean b) {if (b) P |= FLAG_N; else P &= ~FLAG_N;}
	private final void setV(boolean b) {if (b) P |= FLAG_V; else P &= ~FLAG_V;}
	private final void setB(boolean b) {if (b) P |= FLAG_B; else P &= ~FLAG_B;}
	private final void setD(boolean b) {if (b) P |= FLAG_D; else P &= ~FLAG_D;}
	private final void setI(boolean b) {if (b) P |= FLAG_I; else P &= ~FLAG_I;}
	private final void setZ(boolean b) {if (b) P |= FLAG_Z; else P &= ~FLAG_Z;}
	private final void setC(boolean b) {if (b) P |= FLAG_C; else P &= ~FLAG_C;}
	private final boolean getN() {return ((P & FLAG_N) != 0);}
	private final boolean getV() {return ((P & FLAG_V) != 0);}
//	private final boolean getB() {return ((P & FLAG_B) != 0);}
	private final boolean getD() {return ((P & FLAG_D) != 0);}
	private final boolean getI() {return ((P & FLAG_I) != 0);}
	private final boolean getZ() {return ((P & FLAG_Z) != 0);}
	private final boolean getC() {return ((P & FLAG_C) != 0);}

	/**
	 * Fast condition codes. Instead of using bits to encode condition codes,
	 * recent ALU results are cached to that the condition codes can be
	 * handled more easily by the emulator's native hardware.
	 */
	private final boolean getFN() {return ((NZFlags & 0x280) != 0);}
	private final boolean getFNotN() {return ((NZFlags & 0x280) == 0);}
	private final boolean getFZ() {return ((NZFlags & 0xff) == 0);}
	private final boolean getFNotZ() {return ((NZFlags & 0xff) != 0);}
	private final void setFNZ(boolean n, boolean z) {NZFlags = ((n) ? 0x200 : 0x00) | ((z) ? 0x00 : 0x01);}
	private final boolean getFC() {return (result >> 8) != 0;}
	private final boolean getFNotC() {return (result >> 8) == 0;}
	private final int getFC_() {return result >> 8;}
	private final void setFC(boolean c) {result = (c ? 0x100 : 0x00);}

	/*
	 * Macro for page crossing cycle regulation - TODO: Why not used!? CPU probably not cycle accurate.
	 */
//	private final void checkCrossPage(int addr, int offset) {
//		if ((((addr + offset) ^ addr) & 0xff00) != 0) clock++;
//	}
	
	/*
	 * Macros for effective address calculation 
	 * (Macros whose names end with NC do not check for page crossing)
	 */
	private final int eaimm() {
		easp1 = memoryRead(PC);	PC++; return easp1;
	}
	private final int eazp() {
		easp1 = memoryRead(PC); PC++; return easp1;
	}
	private final int eazpx() {
		easp1 = (memoryRead(PC) + X) & 0xff; PC++; return easp1;
	}
	private final int eazpy() {
		easp1 = (memoryRead(PC) + Y) & 0xff; PC++; return easp1;
	}
	private final int eaabs() {
		easp1 = memoryRead(PC);	PC++;
		easp1 += (memoryRead(PC) << 8); PC++;
		return easp1;
	}
	private final int earel() {
//		easp1 = memoryRead(PC); PC++;
//		return ((easp1 & 0x80) != 0) ? easp1 - 256 : easp1;
		easp1 = (byte) memoryRead(PC); PC++; return easp1;
	}
	private final int eaabsx() {
		// No cross page check...
		// easp1 = eaabs();
		// checkCrossPage(easp1, X);
		// return easp1 + X;
		return eaabs() + X;
	}
	private final int eaabsxNC() {
		return eaabs() + X;
	}
	private final int eaabsy() {
		// No cross page check...
		// easp1 = eaabs();
		// checkCrossPage(easp1, Y);
		// return easp1 + Y;
		return eaabs() + Y;
	}
//	private final int eaabsyNC() {
//		return eaabs() + Y;
//	}

	/*
	 * Indirect addressing
	 */
	private final int eaabsind() {
		easp1 = eaabs();
		easp2 = memoryRead(easp1);
		return easp2 + (memoryRead(easp1 + 1) << 8);
	}
	private final int eazpxind() {
		easp1 = eazpx();
		easp2 = zeroPageRead(easp1);
		return easp2 + (zeroPageRead((easp1 + 1) & 0xff) << 8);
	}
	private final int eazpindy() {
		easp1 = eaimm();
		easp2 = zeroPageRead(easp1);
		// No cross page check...
		// easp2 += (zeroPageRead((easp1 + 1) & 0xff) << 8);
		// checkCrossPage(easp2,Y);
		// return easp2 + Y;
		return easp2 + (zeroPageRead((easp1 + 1) & 0xff) << 8) + Y;
	}
//	private final int eazpindyNC() {
//		easp1 = eaimm();
//		easp2 = zeroPageRead(easp1);
//		return easp2 + (zeroPageRead((easp1 + 1) & 0xff) << 8) + Y;
//	}

	/*
	 * New 65C02 addressing mode
	 */
	private final int eazpind() {
		easp1 = eazp();
		easp2 = zeroPageRead(easp1);
		return easp2 + (zeroPageRead((easp1 + 1) & 0xff) << 8);
	}
	private final int eaabsxind() {
		easp1 = eaabs();
		easp2 = memoryRead(easp1);
		return easp2 + (memoryRead(easp1 + 1) << 8) + X;
	}

	/*
	 * Misc. macros
	 */
	private final void adcBCDAdjust() {
		if (getD()) result = BCDTableAdd[result];
	}
	private final void sbcBCDAdjust() {
		if (getD()) result = BCDTableSub[result];
	}
	private final void branch(int operand) {
		// No cross page check...
 		// checkCrossPage(PC, operand);
		PC += operand;
		clock++;
	}

	/** This executes a single instruction. */
	private final void executeInstruction() {
		opcode = memoryRead(PC);
		PC++;
		
		switch(opcode) {
		case 0x69:	// ADC #imm
			operand = eaimm();
			result = operand + A + getFC_();
			setV(!(((operand ^ A) & 0x80) != 0) && (((A ^ result) & 0x80) != 0));
			adcBCDAdjust();
			A = result & 0xff;
			NZFlags = A;
			clock += 2;
			break;
			
		case 0x6D:	// ADC abs
			operand = memoryRead(eaabs());
			result = operand + A + getFC_();
			setV(!(((operand ^ A) & 0x80) != 0) && (((A ^ result) & 0x80) != 0));
			adcBCDAdjust();
			A = result & 0xff;
			NZFlags = A;
			clock += 4;
			break;
			
		case 0x65:	// ADC zp
			operand = zeroPageRead(eazp());
			result = operand + A + getFC_();
			setV(!(((operand ^ A) & 0x80) != 0) && (((A ^ result) & 0x80) != 0));
			adcBCDAdjust();
			A = result & 0xff;
			NZFlags = A;
			clock += 3;
			break;
			
		case 0x61:	// ADC (zp,X)
			operand = memoryRead(eazpxind());
			result = operand + A + getFC_();
			setV(!(((operand ^ A) & 0x80) != 0) && (((A ^ result) & 0x80) != 0));
			adcBCDAdjust();
			A = result & 0xff;
			NZFlags = A;
			clock += 6;
			break;
			
		case 0x71:	// ADC (zp),Y
			operandAddress = eazpindy();
			operand = memoryRead(operandAddress);
			result = operand + A + getFC_();
			setV(!(((operand ^ A) & 0x80) != 0) && (((A ^ result) & 0x80) != 0));
			adcBCDAdjust();
			A = result & 0xff;
			NZFlags = A;
			clock += 5;
			break;
			
		case 0x75:	// ADC zp,X
			operandAddress = eazpx();
			operand = zeroPageRead(operandAddress);
			result = operand + A + getFC_();
			setV(!(((operand ^ A) & 0x80) != 0) && (((A ^ result) & 0x80) != 0));
			adcBCDAdjust();
			A = result & 0xff;
			NZFlags = A;
			clock += 4;
			break;
			
		case 0x7D:	// ADC abs,X
			operand = memoryRead(eaabsx());
			result = operand + A + getFC_();
			setV(!(((operand ^ A) & 0x80) != 0) && (((A ^ result) & 0x80) != 0));
			adcBCDAdjust();
			A = result & 0xff;
			NZFlags = A;
			clock += 4;
			break;
			
		case 0x79:	// ADC abs,Y
			operand = memoryRead(eaabsy());
			result = operand + A + getFC_();
			setV(!(((operand ^ A) & 0x80) != 0) && (((A ^ result) & 0x80) != 0));
			adcBCDAdjust();
			A = result & 0xff;
			NZFlags = A;
			clock += 4;
			break;
			
		case 0x29:	// AND #imm
			A &= eaimm();
			NZFlags = A;
			clock += 2;
			break;
			
		case 0x2D:	// AND abs
			A &= memoryRead(eaabs());
			NZFlags = A;
			clock += 4;
			break;
			
		case 0x25:	// AND zp
			A &= zeroPageRead(eazp());
			NZFlags = A;
			clock += 3;
			break;
			
		case 0x21:	// AND (zp,X)
			A &= memoryRead(eazpxind());
			NZFlags = A;
			clock += 6;
			break;
			
		case 0x31:	// AND (zp),Y
			A &= memoryRead(eazpindy());
			NZFlags = A;
			clock += 5;
			break;
			
		case 0x35:	// AND zp,X
			A &= zeroPageRead(eazpx());
			NZFlags = A;
			clock += 4;
			break;
			
		case 0x3D:	// AND abs,X
			A &= memoryRead(eaabsx());
			NZFlags = A;
			clock += 4;
			break;
			
		case 0x39:	// AND abs,Y
			A &= memoryRead(eaabsy());
			NZFlags = A;
			clock += 4;
			break;
			
		case 0x0E:	// ASL abs
			operandAddress = eaabs();
			operand = memoryRead(operandAddress);
			result = operand << 1;
			NZFlags = result;
			memoryWrite(operandAddress, result);
			clock += 6;
			break;
			
		case 0x06:	// ASL zp
			operandAddress = eazp();
			operand = zeroPageRead(operandAddress);
			result = operand << 1;
			NZFlags = result;
			zeroPageWrite(operandAddress, result);
			clock += 5;
			break;
			
		case 0x0A:	// ASL acc
			result = A << 1;
			A = result & 0xff;
			NZFlags = A;
			clock += 2;
			break;
			
		case 0x16:	// ASL zp,X
			operandAddress = eazpx();
			operand = zeroPageRead(operandAddress);
			result = operand << 1;
			NZFlags = result;
			zeroPageWrite(operandAddress, result);
			clock += 6;
			break;
			
		case 0x1E:	// ASL abs,X
			operandAddress = eaabsx();
			operand = memoryRead(operandAddress);
			result = operand << 1;
			NZFlags = result;
			memoryWrite(operandAddress, result);
			clock += 7;
			break;
			
		case 0x90:	// BCC rr
			operand = earel();
			clock += 2;
			if (getFNotC()) 
				branch(operand);
			break;
			
		case 0xB0:	// BCS rr
			operand = earel();
			clock += 2;
			if (getFC())
				branch(operand);
			break;
			
		case 0xF0:	// BEQ rr
			operand = earel();
			clock += 2;
			if (getFZ()) 
				branch(operand);
			break;
			
		case 0x2C:	// BIT abs
			operand = memoryRead(eaabs());
			setV((operand & 0x40) != 0); 
			NZFlags = ((operand & 0x80) << 2) | (A & operand);
			clock += 4;
			break;
			
		case 0x24:	// BIT zp
			operand = zeroPageRead(eazp());
			setV((operand & 0x40) != 0); 
			NZFlags = ((operand & 0x80) << 2) | (A & operand);
			clock += 3;
			break;
			
		case 0x30:	// BMI rr
			operand = earel();
			clock += 2;
			if (getFN()) 
				branch(operand);
				break;
			
		case 0xD0:	// BNE rr
			operand = earel();
			clock += 2;
			if (getFNotZ()) 
				branch(operand);
			break;
			
		case 0x10:	// BPL rr
			operand = earel();
			clock += 2;
			if (getFNotN()) 
				branch(operand);
			break;
			
		case 0x00:	// BRK
			push(PC >> 8);	// save PCH, PCL & P
			push(PC);
			setN(getFN());
			setZ(getFZ());
			setC(getFC());
			setB(true);
			push(P);
			setI(true);
			PC = memoryRead(0xfffe);
			PC |= memoryRead(0xffff) << 8;
			clock += 7;
			break;
			
		case 0x50:	// BVC rr
			operand = earel();
			clock += 2;
			if (!getV()) 
				branch(operand);
				break;
			
		case 0x70:	// BVS rr
			operand = earel();
			clock += 2;
			if (getV()) 
				branch(operand);
			break;
			
		case 0x18:	// CLC rr
			setFC(false);
			clock += 2;
			break;
			
		case 0xD8:	// CLD
			setD(false);
			clock += 2;
			break;
			
		case 0x58:	// CLI
			setI(false);
			clock += 2;
			if (pendingIRQ > 0) {
				pendingIRQ--;
				assertIRQ();
			}
			break;
			
		case 0xB8:	// CLV
			setV(false);
			clock += 2;
			break;
			
		case 0xC9:	// CMP #imm
			result = 0x100 + A - eaimm();
			NZFlags = result;
			clock += 2;
			break;
			
		case 0xCD:	// CMP abs
			result = 0x100 + A - memoryRead(eaabs());
			NZFlags = result;
			clock += 4;
			break;
			
		case 0xC5:	// CMP zp
			result = 0x100 + A - zeroPageRead(eazp());
			NZFlags = result;
			clock += 3;
			break;
			
		case 0xC1:	// CMP (zp,X)
			result = 0x100 + A - memoryRead(eazpxind());
			NZFlags = result;
			clock += 6;
			break;
			
		case 0xD1:	// CMP (zp),Y
			result = 0x100 + A - memoryRead(eazpindy());
			NZFlags = result;
			clock += 5;
			break;
			
		case 0xD5:	// CMP zp,X
			result = 0x100 + A - zeroPageRead(eazpx());
			NZFlags = result;
			clock += 4;
			break;
			
		case 0xDD:	// CMP abs,X
			result = 0x100 + A - memoryRead(eaabsx());
			NZFlags = result;
			clock += 4;
			break;
			
		case 0xD9:	// CMP abs,Y
			result = 0x100 + A - memoryRead(eaabsy());
			NZFlags = result;
			clock += 4;
			break;
			
		case 0xE0:	// CPX #imm
			result = 0x100 + X - eaimm();
			NZFlags = result;
			clock += 2;
			break;
			
		case 0xEC:	// CPX abs
			result = 0x100 + X - memoryRead(eaabs());
			NZFlags = result;
			clock += 4;
			break;
			
		case 0xE4:	// CPX zp
			result = 0x100 + X - zeroPageRead(eazp());
			NZFlags = result;
			clock += 3;
			break;
			
		case 0xC0:	// CPY #imm
			result = 0x100 + Y - eaimm();
			NZFlags = result;
			clock += 2;
			break;
			
		case 0xCC:	// CPY abs
			result = 0x100 + Y - memoryRead(eaabs());
			NZFlags = result;
			clock += 4;
			break;
			
		case 0xC4:	// CPY zp
			result = 0x100+ Y - zeroPageRead(eazp());
			NZFlags = result;
			clock += 3;
			break;
			
		case 0xCE:	// DEC abs
			operandAddress = eaabs();
			operand = memoryRead(operandAddress);
			NZFlags = operand + 0xff;
			memoryWrite(operandAddress, NZFlags);
			clock += 6;
			break;
			
		case 0xC6:	// DEC zp
			operandAddress = eazp();
			operand = zeroPageRead(operandAddress);
			NZFlags = operand + 0xff;
			zeroPageWrite(operandAddress, NZFlags);
			clock += 5;
			break;
			
		case 0xD6:	// DEC zp,X
			operandAddress = eazpx();
			operand = zeroPageRead(operandAddress);
			NZFlags = operand + 0xff;
			zeroPageWrite(operandAddress, NZFlags);
			clock += 6;
			break;
			
		case 0xDE:	// DEC abs,X
			operandAddress = eaabsx();
			operand = memoryRead(operandAddress);
			NZFlags = operand + 0xff;
			memoryWrite(operandAddress, NZFlags);
			clock += 7;
			break;
			
		case 0xCA:	// DEX
			NZFlags = X + 0xff;
			X = NZFlags & 0xff;
			clock += 2;
			break;
			
		case 0x88:	// DEY
			NZFlags = Y + 0xff;
			Y = NZFlags & 0xff;
			clock += 2;
			break;
			
		case 0x49:	// EOR #imm
			A ^= eaimm();
			NZFlags = A;
			clock += 2;
			break;
			
		case 0x4D:	// EOR abs
			A ^= memoryRead(eaabs());
			NZFlags = A;
			clock += 4;
			break;
			
		case 0x45:	// EOR zp
			A ^= zeroPageRead(eazp());
			NZFlags = A;
			clock += 3;
			break;
			
		case 0x41:	// EOR (zp,X)
			A ^= memoryRead(eazpxind());
			NZFlags = A;
			clock += 6;
			break;
			
		case 0x51:	// EOR (zp),Y
			A ^= memoryRead(eazpindy());
			NZFlags = A;
			clock += 5;
			break;
			
		case 0x55:	// EOR zp,X
			A ^= zeroPageRead(eazpx());
			NZFlags = A;
			clock += 4;
			break;
			
		case 0x5D:	// EOR abs,X
			A ^= memoryRead(eaabsx());
			NZFlags = A;
			clock += 4;
			break;
			
		case 0x59:	// EOR abs,Y
			A ^= memoryRead(eaabsy());
			NZFlags = A;
			clock += 4;
			break;
			
		case 0xEE:	// INC abs
			operandAddress = eaabs();
			operand = memoryRead(operandAddress);
			NZFlags = operand + 1;
			memoryWrite(operandAddress, NZFlags);
			clock += 6;
			break;
			
		case 0xE6:	// INC zp
			operandAddress = eazp();
			operand = zeroPageRead(operandAddress);
			NZFlags = operand + 1;
			zeroPageWrite(operandAddress, NZFlags);
			clock += 5;
			break;
			
		case 0xF6:	// INC zp,X
			operandAddress = eazpx();
			operand = zeroPageRead(operandAddress);
			NZFlags = operand + 1;
			zeroPageWrite(operandAddress, NZFlags);
			clock += 6;
			break;
			
		case 0xFE:	// INC abs,X
			operandAddress = eaabsxNC();
			operand = memoryRead(operandAddress);
			NZFlags = operand + 1;
			memoryWrite(operandAddress, NZFlags);
			clock += 7;
			break;
			
		case 0xE8:	// INX
			NZFlags = X + 1;
			X = NZFlags & 0xff;
			clock += 2;
			break;
			
		case 0xC8:	// INY
			NZFlags = Y + 1;
			Y = NZFlags & 0xff;
			clock += 2;
			break;
			
		case 0x4C:	// JMP abs
			PC = eaabs();
			clock += 3;
			break;
			
		case 0x6C:	// JMP (abs)
			PC = eaabsind();
			clock += 5;
			break;
			
		case 0x20:	// JSR abs
			operandAddress = eaabs();
			PC--;
			push(PC >> 8);
			push(PC);
			PC = operandAddress;
			clock += 6;
			break;
			
		case 0xA9:	// LDA #imm
			A = eaimm();
			NZFlags = A;
			clock += 2;
			break;
			
		case 0xAD:	// LDA abs
			A = memoryRead(eaabs());
			NZFlags = A;
			clock += 4;
			break;
			
		case 0xA5:	// LDA zp
			A = zeroPageRead(eazp());
			NZFlags = A;
			clock += 3;
			break;
			
		case 0xA1:	// LDA (zp,X)
			A = memoryRead(eazpxind());
			NZFlags = A;
			clock += 6;
			break;
			
		case 0xB1:	// LDA (zp),Y
			A = memoryRead(eazpindy());
			NZFlags = A;
			clock += 5;
			break;
			
		case 0xB5:	// LDA zp,X
			A = zeroPageRead(eazpx());
			NZFlags = A;
			clock += 4;
			break;
			
		case 0xBD:	// LDA abs,X
			A = memoryRead(eaabsx());
			NZFlags = A;
			clock += 4;
			break;
			
		case 0xB9:	// LDA abs,Y
			A = memoryRead(eaabsy());
			NZFlags = A;
			clock += 4;
			break;
			
		case 0xA2:	// LDX #imm
			X = eaimm();
			NZFlags = X;
			clock += 2;
			break;
			
		case 0xAE:	// LDX abs
			X = memoryRead(eaabs());
			NZFlags = X;
			clock += 4;
			break;
			
		case 0xA6:	// LDX zp
			X = zeroPageRead(eazp());
			NZFlags = X;
			clock += 3;
			break;
			
		case 0xBE:	// LDX abs,Y
			X = memoryRead(eaabsy());
			NZFlags = X;
			clock += 4;
			break;
			
		case 0xB6:	// LDX zp,Y
			X = zeroPageRead(eazpy());
			NZFlags = X;
			clock += 4;
			break;
			
		case 0xA0:	// LDY #imm
			Y = eaimm();
			NZFlags = Y;
			clock += 2;
			break;
			
		case 0xAC:	// LDY abs
			Y = memoryRead(eaabs());
			NZFlags = Y;
			clock += 4;
			break;
			
		case 0xA4:	// LDY zp
			Y = zeroPageRead(eazp());
			NZFlags = Y;
			clock += 3;
			break;
			
		case 0xB4:	// LDY zp,X
			Y = zeroPageRead(eazpx());
			NZFlags = Y;
			clock += 4;
			break;
			
		case 0xBC:	// LDY abs,X
			Y = memoryRead(eaabsx());
			NZFlags = Y;
			clock += 4;
			break;
			
		case 0x4E:	// LSR abs
			operandAddress = eaabs();
			operand = memoryRead(operandAddress);
			result = (operand & 0x01) << 8;	// just get the C bit
			NZFlags = operand >> 1;		// result in NZFlags
			memoryWrite(operandAddress, NZFlags);
			clock += 6;
			break;
			
		case 0x46:	// LSR zp
			operandAddress = eazp();
			operand = zeroPageRead(operandAddress);
			result = (operand & 0x01) << 8;	// just get the C bit
			NZFlags = operand >> 1;		// result in NZFlags
			zeroPageWrite(operandAddress, NZFlags);
			clock += 5;
			break;
			
		case 0x4A:	// LSR acc
			result = (A & 0x01) << 8;	// just get the C bit
			A >>= 1;
			NZFlags = A;
			clock += 2;
			break;
			
		case 0x56:	// LSR zp,X
			operandAddress = eazpx();
			operand = zeroPageRead(operandAddress);
			result = (operand & 0x01) << 8;	// just get the C bit
			NZFlags = operand >> 1;		// result in NZFlags
			zeroPageWrite(operandAddress, NZFlags);
			clock += 6;
			break;
			
		case 0x5E:	// LSR abs,X
			operandAddress = eaabsx();
			operand = memoryRead(operandAddress);
			result = (operand & 0x01) << 8;	// just get the C bit
			NZFlags = operand >> 1;		// result in NZFlags
			memoryWrite(operandAddress, NZFlags);
			clock += 7;
			break;
			
		case 0xEA:	// NOP
			clock += 2;
			break;
			
		case 0x09:	// ORA #imm
			A |= eaimm();
			NZFlags = A;
			clock += 2;
			break;
			
		case 0x0D:	// ORA abs
			A |= memoryRead(eaabs());
			NZFlags = A;
			clock += 4;
			break;
			
		case 0x05:	// ORA zp
			A |= zeroPageRead(eazp());
			NZFlags = A;
			clock += 3;
			break;
			
		case 0x01:	// ORA (zp,X)
			A |= memoryRead(eazpxind());
			NZFlags = A;
			clock += 6;
			break;
			
		case 0x11:	// ORA (zp),Y
			A |= memoryRead(eazpindy());
			NZFlags = A;
			clock += 5;
			break;
			
		case 0x15:	// ORA zp,X
			A |= zeroPageRead(eazpx());
			NZFlags = A;
			clock += 4;
			break;
			
		case 0x1D:	// ORA abs,X
			A |= memoryRead(eaabsx());
			NZFlags = A;
			clock += 4;
			break;
			
		case 0x19:	// ORA abs,Y
			A |= memoryRead(eaabsy());
			NZFlags = A;
			clock += 4;
			break;
			
		case 0x48:	// PHA
			push(A);
			clock += 3;
			break;
			
		case 0x08:	// PHP
			setN(getFN());
			setZ(getFZ());
			setC(getFC());
			push(P);
			clock += 3;
			break;
			
		case 0x68:	// PLA
			A = pop();
			NZFlags = A;
			clock += 4;
			break;
			
		case 0x28:	// PLP
			P = pop() | 0x20; // fix bug in bit5 of P
			setFC(getC());
			setFNZ(getN(), getZ());
			clock += 4;
			if ((pendingIRQ > 0) && !getI()) {
				pendingIRQ--;
				assertIRQ();
			}
			break;
			
		case 0x2E:	// ROL abs
			operandAddress = eaabs();
			operand = memoryRead(operandAddress);
			result = (operand << 1) | getFC_();
			NZFlags = result;
			memoryWrite(operandAddress, result);
			clock += 6;
			break;
			
		case 0x26:	// ROL zp
			operandAddress = eazp();
			operand = zeroPageRead(operandAddress);
			result = (operand << 1) | getFC_();
			NZFlags = result;
			zeroPageWrite(operandAddress, result);
			clock += 5;
			break;
			
		case 0x2A:	// ROL acc
			result = (A << 1) | getFC_();
			A = result & 0xff;
			NZFlags = A;
			clock += 2;
			break;
			
		case 0x36:	// ROL zp,X
			operandAddress = eazpx();
			operand = zeroPageRead(operandAddress);
			result = (operand << 1) | getFC_();
			NZFlags = result;
			zeroPageWrite(operandAddress, result);
			clock += 6;
			break;
			
		case 0x3E:	// ROL abs,X
			operandAddress = eaabsx();
			operand = memoryRead(operandAddress);
			result = (operand << 1) | getFC_();
			NZFlags = result;
			memoryWrite(operandAddress, result);
			clock += 7;
			break;
			
		case 0x6E:	// ROR abs
			operandAddress = eaabs();
			operand = memoryRead(operandAddress);
			result = ((operand & 0x01) << 8) | (getFC_() << 7) |
				(operand >> 1);
			NZFlags = result;
			memoryWrite(operandAddress, result);
			clock += 6;
			break;
			
		case 0x66:	// ROR zp
			operandAddress = eazp();
			operand = zeroPageRead(operandAddress);
			result = ((operand & 0x01) << 8) | (getFC_() << 7) |
				(operand >> 1);
			NZFlags = result;
			zeroPageWrite(operandAddress, result);
			clock += 5;
			break;
			
		case 0x6A:	// ROR acc
			result = ((A & 0x01) << 8) | (getFC_() << 7) | (A >> 1);
			A = result & 0xff;
			NZFlags = A;
			clock += 2;
			break;
			
		case 0x76:	// ROR zp,X
			operandAddress = eazpx();
			operand = zeroPageRead(operandAddress);
			result = ((operand & 0x01) << 8) | (getFC_() << 7) |
				(operand >> 1);
			NZFlags = result;
			zeroPageWrite(operandAddress, result);
			clock += 6;
			break;
			
		case 0x7E:	// ROR abs,X
			operandAddress = eaabsx();
			operand = memoryRead(operandAddress);
			result = ((operand & 0x01) << 8) | (getFC_() << 7) |
				(operand >> 1);
			NZFlags = result;
			memoryWrite(operandAddress, result);
			clock += 7;
			break;
			
		case 0x40:	// RTI
			P = pop() | 0x20; // bit 5 bug of 6502
			setFC(getC());
			setFNZ(getN(), getZ());
			PC = pop();	// splitting is necessary
			PC += pop() << 8;	// because of nested macros
			clock += 6;
			break;
			
		case 0x60:	// RTS
			PC = pop();	// splitting is necessary
			PC += pop() << 8;	// because of nested macros
			PC++;
			clock += 6;
			break;
			
		case 0xE9:	// SBC #imm
			operand = 255 - eaimm();
			result = operand + A + getFC_();
			setV(!(((operand ^ A) & 0x80) != 0) && (((A ^ result) & 0x80) != 0));
			sbcBCDAdjust();
			A = result & 0xff;
			NZFlags = A;
			clock += 2;
			break;
			
		case 0xED:	// SBC abs
			operand = 255 - memoryRead(eaabs());
			result = operand + A + getFC_();
			setV(!(((operand ^ A) & 0x80) != 0) && (((A ^ result) & 0x80) != 0));
			sbcBCDAdjust();
			A = result & 0xff;
			NZFlags = A;
			clock += 4;
			break;
			
		case 0xE5:	// SBC zp
			operand = 255 - zeroPageRead(eazp());
			result = operand + A + getFC_();
			setV(!(((operand ^ A) & 0x80) != 0) && (((A ^ result) & 0x80) != 0));
			sbcBCDAdjust();
			A = result & 0xff;
			NZFlags = A;
			clock += 3;
			break;
			
		case 0xE1:	// SBC (zp,X)
			operand = 255 - memoryRead(eazpxind());
			result = operand + A + getFC_();
			setV(!(((operand ^ A) & 0x80) != 0) && (((A ^ result) & 0x80) != 0));
			sbcBCDAdjust();
			A = result & 0xff;
			NZFlags = A;
			clock += 6;
			break;
			
		case 0xF1:	// SBC (zp),Y
			operand = 255 - memoryRead(eazpindy());
			result = operand + A + getFC_();
			setV(!(((operand ^ A) & 0x80) != 0) && (((A ^ result) & 0x80) != 0));
			sbcBCDAdjust();
			A = result & 0xff;
			NZFlags = A;
			clock += 5;
			break;
			
		case 0xF5:	// SBC zp,X
			operand = 255 - zeroPageRead(eazpx());
			result = operand + A + getFC_();
			setV(!(((operand ^ A) & 0x80) != 0) && (((A ^ result) & 0x80) != 0));
			sbcBCDAdjust();
			A = result & 0xff;
			NZFlags = A;
			clock += 4;
			break;
			
		case 0xFD:	// SBC abs,X
			operand = 255 - memoryRead(eaabsx());
			result = operand + A + getFC_();
			setV(!(((operand ^ A) & 0x80) != 0) && (((A ^ result) & 0x80) != 0));
			sbcBCDAdjust();
			A = result & 0xff;
			NZFlags = A;
			clock += 4;
			break;
			
		case 0xF9:	// SBC abs,Y
			operand = 255 - memoryRead(eaabsy());
			result = operand + A + getFC_();
			setV(!(((operand ^ A) & 0x80) != 0) && (((A ^ result) & 0x80) != 0));
			sbcBCDAdjust();
			A = result & 0xff;
			NZFlags = A;
			clock += 4;
			break;
			
		case 0x38:	// SEC
			setFC(true);
			clock += 2;
			break;
			
		case 0xF8:	// SED
			setD(true);
			clock += 2;
			break;
			
		case 0x78:	// SEI
			setI(true);
			clock += 2;
			break;
			
		case 0x8D:	// STA abs
			memoryWrite(eaabs(), A);
			clock += 4;
			break;
			
		case 0x85:	// STA zp
			zeroPageWrite(eazp(), A);
			clock += 3;
			break;
			
		case 0x81:	// STA (zp,X)
			memoryWrite(eazpxind(), A);
			clock += 6;
			break;
			
		case 0x91:	// STA (zp),Y
			memoryWrite(eazpindy(), A);
			clock += 6;
			break;
			
		case 0x95:	// STA zp,X
			zeroPageWrite(eazpx(), A);
			clock += 4;
			break;
			
		case 0x9D:	// STA abs,X
			memoryWrite(eaabsx(), A);
			clock += 5;
			break;
			
		case 0x99:	// STA abs,Y
			memoryWrite(eaabsy(), A);
			clock += 5;
			break;
			
		case 0x8E:	// STX abs
			memoryWrite(eaabs(), X);
			clock += 4;
			break;
			
		case 0x86:	// STX zp
			zeroPageWrite(eazp(), X);
			clock += 3;
			break;
			
		case 0x96:	// STX zp,Y
			zeroPageWrite(eazpy(), X);
			clock += 4;
			break;
			
		case 0x8C:	// STY abs
			memoryWrite(eaabs(), Y);
			clock += 4;
			break;
			
		case 0x84:	// STY zp
			zeroPageWrite(eazp(), Y);
			clock += 3;
			break;
			
		case 0x94:	// STY zp,X
			zeroPageWrite(eazpx(), Y);
			clock += 4;
			break;
			
		case 0xAA:	// TAX
			X = A;
			NZFlags = X;
			clock += 2;
			break;
			
		case 0xA8:	// TAY
			Y = A;
			NZFlags = Y;
			clock += 2;
			break;
			
		case 0xBA:	// TSX
			X = S;
			NZFlags = X;
			clock += 2;
			break;
			
		case 0x8A:	// TXA
			A = X;
			NZFlags = A;
			clock += 2;
			break;
			
		case 0x9A:	// TXS
			S = X;
			clock += 2;
			break;
			
		case 0x98:	// TYA
			A = Y;
			NZFlags = A;
			clock += 2;
			break;
			
		/*
		 * 65C02 instructions
		 * note: timing is not correct
		 */	   

		case 0x72:	// ADC (zp)
			operand = memoryRead(eazpind());
			result = operand + A + getFC_();
			setV(!(((operand ^ A) & 0x80) != 0) && (((A ^ result) & 0x80) != 0));
			adcBCDAdjust();
			A = result & 0xff;
			NZFlags = A;
			clock += 5;
			break;
			
		case 0x32:	// AND (zp)
			A &= memoryRead(eazpind());
			NZFlags = A;
			clock += 5;
			break;
			
		case 0x34:	// BIT zp,X
			operand = zeroPageRead(eazpx());
			setV((operand & 0x40) != 0); 
			NZFlags = ((operand & 0x80) << 2) | (A & operand);
			clock += 3;
			break;
			
		case 0x89:	// BIT #imm
			operand = eaimm();
			setV((operand & 0x40) != 0); 
			NZFlags = ((operand & 0x80) << 2) | (A & operand);
			clock += 2;
			break;
			
		case 0x3C:	// BIT abs,X
			operand = eaabsx();
			setV((operand & 0x40) != 0); 
			NZFlags = ((operand & 0x80) << 2) | (A & operand);
			clock += 4;
			break;
			
		case 0x80:	// BRA rr
			operand = earel();
			clock += 2;
			branch(operand);
			break;
			
		case 0xD2:	// CMP (zp)
			result = 0x100 + A - memoryRead(eazpind());
			NZFlags = result;
			clock += 5;
			break;
			
		case 0x3A:	// DEA acc
			NZFlags = A + 0xff;
			A = NZFlags & 0xff;
			clock += 2;
			break;
			
		case 0x52:	// EOR (zp)
			A ^= memoryRead(eazpind());
			NZFlags = A;
			clock += 5;
			break;
			
		case 0x1A:	// INA acc
			NZFlags = A + 1;
			A = NZFlags & 0xff;
			clock += 2;
			break;
			
		case 0x7C:	// JMP (abs,X)
			PC = eaabsxind();
			clock += 6;
			break;
			
		case 0xB2:	// LDA (zp)
			A = memoryRead(eazpind());
			NZFlags = A;
			clock += 5;
			break;
			
		case 0x12:	// ORA (zp)
			A |= memoryRead(eazpind());
			NZFlags = A;
			clock += 5;
			break;
			
		case 0xDA:	// PHX
			push(X);
			clock += 3;
			break;
			
		case 0xFA:	// PLX
			X = pop();
			NZFlags = X;
			clock += 4;
			break;
			
		case 0x5A:	// PHY
			push(Y);
			clock += 3;
			break;
			
		case 0x7A:	// PLY
			Y = pop();
			NZFlags = Y;
			clock += 4;
			break;
			
		case 0xF2:	// SBC (zp)
			operand = 255 - memoryRead(eazpind());
			result = operand + A + getFC_();
			setV(!(((operand ^ A) & 0x80) != 0) && (((A ^ result) & 0x80) != 0));
			sbcBCDAdjust();
			A = result & 0xff;
			NZFlags = A;
			clock += 5;
			break;
			
		case 0x92:	// STA (zp)
			memoryWrite(eazpind(), A);
			clock += 6;
			break;
			
		case 0x9C:	// STZ abs
			memoryWrite(eaabs(), 0);
			clock += 4;
			break;
			
		case 0x64:	// STZ zp
			zeroPageWrite(eazp(), 0);
			clock += 3;
			break;
			
		case 0x74:	// STZ zp,X
			zeroPageWrite(eazpx(), 0);
			clock += 3;
			break;
			
		case 0x9E:	// STZ abs,X
			memoryWrite(eaabsx(), 0);
			clock += 4;
			break;
			
		case 0x1C:	// TRB abs
			operandAddress = eaabs();
			operand = memoryRead(operandAddress);
			setV((operand & 0x40) != 0); 
			NZFlags = ((operand & 0x80) << 2) | (A & operand);
			memoryWrite(operandAddress, (operand & ~A) & 0xff);
			clock += 5;
			break;
			
		case 0x14:	// TRB zp
			operandAddress = eazp();
			operand = zeroPageRead(operandAddress);
			setV((operand & 0x40) != 0); 
			NZFlags = ((operand & 0x80) << 2) | (A & operand);
			zeroPageWrite(operandAddress, (operand & ~A) & 0xff);
			clock += 5;
			break;
			
		case 0x0C:	// TSB abs
			operandAddress = eaabs();
			operand = memoryRead(operandAddress);
			setV((operand & 0x40) != 0); 
			NZFlags = ((operand & 0x80) << 2) | (A & operand);
			memoryWrite(operandAddress, operand | A);
			clock += 5;
			break;
			
		case 0x04:	// TSB zp
			operandAddress = eazp();
			operand = zeroPageRead(operandAddress);
			setV((operand & 0x40) != 0); 
			NZFlags = ((operand & 0x80) << 2) | (A & operand);
			zeroPageWrite(operandAddress, operand | A);
			clock += 5;
			break;

		default:	// unknown instructions
			clock += 2;
		}
		
//		if (PC == 0xB30)
//			throw (new RuntimeException()); // TODO: for breakpoint hack - disable
	}
	
	public int run(int cycles)
	{		
		// Initialize
		long clockEnd = clock + cycles*3;

		while (clock<clockEnd) {
			PC &= 0xffff;
//			System.out.printf("%04x\n", PC);
			executeInstruction();
		}
		
		return (int)(clock-clockEnd)/3;
	}
	
	public final long executeInstructions(int num) {
		// Initialize
		long clockStart = clock;

		for (; num >= 16; num -= 16) {
			PC &= 0xffff;			// Keep PC "sort of" bounded
			executeInstruction(); executeInstruction();
			executeInstruction(); executeInstruction();
			executeInstruction(); executeInstruction();
			executeInstruction(); executeInstruction();
			executeInstruction(); executeInstruction();
			executeInstruction(); executeInstruction();
			executeInstruction(); executeInstruction();
		}
		PC &= 0xffff;
		for (; num > 0; num--)
			executeInstruction();

		return clock - clockStart;
	}

	public final void checkInterrupts() {
		// Reset
		if ((exceptionRegister & SIG_6502_RESET) != 0) {
			onReset();
			
			PC = memoryRead(0xfffc);
			PC |= (memoryRead(0xfffd) << 8);
			S = (S - 3) & 0xff;
			setI(true);
			setD(false); // not on NMOS 6502
			setFC(getC());
			setFNZ(getN(), getZ());
			clock += 7;
			exceptionRegister &= ~SIG_6502_RESET;
		}
		
		// No NMI nor IRQ...
		if ((exceptionRegister & SIG_6502_NMI) != 0) {
			onNMI();

			push(PC >> 8);
			push(PC);
			setN(getFN());
			setZ(getFZ());
			setC(getFC());
			push(P);
			PC = memoryRead(0xfffa);
			PC |= memoryRead(0xfffb) << 8;
			clock += 7;
			exceptionRegister ^= SIG_6502_NMI;
		}
		
		if ((exceptionRegister & SIG_6502_IRQ) != 0) {
			onIRQ();
		
			if (getI())
				pendingIRQ++;
			else {
				push(PC >> 8);
				push(PC);
				setN(getFN());
				setZ(getFZ());
				setC(getFC());
				setB(false);
				push(P);
				setI(true);
				PC = memoryRead(0xfffe);
				PC |= memoryRead(0xffff) << 8;
				clock += 7;
			}
			exceptionRegister ^= SIG_6502_IRQ;
		}
	}
}
