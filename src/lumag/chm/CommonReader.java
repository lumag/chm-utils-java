package lumag.chm;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import lumag.util.ReaderHelper;

abstract class CommonReader extends ReaderHelper {

	private static final byte[] HEADER_FILE = {'I', 'T', 'S', 'F'};
	private static final byte[] HEADER_FILE_SECTION = {(byte) 0xfe, 0x01, 0x00, 0x00};
	protected static final int SECTION_FILE_SIZE = 0;
	protected static final int SECTION_INDEX = 1;
	private static final String FILE_NAME_LIST = "::DataSpace/NameList";

	@SuppressWarnings("unused")
	private long fileSize;
	protected long dataOffset;
	
	private long[] sectionOffsets;
	private long[] sectionLengths;
	protected Map<String, ListingEntry> listing = new LinkedHashMap<String, ListingEntry>();
	private String[] sectionNames;

	protected void readFormatHeader(RandomAccessFile input) throws IOException, FileFormatException {
		checkHeader(input, HEADER_FILE);
		
		int version = readDWord(input);
		System.out.println("ITSF Version: " + version);
		if (version != 4 && version != 3 && version != 2) {
			throw new FileFormatException("ITSF Version " + version + " is unsupported");
		}

		int headerLen = readDWord(input);
//		System.out.println("Header len: " + headerLen);
		if ((version == 2 && headerLen != 0x58) || 
			(version == 3 && headerLen != 0x60) ||
			(version == 4 && headerLen != 0x20)) {
			throw new FileFormatException("bad section length");
		}
		
		readDWord(input); // 1
		
		if (version == 4) {
			dataOffset = readQWord(input);
		}
		
		readDWord(input); // time
		
		readDWord(input); // LCID
		
		if (version == 2 || version == 3) {
			readGUID(input);
			readGUID(input);
		
		
			readSectionTable(input, 2);
		}
		
		if (version == 2) {
			dataOffset = sectionOffsets[SECTION_INDEX] + sectionLengths[SECTION_INDEX];
		} else if (version == 3) {
			dataOffset = readQWord(input);
		}
	}

	protected void readSectionTable(RandomAccessFile input, int size) throws IOException {
		sectionOffsets = new long[size];
		sectionLengths = new long[size];
		
		for (int i = 0; i < size; i++) {
			sectionOffsets[i] = readQWord(input);
			sectionLengths[i] = readQWord(input);
		}
		
		System.out.println("Offsets: " + Arrays.toString(sectionOffsets));
		System.out.println("Lengths: " + Arrays.toString(sectionLengths));
	}

	protected long getSectionOffset(int section) {
		return sectionOffsets[section];
	}

	protected long getSectionLengths(int section) {
		return sectionLengths[section];
	}

	protected void readFileSizeSection(RandomAccessFile input) throws FileFormatException, IOException {
		input.seek(sectionOffsets[SECTION_FILE_SIZE]);
		checkHeader(input, HEADER_FILE_SECTION);
	
		if (sectionLengths[SECTION_FILE_SIZE] < 0x18) {
			throw new FileFormatException("FileSize section is too small");
		} else if (sectionLengths[SECTION_FILE_SIZE] > 0x18) {
			System.out.format("Warning: extra %d bytes at the end of FileSize section%n", sectionLengths[SECTION_FILE_SIZE] - 0x18);
		}
	
		int unk = readDWord(input); // mostly 0. One file with 1
		if (unk != 0) {
			System.out.println("Warning: unknown element expected to be zero: " + unk);
		}
		fileSize = readQWord(input);
		System.out.println("Expected file size: " + fileSize);
	
		readDWord(input); // 0
		readDWord(input); // 0
	
	}

	protected void readListingEntries(RandomAccessFile input, long endPos, Map<String, ListingEntry> list) throws IOException, FileFormatException {
			while (input.getFilePointer() < endPos) {
				String name = readString(input);
				int section = (int) readCWord(input);
				long offset = readCWord(input);
				long len = readCWord(input);
				list.put(name, new ListingEntry(name, section, offset, len));
	//			System.out.println(name);
			}
		}

	protected void readNameList(RandomAccessFile input) throws IOException, FileFormatException {
		ListingEntry entry = listing.get(FILE_NAME_LIST);
	
		input.seek(dataOffset + entry.offset);
	
		short len = readWord(input);
		if (len * 2 != entry.length) {
			throw new FileFormatException("Incorrect " + FILE_NAME_LIST + " length");
		}
		
		short entries = readWord(input);
//		if (entries != 2) {
//			System.out.println("Warning: more than two compression sections");
//		}
		
		sectionNames = new String[entries];
		
		for (int i = 0; i < entries; i++) {
			short nameLen = readWord(input);
			char[] name = new char[nameLen];
			for (int j = 0; j < nameLen; j++) {
				name[j] = (char) readWord(input);
			}
			readWord(input); // terminal zero
			sectionNames[i] = new String(name);
		}
		System.out.println(Arrays.toString(sectionNames));
	}

}