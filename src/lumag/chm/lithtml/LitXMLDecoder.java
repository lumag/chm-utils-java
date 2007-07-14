package lumag.chm.lithtml;

import static lumag.chm.lithtml.LitXMLDecoder.DecodingState.ATTR_CUSTOM;
import static lumag.chm.lithtml.LitXMLDecoder.DecodingState.ATTR_CUSTOM_LEN;
import static lumag.chm.lithtml.LitXMLDecoder.DecodingState.ATTR_NAME;
import static lumag.chm.lithtml.LitXMLDecoder.DecodingState.ATTR_VALUE;
import static lumag.chm.lithtml.LitXMLDecoder.DecodingState.ATTR_VALUE_LEN;
import static lumag.chm.lithtml.LitXMLDecoder.DecodingState.ATTR_VALUE_NUMBER;
import static lumag.chm.lithtml.LitXMLDecoder.DecodingState.FLAG;
import static lumag.chm.lithtml.LitXMLDecoder.DecodingState.TAG_CUSTOM;
import static lumag.chm.lithtml.LitXMLDecoder.DecodingState.TAG_CUSTOM_LEN;
import static lumag.chm.lithtml.LitXMLDecoder.DecodingState.TAG_NAME;
import static lumag.chm.lithtml.LitXMLDecoder.DecodingState.TEXT;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

