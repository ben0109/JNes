package org.jnes.component.impl;

class DMChannel /*implements Channel*/
{
	// Memory reader.
	int address;
	int dmaLength;
//	int cachedAddress;
//	int cached_dmaLength;

	// Sample buffer.
	int currentByte;
	int sampleBits;

	// Output unit.
	int counter;
	int shiftReg;

	// IRQ generator.
	boolean generateIrq;
	boolean irqOccurred;
	
	void setRegister0(int value)
	{
		/*			
		if(machine_type == MACHINE_TYPE_NTSC) {
			period = dmc_period_lut_ntsc[value&0xf];
		} else {
			period = dmc_period_lut_pal[value&0xf];
		}
		*/
	//	looping		= (value&0x40)!=0;
		generateIrq	= (value&0x80)!=0;
	}
	
	void setRegister1(int value)
	{
	//	volume = value&0x7f;
		//output = value&0x7f;
	}
	
	void setRegister2(int value)
	{
		address = (0x4000 + (value << 6));
	}
	
	void setRegister3(int value)
	{
		dmaLength = ((value << 4) + 1);
	}
}