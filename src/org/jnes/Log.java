package org.jnes;


public class Log {
	private static final int logLevel = 1;
	private static final int DEBUG   = 0;
	private static final int INFO    = 1;
	private static final int WARNING = 2;
	private static final int ERROR   = 3;
	private static int pc;

	public void debug(String format, Object... args) {
		if (logLevel<=DEBUG) {
			Object[] args2 = new Object[args.length+1];
			args2[0]=pc;
			for (int i=0; i<args.length; i++) {
				args2[i+1] = args[i];
			}
			System.out.printf("%04x: "+format, args2);
			System.out.println();
		}
	}

	public void info(String format, Object... args) {
		if (logLevel<=INFO) {
			Object[] args2 = new Object[args.length+1];
			args2[0]=pc;
			for (int i=0; i<args.length; i++) {
				args2[i+1] = args[i];
			}
			System.out.printf("%04x: "+format, args2);
			System.out.println();
		}
	}

	public void warning(String format, Object... args) {
		if (logLevel<=WARNING) {
			System.out.printf(format, args);
			System.out.println();
		}
	}

	public void error(String format, Object... args) {
		if (logLevel<=ERROR) {
			System.out.printf(format, args);
			System.out.println();
		}
	}

	public static void setPC(int pc) {
		Log.pc = pc;
	}
}
