package org.jnes.component;


public interface APU extends NESComponent
{	
	int readRegister(int address);
	
	void writeRegister(int address,int value);
	
	void fillBuffer(float frequency,int volume,int[] buffer,int len);
}
