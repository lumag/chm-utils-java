package lumag.util;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Formatter;

public class BasicReader {
	private interface Reader {
		void fill(int len) throws IOException;
		void seek(long pos) throws IOException;
		long offset() throws IOException;
	}

	private final Reader reader;
	private byte[] data;
	private int offset;

	public BasicReader(final RandomAccessFile input) {
		reader = new Reader() {
			public void fill(int len) throws IOException {
				byte[] newData = new byte[len];

				if (offset > 0 && offset < data.length) {
					System.arraycopy(data, offset, newData, 0, data.length - offset);
					input.readFully(newData, data.length - offset, len - (data.length - offset));
				} else {
					input.readFully(newData);
				}
				
				data = newData;
				offset = 0;
			}

			public void seek(long pos) throws IOException {
				input.seek(pos);
				data = null;
				offset = 0;
			}

			public long offset() throws IOException {
				long off = 0;
				if (data != null) {
					off = data.length - offset;
				}
				return input.getFilePointer() - off; 
			}
		};
	}

	public BasicReader(byte[] input) {
		reader = new Reader() {

			public void fill(int len) throws IOException {
				if (BasicReader.this.offset + len > BasicReader.this.data.length) {
					throw new EOFException("Read beyond end of buffer");
				}
			}

			public void seek(long pos) throws IOException {
				if (pos > BasicReader.this.data.length) {
					throw new EOFException("Read beyond end of buffer");
				} else if (pos < 0) {
					throw new IOException("pos less than 0");
				}
			}

			public long offset() throws IOException {
				return BasicReader.this.offset;
			}
			
		};
		data = input;
	}

	private void fill(int len) throws IOException {
		reader.fill(len);
	}

	public long readQWord() throws IOException {
		fill(8);
		return 
			(((long) data[offset ++] & 0xff) << (0 * 8)) | 
			(((long) data[offset ++] & 0xff) << (1 * 8)) |
			(((long) data[offset ++] & 0xff) << (2 * 8)) |
			(((long) data[offset ++] & 0xff) << (3 * 8)) | 
			(((long) data[offset ++] & 0xff) << (4 * 8)) |
			(((long) data[offset ++] & 0xff) << (5 * 8)) |
			(((long) data[offset ++] & 0xff) << (6 * 8)) |
			(((long) data[offset ++] & 0xff) << (7 * 8));
	}


	public int readDWord() throws IOException {
		fill(4);
		return
			((data[offset ++] & 0xff) << (0 * 8)) | 
			((data[offset ++] & 0xff) << (1 * 8)) |
			((data[offset ++] & 0xff) << (2 * 8)) |
			((data[offset ++] & 0xff) << (3 * 8));
	}

	public short readWord() throws IOException {
		fill(2);
		return	(short) (
			((data[offset ++] & 0xff) << (0 * 8)) | 
			((data[offset ++] & 0xff) << (1 * 8)));
	}

	public byte readByte() throws IOException {
		fill(1);
		return data[offset ++];
	}

	public long readCWord() throws IOException {
		long result = 0;
	
		byte b;
		do {
			b = readByte();
			result = (result << 7) | (b & 0x7f);
		} while ((b & 0x80) != 0);
		return result;
	}

	public String readString() throws IOException {
		int len = (int) readCWord();
		if (len < 0) {
			throw new IllegalArgumentException("Incorrect string length");
		}

		fill(len);

		StringBuilder builder = new StringBuilder();
		int ucs32 = 0;
		for (int i = 0, left = 0; i < len; i++) {
			int c = data[offset ++] & 0xff;
			if (left == 0) {
				if ((c & 0x80) == 0) {
					ucs32 = c & 0x7f;
				} else if ((c & 0x40) == 0 || c == 0xff || c == 0xfe) {
					throw new IllegalArgumentException("Bad UTF-8 String!!!");
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
					throw new IllegalArgumentException("Bad UTF-8 String!!!");
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

	public String readGUID() throws IOException {
		Formatter fmt = new Formatter();
		
		int dw = readDWord();
		short w1 = readWord();
		short w2 = readWord();
		byte b1 = readByte();
		byte b2 = readByte();

		fmt.format("{%08X-%04X-%04X-%02X%02X-", dw, w1, w2, b1, b2);

		for (int i = 0; i < 6; i++) {
			byte b = readByte();
			fmt.format("%02X", b);
		}

		fmt.format("}");

		String guid = fmt.toString();
		fmt.close();

//		System.out.println(guid);
		return guid;
	}
	
	public byte[] read(int len) throws IOException {
		fill(len);
		offset += len;
		return Arrays.copyOfRange(data, offset - len, offset);
	}
	
	public void skip(int len) throws IOException {
		fill(len);
		offset += len;
	}
	
	public void seek(long pos) throws IOException {
		reader.seek(pos);
	}

	public long getOffset() throws IOException {
		return reader.offset();
	}
}
