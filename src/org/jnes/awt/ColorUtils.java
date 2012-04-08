package org.jnes.awt;

import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;

public class ColorUtils {
	
	public static final int[] PALETTE = {
		0x525252,
		0x000080,
		0x08008A,
		0x2C007E,
		0x4A004E,
		0x500006,
		0x440000,
		0x260800,
		0x0A2000,
		0x002E00,
		0x003200,
		0x00260A,
		0x001C48,
		0x000000,
		0x000000,
		0x000000,

		0xA4A4A4,
		0x0038CE,
		0x3416EC,
		0x5E04DC,
		0x8C00B0,
		0x9A004C,
		0x901800,
		0x703600,
		0x4C5400,
		0x0E6C00,
		0x007400,
		0x006C2C,
		0x005E84,
		0x000000,
		0x000000,
		0x000000,

		0xFFFFFF,
		0x4C9CFF,
		0x7C78FF,
		0xA664FF,
		0xDA5AFF,
		0xF054C0,
		0xF06A56,
		0xD68610,
		0xBAA400,
		0x76C000,
		0x46CC1A,
		0x2EC866,
		0x34C2BE,
		0x3A3A3A,
		0x000000,
		0x000000,

		0xFFFFFF,
		0xB6DAFF,
		0xC8CAFF,
		0xDAC2FF,
		0xF0BEFF,
		0xFCBCEE,
		0xFAC2C0,
		0xF2CCA2,
		0xE6DA92,
		0xCCE68E,
		0xB8EEA2,
		0xAEEABE,
		0xAEE8E2,
		0xB0B0B0,
		0x000000,
		0x000000
	};

	public static ColorModel createColorModel() {

		byte[] reds = new byte[0x40];
		byte[] greens = new byte[0x40];
		byte[] blues = new byte[0x40];
		for (int i=0; i<0x40; i++) {
			reds[i] = (byte)(PALETTE[i]>>16);
			greens[i] = (byte)(PALETTE[i]>>8);
			blues[i] = (byte)(PALETTE[i]>>0);
		}
		return new IndexColorModel(8, 0x40, reds, greens, blues);
	}

}
