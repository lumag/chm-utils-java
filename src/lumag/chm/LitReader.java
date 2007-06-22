package lumag.chm;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

import lumag.util.ReaderHelper;

public class LitReader extends CommonReader {
	private static final byte[] HEADER_ITOL = {'I', 'T', 'O', 'L'};
	private static final byte[] HEADER_ITLS = {'I', 'T', 'L', 'S'};
	private static final byte[] HEADER_CAOL = {'C', 'A', 'O', 'L'};
	private static final byte[] HEADER_DIRECTORY = {'I', 'F', 'C', 'M'};
	private static final byte[] HEADER_AOLL = {'A', 'O', 'L', 'L'};

	private static final int SECTION_DIRECTORY = 1;
//	private static final int SECTION_DIRECTORY_INDEX = 2;

	private RandomAccessFile inputFile;

	private int directoryBlockSize;
	// private int directoryIndexBlockSize;
	private long rootIndexChunk;
	private long firstListChunk;
	private long lastListChunk;

	public LitReader(String name) throws IOException, FileFormatException {
		System.out.println("MS Reader file " + name);
		inputFile = new RandomAccessFile(name, "r");
		read(inputFile);
	}

	public static void main(String[] args) {
		for (String name: args) {
			try {
				@SuppressWarnings("unused")
				LitReader reader = new LitReader(name);
//				reader.decodeContent(reader.inputFile, "test/decoded_content_file");
				reader.dump("test_lit");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void read(RandomAccessFile input) throws IOException, FileFormatException {
		readITOLITLS(input);
		readSections(input);
		
		readContentData(input);
	}

	private void readITOLITLS(RandomAccessFile input) throws IOException, FileFormatException {
		ReaderHelper.checkHeader(input, HEADER_ITOL);
		ReaderHelper.checkHeader(input, HEADER_ITLS);
		
		int version = ReaderHelper.readDWord(input);
		System.out.println("ITOL/ITLS version: " + version);
		if (version != 1) {
			throw new FileFormatException("ITOL/ITLS Version " + version + " is unsupported");
		}

		int headerLen = ReaderHelper.readDWord(input);
//		System.out.println("Header len: " + headerLen);
		if (headerLen != 0x28) {
			throw new FileFormatException("bad section length");
		}
		
		int entries = ReaderHelper.readDWord(input);
		int postHeaderLen = ReaderHelper.readDWord(input);
//		System.out.println("Post-Header len: " + postHeaderLen);
		if (postHeaderLen != 0xe8) {
			throw new FileFormatException("bad postHeader length");
		}
		ReaderHelper.readGUID(input);
		
		readSectionTable(input, entries);

		readPostHeaderTable(input);
	}
	
	private void readPostHeaderTable(RandomAccessFile input) throws IOException, FileFormatException {
		int ver2 = ReaderHelper.readDWord(input);
		System.out.println("Second version: " + ver2);
		if (ver2 != 2) {
			throw new FileFormatException("2nd version " + ver2 + " is unsupported");
		}
		
		int caolOffset = ReaderHelper.readDWord(input);
		if (caolOffset != 0x98) {
			throw new FileFormatException("'CAOL' header offset isn't 0x98: " + caolOffset);
		}

		rootIndexChunk = ReaderHelper.readQWord(input);
		firstListChunk = ReaderHelper.readQWord(input);
		lastListChunk = ReaderHelper.readQWord(input);

		long unkl = ReaderHelper.readQWord(input); // 0
		if (unkl != 0) {
			System.out.format("Expected 0 for unk, got 0x%08x%n", unkl);
		}

		directoryBlockSize = ReaderHelper.readDWord(input);
		
		/* int density = */ReaderHelper.readDWord(input);

		int unk = ReaderHelper.readDWord(input); // 0
		if (unk != 0) {
			System.out.format("Expected 0 for unk2, got 0x%08x%n", unk);
		}
		
		/* int mainDepth = */ReaderHelper.readDWord(input);

		unkl = ReaderHelper.readQWord(input); // 0
		if (unkl != 0) {
			System.out.format("Expected 0 for unk3, got 0x%08x%n", unkl);
		}

		long directoryEntries = ReaderHelper.readQWord(input);
		System.out.println("Directory entries: " + directoryEntries);
		
		// directory index
		/* long rootIndexChunkIndex = */ReaderHelper.readQWord(input);
		/* long firstListChunkIndex = */ReaderHelper.readQWord(input);
		/* long lastListChunkIndex = */ReaderHelper.readQWord(input);

		unkl = ReaderHelper.readQWord(input); // 0
		if (unkl != 0) {
			System.out.format("Expected 0 for unk4, got 0x%08x%n", unkl);
		}

		/* directoryIndexBlockSize = */ReaderHelper.readDWord(input);
		
		/* int indexDensity = */ReaderHelper.readDWord(input);
		
		unk = ReaderHelper.readDWord(input); // 0
		if (unk != 0) {
			System.out.format("Expected 0 for unk5, got 0x%08x%n", unk);
		}

		/* int indexDepth = */ReaderHelper.readDWord(input);
		
		long flags = ReaderHelper.readQWord(input); // unknown
		System.out.format("Flags: 0x%x%n", flags);
		
		long directoryIndexEntries = ReaderHelper.readQWord(input);
		System.out.println("Directory index entries: " + directoryIndexEntries);
		
		int unk5 = ReaderHelper.readDWord(input);
		int unk6 = ReaderHelper.readDWord(input);
		System.out.format("Unknowns: %08x %08x%n", unk5, unk6);
		
		unkl = ReaderHelper.readQWord(input); // 0
		if (unkl != 0) {
			System.out.format("Expected 0 for unk7, got 0x%08x%n", unkl);
		}
		
		readCAOL(input);
	}

	private void readCAOL(RandomAccessFile input) throws IOException, FileFormatException {
		ReaderHelper.checkHeader(input, HEADER_CAOL);
		
		int version = ReaderHelper.readDWord(input);
		System.out.println("CAOL version: " + version);
		if (version != 2) {
			throw new FileFormatException("CAOL version " + version + " is unsupported");
		}
		
		int caolSize = ReaderHelper.readDWord(input);
		if (caolSize != 0x50) {
			throw new FileFormatException("Bad CAOL length: " + caolSize);
		}
		
		ReaderHelper.readDWord(input); // ??
		ReaderHelper.readDWord(input); // 0 or 0x43ED
		ReaderHelper.readDWord(input); // directory chunk size
		ReaderHelper.readDWord(input); // directory index chunk size
		ReaderHelper.readDWord(input); // field following chunk size
		ReaderHelper.readDWord(input); // field following index chunk size
		ReaderHelper.readDWord(input); // 0
		ReaderHelper.readDWord(input); // 0
		ReaderHelper.readDWord(input); // 0
		
		readFormatHeader(input);
	}

	private void readSections(RandomAccessFile input) throws IOException, FileFormatException {
		readFileSizeSection(input);
		
		readDirectorySection(input);
	}

	private void readDirectorySection(RandomAccessFile input) throws IOException, FileFormatException {
		int numberOfChunks = readIFCM(input, SECTION_DIRECTORY);

		for (int chunk = 0; chunk < numberOfChunks; chunk++) {
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

	private void readIndex(RandomAccessFile input, int chunk) throws IOException {
		// just skip bytes. We don't use index
		input.skipBytes(directoryBlockSize);
	}

	private int readIFCM(RandomAccessFile input, int section) throws IOException, FileFormatException {
		input.seek(getSectionOffset(section));

		ReaderHelper.checkHeader(input, HEADER_DIRECTORY);

		int version = ReaderHelper.readDWord(input);
		System.out.println("IFCM version: " + version);
		if (version != 1) {
			throw new FileFormatException("IFCM version " + version + " is unsupported");
		}
		
		int chunkSize = ReaderHelper.readDWord(input);
		ReaderHelper.readDWord(input); // 0x100000
		ReaderHelper.readDWord(input); // -1
		ReaderHelper.readDWord(input); // -1
		int numberOfChunks = ReaderHelper.readDWord(input);
		ReaderHelper.readDWord(input); // 0
		
		System.out.format("%d chunk(s) of 0x%x bytes%n", numberOfChunks, chunkSize);
		return numberOfChunks;
	}

	private void readListing(RandomAccessFile input, int chunk) throws IOException, FileFormatException {
		ReaderHelper.checkHeader(input, HEADER_AOLL);
		int freeSpace = ReaderHelper.readDWord(input);

		long directoryOffset = getSectionOffset(SECTION_DIRECTORY) + 0x20;
		long endPos = directoryOffset  + (chunk + 1)* directoryBlockSize - freeSpace; 

		/* long chunkNumber = */ReaderHelper.readQWord(input);
		/* long previousChunk = */ReaderHelper.readQWord(input);
		/* long nextChunk = */ReaderHelper.readQWord(input);
		
		/* long unk = */ ReaderHelper.readQWord(input);
		/* int unk1 = */ReaderHelper.readDWord(input);
		/* int unk2 = */ReaderHelper.readDWord(input);
		
		//System.out.println(previousChunk + " <-> " + nextChunk + " : " + unk);
		
		readListingEntries(input, endPos);

		input.skipBytes(freeSpace);
	}


}
