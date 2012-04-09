package org.jnes.component.impl;

public class TriangleChannel
{
	private static final int[] SEQUENCE = {
		15, 14, 13, 12, 11, 10,  9,  8,  7,  6,  5,  4,  3,  2,  1,  0,
		 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	
	private LinearCounter linearCounter = new LinearCounter();
	private LengthCounter lengthCounter = new LengthCounter();
	
	int timer,period;
	private int sequence = 0;
	
	public void setRegister0(int value)
	{
		linearCounter.reload	= (value&0x7f);
//		looping					= (value&0x80)!=0;
	}
	
	public void setRegister2(int value)
	{
		period = ((period & ~0xff) | (value&0xff));
	}
	
	public void setRegister3(int value)
	{
		period = ((period & ~0x700) | ((value<<8)&0x700));
		
		if(!lengthCounter.halt) {
			lengthCounter.length = APUImpl.LENGTH_LUT[(value >> 3)];
		}
	}
	
	public void cpuClock()
	{
		if (--timer<=0) {
			timer = period;
			sequence = (sequence+1)&0x1f;
		}
	}
	
	public void fcClock(int step)
	{
		switch (step) {
		case 0:
		case 2:
			linearCounter.fcClock();
			/* fallthrough */
		case 1:
		case 3:
			lengthCounter.fcClock();
			break;
		}
	}
	
	public boolean isActive()
	{
		return lengthCounter.isActive() && linearCounter.isActive();
	}
	
	public void setLengthCounterHaltFlag(boolean f)
	{
		lengthCounter.halt = f;
	}
	
	public int getOutput()
	{
		return isActive() ? SEQUENCE[sequence] : 0; 
	}
}
