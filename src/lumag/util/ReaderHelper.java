package lumag.util;

import java.io.DataInput;
import java.io.IOException;
import java.util.Arrays;

import lumag.chm.FileFormatException;

public class ReaderHelper {

	public static long readQWord(DataInput input) throws IOException {
		byte[] data = new byte[8];
		
		input.readFully(data);
		
		return getQWord(data, 0);
	}

	public static long getQWord(byte[] data, int offset) {
		return 
			(((long) data[offset + 0] & 0xff) << (0 * 8)) | 
			(((long) data[offset + 1] & 0xff) << (1 * 8)) |
			(((long) data[offset + 2] & 0xff) << (2 * 8)) |
			(((long) data[offset + 3] & 0xff) << (3 * 8)) | 
			(((long) data[offset + 4] & 0xff) << (4 * 8)) |
			(((long) data[offset + 5] & 0xff) << (5 * 8)) |
			(((long) data[offset + 6] & 0xff) << (6 * 8)) |
			(((long) data[offset + 7] & 0xff) << (7 * 8));
	}

	public static int readDWord(DataInput input) throws IOException {
		byte[] data = new byte[4];
		
		input.readFully(data);
		
		return getDWord(data, 0);
	}

	public static int getDWord(byte[] data, int offset) {
		return
			((data[offset + 0] & 0xff) << (0 * 8)) | 
			((data[offset + 1] & 0xff) << (1 * 8)) |
			((data[offset + 2] & 0xff) << (2 * 8)) |
			((data[offset + 3] & 0xff) << (3 * 8));
	}

	public static short readWord(DataInput input) throws IOException {
		byte[] data = new byte[2];
		
		input.readFully(data);
		
		return	(short) (
				((data[0] & 0xff) << (0 * 8)) | 
				((data[1] & 0xff) << (1 * 8)));
	}

	public static long readCWord(DataInput input) throws IOException {
		long result = 0;
	
		int b;
		do {
			b = input.readByte();
			result = (result << 7) | (b & 0x7f);
		} while ((b & 0x80) != 0);
		return result;
	}

	public static String readString(DataInput input) throws IOException, FileFormatException {
		int len = (int) readCWord(input);
		if (len < 0) {
			throw new FileFormatException("Incorrect string length");
		}
		byte[] buf = new byte[len];
		input.readFully(buf);
		
		StringBuilder builder = new StringBuilder();
		int ucs32 = 0;
		for (int i = 0, left = 0; i < len; i++) {
			int c = buf[i] & 0xff;
			if (left == 0) {
				if ((c & 0x80) == 0) {
					ucs32 = c & 0x7f;
				} else if ((c & 0x40) == 0 || c == 0xff || c == 0xfe) {
					throw new FileFormatException("Bad UTF-8 String!!!");
				} else if ((c & 0x20) == 0) {
					left = 1;
					ucs32 = c & 0x1f;
				} else if ((c & 0x10) == 0) {
					left = 2;
					ucs32 = c & 0x0f;
				} else if ((c & 0x08) == 0) {
					left = 3;
					ucs32 = c & 0x07; 
				} else if ((c & 0x04) == 0) {
					left = 4;
					ucs32 = c & 0x03;
				} else if ((c & 0x02) == 0) {
					left = 5;
					ucs32 = c & 0x01;
				}
			} else { // left != 0
				left --;
				if ((c & 0xc0) != 0x80) {
					throw new FileFormatException("Bad UTF-8 String!!!");
				}
				ucs32 = (ucs32 << 6) | (c & 0x3f);
			}
			
			if (left == 0) {
				builder.append(Character.toChars(ucs32));
			}
		}
		String str = builder.toString();
		return str;
	}

	private static char getChar(int b) {
		int temp = b & 0xf;
		return (char) (temp < 10 ? temp + '0' : temp - 10 + 'A');
	}

	public static String readGUID(DataInput input) throws IOException {
			StringBuilder builder = new StringBuilder();
			
			builder.append('{');
			int dw = readDWord(input);
			for (int i = 7; i >= 0; i --) {
				builder.append(getChar(dw >> (4*i)));
			}
	
			builder.append('-');
	
			short w1 = readWord(input);
			for (int i = 3; i >= 0; i --) {
				builder.append(getChar(w1 >> (4*i)));
			}
	
			builder.append('-');
	
			short w2 = readWord(input);
			for (int i = 3; i >= 0; i --) {
				builder.append(getChar(w2 >> (4*i)));
			}
	
			builder.append('-');
	
			for (int i = 0; i < 4; i++) {
				int b = input.readByte();
				builder.append(getChar(b >> 4));
				builder.append(getChar(b >> 0));
			}
	
			builder.append('-');
	
			for (int i = 0; i < 4; i++) {
				int b = input.readByte();
				builder.append(getChar(b >> 4));
				builder.append(getChar(b >> 0));
			}
	
			builder.append('}');
	
	//		System.out.println(builder.toString());
			return builder.toString();
		}

	public static void checkHeader(DataInput input, byte[] expected) throws IOException, FileFormatException {
		byte[] header = new byte[expected.length];
		input.readFully(header);
		
		if (!Arrays.equals(header, expected)) {
			throw new FileFormatException("bad file header");
		}
	}

}