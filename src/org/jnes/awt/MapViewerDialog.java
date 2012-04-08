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

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;

import org.jnes.component.impl.PPUImpl;

public class MapViewerDialog extends JDialog implements ActionListener {
	
	final TilesViewerComponent tiles;
	
	public MapViewerDialog(PPUImpl ppu) {
		tiles = new TilesViewerComponent(ppu);
		
		setLayout(new BorderLayout());
		add(tiles,BorderLayout.CENTER);
		
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
	}
	
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if ("close".equals(command)) {
			setVisible(false);
		} else if ("update".equals(command)) {
			tiles.update();
		}
	}

	class TilesViewerComponent extends JComponent {
		
		final PPUImpl ppu;
		
		final ColorModel model = ColorUtils.createColorModel();

		final byte[] array = new byte[0x200*0x1e0];

		int scaleFactor = 1;

		Image img;

		public TilesViewerComponent(final PPUImpl ppu) {
			this.ppu = ppu;
			update();
		}

		@Override
		public void paint(Graphics g) {
			g.drawImage(img, 0,0,0x200*scaleFactor,0x1e0*scaleFactor, 0,0,0x200,0x1e0, null);
		}

		private void update() {
			ppu.showScreens(array,0,0x200);
			img = createImage(new MemoryImageSource(0x200, 0x1e0, model, array, 0, 0x200));
			repaint();
		}
		
		@Override
		public Dimension getPreferredSize() {
			return new Dimension(0x200*scaleFactor, 0x1e0*scaleFactor);
		}
	}
}
