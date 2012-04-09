package org.jnes.component.impl;

public class Sweep
{
	public int timer;

	public boolean enabled;
	public int period;
	public int shifts;
	public boolean invert;
	
	public boolean reload;
	public int rawPeriod;

	public boolean fcClock()
	{
		if (enabled) {
			if (--timer<0) {
				int delta = rawPeriod>>shifts;
				if (invert) {
					rawPeriod -= delta;
				} else {
					rawPeriod += delta;
				}
				timer = period;
			}
		}
		if (reload) {
			timer = period;
			reload = false;
		}
		return true;
	}
	
	public boolean silence()
	{
		return (rawPeriod<8) || (rawPeriod>0x7ff);
	}
}
