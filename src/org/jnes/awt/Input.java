package org.jnes.awt;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.Map;

import org.jnes.component.Controller;
import org.jnes.component.JoypadState;

public class Input implements KeyListener {

	private Controller io;
	private Map<Integer,EventMapping> mapping;
	
	public Input(Controller io) {

		this.io = io;
		
		mapping = new HashMap<Integer, EventMapping>();
		mapping.put(KeyEvent.VK_UP,     new EventMapping(1, Button.Up));
		mapping.put(KeyEvent.VK_DOWN,   new EventMapping(1, Button.Down));
		mapping.put(KeyEvent.VK_LEFT,   new EventMapping(1, Button.Left));
		mapping.put(KeyEvent.VK_RIGHT,  new EventMapping(1, Button.Right));
		mapping.put(KeyEvent.VK_W,      new EventMapping(1, Button.A));
		mapping.put(KeyEvent.VK_X,      new EventMapping(1, Button.B));

		mapping.put(KeyEvent.VK_SPACE,  new EventMapping(1, Button.Select));
		mapping.put(KeyEvent.VK_ENTER,  new EventMapping(1, Button.Start));
	}

	private void setForMapping(int key, boolean state) {
		
		EventMapping em = mapping.get(key);
		if (em!=null) {
			JoypadState j = (em.joypadNumber==1) ? io.getJoypad1() : io.getJoypad2();
			switch (em.button) {
			case Up:     j.up     = state; break;
			case Down:   j.down   = state; break;
			case Left:   j.left   = state; break;
			case Right:  j.right  = state; break;
			case A:      j.a      = state; break;
			case B:      j.b      = state; break;
			case Start:  j.start  = state; break;
			case Select: j.select = state; break;
			default:
			}
		}
	}

	private enum Button { Up, Down, Left, Right, A, B, Start, Select };
	private class EventMapping {
		
		int joypadNumber;
		Button button;
		
		public EventMapping(int joypadNumber, Button button) {
			super();
			this.joypadNumber = joypadNumber;
			this.button = button;
		}
	}
	
	public void keyPressed(KeyEvent e) {
		
		int keyCode = e.getKeyCode();
		setForMapping(keyCode, true);
	}
	
	public void keyReleased(KeyEvent e) {
		setForMapping(e.getKeyCode(), false);
	}
	
	public void keyTyped(KeyEvent e) {
		// nothing		
	}
}
