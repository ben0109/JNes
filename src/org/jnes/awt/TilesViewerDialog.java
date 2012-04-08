package org.jnes.awt;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.ColorModel;
import java.awt.image.MemoryImageSource;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.jnes.component.PPU;

public class TilesViewerDialog extends JDialog implements ActionListener {
	
	final TilesViewerComponent tiles;
	
	public TilesViewerDialog(PPU ppu) {
		tiles = new TilesViewerComponent(ppu);
		
		setLayout(new BorderLayout());
		add(tiles,BorderLayout.CENTER);
		
		JPanel palettes = new JPanel();
		palettes.setLayout(new GridLayout(0,1,0,10));
		JRadioButton palette0 = new JRadioButton("Palette 0");
		JRadioButton palette1 = new JRadioButton("Palette 1");
		JRadioButton palette2 = new JRadioButton("Palette 2");
		JRadioButton palette3 = new JRadioButton("Palette 3");
		palettes.add(palette0);
		palettes.add(palette1);
		palettes.add(palette2);
		palettes.add(palette3);
		ButtonGroup group = new ButtonGroup();
		group.add(palette0);
		group.add(palette1);
		group.add(palette2);
		group.add(palette3);
		palette0.setSelected(true);
		add(palettes,BorderLayout.WEST);
		
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(1,0,10,0));
		JButton update = new JButton("Update");
		JButton close = new JButton("Close");
		panel.add(update);
		panel.add(close);
		add(panel,BorderLayout.SOUTH);
		
		pack();
		setResizable(false);
		
		update.setActionCommand("update");
		update.addActionListener(this);
		close.setActionCommand("close");
		close.addActionListener(this);
		palette0.setActionCommand("palette0");
		palette0.addActionListener(this);
		palette1.setActionCommand("palette1");
		palette1.addActionListener(this);
		palette2.setActionCommand("palette2");
		palette2.addActionListener(this);
		palette3.setActionCommand("palette3");
		palette3.addActionListener(this);
	}
	
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if ("close".equals(command)) {
			setVisible(false);
		} else if ("update".equals(command)) {
			tiles.refresh();
		} else if ("palette0".equals(command)) {
			tiles.setPalette(0);
		} else if ("palette1".equals(command)) {
			tiles.setPalette(1);
		} else if ("palette2".equals(command)) {
			tiles.setPalette(2);
		} else if ("palette3".equals(command)) {
			tiles.setPalette(3);
		}
	}

	class TilesViewerComponent extends JComponent {
		
		final PPU ppu;
		
		final ColorModel model = ColorUtils.createColorModel();

		final byte[] array = new byte[0x80*0x100];
		
		int palette = 0;
		
		int scaleFactor = 2;

		Image img;

		public TilesViewerComponent(final PPU ppu) {
			this.ppu = ppu;
			refresh();
		}

		@Override
		public void paint(Graphics g) {
			g.drawImage(img, 0,0,0x80*scaleFactor,0x100*scaleFactor, 0,0,0x80,0x100, null);
		}

		private void refresh() {
			ppu.showTiles(array,0,0x80, palette);
			MemoryImageSource mis = new MemoryImageSource(0x80, 0x100, model, array, 0, 0x80);
			img = createImage(mis);
			repaint();
		}
		
		@Override
		public Dimension getPreferredSize() {
			return new Dimension(0x80*scaleFactor, 0x100*scaleFactor);
		}
		
		void setPalette(int palette) {
			this.palette = palette;
			refresh();
		}
	}
}