import lumag.util.BasicReader;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class LitXMLDecoder {
	public enum LitXMLType {
		META{
			ITag[] getTags() {
				return MetaTags.values();
			}
		},
		HTML{
			ITag[] getTags() {
				return HtmlTags.values();
			}
		},

		;
		abstract ITag[] getTags();
	}
	enum DecodingState {
		TEXT,
		FLAG,
		TAG_NAME,
		TAG_CUSTOM_LEN,
		TAG_CUSTOM,
		ATTR_NAME,
		ATTR_CUSTOM_LEN,
		ATTR_CUSTOM,
		ATTR_VALUE_LEN,
		ATTR_VALUE,
		ATTR_VALUE_NUMBER,
	}
	
	private static class CustomTag implements ITag {
		private String name;

		CustomTag(String s) {
			this.name = s;
		}

		public String getAttribute(int num) {
			return null;
		}

		@Override
		public String toString() {
			return name;
		}
		
		
	}

	@SuppressWarnings("unused")
	private static final int FLAGS_OPEN = 0x01;
	@SuppressWarnings("unused")
	private static final int FLAGS_CLOSE = 0x02;
	@SuppressWarnings("unused")
	private static final int FLAGS_BLOCK = 0x04;
	@SuppressWarnings("unused")
	private static final int FLAGS_HEAD = 0x08;

	@SuppressWarnings("null")
	public void decode(BasicReader reader, LitXMLType type, ContentHandler handler) throws IOException, SAXException {
		int flags = 0;
		Deque<ITag> tagStack = new ArrayDeque<ITag>();
		String attributeName = null;
		int val_len = 0;

		DecodingState state = TEXT;
		StringBuilder builder = new StringBuilder();
		
		Map<String, String> attributes = null;

		ITag[] tags = type.getTags();

		handler.startDocument();

		while (!reader.isEOF()) {
			int ucs32 = reader.readUtf8Char();
//			System.err.format("%16s: %d%n", state, ucs32);

			switch (state) {
			default:
				throw new InternalError("Unhandled state: " + state);
			case TEXT:
				if (ucs32 == 0) {
					if (builder.length() != 0) {
//						System.err.format("Text: '%s'%n", builder.toString());
						char[] ch = builder.toString().toCharArray();
						handler.characters(ch, 0, ch.length);
					}
					builder = null;
					state = FLAG;
				} else {
					for (char c: Character.toChars(ucs32)) {
						builder.append(c);
					}
				}
				break;
			case FLAG:
				flags = ucs32;
//				System.err.print("Flags: ");
//				if ((flags & FLAGS_OPEN) != 0) {
//					System.err.print("open ");
//				}
//				if ((flags & FLAGS_CLOSE) != 0) {
//					System.err.print("close ");
//				}
//				if ((flags & FLAGS_BLOCK) != 0) {
//					System.err.print("block ");
//				}
//				if ((flags & FLAGS_HEAD) != 0) {
//					System.err.print("head ");
//				}
//				System.err.println();
				if ((flags & ~0xf) != 0) {
					System.err.print("FLAG 0x" + Integer.toHexString((flags & ~0xf)));
				}
				state = TAG_NAME;
				break;
			case TAG_NAME:
				if (ucs32 == 0) {
					handler.endElement("", "", tagStack.pop().toString());

					state = TEXT;
					builder = new StringBuilder();
				} else if (ucs32 == 0x8000){
					state = TAG_CUSTOM_LEN; 
				} else {
					tagStack.push(tags[ucs32]);
//					System.err.format("Tag: %s%n", tagStack.peek());
					attributes = new LinkedHashMap<String, String>();
					state = ATTR_NAME;
				}
				break;
			case TAG_CUSTOM_LEN:
//				System.err.format("CustomLen: %x%n", ucs32);
				val_len = ucs32 - 1;
				builder = new StringBuilder();
				state = TAG_CUSTOM;
				break;
			case TAG_CUSTOM:
				builder.append(Character.toChars(ucs32));
				val_len --;
				if (val_len == 0) {
					tagStack.push(new CustomTag(builder.toString()));
					builder = null;
//					System.err.format("Tag: %s%n", tagStack.peek());
					attributes = new LinkedHashMap<String, String>();
					state = ATTR_NAME;
				}
				break;
			case ATTR_NAME:
				if (ucs32 == 0) {
					AttributesImpl atts = new AttributesImpl();
					for (Map.Entry<String, String> a: attributes.entrySet()) {
						atts.addAttribute("", "", a.getKey(), "CDATA", a.getValue());
					}
					attributes  = null;
					handler.startElement("", "", tagStack.peek().toString(), atts);
					if ((flags & FLAGS_CLOSE) != 0) {
						handler.endElement("", "", tagStack.pop().toString());
					}
					state = TEXT;
					builder = new StringBuilder();
				} else if (ucs32 == 0x8000){
					state = ATTR_CUSTOM_LEN;
				} else {
//					System.err.format("Attr: %s %x%n", tag, ucs32);
					attributeName = tagStack.peek().getAttribute(ucs32);

					if (attributeName == null) {
//						System.err.println("Warning: for tag '" + tagStack.peek() + "' unknown attribute " + Integer.toHexString(ucs32));
						attributeName = "_0x" + Integer.toHexString(ucs32) + "_";
					}
					state = ATTR_VALUE_LEN;
				}
				break;
			case ATTR_CUSTOM_LEN:
//				System.err.format("AttrCustomLen: %x%n", ucs32);
				val_len = ucs32 - 1;
				builder = new StringBuilder();
				state = ATTR_CUSTOM;
				break;
			case ATTR_CUSTOM:
				val_len --;
				builder.append(Character.toChars(ucs32));
				if (val_len == 0) {
					attributeName = builder.toString();
					builder = null;
//					System.err.format("AttrCustom: %s %s%n", tag, attributeName);
					state = ATTR_VALUE_LEN;
				}
				break;
			case ATTR_VALUE_LEN:
//				System.err.format("AttrValLen: %x%n", ucs32);
				if (ucs32 == 0xffff) {
					state = ATTR_VALUE_NUMBER;
				} else {
					val_len = ucs32 - 1;
					builder = new StringBuilder();
					if (val_len == 0) {
						state = ATTR_NAME;
					} else {
						state = ATTR_VALUE;
					}
				}
				break;
			case ATTR_VALUE:
				val_len --;
				builder.append(Character.toChars(ucs32));
				if (val_len == 0) {
					String value = builder.toString();
					builder = null;
					attributes.put(attributeName, value);
					if (attributeName.charAt(0) == '_') {
						System.err.format("Tag '%s' unknown attribute '%s' value '%s'%n",
								tagStack.peek(),
								attributeName.substring(1, attributeName.length() - 1),
								value);
					}
					attributeName = null;
//					System.err.format("AttrVal: %s%n", value);
					state = ATTR_NAME;
				}
				break;
			case ATTR_VALUE_NUMBER:
//				System.err.format("AttrVal: %x%n", ucs32);
				attributes.put(attributeName, Integer.toString(ucs32));
				if (attributeName.charAt(0) == '_') {
					System.err.format("Tag '%s' unknown attribute '%s' value '%d'%n",
							tagStack.peek(),
							attributeName.substring(1, attributeName.length() - 1),
							ucs32);
				}
				attributeName = null;
				state = ATTR_NAME;
				break;
			}
		}
	}

}
