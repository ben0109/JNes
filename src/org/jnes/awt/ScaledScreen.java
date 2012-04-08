package org.jnes.awt;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.ColorModel;
import java.awt.image.MemoryImageSource;

import javax.swing.JComponent;

import org.jnes.component.impl.PPUImpl;

public class ScaledScreen extends JComponent {

	private static final long serialVersionUID = 1861029798568680090L;
	private int scaleFactor;
	private final PPUImpl ppu;
	public final ColorModel colorModel;
	
	public ScaledScreen(PPUImpl ppu, int scaleFactor) {

		this.ppu = ppu;
		colorModel = ColorUtils.createColorModel();

		this.scaleFactor = scaleFactor;
	}

	public void paint(Graphics g) {

		// create image
		byte[] array = ppu.getTargetArray();
		MemoryImageSource mis = new MemoryImageSource(
				0x100, 0x100, 
				colorModel, array, 0, 0x100);
		Image image = createImage(mis);
		
		// scale and draw
		g.drawImage(image, 0, 0, scaleFactor * 0x100, scaleFactor * 0x100, null);
	}

	public int getScaleFactor() {
		return scaleFactor;
	}

	public void setScaleFactor(int scaleFactor) {
		this.scaleFactor = scaleFactor;
	}
	
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(0x100*scaleFactor, 0x100*scaleFactor);
	}
}
