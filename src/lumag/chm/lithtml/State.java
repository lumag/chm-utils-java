/**
 * 
 */
package lumag.chm.lithtml;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;


import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

class State {
	private final ContentHandler handler;

	private Decoder decoder;

	private int flags;

	private Deque<ITag> tagStack = new ArrayDeque<ITag>();
	private Map<String, String> attributes;

	private String attributeName;

	private StringBuilder builder;
	private int valueLength;

	private final ITag[] tags;

	State(ContentHandler handler, ITag[] tags) {
		this.handler = handler;
		this.tags = tags;
	}
	
	void start() throws SAXException {
		handler.startDocument();

		decoder = Decoder.TEXT;
		startString();
	}

	void process(int ucs32) throws SAXException {
		decoder = decoder.process(this, ucs32);
	}
	
	void finish() throws SAXException {
		decoder = null;

		handler.endDocument();
	}
	
	void openElement(int number) {
		openElement(tags[number]);
	}
	
	void openElement(String name) {
		openElement(new CustomTag(name, tags[0]));
	}

	private void openElement(ITag tag) {
		tagStack.push(tag);

		if (attributes == null) {
			attributes = new LinkedHashMap<String, String>();
		}

	}

	void addAttribute(String name) {
		attributeName = name;
	}

	void setAttributeValue(String value) {
		attributes.put(attributeName, value);
		
		if (attributeName.charAt(0) == '_') {
			System.err.format("Tag '%s' unknown attribute '%s' value '%s'%n",
					tagStack.peek(),
					attributeName.substring(1, attributeName.length() - 1),
					value);
		}


		attributeName = null;
	}

	void startElement() throws SAXException {
		AttributesImpl atts = new AttributesImpl();
		for (Map.Entry<String, String> a: attributes.entrySet()) {
			atts.addAttribute("", "", a.getKey(), "CDATA", a.getValue());
		}
		attributes  = null;
		handler.startElement("", "", tagStack.peek().toString(), atts);
	}

	void endElement() throws SAXException {
		handler.endElement("", "", tagStack.pop().toString());
	}

	void text(String text) throws SAXException {
		handler.characters(text.toCharArray(), 0, text.length());
	}
	
	void startString(int len) {
		valueLength = len;
		builder = new StringBuilder();
	}
	
	void startString() {
		startString(0);
	}

	boolean readUtf8Char(int c) {
		builder.append(Character.toChars(c));
		if (valueLength != 0) {
			valueLength --;
		}
		return valueLength == 0;
	}

	String getAttribute(int ucs32) {
		return tagStack.peek().getAttribute(ucs32);
	}

	String getString() {
		String str = builder.toString();
		builder = null;
		return str;
	}

	void setFlags(int flags) {
		this.flags = flags;
	}

	int getFlags() {
		return flags;
	}
}
