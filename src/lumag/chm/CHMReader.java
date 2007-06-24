package lumag.chm;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

public class CHMReader extends CommonReader {
	private static final byte[] HEADER_INDEX_SECTION = {'I', 'T', 'S', 'P'};
	private static final byte[] HEADER_PMGL = {'P', 'M', 'G', 'L'};
	
	private int directoryBlockSize;
	private long directoryOffset;
	
	public CHMReader(String name) throws IOException, FileFormatException {
		super(new RandomAccessFile(name, "r"));
		System.out.println("CHM file " + name);
		read();
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
	
	private void read() throws IOException, FileFormatException {
		readFormatHeader();
		readFileSizeSection();
		readIndexSection();

		readContentData();
	}

	private void readIndexSection() throws IOException, FileFormatException {
		reader.seek(getSectionOffset(SECTION_INDEX));

		checkHeader(HEADER_INDEX_SECTION);
		
		int version = reader.readDWord();
		System.out.println("ITSP Version: " + version);
		if (version != 1) {
			throw new FileFormatException("ITSP Version " + version + " is unsupported");
		}
		
		int headerLen = reader.readDWord();
		if (headerLen != 0x54) {
			throw new FileFormatException("bad section length");
		}
		
		directoryOffset = getSectionOffset(SECTION_INDEX) + headerLen;
		
		int unk = reader.readDWord();
		if (unk != 0xa) {
			System.out.format("Expected 0x0a for unknown data, got 0x%x%n", unk);
		}
		
		directoryBlockSize = reader.readDWord();
		/*int quickRefDensity = */reader.readDWord();
		/*int indexTreeDepth = */reader.readDWord();
		int rootIndexChunk = reader.readDWord();
		int firstListChunk = reader.readDWord();
		int lastListChunk = reader.readDWord();
		int unk2 = reader.readDWord(); // -1
		if (unk2 != -1) {
			System.out.format("Expected -1 for unk2, got 0x%08x%n", unk2);
		}
		int directoryChunks = reader.readDWord();
		
//		System.out.format("%x: %d %d %d %d%n", directoryBlockSize, rootIndexChunk, firstListChunk, lastListChunk, directoryChunks);

		reader.readDWord(); // lcid
		reader.readGUID(); // guid

		int len2 = reader.readDWord();
		if (len2 != headerLen) {
			System.out.format("Bad second length: expected 0x%x, got 0x%x", headerLen, len2);
		}
		int unk3 = reader.readDWord(); // -1
		if (unk3 != -1) {
			System.out.format("Expected -1 for unk3, got 0x%08x%n", unk3);
		}
		int unk4 = reader.readDWord(); // -1
		if (unk4 != -1) {
			System.out.format("Expected -1 for unk4, got 0x%08x%n", unk4);
		}
		int unk5 = reader.readDWord(); // -1
		if (unk5 != -1) {
			System.out.format("Expected -1 for unk5, got 0x%08x%n", unk5);
		}
		
		for (int chunk = 0; chunk < directoryChunks; chunk++) {
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

	private void readListing(int chunk) throws IOException, FileFormatException {
		checkHeader(HEADER_PMGL);
		int freeSpace = reader.readDWord();
		long endPos = directoryOffset + (chunk + 1)* directoryBlockSize - freeSpace; 

		int unk = reader.readDWord();
		if (unk != 0) {
			System.out.format("Expected 0x0 for unknown data, got 0x%x%n", unk);
		}

		/*int previousChunk = */reader.readDWord();
		/*int nextChunk = */reader.readDWord();
		
		//System.out.println(previousChunk + " <-> " + nextChunk);
		
		readListingEntries(endPos);

		reader.skip(freeSpace);
	}

	private void readIndex(int chunk) throws IOException {
		// just skip bytes. We don't use index
		reader.skip(directoryBlockSize);
	}

}
