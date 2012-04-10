package org.jnes.awt;

import java.awt.BorderLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileFilter;

import org.jnes.Emulator;
import org.jnes.NESSystem;
import org.jnes.NESSystemImpl;
import org.jnes.RomName;
import org.jnes.component.CPU;
import org.jnes.component.Controller;
import org.jnes.component.impl.APUImpl;
import org.jnes.component.impl.ControllerImpl;
import org.jnes.component.impl.Emu6502;
import org.jnes.component.impl.PPUImpl;
import org.jnes.mapper.DebugMapper;
import org.jnes.mapper.Mapper;
import org.jnes.mapper.NESFileLoader;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class Jnes extends JComponent {

	private static final long serialVersionUID = -3760424184227988879L;
	
	/**
	 * default scalefactor
	 */
	private static int DEFAULT_SCALE_FACTOR = 2;
	
	public static void main(String[] args) throws Exception {

		new Jnes();
	}
	
	/**
	 * the window
	 */
	final JFrame mainWindow;
	
	private String romPath;
	private String romName;
	
	/** display */
	private ScaledScreen screen;
	
	/** sound */
	private Sound sound;

	/** time of the last call to runForABit, in ms */
	private long next;
	
	/** time between two frames, in ms */
	private long frameDuration;
	
	private Emulator emulator;

	/** handles events */
	private Input input;

	/**
	 * setup the window and run the emulator
	 * @throws IOException
	 * @throws LineUnavailableException
	 * @throws SAXException
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws ClassNotFoundException 
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException 
	 * @throws NoSuchMethodException 
	 * @throws IllegalArgumentException 
	 * @throws SecurityException 
	 */
	public Jnes() throws IOException, LineUnavailableException, SAXException,
				ClassNotFoundException, InstantiationException,
				IllegalAccessException, InvocationTargetException, SecurityException, IllegalArgumentException, NoSuchMethodException {
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (UnsupportedLookAndFeelException ex) {
			System.out.println("Unable to load native look and feel");
		}

		setup(DEFAULT_SCALE_FACTOR);
		
		JPanel panel = new JPanel(new BorderLayout());	    
	    final JMenuBar menuBar = buildMenuBar();
		panel.add(menuBar, BorderLayout.NORTH);
	    panel.add(screen, BorderLayout.CENTER);
	    
	    mainWindow = new JFrame("JNes");
	    mainWindow.getContentPane().add(panel);
	    mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    mainWindow.setVisible(true);
	    mainWindow.requestFocus();
	    mainWindow.setResizable(false);
		mainWindow.addKeyListener(input);
	    mainWindow.pack();
	}
	
	private class RomFilter extends FileFilter {
		@Override
		public boolean accept(File f) {
			String lcName = f.getName().toLowerCase();
			return f.isDirectory()
				|| lcName.endsWith(".nes");
		}
		
		@Override
		public String getDescription() {
			return "Rom files (*.nes)";
		}
	}
	
	/**
	 * handler for "quit" event
	 */
	public void onFileQuit() {
		System.exit(0);
	}

	/**
	 * changes the scale. called through UI
	 * @param n the new scale
	 */
	public void setScaleFactor(int n) {
		screen.setScaleFactor(n);
	    mainWindow.pack();
	}
	
	/**
	 * build the menu bar from a config file
	 * @return the configured menu bar
	 * @throws IOException
	 * @throws SAXException
	 */
	private JMenuBar buildMenuBar() throws IOException, SAXException {
	    JMenuBar mb = new JMenuBar();
	    
	    ContentHandler handler = new MenuReader(this,mb);
	    
	    XMLReader parser = XMLReaderFactory.createXMLReader();
	    parser.setContentHandler(handler);
	    InputStream is = this.getClass().getResourceAsStream("/org/jnes/awt/menu.xml");
		parser.parse(new InputSource(is));
		
	    return mb;
	}
	
	
	
	


	CPU cpu;
	PPUImpl ppu;
	APUImpl apu;

	public void setup(int scaleFactor)
			throws IOException, LineUnavailableException, InstantiationException,
			SecurityException, IllegalArgumentException, NoSuchMethodException,
			IllegalAccessException, InvocationTargetException {

		InputStream is = new FileInputStream(RomName.ROM);
		final Mapper m = new DebugMapper(new NESFileLoader().load(is));
		is.close();

		cpu = new Emu6502();
//		cpu = new CPUImpl();
		apu = new APUImpl();
		ppu = new PPUImpl();
		Controller controller = new ControllerImpl();

		NESSystem system = new NESSystemImpl(cpu,apu,ppu,controller);
		system.setMapper(m);
		m.setSystem(system);
		
		emulator = new Emulator(m,cpu,ppu);
		sound = new Sound(apu);
		input = new Input(controller);

		screen = new ScaledScreen(ppu,scaleFactor);	
		screen.setScaleFactor(scaleFactor);
		add(screen);

		next = getTime();
		/*
		new Timer().schedule(new TimerTask() {
			public void run() {
				runOneFrame();
			}
		}, 0, 5);
		*/
		new Thread(new Runnable() {
			public void run() {
				while (true) {
					long now = getTime();
					if (now>next) {
						emulator.runForOneScreen();
						screen.repaint();
						sound.update();
						next += 1000000/60;
					}
					Thread.yield();
				}
			}
		}).start();
	}
	
	public void reset() {
		repaint();
	}
	private boolean paused = false;
	public void pause() {
		paused = !paused;
		next = getTime();
	}
	
	


	/**
	 * changes the frame skip.
	 * @param n
	 */
	public void setFrameSkip(int n) {
		// TODO implements adjustable frame skip
	}

	private long getTime() {
		return System.nanoTime()/1000;
	}

	public Input getInput() {
		return input;
	}
	

	
	TilesViewerDialog tilesViewer;
	
	MapViewerDialog mapViewer;
	
	public void showTilesViewer() {
		if (tilesViewer == null) {
			tilesViewer = new TilesViewerDialog(ppu);
		}
		tilesViewer.setVisible(true);
	}
	
	public void showMapViewer() {
		if (mapViewer == null) {
			mapViewer = new MapViewerDialog(ppu);
		}
		mapViewer.setVisible(true);
	}
}
