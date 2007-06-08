package lumag.chm;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;

public class LitReader extends MSReader {
	private static final byte[] HEADER_ITOL = {'I', 'T', 'O', 'L'};
	private static final byte[] HEADER_ITLS = {'I', 'T', 'L', 'S'};
	private static final byte[] HEADER_CAOL = {'C', 'A', 'O', 'L'};
	private static final byte[] HEADER_DIRECTORY = {'I', 'F', 'C', 'M'};
	private static final byte[] HEADER_AOLL = {'A', 'O', 'L', 'L'};

	private static final int SECTION_DIRECTORY = 1;
//	private static final int SECTION_DIRECTORY_INDEX = 2;

	private RandomAccessFile inputFile;

	private int directoryBlockSize;
	private int directoryIndexBlockSize;
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
	@SuppressWarnings("unused")
	private void dump(String path) throws IOException, FileFormatException {
		File parent = new File(path);
		parent.mkdirs();
		for (ListingEntry entry: listing.values()) {
			File f = new File(parent, entry.name);
			System.out.println(entry.name + ": " + entry.section + " @ " + entry.offset + " = " + entry.length);
			f.getParentFile().mkdirs();
			if (entry.name.charAt(entry.name.length() - 1) == '/') {
				f.mkdir();
			} else if (entry.section == 0) {
				OutputStream output = new BufferedOutputStream(new FileOutputStream(f));
				inputFile.seek(dataOffset + entry.offset);
				long len = entry.length;
				byte buf[] = new byte[1024];
				while (len > 0) {
					final int toRead = (int) (len > buf.length?
											  buf.length:
											  len);
					inputFile.readFully(buf, 0, toRead);

					output.write(buf, 0, toRead);
					len -= toRead;
				}
				output.close();
			}
		}
	}

	private void read(RandomAccessFile input) throws IOException, FileFormatException {
		readITOLITLS(input);
		readSections(input);
		
		readNameList(input);
	}

	private void readITOLITLS(RandomAccessFile input) throws IOException, FileFormatException {
		checkHeader(input, HEADER_ITOL);
		checkHeader(input, HEADER_ITLS);
		
		int version = readDWord(input);
		System.out.println("ITOL/ITLS version: " + version);
		if (version != 1) {
			throw new FileFormatException("ITOL/ITLS Version " + version + " is unsupported");
		}

		int headerLen = readDWord(input);
//		System.out.println("Header len: " + headerLen);
		if (headerLen != 0x28) {
			throw new FileFormatException("bad section length");
		}
		
		int entries = readDWord(input);
		int postHeaderLen = readDWord(input);
//		System.out.println("Post-Header len: " + postHeaderLen);
		if (postHeaderLen != 0xe8) {
			throw new FileFormatException("bad postHeader length");
		}
		readGUID(input);
		
		readSectionTable(input, entries);

		readPostHeaderTable(input);
	}
	
