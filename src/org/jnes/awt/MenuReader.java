package org.jnes.awt;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Stack;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class MenuReader extends DefaultHandler {
	
	private ResourceBundle bundle = ResourceBundle.getBundle("menu");
	
	private Object target;

	private Stack<Object> stack = new Stack<Object>();

	public MenuReader(Object _this, JMenuBar root) {
		this.target = _this;
		stack.push(root);
	}
	
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {

		if ("menubar".equals(localName)) {
			if (stack.size()>1) {
				throw new SAXException("menubar is allowed only at root");
			}
			
		} else if ("menu".equals(localName)) {
			stack.push(createMenu(atts));
			
		} else if ("item".equals(localName)) {
			stack.push(createItem(atts));
			
		} else if ("action".equals(localName)) {
			stack.push(createAction(atts));
			
		} else if ("param".equals(localName)) {
			stack.push(createParam(atts));
			
		} else if ("radio-group".equals(localName)) {
			stack.push(createRadioGroup(atts));
			
		} else {
			throw new SAXException("unrecognized element "+localName);
		}
		
	}

	private String getResource(String caption) {
		if (!caption.startsWith("$"))
			return caption;
		
		try {
			return bundle.getString(caption.substring(1));
		} catch (MissingResourceException e) {
			System.err.println("Warning : missing resource "+caption);
			return caption;
		}
	}
	
	private Object createMenu(Attributes atts) throws SAXException {
		String caption = getAttribute(atts, "caption");
		return new JMenu(getResource(caption));
	}

	private Object createItem(Attributes atts) throws SAXException {
		String caption = getAttribute(atts, "caption");
		return new JMenuItem(getResource(caption));
	}

	private Object createAction(Attributes atts) throws SAXException {
		String method = getAttribute(atts, "method");
		return new ActionDescriptor(method);
	}

	private Object createParam(Attributes atts) throws SAXException {
		String type = atts.getValue("type");
		if (type==null) {
			type = "xsi:string";
		}
		String value = getAttribute(atts, "value");
		return new MethodParameter(type,value);
	}

	private Object createRadioGroup(Attributes atts) throws SAXException {
		return new ButtonGroup();
	}

	private String getAttribute(Attributes atts, String string) throws SAXException {
		String caption = atts.getValue(string);
		if (caption==null)
			throw new SAXException("missing attribute: "+string);
		return caption;
	}

	public void endElement(String uri, String localName, String qName) throws SAXException {
		
		Object top = stack.pop();
		if (top instanceof JMenuBar) {
			return;
		}
		
		Object next = stack.peek();
		if (top instanceof JMenu) {
			JMenu menu = (JMenu)top;
			if (next instanceof JMenuBar) {
				((JMenuBar)next).add(menu);
			} else if (next instanceof JMenu) {
				((JMenu)next).add(menu);				
			} else {
				throw new SAXException("cannot use menu in "+next.getClass().getSimpleName());
			}
			
		} else if (top instanceof JMenuItem) {
			JMenuItem item = (JMenuItem)top;
			if (next instanceof JMenu) {
				((JMenu)next).add(item);				
			} else {
				throw new SAXException("cannot use item in "+next.getClass().getSimpleName());
			}
			
		} else if (top instanceof ActionDescriptor) {
			ActionDescriptor listener = (ActionDescriptor)top;
			listener.onConfigurationEnd();
			if (next instanceof JMenuItem) {
				((JMenuItem)next).addActionListener(listener);				
			} else {
				throw new SAXException("cannot use action in "+next.getClass().getSimpleName());
			}
			
		} else if (top instanceof MethodParameter) {
			MethodParameter param = (MethodParameter)top;
			if (next instanceof ActionDescriptor) {
				((ActionDescriptor)next).addParameter(param);				
			} else {
				throw new SAXException("cannot use item in "+next.getClass().getSimpleName());
			}
		}
		
	}
	
	public class ActionDescriptor implements ActionListener {
		
		private final String methodName;
		private final ArrayList<MethodParameter> parameters;
		private Method method;
		private Object[] parameterArray;
		boolean useEvent = true;

		public ActionDescriptor(String methodName) {
			this.methodName = methodName;
			this.parameters = new ArrayList<MethodParameter>();
		}
		
		public void actionPerformed(ActionEvent event) {

			try {
				if (useEvent) {
					parameterArray[0] = event;
				}
				method.invoke(target, parameterArray);
			
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		public void onConfigurationEnd() throws SAXException {
			
			try {
				try {
					useEvent = true;
					
					Class[] parameterTypes = new Class[parameters.size()+1];
					parameterTypes[0] = ActionEvent.class;
					int i=1;
					for (MethodParameter arg : parameters) {
						parameterTypes[i++] = arg.getType();
					}
					method = target.getClass().getMethod(methodName, parameterTypes);
					
					parameterArray = new Object[parameters.size()+1];
					int j=1;
					for (MethodParameter arg : parameters) {
						parameterArray[j++] = arg.getValue();
					}
	
				} catch (NoSuchMethodException e) {
					useEvent = false;
					
					Class[] parameterTypes = new Class[parameters.size()];
					int i=0;
					for (MethodParameter arg : parameters) {
						parameterTypes[i++] = arg.getType();
					}
					method = target.getClass().getMethod(methodName, parameterTypes);
					
					parameterArray = new Object[parameters.size()];
					int j=0;
					for (MethodParameter arg : parameters) {
						parameterArray[j++] = arg.getValue();
					}
				}
				
			} catch (NoSuchMethodException e) {
				throw new SAXException("no method "+methodName,e);
			}
		}
		
		public void addParameter(MethodParameter param) {
			parameters.add(param);
		}
	}
	
	public class MethodParameter {

		private Class type;
		private Object value;

		public MethodParameter(String type, String value) throws SAXException {
			if (type==null) {
				type = "xsd:string";
			}
			if ("xsd:string".equals(type)) {
				this.type = String.class;
				this.value = value;
			
			} else if ("xsd:int".equals(type)) {
				this.type = int.class;
				this.value = Integer.parseInt(value);
			
			} else if ("xsd:double".equals(type)) {
				this.type = double.class;
				this.value = Double.parseDouble(value);

			} else if ("xsd:boolean".equals(type)) {
				this.type = boolean.class;
				this.value = Boolean.parseBoolean(value);
			
			} else {
				throw new SAXException("unknown type "+type);
			}
		}

		public Object getValue() {
			return value;
		}

		public Class getType() {
			return type;
		}
	}
}
