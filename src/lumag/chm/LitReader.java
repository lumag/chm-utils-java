package lumag.chm;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

public class LitReader extends CommonReader {
	private static final byte[] HEADER_ITOL = {'I', 'T', 'O', 'L'};
	private static final byte[] HEADER_ITLS = {'I', 'T', 'L', 'S'};
	private static final byte[] HEADER_CAOL = {'C', 'A', 'O', 'L'};
	private static final byte[] HEADER_DIRECTORY = {'I', 'F', 'C', 'M'};
	private static final byte[] HEADER_AOLL = {'A', 'O', 'L', 'L'};

	private static final int SECTION_DIRECTORY = 1;
//	private static final int SECTION_DIRECTORY_INDEX = 2;

	private int directoryBlockSize;
	// private int directoryIndexBlockSize;
	private long rootIndexChunk;
	private long firstListChunk;
	private long lastListChunk;

	public LitReader(String name) throws IOException, FileFormatException {
		super(new RandomAccessFile(name, "r"));
		System.out.println("MS Reader file " + name);
		read();
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

	private void read() throws IOException, FileFormatException {
		readITOLITLS();
		readSections();
		
		readContentData();
	}

	private void readITOLITLS() throws IOException, FileFormatException {
		checkHeader(HEADER_ITOL);
		checkHeader(HEADER_ITLS);
		
		int version = reader.readDWord();
		System.out.println("ITOL/ITLS version: " + version);
		if (version != 1) {
			throw new FileFormatException("ITOL/ITLS Version " + version + " is unsupported");
		}

		int headerLen = reader.readDWord();
//		System.out.println("Header len: " + headerLen);
		if (headerLen != 0x28) {
			throw new FileFormatException("bad section length");
		}
		
		int entries = reader.readDWord();
		int postHeaderLen = reader.readDWord();
//		System.out.println("Post-Header len: " + postHeaderLen);
		if (postHeaderLen != 0xe8) {
			throw new FileFormatException("bad postHeader length");
		}
		reader.readGUID();
		
		readSectionTable(entries);

		readPostHeaderTable();
	}
	
	private void readPostHeaderTable() throws IOException, FileFormatException {
		int ver2 = reader.readDWord();
		System.out.println("Second version: " + ver2);
		if (ver2 != 2) {
			throw new FileFormatException("2nd version " + ver2 + " is unsupported");
		}
		
		int caolOffset = reader.readDWord();
		if (caolOffset != 0x98) {
			throw new FileFormatException("'CAOL' header offset isn't 0x98: " + caolOffset);
		}

		rootIndexChunk = reader.readQWord();
		firstListChunk = reader.readQWord();
		lastListChunk = reader.readQWord();

		long unkl = reader.readQWord(); // 0
		if (unkl != 0) {
			System.out.format("Expected 0 for unk, got 0x%08x%n", unkl);
		}

		directoryBlockSize = reader.readDWord();
		
		/* int density = */reader.readDWord();

		int unk = reader.readDWord(); // 0
		if (unk != 0) {
			System.out.format("Expected 0 for unk2, got 0x%08x%n", unk);
		}
		
		/* int mainDepth = */reader.readDWord();

		unkl = reader.readQWord(); // 0
		if (unkl != 0) {
			System.out.format("Expected 0 for unk3, got 0x%08x%n", unkl);
		}

		long directoryEntries = reader.readQWord();
		System.out.println("Directory entries: " + directoryEntries);
		
		// directory index
		/* long rootIndexChunkIndex = */reader.readQWord();
		/* long firstListChunkIndex = */reader.readQWord();
		/* long lastListChunkIndex = */reader.readQWord();

		unkl = reader.readQWord(); // 0
		if (unkl != 0) {
			System.out.format("Expected 0 for unk4, got 0x%08x%n", unkl);
		}

		/* directoryIndexBlockSize = */reader.readDWord();
		
		/* int indexDensity = */reader.readDWord();
		
		unk = reader.readDWord(); // 0
		if (unk != 0) {
			System.out.format("Expected 0 for unk5, got 0x%08x%n", unk);
		}

		/* int indexDepth = */reader.readDWord();
		
		long flags = reader.readQWord(); // unknown
		System.out.format("Flags: 0x%x%n", flags);
		
		long directoryIndexEntries = reader.readQWord();
		System.out.println("Directory index entries: " + directoryIndexEntries);
		
		int unk5 = reader.readDWord();
		int unk6 = reader.readDWord();
		System.out.format("Unknowns: %08x %08x%n", unk5, unk6);
		
		unkl = reader.readQWord(); // 0
		if (unkl != 0) {
			System.out.format("Expected 0 for unk7, got 0x%08x%n", unkl);
		}
		
		readCAOL();
	}

	private void readCAOL() throws IOException, FileFormatException {
		checkHeader(HEADER_CAOL);
		
		int version = reader.readDWord();
		System.out.println("CAOL version: " + version);
		if (version != 2) {
			throw new FileFormatException("CAOL version " + version + " is unsupported");
		}
		
		int caolSize = reader.readDWord();
		if (caolSize != 0x50) {
			throw new FileFormatException("Bad CAOL length: " + caolSize);
		}
		
		reader.readDWord(); // ??
		reader.readDWord(); // 0 or 0x43ED
		reader.readDWord(); // directory chunk size
		reader.readDWord(); // directory index chunk size
		reader.readDWord(); // field following chunk size
		reader.readDWord(); // field following index chunk size
		reader.readDWord(); // 0
		reader.readDWord(); // 0
		reader.readDWord(); // 0
		
		readFormatHeader();
	}

	private void readSections() throws IOException, FileFormatException {
		readFileSizeSection();
		
		readDirectorySection();
	}

	private void readDirectorySection() throws IOException, FileFormatException {
		int numberOfChunks = readIFCM(SECTION_DIRECTORY);

		for (int chunk = 0; chunk < numberOfChunks; chunk++) {
			if (chunk == rootIndexChunk) {
				readIndex(chunk);
			} else if (chunk >= firstListChunk && chunk <= lastListChunk) {
				readListing(chunk);
			} else {
				byte[] header = reader.read(4);
				char[] cheader = new char[4];
				for (int j = 0; j < 4; j++) {
					cheader[j] = (char) header[j];
				}
				System.out.println("unknown directory chunk: " + chunk + Arrays.toString(cheader));
				reader.skip(directoryBlockSize - 4);
			}
		}
		
	}

	private void readIndex(int chunk) throws IOException {
		// just skip bytes. We don't use index
		reader.skip(directoryBlockSize);
	}

	private int readIFCM(int section) throws IOException, FileFormatException {
		reader.seek(getSectionOffset(section));

		checkHeader(HEADER_DIRECTORY);

		int version = reader.readDWord();
		System.out.println("IFCM version: " + version);
		if (version != 1) {
			throw new FileFormatException("IFCM version " + version + " is unsupported");
		}
		
		int chunkSize = reader.readDWord();
		reader.readDWord(); // 0x100000
		reader.readDWord(); // -1
		reader.readDWord(); // -1
		int numberOfChunks = reader.readDWord();
		reader.readDWord(); // 0
		
		System.out.format("%d chunk(s) of 0x%x bytes%n", numberOfChunks, chunkSize);
		return numberOfChunks;
	}

	private void readListing(int chunk) throws IOException, FileFormatException {
		checkHeader(HEADER_AOLL);
		int freeSpace = reader.readDWord();

		long directoryOffset = getSectionOffset(SECTION_DIRECTORY) + 0x20;
		long endPos = directoryOffset  + (chunk + 1)* directoryBlockSize - freeSpace; 

		/* long chunkNumber = */reader.readQWord();
		/* long previousChunk = */reader.readQWord();
		/* long nextChunk = */reader.readQWord();
		
		/* long unk = */ reader.readQWord();
		/* int unk1 = */reader.readDWord();
		/* int unk2 = */reader.readDWord();
		
		//System.out.println(previousChunk + " <-> " + nextChunk + " : " + unk);
		
		readListingEntries(endPos);

		reader.skip(freeSpace);
	}


}