	private void readPostHeaderTable(RandomAccessFile input) throws IOException, FileFormatException {
		int ver2 = readDWord(input);
		System.out.println("Second version: " + ver2);
		if (ver2 != 2) {
			throw new FileFormatException("2nd version " + ver2 + " is unsupported");
		}
		
		int caolOffset = readDWord(input);
		if (caolOffset != 0x98) {
			throw new FileFormatException("'CAOL' header offset isn't 0x98: " + caolOffset);
		}

		rootIndexChunk = readQWord(input);
		firstListChunk = readQWord(input);
		lastListChunk = readQWord(input);

		long unkl = readQWord(input); // 0
		if (unkl != 0) {
			System.out.format("Expected 0 for unk, got 0x%08x%n", unkl);
		}

		directoryBlockSize = readDWord(input);
		
		int density = readDWord(input);

		int unk = readDWord(input); // 0
		if (unk != 0) {
			System.out.format("Expected 0 for unk2, got 0x%08x%n", unk);
		}
		
		int mainDepth = readDWord(input);

		unkl = readQWord(input); // 0
		if (unkl != 0) {
			System.out.format("Expected 0 for unk3, got 0x%08x%n", unkl);
		}

		long directoryEntries = readQWord(input);
		System.out.println("Directory entries: " + directoryEntries);
		
		// directory index
		long rootIndexChunkIndex = readQWord(input);
		long firstListChunkIndex = readQWord(input);
		long lastListChunkIndex = readQWord(input);

		unkl = readQWord(input); // 0
		if (unkl != 0) {
			System.out.format("Expected 0 for unk4, got 0x%08x%n", unkl);
		}

		directoryIndexBlockSize = readDWord(input);
		
		int indexDensity = readDWord(input);
		
		unk = readDWord(input); // 0
		if (unk != 0) {
			System.out.format("Expected 0 for unk5, got 0x%08x%n", unk);
		}

		int indexDepth = readDWord(input);
		
		long flags = readQWord(input); // unknown
		System.out.format("Flags: 0x%x%n", flags);
		
		long directoryIndexEntries = readQWord(input);
		System.out.println("Directory index entries: " + directoryIndexEntries);
		
		int unk5 = readDWord(input);
		int unk6 = readDWord(input);
		System.out.format("Unknowns: %08x %08x%n", unk5, unk6);
		
		unkl = readQWord(input); // 0
		if (unkl != 0) {
			System.out.format("Expected 0 for unk7, got 0x%08x%n", unkl);
		}
		
		readCAOL(input);
	}

	private void readCAOL(RandomAccessFile input) throws IOException, FileFormatException {
		checkHeader(input, HEADER_CAOL);
		
		int version = readDWord(input);
		System.out.println("CAOL version: " + version);
		if (version != 2) {
			throw new FileFormatException("CAOL version " + version + " is unsupported");
		}
		
		int caolSize = readDWord(input);
		if (caolSize != 0x50) {
			throw new FileFormatException("Bad CAOL length: " + caolSize);
		}
		
		readDWord(input); // ??
		readDWord(input); // 0 or 0x43ED
		readDWord(input); // directory chunk size
		readDWord(input); // directory index chunk size
		readDWord(input); // field following chunk size
		readDWord(input); // field following index chunk size
		readDWord(input); // 0
		readDWord(input); // 0
		readDWord(input); // 0
		
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
		// TODO Auto-generated method stub
		
	}

	private void readIndex(RandomAccessFile input, int chunk) throws IOException {
		// just skip bytes. We don't use index
		input.skipBytes(directoryBlockSize);
	}

	private int readIFCM(RandomAccessFile input, int section) throws IOException, FileFormatException {
		input.seek(getSectionOffset(section));

		checkHeader(input, HEADER_DIRECTORY);

		int version = readDWord(input);
		System.out.println("IFCM version: " + version);
		if (version != 1) {
			throw new FileFormatException("IFCM version " + version + " is unsupported");
		}
		
		int chunkSize = readDWord(input);
		readDWord(input); // 0x100000
		readDWord(input); // -1
		readDWord(input); // -1
		int numberOfChunks = readDWord(input);
		readDWord(input); // 0
		
		System.out.format("%d chunk(s) of 0x%x bytes%n", numberOfChunks, chunkSize);
		return numberOfChunks;
	}

	private void readListing(RandomAccessFile input, int chunk) throws IOException, FileFormatException {
		checkHeader(input, HEADER_AOLL);
		int freeSpace = readDWord(input);

		long directoryOffset = getSectionOffset(SECTION_DIRECTORY) + 0x20;
		long endPos = directoryOffset  + (chunk + 1)* directoryBlockSize - freeSpace; 

		/* long chunkNumber = */readQWord(input);
		/* long previousChunk = */readQWord(input);
		/* long nextChunk = */readQWord(input);
		
		/* long unk = */ readQWord(input);
		/* int unk1 = */readDWord(input);
		/* int unk2 = */readDWord(input);
		
		//System.out.println(previousChunk + " <-> " + nextChunk + " : " + unk);
		
		readListingEntries(input, endPos, listing);

		input.skipBytes(freeSpace);
	}


}
