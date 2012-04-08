package org.jnes.component.impl;

import org.jnes.NESSystem;
import org.jnes.component.CPU;

public class CPUImpl implements CPU {

    private static int[] CYCLES =
    {
      7,6,2,8,3,3,5,5,3,2,2,2,4,4,6,6,
      2,5,2,8,4,4,6,6,2,4,2,7,5,5,7,7,
      6,6,2,8,3,3,5,5,4,2,2,2,4,4,6,6,
      2,5,2,8,4,4,6,6,2,4,2,7,5,5,7,7,
      6,6,2,8,3,3,5,5,3,2,2,2,3,4,6,6,
      2,5,2,8,4,4,6,6,2,4,2,7,5,5,7,7,
      6,6,2,8,3,3,5,5,4,2,2,2,5,4,6,6,
      2,5,2,8,4,4,6,6,2,4,2,7,5,5,7,7,
      2,6,2,6,3,3,3,3,2,2,2,2,4,4,4,4,
      2,6,2,6,4,4,4,4,2,5,2,5,5,5,5,5,
      2,6,2,6,3,3,3,3,2,2,2,2,4,4,4,4,
      2,5,2,5,4,4,4,4,2,4,2,5,4,4,4,4,
      2,6,2,8,3,3,5,5,2,2,2,2,4,4,6,6,
      2,5,2,8,4,4,6,6,2,4,2,7,5,5,7,7,
      2,6,2,8,3,3,5,5,2,2,2,2,4,4,6,6,
      2,5,2,8,4,4,6,6,2,4,2,7,5,5,7,7
    };

    private static final int MASK_N  = 0x80;
    private static final int MASK_V  = 0x40;
    private static final int MASK_B  = 0x10;
    private static final int MASK_D  = 0x08;
    private static final int MASK_I  = 0x04;
    private static final int MASK_Z  = 0x02;
    private static final int MASK_C  = 0x01;

    private static int[] TABLE_NZ;
    static {
        TABLE_NZ = new int[0x100];
        for (int i=0; i<0x100; i++) {
            int n = (i&0x80) != 0 ? MASK_N : 0;
            int z = (i==0) ? MASK_Z : 0;
            TABLE_NZ[i] = n|z;
        }
    };

    private NESSystem system;
    public int a;
    public int x;
    public int y;
    public int pc;
    public int s;
    public int flagC;
    public int flagNZ;
    public int flagI;
    public int flagD;
    public int flagB;
    public int flagV;

    public void setSystem(NESSystem system) {
        this.system = system;
    }

    public void reset() {
        a = x = y = pc = s = 0;
        flagC = flagNZ = flagI = flagD = flagB = 0;
        pc = 0xFFFC;
        pc = readNextWord();
    }

    public void nmi() {
        int l=system.cpuRead(0xfffa);
        int h=system.cpuRead(0xfffb);
        pushWord(pc);
        pc=(h<<8)|l;
        php();
    }

    public void irq() {
        if (flagI!=0) {
            return;
        }
        pushWord(pc);
        php();
        int l=system.cpuRead(0xfffe);
        int h=system.cpuRead(0xffff);
        pc=(h<<8)|l;
        flagI = MASK_I;
        flagD = 0;
    }
    
    int[] pcs = new int[0x1000];
    int index = 0;

