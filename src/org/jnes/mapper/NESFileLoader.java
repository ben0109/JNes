package org.jnes.mapper;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jnes.Log;


public class NESFileLoader {
	
	private Log logger = new Log();

	private static final String ERROR_NOT_A_MAPPER = "(ignored) not a mapper type : ";

	private static final String ERROR_CLASS_NOT_FOUND = "(ignored) class not found : ";

	private static final String ERROR_BAD_NUMBER = "(ignored) bad number format : ";

	private static final String RESOURCE_BUNDLE_MAPPER = "mapper";

	private static final String ERROR_BAD_NES_FILE = "Not a .nes file";
	
	private Map<Integer,Class<? extends Mapper>> mapperClasses;
	
	public NESFileLoader() {
		mapperClasses = new HashMap<Integer, Class<? extends Mapper>>();

		ResourceBundle rb = ResourceBundle.getBundle(RESOURCE_BUNDLE_MAPPER);
		Enumeration<String> e = rb.getKeys();
		while (e.hasMoreElements()) {
			String key = e.nextElement();
			String className = rb.getString(key);
			try {
				mapperClasses.put(parseInt(key), getMapperClass(parseInt(key), className));
				
			} catch (ClassNotFoundException exc) {
				logger.warning(ERROR_CLASS_NOT_FOUND+className);
				
			} catch (ClassCastException exc) {
				logger.warning(ERROR_NOT_A_MAPPER+className);
			
			} catch (NumberFormatException exc) {
				logger.warning(ERROR_BAD_NUMBER+key);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private Class<? extends Mapper> getMapperClass(int n, String className) throws ClassNotFoundException, ClassCastException {
		Class<?> cls = Class.forName(className);
		if (!Mapper.class.isAssignableFrom(cls)) {
			throw new ClassCastException(ERROR_NOT_A_MAPPER+className);
		}
		return (Class<? extends Mapper>)cls;
	}
	
	private int parseInt(String s) throws NumberFormatException {
		Matcher m;
		Pattern hex = Pattern.compile("\\s*0x([0-9a-fA-F]+)\\s*");
		m = hex.matcher(s);
		if (m.matches()) {
			String s2 = m.group(1);
			int n=0;
			for (int i=0; i<s2.length(); i++) {
				char c = s2.charAt(i);
				int d=0;
				if (c>='0' && c<='9') {
					d = c-'0';
				} else if (c>='a' && c<='f') {
					d = 10+(c-'a');
				} else if (c>='A' && c<='F') {
					d = 10+(c-'A');
				}
				n = (n<<4) + d;
			}
			return n;
		}

		Pattern dec = Pattern.compile("\\s*([0-9]+)\\s*");
		m = dec.matcher(s);
		if (m.matches()) {
			String s2 = m.group(1);
			return Integer.parseInt(s2);
		}
		
		throw new NumberFormatException("bad integer format "+s);
	}

	public Mapper load(InputStream i)
			throws IOException, SecurityException, IllegalArgumentException,
					NoSuchMethodException, InstantiationException,
					IllegalAccessException, InvocationTargetException {

		byte[] header = read(i, 16);
		
		_assert(header[0]=='N',ERROR_BAD_NES_FILE);
		_assert(header[1]=='E',ERROR_BAD_NES_FILE);
		_assert(header[2]=='S',ERROR_BAD_NES_FILE);
		_assert(header[3]==0x1A,ERROR_BAD_NES_FILE);
		
		
		// clear "DISKDUDE!" garbage
		if (header[15]!=0) {
			for (int j=7; j<16; j++) {
				header[j] = 0;
			}
		}
	
		int nbRomBanks = header[4]&0xff;
		int nbVRomBanks = header[5]&0xff;
		int flags1 = header[6]&0xff;
		int flags2 = header[7]&0xff;
		int nbRamBanks = header[8]&0xff;
		//boolean pal = ((header[9]&1) != 0);
		
		int mapperType = (flags1>>4) | (flags2&0xF0);
		Class<? extends Mapper> mapperClass = mapperClasses.get(mapperType);
		if (mapperClass==null) {
			throw new Error("unknown mapper type "+mapperType);
		}
		logger.info("using mapper %s", mapperClass.getSimpleName());
		
		byte[][] romBanks = new byte[nbRomBanks][];
		for (int k=0; k<nbRomBanks; k++)
			romBanks[k] = read(i,0x4000);
		
		byte[][] vromBanks = new byte[nbVRomBanks][];
		for (int k=0; k<nbVRomBanks; k++)
			vromBanks[k] = read(i,0x2000);

		return instantiateMapper(mapperClass, romBanks, nbRamBanks, vromBanks);
	}
	
	private Mapper instantiateMapper(Class<? extends Mapper> type,byte[][] romBanks, int nbRamBanks, byte[][] vromBanks)
			throws SecurityException, NoSuchMethodException,
					IllegalArgumentException, InstantiationException,
					IllegalAccessException, InvocationTargetException {

		Class[] paramTypes = new Class[] { romBanks.getClass(), int.class, vromBanks.getClass() };
		Object[] params = new Object[] { romBanks, nbRamBanks, vromBanks };
		Constructor<? extends Mapper> constructor = type.getConstructor(paramTypes);
		return constructor.newInstance(params);
	}
	
	private void _assert(boolean b, String msg) {
		if (!b) {
			throw new Error(msg);
		}
	}

	private byte[] read(InputStream i,int len) throws IOException {
		byte[] r = new byte[len];
		int p=0;
		while (p<len) {
			int n = i.read(r, p, len-p);
			if (n<0)
				throw new IOException("unexpected end of file");
			p += n;
		}
		return r;
	}
}
