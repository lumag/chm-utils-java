package lumag.chm;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class CHMReader {
	private static final String FILE_RESET_TABLE = "::DataSpace/Storage/MSCompressed/Transform/{7FC28940-9D31-11D0-9B27-00A0C91E9C7C}/InstanceData/ResetTable";
	private static final String FILE_NAME_LIST = "::DataSpace/NameList";
	private static final String FILE_CONTROL_DATA = "::DataSpace/Storage/MSCompressed/ControlData";
	private static final String FILE_CONTENT = "::DataSpace/Storage/MSCompressed/Content";

	private static final byte[] FILE_HEADER = {'I', 'T', 'S', 'F'};
	private static final byte[] INDEX_SECTION = {'I', 'T', 'S', 'P'};
	private static final byte[] FILE_SECTION = {(byte) 0xfe, 0x01, 0x00, 0x00};
	private static final byte[] PMGL_HEADER = {'P', 'M', 'G', 'L'};
	private static final byte[] LZXC_HEADER = {'L', 'Z', 'X', 'C'};
	
	private class ListingEntry {
		public final String name;
		public final int section;
		public final long offset;
		public final long length;

		public ListingEntry(final String name, final int section, final long offset, final long length) {
			this.name = name;
			this.section = section;
			this.offset = offset;
			this.length = length;
		}
		
		@Override
		public String toString() {
			return "File '" + name + "'" +
				" is at section " + section +
				" offset " + offset +
				" length " + length; 
		}
	}

	private RandomAccessFile input;

	private long dataOffset;
	private long fileSize;
	private long fileSectionOffset;
	private long fileSectionLength;
	private long indexSectionOffset;
	private long indexSectionLength;
	private int directoryBlockSize;
	private int directoryChunks;
	private long directoryOffset;
	private Map<String, ListingEntry> listing = new LinkedHashMap<String, ListingEntry>();
	private String[] sectionNames;
	private long[] resets;
	private int resetBlockInterval;
	private int resetInterval;
	private int windowSize;
	private long uncompressedLength;
	private long compressedLength;

	public CHMReader(String name) throws IOException {
		System.out.println("CHM file " + name);
		input = new RandomAccessFile(name, "r");
	}

	public static void main(String[] args) {
		for (String name: args) {
			try {
				CHMReader reader = new CHMReader(name);
				reader.read();
				reader.decodeContent("/tmp/test");
//				reader.dump("test");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	@SuppressWarnings("unused")
	private void dump(String path) throws IOException {
		File parent = new File(path);
		parent.mkdirs();
		for (ListingEntry entry: listing.values()) {
			File f = new File(parent, entry.name);
			System.out.println(entry.name + ": " + entry.section + " @ " + entry.offset + " = " + entry.length);
			if (entry.name.charAt(entry.name.length() - 1) == '/') {
				f.mkdir();
			} else if (entry.section == 0) {
				if (entry.name.charAt(0) == ':') {
					// XXX: skip it after debugging is finished
					// system file
					f.getParentFile().mkdirs();
				}
				OutputStream output = new BufferedOutputStream(new FileOutputStream(f));
				input.seek(dataOffset + entry.offset);
				long len = entry.length;
				byte buf[] = new byte[1024];
				while (len > 0) {
					int read = input.read(buf, 0,
							(int) (len > buf.length?
									buf.length:
									len));

					if (read == -1) {
						output.close();
						throw new IOException("read returned -1");
					}
					output.write(buf, 0, read);
					len -= read;
				}
				output.close();
			}
		}
	}

	private long readQword() throws IOException {
		byte[] data = new byte[8];
		
		input.read(data);
		
		return	
				((data[0] & 0xff) << (0 * 8)) | 
				((data[1] & 0xff) << (1 * 8)) |
				((data[2] & 0xff) << (2 * 8)) |
				((data[3] & 0xff) << (3 * 8)) | 
				((data[4] & 0xff) << (4 * 8)) |
				((data[5] & 0xff) << (5 * 8)) |
				((data[6] & 0xff) << (6 * 8)) |
				((data[7] & 0xff) << (7 * 8));
	}
	
	private int readDword() throws IOException {
		byte[] data = new byte[4];
		
		input.read(data);
		
		return	
				((data[0] & 0xff) << (0 * 8)) | 
				((data[1] & 0xff) << (1 * 8)) |
				((data[2] & 0xff) << (2 * 8)) |
				((data[3] & 0xff) << (3 * 8));
	}
	
	private short readWord() throws IOException {
		byte[] data = new byte[2];
		
		input.read(data);
		
		return	(short) (
				((data[0] & 0xff) << (0 * 8)) | 
				((data[1] & 0xff) << (1 * 8)));
	}
	
	private long readCWord() throws IOException {
		long result = 0;

		int b;
		do {
			b = input.read();
			result = (result << 7) | (b & 0x7f);
		} while ((b & 0x80) != 0);
		return result;
	}

	private String readString() throws IOException, FileFormatException {
		int len = (int) readCWord();
		byte[] buf = new byte[len];
		input.read(buf);
		
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

	private char getChar(int b) {
		int temp = b & 0xf;
		return (char) (temp < 10 ? temp + '0' : temp - 10 + 'A');
	}
	
	private String readGUID() throws IOException {
		StringBuilder builder = new StringBuilder();
		
		builder.append('{');
		int dw = readDword();
		for (int i = 7; i >= 0; i --) {
			builder.append(getChar(dw >> (4*i)));
		}

		builder.append('-');

		short w1 = readWord();
		for (int i = 3; i >= 0; i --) {
			builder.append(getChar(w1 >> (4*i)));
		}

		builder.append('-');

		short w2 = readWord();
		for (int i = 3; i >= 0; i --) {
			builder.append(getChar(w2 >> (4*i)));
		}

		builder.append('-');

		for (int i = 0; i < 4; i++) {
			int b = input.read();
			builder.append(getChar(b >> 4));
			builder.append(getChar(b >> 0));
		}

		builder.append('-');

		for (int i = 0; i < 4; i++) {
			int b = input.read();
			builder.append(getChar(b >> 4));
			builder.append(getChar(b >> 0));
		}

		builder.append('}');

//		System.out.println(builder.toString());
		return builder.toString();
	}

	private void checkHeader(byte[] expected) throws IOException, FileFormatException {
		byte[] header = new byte[expected.length];
		input.read(header);
		
		if (!Arrays.equals(header, expected)) {
			throw new FileFormatException("bad file header");
		}
	}

	private void read() throws IOException, FileFormatException {
		readFormatHeader();
		readFileSizeSection();
		readIndexSection();
		readNameList();
		
		readResetTable();
		readControlData();
	}

	private void readFormatHeader() throws IOException, FileFormatException {
		input.seek(0);

		checkHeader(FILE_HEADER);
		
		int version = readDword();
		System.out.println("ITSF Version: " + version);
		if (version != 3 && version != 2) {
			throw new FileFormatException("ITSF Version " + version + " is unsupported");
		}

		int headerLen = readDword();
//		System.out.println("Header len: " + headerLen);
		if ((version == 2 && headerLen != 0x58) || 
			(version == 3 && headerLen != 0x60)) {
			throw new FileFormatException("bad section length");
		}
		
		readDword(); // 1
		
		readDword(); // time
		
		readDword(); // LCID
		
		readGUID();
		readGUID();
		
		fileSectionOffset = readQword();
		fileSectionLength = readQword();
		indexSectionOffset = readQword();
		indexSectionLength = readQword();
		
		if (version == 3) {
			dataOffset = readQword();
		} else {
			dataOffset = indexSectionOffset + indexSectionLength;
		}
	}
	
	private void readFileSizeSection() throws FileFormatException, IOException {
		input.seek(fileSectionOffset);
		checkHeader(FILE_SECTION);

		if (fileSectionLength < 0x18) {
			throw new FileFormatException("FileSize section is too small");
		} else if (fileSectionLength > 0x18) {
			System.out.format("Warning: extra %d bytes at the end of FileSize section%n", fileSectionLength - 0x18);
		}

		
		int unk = readDword(); // mostly 0. One file with 1
		if (unk != 0) {
			System.out.println("Warning: unknown element expected to be zero: " + unk);
		}
		fileSize = readQword();
		if (fileSize != input.length()) {
			// TODO: support truncated files
			throw new FileFormatException("Bad file size : "
					+ fileSize + " != " + input.length());
		}

		readDword(); // 0
		readDword(); // 0

	}

	private void readIndexSection() throws IOException, FileFormatException {
		input.seek(indexSectionOffset);

		checkHeader(INDEX_SECTION);
		
		int version = readDword();
		System.out.println("ITSP Version: " + version);
		if (version != 1) {
			throw new FileFormatException("ITSP Version " + version + " is unsupported");
		}
		
		int headerLen = readDword();
		if (headerLen != 0x54) {
			throw new FileFormatException("bad section length");
		}
		
		directoryOffset = indexSectionOffset + headerLen;
		
		int unk = readDword();
		if (unk != 0xa) {
			System.out.format("Expected 0x0a for unknown data, got 0x%x%n", unk);
		}
		
		directoryBlockSize = readDword();
		/*int quickRefDensity = */readDword();
		/*int indexTreeDepth = */readDword();
		int rootIndexChunk = readDword();
		int firstListChunk = readDword();
		int lastListChunk = readDword();
		int unk2 = readDword(); // -1
		if (unk2 != -1) {
			System.out.format("Expected -1 for unk2, got 0x%08x%n", unk2);
		}
		directoryChunks = readDword();
		
//		System.out.format("%x: %d %d %d %d%n", directoryBlockSize, rootIndexChunk, firstListChunk, lastListChunk, directoryChunks);

		readDword(); // lcid
		readGUID(); // guid

		int len2 = readDword();
		if (len2 != headerLen) {
			System.out.format("Bad second length: expected 0x%x, got 0x%x", headerLen, len2);
		}
		int unk3 = readDword(); // -1
		if (unk3 != -1) {
			System.out.format("Expected -1 for unk3, got 0x%08x%n", unk3);
		}
		int unk4 = readDword(); // -1
		if (unk4 != -1) {
			System.out.format("Expected -1 for unk4, got 0x%08x%n", unk4);
		}
		int unk5 = readDword(); // -1
		if (unk5 != -1) {
			System.out.format("Expected -1 for unk5, got 0x%08x%n", unk5);
		}
		
		for (int chunk = 0; chunk < directoryChunks; chunk++) {
			if (chunk == rootIndexChunk) {
				readIndex(chunk);
			} else if (chunk >= firstListChunk && chunk <= lastListChunk) {
				readListing(chunk);
			} else {
				byte[] header = new byte[4];
				input.read(header);
				char[] cheader = new char[4];
				for (int j = 0; j < 4; j++) {
					cheader[j] = (char) header[j];
				}
				System.out.println("unknown directory chunk: " + chunk + Arrays.toString(cheader));
				input.skipBytes(directoryBlockSize - 4);
			}
		}
	}

	private void readListing(int chunk) throws IOException, FileFormatException {
		checkHeader(PMGL_HEADER);
		int freeSpace = readDword();
		long endPos = directoryOffset + (chunk + 1)* directoryBlockSize - freeSpace; 

		int unk = readDword();
		if (unk != 0) {
			System.out.format("Expected 0x0 for unknown data, got 0x%x%n", unk);
		}

		/*int previousChunk = */readDword();
		/*int nextChunk = */readDword();
		
		//System.out.println(previousChunk + " <-> " + nextChunk);
		
		while (input.getFilePointer() < endPos) {
			String name = readString();
			int section = (int) readCWord();
			long offset = readCWord();
			long len = readCWord();
			listing.put(name, new ListingEntry(name, section, offset, len));
//			System.out.println(name);
		}

		input.skipBytes(freeSpace);
	}

	private void readIndex(int chunk) throws IOException {
		// just skip bytes. We don't use index
		input.skipBytes(directoryBlockSize);
	}

	private void readNameList() throws IOException, FileFormatException {
		ListingEntry entry = listing.get(FILE_NAME_LIST);

		input.seek(dataOffset + entry.offset);

		short len = readWord();
		if (len * 2 != entry.length) {
			throw new FileFormatException("Incorrect " + FILE_NAME_LIST + " length");
		}
		
		short entries = readWord();
		if (entries != 2) {
			System.out.println("Warning: more than two compression sections");
		}
		
		sectionNames = new String[entries];
		
		for (int i = 0; i < entries; i++) {
			short nameLen = readWord();
			char[] name = new char[nameLen];
			for (int j = 0; j < nameLen; j++) {
				name[j] = (char) readWord();
			}
			readWord(); // terminal zero
			sectionNames[i] = new String(name);
		}
		System.out.println(Arrays.toString(sectionNames));
	}
	
	private void readResetTable() throws FileFormatException, IOException {
		ListingEntry entry = listing.get(FILE_RESET_TABLE);
		if (entry == null || entry.section != 0 || entry.length < 0x30) {
			throw new FileFormatException("Bad ResetTable file");
		}
		
		input.seek(dataOffset + entry.offset);
		int version = readDword();
		System.out.println("ResetTable Version: " + version);
		if (version != 2) {
			throw new FileFormatException("ResetTable Version " + version + " is unsupported");
		}
		
		int resetNum = readDword();
		System.out.println(resetNum + " entries in ResetTable");
		resets = new long[resetNum];

		int entrySize = readDword();
		if (entrySize != 8) {
			throw new FileFormatException("Size of ResetTable entry isn't 8 (" + entrySize + ")");
		}
		
		int headerSize = readDword();
		if (headerSize != 0x28) {
			throw new FileFormatException("Unsupported ResetTable header size: " + headerSize);
		}
		
		uncompressedLength = readQword();
		compressedLength = readQword();
		
		System.out.format("Content compression 0x%x -> 0x%x%n", uncompressedLength, compressedLength);
		
		long blockSize = readQword();
		if (blockSize != 0x8000) {
			throw new FileFormatException("BlockSize isn't 0x8000: " + blockSize);
		}
		
		for (int i = 0; i < resetNum; i++) {
			resets[i] = readQword();
		}
	}
	
	private void readControlData() throws FileFormatException, IOException {
		ListingEntry entry = listing.get(FILE_CONTROL_DATA);
		if (entry == null || entry.section != 0 || entry.length < 0x1c) {
			throw new FileFormatException("Bad ControlData file: " + entry);
		}
		
		input.seek(dataOffset + entry.offset);
		
		int size = readDword();
		if (size != 6) {
			throw new FileFormatException("Bad size of ControlData: " + size);
		}
		
		checkHeader(LZXC_HEADER);
		
		int version = readDword();
		System.out.println("ControlData Version: " + version);
		if (version != 2) {
			throw new FileFormatException("ControlData Version " + version + " is unsupported");
		}
		
		resetInterval = readDword();
		windowSize = readDword();
		int cacheSize = readDword();
		
		System.out.println("Resets parameters: " + resetInterval + ", " + windowSize + ", " + cacheSize);
		if (resetInterval % (windowSize / 2) != 0) {
			throw new FileFormatException("Unsupported reset interval value");
		}

		int unk = readDword();
		if (unk != 0) {
			System.out.println("Warning: unknown element expected to be zero: " + unk);
		}
		
		if (version == 2) {
			resetInterval *= 0x8000;
			windowSize *= 0x8000;
		}
		
		resetBlockInterval = resetInterval / (windowSize / 2) * cacheSize;
		System.out.println("ResetBlockInterval: " + resetBlockInterval);
	}

	@SuppressWarnings("unused")
	private void decodeContent(String fname) throws IOException, FileFormatException {
		ListingEntry entry = listing.get(FILE_CONTENT);
		
		input.seek(dataOffset + entry.offset);
		OutputStream output = new BufferedOutputStream(new FileOutputStream(fname));
		LZXDecompressor d = new LZXDecompressor(windowSize); 

//		final long maxBlock = (uncompressedLength + 0x8000 - 1)/ 0x8000;
		final long maxBlock = resets.length;
		for (int i = 0; i < maxBlock ; i++) {
			if (i % resetBlockInterval == 0) {
				d.reset(false);
			}

			int compBlockLen;
			int uncompBlockLen;
			if (i != maxBlock-1) {
				compBlockLen = (int) (resets[i+1] - resets[i]);
				uncompBlockLen = 0x8000;
			} else {
				compBlockLen = (int) (compressedLength - resets[i]);
				uncompBlockLen = (int) (uncompressedLength % 0x8000);
			}
//			compBlockLen = uncompBlockLen + 6144;
			System.out.format("Decode %d %04x %04x%n", i, compBlockLen, uncompBlockLen);
			d.decode(input, compBlockLen, output, uncompBlockLen);
			
		}
		output.close();
	}


}
