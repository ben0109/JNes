package org.jnes.awt;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import org.jnes.component.APU;


public class Sound {

	private static final int SAMPLE_RATE = 22050;

	private static final int BUFFER_LENGTH = 1024;

	private static final int BUFFER_LATENCY = 50;

	private SourceDataLine dataLine;
	
	private int[] intBuffer = new int[BUFFER_LENGTH];

	private byte[] buffer = new byte[2*BUFFER_LENGTH];

	private long startTime = 0;
	
	private int samplesWritten = 0;
	
	private APU apu;
	
	public Sound(APU apu) throws LineUnavailableException {

		this.apu = apu;
		
		dataLine = getDataLine();
		dataLine.start();
	}
	
	private SourceDataLine getDataLine() {
		
		SourceDataLine line;
		AudioFormat format = new AudioFormat(SAMPLE_RATE,16,1,true,false);
		DataLine.Info info = new DataLine.Info(SourceDataLine.class,  format);
		
		if (!AudioSystem.isLineSupported(info)) {
			throw new Error("unsupported audio format");
		}
		
		// Obtain and open the line.
		try {
		    line = (SourceDataLine) AudioSystem.getLine(info);
		    line.addLineListener(new LineListener() {
		    	public void update(LineEvent event) {
		    		if (event.getType().equals(LineEvent.Type.START)) {
		    			startTime = System.currentTimeMillis();
		    			samplesWritten = 0;
		    		}
		    	}
		    });
		    line.open(format);
			return line;

		} catch (LineUnavailableException ex) {
			throw new Error("error while opening audio line");
		}
	}
	
	public void update() {
		
		int n;
		if (startTime>0) {
			long delta = System.currentTimeMillis()-startTime;
			n = (int)((delta+BUFFER_LATENCY)*SAMPLE_RATE/1000)-samplesWritten;
			n = Math.max(n,0);
			n = Math.min(n,BUFFER_LENGTH);
			samplesWritten += n;
		} else {
			n = BUFFER_LATENCY;
		}
		
		apu.fillBuffer(SAMPLE_RATE, 0x2000, intBuffer, n);

		for (int i=0; i<n; i++) {
			int sample = intBuffer[i];
			buffer[2*i+0] = (byte) (sample & 0xFF);
			buffer[2*i+1] = (byte) (sample >> 8);
		}

		dataLine.write(buffer, 0, 2*n);
	}
	
	public void close() {

		dataLine.close();
		dataLine = null;
	}
}
