package org.jnes.component.impl;

public class Envelope {

	public int timer;
	
	public int period;
	public int counter;
	public boolean loop;
	public boolean constant;
	
	public boolean start;
	
	public void fcClock()
	{
		if (start) {
			start = false;
			counter = 15;
			timer = period;
		} else {
			if (--timer<=0) {
				timer = period;
				if (counter>0) {
					--counter;
				} else if (loop) {
					counter = 15;
				}
			}
		}
	}
	
	public int getVolume() {
		return constant ? period : counter;
	}
}
