package org.jnes.component;

public interface Controller extends NESComponent {

	int read4016();

	void write4016(int value);

	int read4017();
	
	public JoypadState getJoypad1();

	public void setJoypad1(JoypadState j);

	public JoypadState getJoypad2();
	
	public void setJoypad2(JoypadState j);
}
