package org.jnes.component.impl;

public class PulseChannel
{
	private static final int[] DUTY_CYCLE_LUT = { 0x40, 0x60, 0x78, 0x9f };
	
	public Envelope envelope = new Envelope();
	public Sweep sweep = new Sweep();
	public LengthCounter lengthCounter = new LengthCounter();
	
	int timer;
	private int sequence, sequencePosition;
	
	public void setRegister0(int value)
	{
		sequence			= DUTY_CYCLE_LUT[(value & 0xc0)>>6];
		
		lengthCounter.halt	= (value & 0x20)!=0;
		envelope.loop		= (value & 0x20)!=0;
		envelope.constant	= (value & 0x10)!=0;
		envelope.period		= (value & 0x0f);
		envelope.start		= true;
	}
	
	public void setRegister1(int value)
	{
		sweep.enabled	= (value & 0x80)!=0;
		sweep.period	= (value & 0x70) >> 4;
		sweep.invert	= (value & 0x08)!=0;
		sweep.shifts	= (value & 0x07);
		sweep.reload	= true;	
	}
	
	public void setRegister2(int value)
	{
		sweep.rawPeriod = ((sweep.rawPeriod & ~0xff) | (value&0xff));
	}
	
	public void setRegister3(int value)
	{
		sweep.rawPeriod = ((sweep.rawPeriod & 0xff) | ((value&7)<<8));
		
		if(!lengthCounter.halt) {
			lengthCounter.length = APUImpl.LENGTH_LUT[(value&0xf8) >> 3];
		}

		sequencePosition = 0;
		envelope.start = true;	
	}
	
	public void cpuClock()
	{
		if (--timer<=0) {
			timer = sweep.rawPeriod<<1;
			sequencePosition = (sequencePosition+1)&7;
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
			sweep.fcClock();
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
		boolean outputOn = ((sequence>>sequencePosition)&1)!=0;
		return (outputOn && isActive() && !sweep.silence()) ? envelope.getVolume() : 0; 
	}
}
