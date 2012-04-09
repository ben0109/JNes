package org.jnes.component.impl;

import org.jnes.Log;
import org.jnes.NESSystem;
import org.jnes.component.APU;

public class APUImpl implements APU
{
	public static final int[] LENGTH_LUT = {
		10,254, 20,  2, 40,  4, 80,  6, 160,  8, 60, 10, 14, 12, 26, 14,
		12, 16, 24, 18, 48, 20, 96, 22, 192, 24, 72, 26, 16, 28, 32, 30
	};
	
	private NESSystem system;
	private Log logger = new Log();
		
	private PulseChannel	pulse0		= new PulseChannel();
	private PulseChannel	pulse1		= new PulseChannel();
	private TriangleChannel	triangle	= new TriangleChannel();
	private NoiseChannel	noise		= new NoiseChannel();
	private DMChannel		dmc			= new DMChannel();

	private boolean fcMode5Step		= false;
	private boolean fcInhibitIRQ	= false;
	private int fcTimer,fcStep		= 0;
	private int baseTimer,baseStep	= 0;
	
	
	@Override
	public void setSystem(NESSystem system)
	{
		this.system = system;
	}

	@Override
	public int readRegister(int address)
	{
		if (address==0x4015) {
			int value = 0;
			if (pulse0.isActive())		{ value |= 0x01; }
			if (pulse1.isActive())		{ value |= 0x02; }
			if (triangle.isActive())	{ value |= 0x04; }
			if (noise.isActive())		{ value |= 0x08; }
			//if (dmc.isActive())		{ value |= 0x10; }
			//if (dmc.irqOccurred)		{ value |= 0x80; }
			return value;
		} else {
			return 0;
		}
	}
	
	@Override
	public void writeRegister(int address, int value)
	{
		switch (address) {
		case 0x4000: pulse0		.setRegister0(value); break;
		case 0x4001: pulse0		.setRegister1(value); break;
		case 0x4002: pulse0		.setRegister2(value); break;
		case 0x4003: pulse0		.setRegister3(value); break;
		
		case 0x4004: pulse1		.setRegister0(value); break;
		case 0x4005: pulse1		.setRegister1(value); break;
		case 0x4006: pulse1		.setRegister2(value); break;
		case 0x4007: pulse1		.setRegister3(value); break;
		
		case 0x4008: triangle	.setRegister0(value); break;
		case 0x400a: triangle	.setRegister2(value); break;
		case 0x400b: triangle	.setRegister3(value); break;
		
		case 0x400c: noise		.setRegister0(value); break;
		case 0x400e: noise		.setRegister2(value); break;
		case 0x400f: noise		.setRegister3(value); break;
		
		case 0x4010: dmc		.setRegister0(value); break;
		case 0x4011: dmc		.setRegister1(value); break;
		case 0x4012: dmc		.setRegister2(value); break;
		case 0x4013: dmc		.setRegister3(value); break;
		
		case 0x4015:
			pulse0	.setLengthCounterHaltFlag((value&0x01)!=0);
			pulse1	.setLengthCounterHaltFlag((value&0x02)!=0);
			triangle.setLengthCounterHaltFlag((value&0x04)!=0);
			noise	.setLengthCounterHaltFlag((value&0x08)!=0);

			boolean enableDmc		= (value&0x10)!=0;
			if (enableDmc) { /* ... */ }
			dmc.irqOccurred = false;
			break;
			
		case 0x4017:
			fcMode5Step = (value&0x80)!=0;
			fcInhibitIRQ = (value&0x40)!=0;
			break;
		}
		
	}
	
	private void clock()
	{
		if (--fcTimer<=0) {
			fcTimer = 3728;
			fcStep++;
			if (fcStep>4 || (fcStep==4 && !fcMode5Step)) {
				fcStep = 0;
			}
			
			pulse0.fcClock(fcStep);
			pulse1.fcClock(fcStep);
			triangle.fcClock(fcStep);
			noise.fcClock(fcStep);
		}
		pulse0.cpuClock();
		pulse1.cpuClock();
		triangle.cpuClock();
		noise.cpuClock();
	}
	
	private float nextSample()
	{
		clock();
		return 0.00752f*(pulse0.getOutput() + pulse1.getOutput())
			+ 0.00851f*triangle.getOutput()
			+ 0.00494f*noise.getOutput()
		// + 0.00335*dmc.getOutput()
		;
	}
	
	@Override
	public void fillBuffer(float frequency, int volume, int[] buffer, int len)
	{
		float systemSamplePeriod = (float)(12/21000000.);
		float targetSamplePeriod = (float)(1/frequency);
		
		float time = 0;
		for (int i=0; i<len; i++) {
			int nsamples = 0;
			float accumulator = 0;
			while (time < targetSamplePeriod) {
				time += systemSamplePeriod;
				accumulator += nextSample();
				nsamples++;
			}
			time -= targetSamplePeriod;
			float sample = volume*accumulator / nsamples;
			buffer[i] = (int)sample;
		}
	}
}
