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

import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;

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

	private static final int FLAGS_OPEN = 0x01;
	private static final int FLAGS_CLOSE = 0x02;

	private BasicReader reader;
	private Appendable output;

	public HtmlDecoder(String filename, Appendable output) throws IOException {
		reader = new BasicReader(new RandomAccessFile(filename, "r"));
		this.output = output;
	}

	@SuppressWarnings("null")
	public void decode() throws IOException {
		int flags;
		String tag = null;
		int val_len = 0;

		DecodingState state = TEXT;
		StringBuilder builder = new StringBuilder();

		Enum<?>[] tags = MetaTags.values();

		while (true) {
			int ucs32 = reader.readUtf8Char();
			System.out.format("%16s: %d%n", state, ucs32);

			switch (state) {
			default:
				throw new IllegalStateException("State is unsupported: " + state);
			case TEXT:
				if (ucs32 == 0) {
					System.out.format("Text: '%s'%n", builder.toString());
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
				System.out.format("Flags: %x%n", flags);
				state = TAG_NAME;
//				if ((flags & ~0x3) != 0) {
//				throw new RuntimeException("Bad flags value: " + (flags & ~0x3));
//				}
				break;
			case TAG_NAME:
				if (ucs32 == 0) {
					state = TEXT;
					builder = new StringBuilder();
				} else if (ucs32 == 0x8000){
					state = TAG_CUSTOM_LEN; 
				} else {
					tag = tags[ucs32].toString();
					System.out.format("Tag: %s%n", tag);
					state = ATTR_NAME;
				}
				break;
			case TAG_CUSTOM_LEN:
//				System.out.format("CustomLen: %x%n", ucs32);
				val_len = ucs32 - 1;
				builder = new StringBuilder();
				state = TAG_CUSTOM;
				break;
			case TAG_CUSTOM:
				builder.append(Character.toChars(ucs32));
				val_len --;
				if (val_len == 0) {
					System.out.format("Custom: %s%n", builder.toString());
					tag = builder.toString();
					builder = null;
					state = ATTR_NAME;
				}
				break;
			case ATTR_NAME:
				if (ucs32 == 0) {
					state = TEXT;
					builder = new StringBuilder();
				} else if (ucs32 == 0x8000){
					state = ATTR_CUSTOM_LEN;
				} else {
					System.out.format("Attr: %s %x%n", tag, ucs32);
					state = ATTR_VALUE_LEN;
				}
				break;
			case ATTR_CUSTOM_LEN:
//				System.out.format("AttrCustomLen: %x%n", ucs32);
				val_len = ucs32 - 1;
				builder = new StringBuilder();
				state = ATTR_CUSTOM;
				break;
			case ATTR_CUSTOM:
				val_len --;
				builder.append(Character.toChars(ucs32));
				if (val_len == 0) {
					System.out.format("AttrCustom: %s %s%n", tag, builder.toString());
					builder = null;
					state = ATTR_VALUE_LEN;
				}
				break;
			case ATTR_VALUE_LEN:
//				System.out.format("AttrValLen: %x%n", ucs32);
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
					System.out.format("AttrVal: %s%n", builder.toString());
					builder = null;
					state = ATTR_NAME;
				}
				break;
			case ATTR_VALUE_NUMBER:
				System.out.format("AttrVal: %x%n", ucs32);
				state = ATTR_NAME;
				break;
			}
		}
	}

	public static void main(String[] args) throws Exception {
		FileWriter out = new FileWriter(args[1]);
		try  {
			new HtmlDecoder(args[0], out).decode();
		} finally {
			out.close();
		}
	}

}
