package org.jnes.component.impl;

public class NoiseChannel
{
	private static final int[] NTSC_PERIOD_LUT = { 4, 8, 16, 32, 64, 96, 128, 160, 202, 254, 380, 508, 762, 1016, 2034, 4068 };
	private static final int[] PAL_PERIOD_LUT  = { 4, 7, 14, 30, 60, 88, 118, 148, 188, 236, 354, 472, 708,  944, 1890, 3778 };
	
	private Envelope envelope = new Envelope();
	private LengthCounter lengthCounter = new LengthCounter();
	
	int timer,period;
	private int shift = 1,tapBit = 1;
	
	public void setRegister0(int value)
	{/*
		lengthCounter.halt	= (value & 0x20)!=0;
		envelope.loop		= (value & 0x20)!=0;
		envelope.constant	= (value & 0x10)!=0;
		envelope.period		= (value & 0x0f);*/
	}
	
	public void setRegister2(int value)
	{
		period = NTSC_PERIOD_LUT[period&0xf];
		tapBit = ((value & 0x80)!=0) ? 6 : 1;
	}
	
	public void setRegister3(int value)
	{
		if(!lengthCounter.halt) {
			lengthCounter.length = APUImpl.LENGTH_LUT[(value&0xf8) >> 3];
		}

		envelope.start = true;	
	}
	
	public void cpuClock()
	{
		if (--timer<=0) {
			timer = period;
			int feedback = (shift^(shift>>tapBit))&1;
			shift = (shift>>1) | (feedback<<14);
		}
	}
	
	public void fcClock(int step)
	{
		switch (step) {
		case 0:
		case 2:
			envelope.fcClock();
			/* fallthrough */
		case 1:
		case 3:
			lengthCounter.fcClock();
			break;
		}
	}
	
	public boolean isActive()
	{
		return lengthCounter.isActive();
	}
	
	public void setLengthCounterHaltFlag(boolean f)
	{
		lengthCounter.halt = f;
	}
	
	public int getOutput()
	{
		boolean outputOn = (shift&1)==0;
		return (outputOn && isActive()) ? envelope.getVolume() : 0; 
	}
}
