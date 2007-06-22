package lumag.chm;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

import lumag.util.ReaderHelper;

public class CHMReader extends CommonReader {
	private static final byte[] HEADER_INDEX_SECTION = {'I', 'T', 'S', 'P'};
	private static final byte[] HEADER_PMGL = {'P', 'M', 'G', 'L'};
	
	private int directoryBlockSize;
	private long directoryOffset;
	
	private RandomAccessFile inputFile;

	public CHMReader(String name) throws IOException, FileFormatException {
		System.out.println("CHM file " + name);
		inputFile = new RandomAccessFile(name, "r");
		read(inputFile);
	}

	public static void main(String[] args) {
		for (String name: args) {
			try {
				CommonReader reader = new CHMReader(name);
//				reader.decodeContent(reader.inputFile, "test/decoded_content_file");
				reader.dump("test");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void read(RandomAccessFile input) throws IOException, FileFormatException {
		readFormatHeader(input);
		readFileSizeSection(input);
		readIndexSection(input);

		readContentData(input);
	}

	private void readIndexSection(RandomAccessFile input) throws IOException, FileFormatException {
		input.seek(getSectionOffset(SECTION_INDEX));

		ReaderHelper.checkHeader(input, HEADER_INDEX_SECTION);
		
		int version = ReaderHelper.readDWord(input);
		System.out.println("ITSP Version: " + version);
		if (version != 1) {
			throw new FileFormatException("ITSP Version " + version + " is unsupported");
		}
		
		int headerLen = ReaderHelper.readDWord(input);
		if (headerLen != 0x54) {
			throw new FileFormatException("bad section length");
		}
		
		directoryOffset = getSectionOffset(SECTION_INDEX) + headerLen;
		
		int unk = ReaderHelper.readDWord(input);
		if (unk != 0xa) {
			System.out.format("Expected 0x0a for unknown data, got 0x%x%n", unk);
		}
		
		directoryBlockSize = ReaderHelper.readDWord(input);
		/*int quickRefDensity = */ReaderHelper.readDWord(input);
		/*int indexTreeDepth = */ReaderHelper.readDWord(input);
		int rootIndexChunk = ReaderHelper.readDWord(input);
		int firstListChunk = ReaderHelper.readDWord(input);
		int lastListChunk = ReaderHelper.readDWord(input);
		int unk2 = ReaderHelper.readDWord(input); // -1
		if (unk2 != -1) {
			System.out.format("Expected -1 for unk2, got 0x%08x%n", unk2);
		}
		int directoryChunks = ReaderHelper.readDWord(input);
		
//		System.out.format("%x: %d %d %d %d%n", directoryBlockSize, rootIndexChunk, firstListChunk, lastListChunk, directoryChunks);

		ReaderHelper.readDWord(input); // lcid
		ReaderHelper.readGUID(input); // guid

		int len2 = ReaderHelper.readDWord(input);
		if (len2 != headerLen) {
			System.out.format("Bad second length: expected 0x%x, got 0x%x", headerLen, len2);
		}
		int unk3 = ReaderHelper.readDWord(input); // -1
		if (unk3 != -1) {
			System.out.format("Expected -1 for unk3, got 0x%08x%n", unk3);
		}
		int unk4 = ReaderHelper.readDWord(input); // -1
		if (unk4 != -1) {
			System.out.format("Expected -1 for unk4, got 0x%08x%n", unk4);
		}
		int unk5 = ReaderHelper.readDWord(input); // -1
		if (unk5 != -1) {
			System.out.format("Expected -1 for unk5, got 0x%08x%n", unk5);
		}
		
		for (int chunk = 0; chunk < directoryChunks; chunk++) {
			if (chunk == rootIndexChunk) {
				readIndex(input, chunk);
			} else if (chunk >= firstListChunk && chunk <= lastListChunk) {
				readListing(input, chunk);
			} else {
				byte[] header = new byte[4];
				input.readFully(header);
				char[] cheader = new char[4];
				for (int j = 0; j < 4; j++) {
					cheader[j] = (char) header[j];
				}
				System.out.println("unknown directory chunk: " + chunk + Arrays.toString(cheader));
				input.skipBytes(directoryBlockSize - 4);
			}
		}
	}

	private void readListing(RandomAccessFile input, int chunk) throws IOException, FileFormatException {
		ReaderHelper.checkHeader(input, HEADER_PMGL);
		int freeSpace = ReaderHelper.readDWord(input);
		long endPos = directoryOffset + (chunk + 1)* directoryBlockSize - freeSpace; 

		int unk = ReaderHelper.readDWord(input);
		if (unk != 0) {
			System.out.format("Expected 0x0 for unknown data, got 0x%x%n", unk);
		}

		/*int previousChunk = */ReaderHelper.readDWord(input);
		/*int nextChunk = */ReaderHelper.readDWord(input);
		
		//System.out.println(previousChunk + " <-> " + nextChunk);
		
		readListingEntries(input, endPos);

		input.skipBytes(freeSpace);
	}

	private void readIndex(RandomAccessFile input, int chunk) throws IOException {
		// just skip bytes. We don't use index
		input.skipBytes(directoryBlockSize);
	}

}
