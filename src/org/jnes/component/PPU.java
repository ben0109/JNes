package org.jnes.component;


public interface PPU extends NESComponent {

	void endLine();

	int getLineNumber();
	
	int readRegister(int address);
	
	void writeRegister(int address,int value);
	
	
	void showTiles(byte[] array, int offset, int scanline, int palette);
}
