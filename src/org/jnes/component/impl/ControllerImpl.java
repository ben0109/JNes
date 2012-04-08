package org.jnes.component.impl;

import org.jnes.NESSystem;
import org.jnes.component.Controller;
import org.jnes.component.JoypadState;


public class ControllerImpl implements Controller {
	
	private int latch1;
	private int latch2;
	private JoypadState joypad1 = new JoypadState();
	private JoypadState joypad2 = new JoypadState();
	
	public void setSystem(NESSystem system) {
		
	}

	public int read4016() {
		return 0x40 | (getBit(joypad1,latch1++) ? 1 : 0);
	}

	public int read4017() {
		return 0x40 | (getBit(joypad2,latch2++) ? 1 : 0);
	}

	public void write4016(int value) {
		if ((value&1)!=0) {
			latch1 = 0;
			latch2 = 0;
		}
	}
	
	public JoypadState getJoypad1() {
		return joypad1;
	}

	public void setJoypad1(JoypadState j) {
		joypad1 = j;
	}

	public JoypadState getJoypad2() {
		return joypad2;
	}
	
	public void setJoypad2(JoypadState j) {
		joypad2 = j;
	}
	
	private boolean getBit(JoypadState j, int n) {
		switch (n) {
		case 0: return j.a;
		case 1: return j.b;
		case 2: return j.select;
		case 3: return j.start;
		case 4: return j.up;
		case 5: return j.down;
		case 6: return j.left;
		case 7: return j.right;
		default: return false;
		}
	}
}
