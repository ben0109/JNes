package org.jnes.component.impl;

public class LinearCounter
{
	public int counter,reload;
	public boolean halt,control;

	public void fcClock()
	{
		if (halt) {
			counter = reload;
		} else if (counter>0) {
			counter--;
		}
		if (!control) {
			halt = true;
		}
	}
	
	public boolean isActive()
	{
		return counter>0;
	}
}
