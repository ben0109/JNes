package org.jnes.component.impl;

public class LengthCounter {
	
	public int length;
	public boolean halt;

	public boolean fcClock()
	{
		if (!halt && length>0) {
			length--;
			if (length==0) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isActive()
	{
		return halt || length>0;
	}
}
