package lumag.chm.lithtml;

import static lumag.chm.lithtml.HtmlDecoder.DecodingState.ATTR_CUSTOM;
import static lumag.chm.lithtml.HtmlDecoder.DecodingState.ATTR_CUSTOM_LEN;
import static lumag.chm.lithtml.HtmlDecoder.DecodingState.ATTR_NAME;
import static lumag.chm.lithtml.HtmlDecoder.DecodingState.ATTR_VALUE;
import static lumag.chm.lithtml.HtmlDecoder.DecodingState.ATTR_VALUE_LEN;
import static lumag.chm.lithtml.HtmlDecoder.DecodingState.ATTR_VALUE_NUMBER;
import static lumag.chm.lithtml.HtmlDecoder.DecodingState.FLAG;
import static lumag.chm.lithtml.HtmlDecoder.DecodingState.TAG_CUSTOM;
import static lumag.chm.lithtml.HtmlDecoder.DecodingState.TAG_CUSTOM_LEN;
import static lumag.chm.lithtml.HtmlDecoder.DecodingState.TAG_NAME;
import static lumag.chm.lithtml.HtmlDecoder.DecodingState.TEXT;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

import lumag.util.BasicReader;

public class HtmlDecoder {
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

	@SuppressWarnings("unused")
	private static final int FLAGS_OPEN = 0x01;
	@SuppressWarnings("unused")
	private static final int FLAGS_CLOSE = 0x02;
	@SuppressWarnings("unused")
	private static final int FLAGS_BLOCK = 0x04;
	@SuppressWarnings("unused")
	private static final int FLAGS_HEAD = 0x08;

	private final BasicReader reader;
	private final Appendable output;

	public HtmlDecoder(String filename, Appendable output) throws IOException {
		reader = new BasicReader(new RandomAccessFile(filename, "r"));
		this.output = output;
	}

	@SuppressWarnings("null")
	public void decode() throws IOException {
		int flags = 0;
		Deque<String> tagStack = new ArrayDeque<String>();
		String attributeName = null;
		int val_len = 0;

		DecodingState state = TEXT;
		StringBuilder builder = new StringBuilder();
		
		Map<String, String> attributes = null;

		ITag[] tags = HtmlTags.values();
		Map<Integer, String> attrMap = tags[0].getCommonAttributes();
		Map<Integer, String> tagAttrMap = null;

		while (true) {
			int ucs32 = reader.readUtf8Char();
//			System.err.format("%16s: %d%n", state, ucs32);

			switch (state) {
			default:
				throw new IllegalStateException("State is unsupported: " + state);
			case TEXT:
				if (ucs32 == 0) {
					if (builder.length() != 0) {
						output.append(builder.toString());
//						System.err.format("Text: '%s'%n", builder.toString());
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
					output.append("</");
					output.append(tagStack.pop());
					output.append('>');

					state = TEXT;
					builder = new StringBuilder();
				} else if (ucs32 == 0x8000){
					state = TAG_CUSTOM_LEN; 
				} else {
					tagStack.push(tags[ucs32].toString());
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
					tagStack.push(builder.toString());
					builder = null;
//					System.err.format("Tag: %s%n", tagStack.peek());
					attributes = new LinkedHashMap<String, String>();
					state = ATTR_NAME;
				}
				break;
			case ATTR_NAME:
				if (ucs32 == 0) {
					output.append('<');
					output.append(tagStack.peek());
					for (Map.Entry<String, String> a: attributes.entrySet()) {
						output.append(' ');
						output.append(a.getKey());
						output.append("=\"");
						output.append(a.getValue());
						output.append('"');
					}
					attributes  = null;
					if ((flags & FLAGS_CLOSE) != 0) {
						output.append(" /");
						tagStack.pop();
					}
					output.append(">");
					state = TEXT;
					builder = new StringBuilder();
				} else if (ucs32 == 0x8000){
					state = ATTR_CUSTOM_LEN;
				} else {
					if (tagAttrMap != null) {
						attributeName = tagAttrMap.get(ucs32);
					}

					if (attributeName == null) {
						attributeName = attrMap.get(ucs32);						
					}

					if (attributeName == null) {
						System.err.println("Warning: for tag '" + tagStack.peek() + "' unknown attribute " + Integer.toHexString(ucs32));
						attributeName = "_0x" + Integer.toHexString(ucs32) + "_";
					}
//					System.err.format("Attr: %s %x%n", tag, ucs32);
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
					attributeName = null;
//					System.err.format("AttrVal: %s%n", value);
					state = ATTR_NAME;
				}
				break;
			case ATTR_VALUE_NUMBER:
//				System.err.format("AttrVal: %x%n", ucs32);
				attributes.put(attributeName, Integer.toString(ucs32));
				attributeName = null;
				state = ATTR_NAME;
				break;
			}
		}
	}

	public static void main(String[] args) throws Exception {
		new HtmlDecoder(args[0], System.out).decode();
	}

}