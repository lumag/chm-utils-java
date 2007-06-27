package lumag.chm;

import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;

import lumag.util.BasicReader;

import static lumag.chm.HtmlDecoder.DecodingState.*;

public class HtmlDecoder {
	enum DecodingState {
		TEXT,
		FLAG,
		TAG,
		ATTR_NAME,
		ATTR_VAL_LEN,
		ATTR_VALUE,
	};
	private BasicReader reader;
	private Appendable output;

	public HtmlDecoder(String filename, Appendable output) throws IOException {
		reader = new BasicReader(new RandomAccessFile(filename, "r"));
		this.output = output;
	}

	public void decode() throws IOException {
		DecodingState state = TEXT;
		int flags;
		int tag;
		int val_len = 0;

		while (true) {
			int ucs32 = reader.readUtf8Char();
			
			switch (state) {
			default:
				throw new IllegalStateException("State is unsupported: " + state);
			case TEXT:
				if (ucs32 == 0) {
					state = FLAG;
				} else {
					for (char c: Character.toChars(ucs32)) {
						output.append(c);
					}
				}
				break;
			case FLAG:
				if (ucs32 == 0) {
					state = TEXT;
				} else {
					flags = ucs32;
					state = TAG;
					System.out.format("Flags: %x%n", flags);
				}
				break;
			case TAG:
				if (ucs32 == 0) {
					state = TEXT;
				} else {
					System.out.format("Tag: %x%n", ucs32);
					tag = ucs32;
					state = ATTR_NAME;
				}
				break;
			case ATTR_NAME:
				if (ucs32 == 0) {
					state = TEXT;
				} else {
					System.out.format("Attr: %x%n", ucs32);
					state = ATTR_VAL_LEN;
				}
				break;
			case ATTR_VAL_LEN:
				System.out.format("AttrValLen: %x%n", ucs32);
				val_len = ucs32 - 1;
				state = ATTR_VALUE;
				break;
			case ATTR_VALUE:
				if (ucs32 == 0) {
					state = TEXT;
				} else {
					System.out.format("AttrVal: %x%n", ucs32);
					if (val_len == 0xfffe) {
						// number;
						state = ATTR_NAME;
					} else {
						val_len --;
						if (val_len == 0) {
							state = ATTR_NAME;
						}
					}
				}
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