    public int run(int cycles) {
        while (cycles>0) {
//        	System.out.printf("%04x\n", pc);
            int opcode = readNextByte();
            pcs[(++index) & 0xfff] = pc;
        	//System.err.printf("%04x : %02x\n",pc,opcode);
            cycles -= CYCLES[opcode];
            switch (opcode) {
            case 0x00: { flagB=MASK_B; irq(); break; }
            case 0x01: { int ia=(readNextByte()+x)&0xFF; int address=system.cpuRead(ia)|(system.cpuRead(ia+1)<<8);int m=system.cpuRead(address); a|=m; flagNZ=TABLE_NZ[a]; break; }
            case 0x05: { int address=readNextByte();int m=system.cpuRead(address); a|=m; flagNZ=TABLE_NZ[a]; break; }
            case 0x06: { int address=readNextByte();int m=system.cpuRead(address); int r=(m<<1)&0xff; flagC=m>>7; flagNZ=TABLE_NZ[r]; system.cpuWrite(address,r); break; }
            case 0x08: { php(); break; }
            case 0x09: { int value=readNextByte();int m=value; a|=m; flagNZ=TABLE_NZ[a]; break; }
            case 0x0a: { int m=a; int r=(m<<1)&0xff; flagC=m>>7; flagNZ=TABLE_NZ[r]; a=r; break; }
            case 0x0d: { int address=readNextWord();int m=system.cpuRead(address); a|=m; flagNZ=TABLE_NZ[a]; break; }
            case 0x0e: { int address=readNextWord();int m=system.cpuRead(address); int r=(m<<1)&0xff; flagC=m>>7; flagNZ=TABLE_NZ[r]; system.cpuWrite(address,r); break; }
            case 0x10: { int offset=(byte)readNextByte(); int address=pc+offset;if ((flagNZ&MASK_N)==0) { pc = address; } break; }
            case 0x11: { int ia=readNextByte(); int address=((system.cpuRead(ia)|(system.cpuRead(ia+1)<<8))+y)&0xffff;int m=system.cpuRead(address); a|=m; flagNZ=TABLE_NZ[a]; break; }
            case 0x15: { int address=(readNextByte()+x)&0xff;int m=system.cpuRead(address); a|=m; flagNZ=TABLE_NZ[a]; break; }
            case 0x16: { int address=(readNextByte()+x)&0xff;int m=system.cpuRead(address); int r=(m<<1)&0xff; flagC=m>>7; flagNZ=TABLE_NZ[r]; system.cpuWrite(address,r); break; }
            case 0x18: { flagC=0; break; }
            case 0x19: { int address=(readNextWord()+y)&0xffff;int m=system.cpuRead(address); a|=m; flagNZ=TABLE_NZ[a]; break; }
            case 0x1d: { int address=(readNextWord()+x)&0xffff;int m=system.cpuRead(address); a|=m; flagNZ=TABLE_NZ[a]; break; }
            case 0x1e: { int address=(readNextWord()+x)&0xffff;int m=system.cpuRead(address); int r=(m<<1)&0xff; flagC=m>>7; flagNZ=TABLE_NZ[r]; system.cpuWrite(address,r); break; }
            case 0x20: { int address=readNextWord();pc--; pushWord(pc); pc=address; break; }
            case 0x21: { int ia=(readNextByte()+x)&0xFF; int address=system.cpuRead(ia)|(system.cpuRead(ia+1)<<8);int m=system.cpuRead(address); a&=m; flagNZ=TABLE_NZ[a]; break; }
            case 0x24: { int address=readNextByte();int m=system.cpuRead(address); int r=a&m; flagNZ=(m&0x80)|((r==0)?2:0); flagV=m&0x40; break; }
            case 0x25: { int address=readNextByte();int m=system.cpuRead(address); a&=m; flagNZ=TABLE_NZ[a]; break; }
            case 0x26: { int address=readNextByte();int m=system.cpuRead(address); int r=((m<<1)|flagC)&0xff; flagC=m>>7; flagNZ=TABLE_NZ[r]; system.cpuWrite(address,r); break; }
            case 0x28: { plp(); break; }
            case 0x29: { int value=readNextByte();int m=value; a&=m; flagNZ=TABLE_NZ[a]; break; }
            case 0x2a: { int m=a; int r=((m<<1)|flagC)&0xff; flagC=m>>7; flagNZ=TABLE_NZ[r]; a=r; break; }
            case 0x2c: { int address=readNextWord();int m=system.cpuRead(address); int r=a&m; flagNZ=(m&0x80)|((r==0)?2:0); flagV=m&0x40; break; }
            case 0x2d: { int address=readNextWord();int m=system.cpuRead(address); a&=m; flagNZ=TABLE_NZ[a]; break; }
            case 0x2e: { int address=readNextWord();int m=system.cpuRead(address); int r=((m<<1)|flagC)&0xff; flagC=m>>7; flagNZ=TABLE_NZ[r]; system.cpuWrite(address,r); break; }
            case 0x30: { int offset=(byte)readNextByte(); int address=pc+offset;if ((flagNZ&MASK_N)!=0) { pc = address; } break; }
            case 0x31: { int ia=readNextByte(); int address=((system.cpuRead(ia)|(system.cpuRead(ia+1)<<8))+y)&0xffff;int m=system.cpuRead(address); a&=m; flagNZ=TABLE_NZ[a]; break; }
            case 0x35: { int address=(readNextByte()+x)&0xff;int m=system.cpuRead(address); a&=m; flagNZ=TABLE_NZ[a]; break; }
            case 0x36: { int address=(readNextByte()+x)&0xff;int m=system.cpuRead(address); int r=((m<<1)|flagC)&0xff; flagC=m>>7; flagNZ=TABLE_NZ[r]; system.cpuWrite(address,r); break; }
            case 0x38: { flagC=MASK_C; break; }
            case 0x39: { int address=(readNextWord()+y)&0xffff;int m=system.cpuRead(address); a&=m; flagNZ=TABLE_NZ[a]; break; }
            case 0x3d: { int address=(readNextWord()+x)&0xffff;int m=system.cpuRead(address); a&=m; flagNZ=TABLE_NZ[a]; break; }
            case 0x3e: { int address=(readNextWord()+x)&0xffff;int m=system.cpuRead(address); int r=((m<<1)|flagC)&0xff; flagC=m>>7; flagNZ=TABLE_NZ[r]; system.cpuWrite(address,r); break; }
            case 0x40: { plp();pc=popWord(); break; }
            case 0x41: { int ia=(readNextByte()+x)&0xFF; int address=system.cpuRead(ia)|(system.cpuRead(ia+1)<<8);int m=system.cpuRead(address); a^=m; flagNZ=TABLE_NZ[a]; break; }
            case 0x45: { int address=readNextByte();int m=system.cpuRead(address); a^=m; flagNZ=TABLE_NZ[a]; break; }
            case 0x46: { int address=readNextByte();int m=system.cpuRead(address); int r=m>>1; flagC=m&1; flagNZ=TABLE_NZ[r]; system.cpuWrite(address,r); break; }
            case 0x48: { push(a); break; }
            case 0x49: { int value=readNextByte();int m=value; a^=m; flagNZ=TABLE_NZ[a]; break; }
            case 0x4a: { int m=a; int r=m>>1; flagC=m&1; flagNZ=TABLE_NZ[r]; a=r; break; }
            case 0x4c: { int address=readNextWord();pc=address; break; }
            case 0x4d: { int address=readNextWord();int m=system.cpuRead(address); a^=m; flagNZ=TABLE_NZ[a]; break; }
            case 0x4e: { int address=readNextWord();int m=system.cpuRead(address); int r=m>>1; flagC=m&1; flagNZ=TABLE_NZ[r]; system.cpuWrite(address,r); break; }
            case 0x50: { int offset=(byte)readNextByte(); int address=pc+offset;if (flagV==0) { pc = address; } break; }
            case 0x51: { int ia=readNextByte(); int address=((system.cpuRead(ia)|(system.cpuRead(ia+1)<<8))+y)&0xffff;int m=system.cpuRead(address); a^=m; flagNZ=TABLE_NZ[a]; break; }
            case 0x55: { int address=(readNextByte()+x)&0xff;int m=system.cpuRead(address); a^=m; flagNZ=TABLE_NZ[a]; break; }
            case 0x56: { int address=(readNextByte()+x)&0xff;int m=system.cpuRead(address); int r=m>>1; flagC=m&1; flagNZ=TABLE_NZ[r]; system.cpuWrite(address,r); break; }
            case 0x58: { flagI=0; break; }
            case 0x59: { int address=(readNextWord()+y)&0xffff;int m=system.cpuRead(address); a^=m; flagNZ=TABLE_NZ[a]; break; }
            case 0x5d: { int address=(readNextWord()+x)&0xffff;int m=system.cpuRead(address); a^=m; flagNZ=TABLE_NZ[a]; break; }
            case 0x5e: { int address=(readNextWord()+x)&0xffff;int m=system.cpuRead(address); int r=m>>1; flagC=m&1; flagNZ=TABLE_NZ[r]; system.cpuWrite(address,r); break; }
            case 0x60: { pc=(popWord()+1)&0xffff; break; }
            case 0x61: { int ia=(readNextByte()+x)&0xFF; int address=system.cpuRead(ia)|(system.cpuRead(ia+1)<<8);int m=system.cpuRead(address); int r=a+m+flagC; flagC=r>>8; r&=0xFF; flagNZ=TABLE_NZ[r]; flagV=(~(a^m)&(a^r)&0x80)>>1; a=r; break; }
            case 0x65: { int address=readNextByte();int m=system.cpuRead(address); int r=a+m+flagC; flagC=r>>8; r&=0xFF; flagNZ=TABLE_NZ[r]; flagV=(~(a^m)&(a^r)&0x80)>>1; a=r; break; }
            case 0x66: { int address=readNextByte();int m=system.cpuRead(address); int r=(m>>1)|(flagC<<7); flagC=m&1; flagNZ=TABLE_NZ[r]; system.cpuWrite(address,r); break; }
            case 0x68: { a=pop(); flagNZ=TABLE_NZ[a]; break; }
            case 0x69: { int value=readNextByte();int m=value; int r=a+m+flagC; flagC=r>>8; r&=0xFF; flagNZ=TABLE_NZ[r]; flagV=(~(a^m)&(a^r)&0x80)>>1; a=r; break; }
            case 0x6a: { int m=a; int r=(m>>1)|(flagC<<7); flagC=m&1; flagNZ=TABLE_NZ[r]; a=r; break; }
            case 0x6c: { int ia=readNextWord(); int address=system.cpuRead(ia)|(system.cpuRead(ia+1)<<8);pc=address; break; }
            case 0x6d: { int address=readNextWord();int m=system.cpuRead(address); int r=a+m+flagC; flagC=r>>8; r&=0xFF; flagNZ=TABLE_NZ[r]; flagV=(~(a^m)&(a^r)&0x80)>>1; a=r; break; }
            case 0x6e: { int address=readNextWord();int m=system.cpuRead(address); int r=(m>>1)|(flagC<<7); flagC=m&1; flagNZ=TABLE_NZ[r]; system.cpuWrite(address,r); break; }
            case 0x70: { int offset=(byte)readNextByte(); int address=pc+offset;if (flagV!=0) { pc = address; } break; }
            case 0x71: { int ia=readNextByte(); int address=((system.cpuRead(ia)|(system.cpuRead(ia+1)<<8))+y)&0xffff;int m=system.cpuRead(address); int r=a+m+flagC; flagC=r>>8; r&=0xFF; flagNZ=TABLE_NZ[r]; flagV=(~(a^m)&(a^r)&0x80)>>1; a=r; break; }
            case 0x75: { int address=(readNextByte()+x)&0xff;int m=system.cpuRead(address); int r=a+m+flagC; flagC=r>>8; r&=0xFF; flagNZ=TABLE_NZ[r]; flagV=(~(a^m)&(a^r)&0x80)>>1; a=r; break; }
            case 0x76: { int address=(readNextByte()+x)&0xff;int m=system.cpuRead(address); int r=(m>>1)|(flagC<<7); flagC=m&1; flagNZ=TABLE_NZ[r]; system.cpuWrite(address,r); break; }
            case 0x78: { flagI=MASK_I; break; }
            case 0x79: { int address=(readNextWord()+y)&0xffff;int m=system.cpuRead(address); int r=a+m+flagC; flagC=r>>8; r&=0xFF; flagNZ=TABLE_NZ[r]; flagV=(~(a^m)&(a^r)&0x80)>>1; a=r; break; }
            case 0x7d: { int address=(readNextWord()+x)&0xffff;int m=system.cpuRead(address); int r=a+m+flagC; flagC=r>>8; r&=0xFF; flagNZ=TABLE_NZ[r]; flagV=(~(a^m)&(a^r)&0x80)>>1; a=r; break; }
            case 0x7e: { int address=(readNextWord()+x)&0xffff;int m=system.cpuRead(address); int r=(m>>1)|(flagC<<7); flagC=m&1; flagNZ=TABLE_NZ[r]; system.cpuWrite(address,r); break; }
            case 0x81: { int ia=(readNextByte()+x)&0xFF; int address=system.cpuRead(ia)|(system.cpuRead(ia+1)<<8);system.cpuWrite(address,a); break; }
            case 0x84: { int address=readNextByte();system.cpuWrite(address,y); break; }
            case 0x85: { int address=readNextByte();system.cpuWrite(address,a); break; }
            case 0x86: { int address=readNextByte();system.cpuWrite(address,x); break; }
            case 0x88: { int m=y; int r=(m-1)&0xff; flagNZ=TABLE_NZ[r]; y=r; break; }
            case 0x8a: { a=x; flagNZ=TABLE_NZ[x]; break; }
            case 0x8c: { int address=readNextWord();system.cpuWrite(address,y); break; }
            case 0x8d: { int address=readNextWord();system.cpuWrite(address,a); break; }
            case 0x8e: { int address=readNextWord();system.cpuWrite(address,x); break; }
            case 0x90: { int offset=(byte)readNextByte(); int address=pc+offset;if (flagC==0) { pc = address; } break; }
            case 0x91: { int ia=readNextByte(); int address=((system.cpuRead(ia)|(system.cpuRead(ia+1)<<8))+y)&0xffff;system.cpuWrite(address,a); break; }
            case 0x94: { int address=(readNextByte()+x)&0xff;system.cpuWrite(address,y); break; }
            case 0x95: { int address=(readNextByte()+x)&0xff;system.cpuWrite(address,a); break; }
            case 0x96: { int address=(readNextByte()+y)&0xff;system.cpuWrite(address,x); break; }
            case 0x98: { a=y; flagNZ=TABLE_NZ[y]; break; }
            case 0x99: { int address=(readNextWord()+y)&0xffff;system.cpuWrite(address,a); break; }
            case 0x9a: { s=x; flagNZ=TABLE_NZ[x]; break; }
            case 0x9d: { int address=(readNextWord()+x)&0xffff;system.cpuWrite(address,a); break; }
            case 0xa0: { int value=readNextByte();int m=value;y=m; flagNZ=TABLE_NZ[m]; break; }
            case 0xa1: { int ia=(readNextByte()+x)&0xFF; int address=system.cpuRead(ia)|(system.cpuRead(ia+1)<<8);int m=system.cpuRead(address);a=m; flagNZ=TABLE_NZ[m]; break; }
            case 0xa2: { int value=readNextByte();int m=value;x=m; flagNZ=TABLE_NZ[m]; break; }
            case 0xa4: { int address=readNextByte();int m=system.cpuRead(address);y=m; flagNZ=TABLE_NZ[m]; break; }
            case 0xa5: { int address=readNextByte();int m=system.cpuRead(address);a=m; flagNZ=TABLE_NZ[m]; break; }
            case 0xa6: { int address=readNextByte();int m=system.cpuRead(address);x=m; flagNZ=TABLE_NZ[m]; break; }
            case 0xa8: { y=a; flagNZ=TABLE_NZ[a]; break; }
            case 0xa9: { int value=readNextByte();int m=value;a=m; flagNZ=TABLE_NZ[m]; break; }
            case 0xaa: { x=a; flagNZ=TABLE_NZ[a]; break; }
            case 0xac: { int address=readNextWord();int m=system.cpuRead(address);y=m; flagNZ=TABLE_NZ[m]; break; }
            case 0xad: { int address=readNextWord();int m=system.cpuRead(address);a=m; flagNZ=TABLE_NZ[m]; break; }
            case 0xae: { int address=readNextWord();int m=system.cpuRead(address);x=m; flagNZ=TABLE_NZ[m]; break; }
            case 0xb0: { int offset=(byte)readNextByte(); int address=pc+offset;if (flagC!=0) { pc = address; } break; }
            case 0xb1: { int ia=readNextByte(); int address=((system.cpuRead(ia)|(system.cpuRead(ia+1)<<8))+y)&0xffff;int m=system.cpuRead(address);a=m; flagNZ=TABLE_NZ[m]; break; }
            case 0xb4: { int address=(readNextByte()+x)&0xff;int m=system.cpuRead(address);y=m; flagNZ=TABLE_NZ[m]; break; }
            case 0xb5: { int address=(readNextByte()+x)&0xff;int m=system.cpuRead(address);a=m; flagNZ=TABLE_NZ[m]; break; }
            case 0xb6: { int address=(readNextByte()+y)&0xff;int m=system.cpuRead(address);x=m; flagNZ=TABLE_NZ[m]; break; }
            case 0xb8: { flagV=0; break; }
            case 0xb9: { int address=(readNextWord()+y)&0xffff;int m=system.cpuRead(address);a=m; flagNZ=TABLE_NZ[m]; break; }
            case 0xba: { x=s; flagNZ=TABLE_NZ[s]; break; }
            case 0xbc: { int address=(readNextWord()+x)&0xffff;int m=system.cpuRead(address);y=m; flagNZ=TABLE_NZ[m]; break; }
            case 0xbd: { int address=(readNextWord()+x)&0xffff;int m=system.cpuRead(address);a=m; flagNZ=TABLE_NZ[m]; break; }
            case 0xbe: { int address=(readNextWord()+y)&0xffff;int m=system.cpuRead(address);x=m; flagNZ=TABLE_NZ[m]; break; }
            case 0xc0: { int value=readNextByte();int m=value; int r=y-m; flagC=(~(r>>8))&1; flagNZ=TABLE_NZ[r&0xff]; break; }
            case 0xc1: { int ia=(readNextByte()+x)&0xFF; int address=system.cpuRead(ia)|(system.cpuRead(ia+1)<<8);int m=system.cpuRead(address); int r=a-m; flagC=(~(r>>8))&1; flagNZ=TABLE_NZ[r&0xff]; break; }
            case 0xc4: { int address=readNextByte();int m=system.cpuRead(address); int r=y-m; flagC=(~(r>>8))&1; flagNZ=TABLE_NZ[r&0xff]; break; }
            case 0xc5: { int address=readNextByte();int m=system.cpuRead(address); int r=a-m; flagC=(~(r>>8))&1; flagNZ=TABLE_NZ[r&0xff]; break; }
            case 0xc6: { int address=readNextByte();int m=system.cpuRead(address); int r=(m-1)&0xff; flagNZ=TABLE_NZ[r]; system.cpuWrite(address,r); break; }
            case 0xc8: { int m=y; int r=(m+1)&0xff; flagNZ=TABLE_NZ[r]; y=r; break; }
            case 0xc9: { int value=readNextByte();int m=value; int r=a-m; flagC=(~(r>>8))&1; flagNZ=TABLE_NZ[r&0xff]; break; }
            case 0xca: { int m=x; int r=(m-1)&0xff; flagNZ=TABLE_NZ[r]; x=r; break; }
            case 0xcc: { int address=readNextWord();int m=system.cpuRead(address); int r=y-m; flagC=(~(r>>8))&1; flagNZ=TABLE_NZ[r&0xff]; break; }
            case 0xcd: { int address=readNextWord();int m=system.cpuRead(address); int r=a-m; flagC=(~(r>>8))&1; flagNZ=TABLE_NZ[r&0xff]; break; }
            case 0xce: { int address=readNextWord();int m=system.cpuRead(address); int r=(m-1)&0xff; flagNZ=TABLE_NZ[r]; system.cpuWrite(address,r); break; }
            case 0xd0: { int offset=(byte)readNextByte(); int address=pc+offset;if ((flagNZ&MASK_Z)==0) { pc = address; } break; }
            case 0xd1: { int ia=readNextByte(); int address=((system.cpuRead(ia)|(system.cpuRead(ia+1)<<8))+y)&0xffff;int m=system.cpuRead(address); int r=a-m; flagC=(~(r>>8))&1; flagNZ=TABLE_NZ[r&0xff]; break; }
            case 0xd5: { int address=(readNextByte()+x)&0xff;int m=system.cpuRead(address); int r=a-m; flagC=(~(r>>8))&1; flagNZ=TABLE_NZ[r&0xff]; break; }
            case 0xd6: { int address=(readNextByte()+x)&0xff;int m=system.cpuRead(address); int r=(m-1)&0xff; flagNZ=TABLE_NZ[r]; system.cpuWrite(address,r); break; }
            case 0xd8: { flagD=0; break; }
            case 0xd9: { int address=(readNextWord()+y)&0xffff;int m=system.cpuRead(address); int r=a-m; flagC=(~(r>>8))&1; flagNZ=TABLE_NZ[r&0xff]; break; }
            case 0xdd: { int address=(readNextWord()+x)&0xffff;int m=system.cpuRead(address); int r=a-m; flagC=(~(r>>8))&1; flagNZ=TABLE_NZ[r&0xff]; break; }
            case 0xde: { int address=(readNextWord()+x)&0xffff;int m=system.cpuRead(address); int r=(m-1)&0xff; flagNZ=TABLE_NZ[r]; system.cpuWrite(address,r); break; }
            case 0xe0: { int value=readNextByte();int m=value; int r=x-m; flagC=(~(r>>8))&1; flagNZ=TABLE_NZ[r&0xff]; break; }
            case 0xe1: { int ia=(readNextByte()+x)&0xFF; int address=system.cpuRead(ia)|(system.cpuRead(ia+1)<<8);int m=system.cpuRead(address); int r=a-m-(1-flagC); flagC=(~(r>>8))&1; r&=0xFF; flagNZ=TABLE_NZ[r]; flagV=((a^m)&(a^r)&0x80)>>1; a=r; break; }
            case 0xe4: { int address=readNextByte();int m=system.cpuRead(address); int r=x-m; flagC=(~(r>>8))&1; flagNZ=TABLE_NZ[r&0xff]; break; }
            case 0xe5: { int address=readNextByte();int m=system.cpuRead(address); int r=a-m-(1-flagC); flagC=(~(r>>8))&1; r&=0xFF; flagNZ=TABLE_NZ[r]; flagV=((a^m)&(a^r)&0x80)>>1; a=r; break; }
            case 0xe6: { int address=readNextByte();int m=system.cpuRead(address); int r=(m+1)&0xff; flagNZ=TABLE_NZ[r]; system.cpuWrite(address,r); break; }
            case 0xe8: { int m=x; int r=(m+1)&0xff; flagNZ=TABLE_NZ[r]; x=r; break; }
            case 0xe9: { int value=readNextByte();int m=value; int r=a-m-(1-flagC); flagC=(~(r>>8))&1; r&=0xFF; flagNZ=TABLE_NZ[r]; flagV=((a^m)&(a^r)&0x80)>>1; a=r; break; }
            case 0xea: {  break; }
            case 0xec: { int address=readNextWord();int m=system.cpuRead(address); int r=x-m; flagC=(~(r>>8))&1; flagNZ=TABLE_NZ[r&0xff]; break; }
            case 0xed: { int address=readNextWord();int m=system.cpuRead(address); int r=a-m-(1-flagC); flagC=(~(r>>8))&1; r&=0xFF; flagNZ=TABLE_NZ[r]; flagV=((a^m)&(a^r)&0x80)>>1; a=r; break; }
            case 0xee: { int address=readNextWord();int m=system.cpuRead(address); int r=(m+1)&0xff; flagNZ=TABLE_NZ[r]; system.cpuWrite(address,r); break; }
            case 0xf0: { int offset=(byte)readNextByte(); int address=pc+offset;if ((flagNZ&MASK_Z)!=0) { pc = address; } break; }
            case 0xf1: { int ia=readNextByte(); int address=((system.cpuRead(ia)|(system.cpuRead(ia+1)<<8))+y)&0xffff;int m=system.cpuRead(address); int r=a-m-(1-flagC); flagC=(~(r>>8))&1; r&=0xFF; flagNZ=TABLE_NZ[r]; flagV=((a^m)&(a^r)&0x80)>>1; a=r; break; }
            case 0xf5: { int address=(readNextByte()+x)&0xff;int m=system.cpuRead(address); int r=a-m-(1-flagC); flagC=(~(r>>8))&1; r&=0xFF; flagNZ=TABLE_NZ[r]; flagV=((a^m)&(a^r)&0x80)>>1; a=r; break; }
            case 0xf6: { int address=(readNextByte()+x)&0xff;int m=system.cpuRead(address); int r=(m+1)&0xff; flagNZ=TABLE_NZ[r]; system.cpuWrite(address,r); break; }
            case 0xf8: { flagD=MASK_D; break; }
            case 0xf9: { int address=(readNextWord()+y)&0xffff;int m=system.cpuRead(address); int r=a-m-(1-flagC); flagC=(~(r>>8))&1; r&=0xFF; flagNZ=TABLE_NZ[r]; flagV=((a^m)&(a^r)&0x80)>>1; a=r; break; }
            case 0xfd: { int address=(readNextWord()+x)&0xffff;int m=system.cpuRead(address); int r=a-m-(1-flagC); flagC=(~(r>>8))&1; r&=0xFF; flagNZ=TABLE_NZ[r]; flagV=((a^m)&(a^r)&0x80)>>1; a=r; break; }
            case 0xfe: { int address=(readNextWord()+x)&0xffff;int m=system.cpuRead(address); int r=(m+1)&0xff; flagNZ=TABLE_NZ[r]; system.cpuWrite(address,r); break; }
            default:
            	for (int i=0; i<0x1000; i++) System.err.printf("%04x\n", pcs[(index+i)&0xfff]);
            	log("%04x : invalid opcode %02x\n",pc,opcode);
            }
            if ((a&0xffffff00) != 0)
            	break;
            if ((s&0xffffff00) != 0)
            	break;
            if ((x&0xffffff00) != 0)
            	break;
            if ((y&0xffffff00) != 0)
            	break;
            if ((flagC&~MASK_C) != 0)
            	break;
            if ((flagNZ&~(MASK_N|MASK_Z)) != 0)
            	break;
            if ((flagI&~MASK_I) != 0)
            	break;
            if ((flagD&~MASK_D) != 0)
            	break;
            if ((flagB&~MASK_B) != 0)
            	break;
            if ((flagV&~MASK_V) != 0)
            	break;
        }
        return cycles;
    }

    private void log(String format, Object... params) {
        System.err.printf(format,params);
    }

    private int readNextByte() {
        return system.cpuRead(pc++);
    }

    private int readNextWord() {
        int l = system.cpuRead(pc++);
        int h = system.cpuRead(pc++);
        return l|(h<<8);
    }

    private void php() {
        int p=flagC|flagNZ|flagI|flagD|flagB|0x20|flagV;
        push(p);
    }

    private void plp() {
        int p=pop();
        flagC  = p&MASK_C;
        flagNZ = p&(MASK_N|MASK_Z);
        flagI  = p&MASK_I;
        flagD  = p&MASK_D;
        flagB  = p&MASK_B;
        flagV  = p&MASK_V;
    }

    private void pushWord(int w) {
        push(w>>8);
        push(w&0xff);
    }

    private int popWord() {
        int l=pop();
        int h=pop();
        return l|(h<<8);
    }

    private void push(int value) {
        s=(s-1)&0xFF;
        system.cpuWrite(0x100|s,value);
    }

    private int pop() {
        int r=system.cpuRead(0x100|s);
        s=(s+1)&0xFF;
        return r;
    }
}
