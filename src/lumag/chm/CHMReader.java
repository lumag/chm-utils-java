package lumag.chm;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;

import lumag.util.FixedSizeCache;
import lumag.util.MemoryUtils;

public class CHMReader extends MSReader {
	private static final String FILE_RESET_TABLE = "::DataSpace/Storage/MSCompressed/Transform/{7FC28940-9D31-11D0-9B27-00A0C91E9C7C}/InstanceData/ResetTable";
	private static final String FILE_CONTROL_DATA = "::DataSpace/Storage/MSCompressed/ControlData";
	private static final String FILE_CONTENT = "::DataSpace/Storage/MSCompressed/Content";

	private static final byte[] HEADER_INDEX_SECTION = {'I', 'T', 'S', 'P'};
	private static final byte[] HEADER_PMGL = {'P', 'M', 'G', 'L'};
	private static final byte[] HEADER_LZXC = {'L', 'Z', 'X', 'C'};
	
	private FixedSizeCache<Integer, byte[]> blockCache = new FixedSizeCache<Integer, byte[]>();
	private int lastBlock = -1;
	
	private int directoryBlockSize;
	private long directoryOffset;
	private long[] resets;
	private int resetBlockInterval;
	private int resetInterval;
	private int windowSize;
	private long uncompressedLength;
	private long compressedLength;
	private LZXDecompressor decompressor;
	private long contentOffset;
	
	private RandomAccessFile inputFile;

	public CHMReader(String name) throws IOException, FileFormatException {
		System.out.println("CHM file " + name);
		inputFile = new RandomAccessFile(name, "r");
		read(inputFile);
	}

	public static void main(String[] args) {
		for (String name: args) {
			try {
				CHMReader reader = new CHMReader(name);
//				reader.decodeContent(reader.inputFile, "test/decoded_content_file");
				reader.dump("test");
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
			} else {
				byte[] data = getFile(entry.name);
				OutputStream output = new BufferedOutputStream(new FileOutputStream(f));
				output.write(data);
				output.close();
			}
		}
	}

	private void read(RandomAccessFile input) throws IOException, FileFormatException {
		readFormatHeader(input);
		readFileSizeSection(input);
		readIndexSection(input);

		readNameList(input);
		readResetTable(input);
		readControlData(input);
		
		ListingEntry entry = listing.get(FILE_CONTENT);
		contentOffset = dataOffset + entry.offset;
	}

	private void readIndexSection(RandomAccessFile input) throws IOException, FileFormatException {
		input.seek(getSectionOffset(SECTION_INDEX));

		checkHeader(input, HEADER_INDEX_SECTION);
		
		int version = readDWord(input);
		System.out.println("ITSP Version: " + version);
		if (version != 1) {
			throw new FileFormatException("ITSP Version " + version + " is unsupported");
		}
		
		int headerLen = readDWord(input);
		if (headerLen != 0x54) {
			throw new FileFormatException("bad section length");
		}
		
		directoryOffset = getSectionOffset(SECTION_INDEX) + headerLen;
		
		int unk = readDWord(input);
		if (unk != 0xa) {
			System.out.format("Expected 0x0a for unknown data, got 0x%x%n", unk);
		}
		
		directoryBlockSize = readDWord(input);
		/*int quickRefDensity = */readDWord(input);
		/*int indexTreeDepth = */readDWord(input);
		int rootIndexChunk = readDWord(input);
		int firstListChunk = readDWord(input);
		int lastListChunk = readDWord(input);
		int unk2 = readDWord(input); // -1
		if (unk2 != -1) {
			System.out.format("Expected -1 for unk2, got 0x%08x%n", unk2);
		}
		int directoryChunks = readDWord(input);
		
//		System.out.format("%x: %d %d %d %d%n", directoryBlockSize, rootIndexChunk, firstListChunk, lastListChunk, directoryChunks);

		readDWord(input); // lcid
		readGUID(input); // guid

		int len2 = readDWord(input);
		if (len2 != headerLen) {
			System.out.format("Bad second length: expected 0x%x, got 0x%x", headerLen, len2);
		}
		int unk3 = readDWord(input); // -1
		if (unk3 != -1) {
			System.out.format("Expected -1 for unk3, got 0x%08x%n", unk3);
		}
		int unk4 = readDWord(input); // -1
		if (unk4 != -1) {
			System.out.format("Expected -1 for unk4, got 0x%08x%n", unk4);
		}
		int unk5 = readDWord(input); // -1
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
		checkHeader(input, HEADER_PMGL);
		int freeSpace = readDWord(input);
		long endPos = directoryOffset + (chunk + 1)* directoryBlockSize - freeSpace; 

		int unk = readDWord(input);
		if (unk != 0) {
			System.out.format("Expected 0x0 for unknown data, got 0x%x%n", unk);
		}

		/*int previousChunk = */readDWord(input);
		/*int nextChunk = */readDWord(input);
		
		//System.out.println(previousChunk + " <-> " + nextChunk);
		
		readListingEntries(input, endPos, listing);

		input.skipBytes(freeSpace);
	}

	private void readIndex(RandomAccessFile input, int chunk) throws IOException {
		// just skip bytes. We don't use index
		input.skipBytes(directoryBlockSize);
	}

	private void readResetTable(RandomAccessFile input) throws FileFormatException, IOException {
		ListingEntry entry = listing.get(FILE_RESET_TABLE);
		if (entry == null || entry.section != 0 || entry.length < 0x30) {
			throw new FileFormatException("Bad ResetTable file");
		}
		
		input.seek(dataOffset + entry.offset);
		int version = readDWord(input);
		System.out.println("ResetTable Version: " + version);
		if (version != 2) {
			throw new FileFormatException("ResetTable Version " + version + " is unsupported");
		}
		
		int resetNum = readDWord(input);
		System.out.println(resetNum + " entries in ResetTable");
		resets = new long[resetNum];

		int entrySize = readDWord(input);
		if (entrySize != 8) {
			throw new FileFormatException("Size of ResetTable entry isn't 8 (" + entrySize + ")");
		}
		
		int headerSize = readDWord(input);
		if (headerSize != 0x28) {
			throw new FileFormatException("Unsupported ResetTable header size: " + headerSize);
		}
		
		uncompressedLength = readQWord(input);
		compressedLength = readQWord(input);
		
		System.out.format("Content compression 0x%x -> 0x%x%n", uncompressedLength, compressedLength);
		
		long blockSize = readQWord(input);
		if (blockSize != 0x8000) {
			throw new FileFormatException("BlockSize isn't 0x8000: " + blockSize);
		}
		
		for (int i = 0; i < resetNum; i++) {
			resets[i] = readQWord(input);
		}
	}
	
	private void readControlData(RandomAccessFile input) throws FileFormatException, IOException {
		ListingEntry entry = listing.get(FILE_CONTROL_DATA);
		if (entry == null || entry.section != 0 || entry.length < 0x1c) {
			throw new FileFormatException("Bad ControlData file: " + entry);
		}
		
		input.seek(dataOffset + entry.offset);
		
		int size = readDWord(input);
		if (size != 6) {
			throw new FileFormatException("Bad size of ControlData: " + size);
		}
		
		checkHeader(input, HEADER_LZXC);
		
		int version = readDWord(input);
		System.out.println("ControlData Version: " + version);
		if (version != 2) {
			throw new FileFormatException("ControlData Version " + version + " is unsupported");
		}
		
		resetInterval = readDWord(input);
		windowSize = readDWord(input);
		int cacheSize = readDWord(input);
		
		System.out.println("Resets parameters: " + resetInterval + ", " + windowSize + ", " + cacheSize);
		if (resetInterval % (windowSize / 2) != 0) {
			throw new FileFormatException("Unsupported reset interval value");
		}

		int unk = readDWord(input);
		if (unk != 0) {
			System.out.println("Warning: unknown element expected to be zero: " + unk);
		}
		
		if (version == 2) {
			resetInterval *= 0x8000;
			windowSize *= 0x8000;
		}
		
		decompressor = new LZXDecompressor(windowSize);

		resetBlockInterval = resetInterval / (windowSize / 2) * cacheSize;
		System.out.println("ResetBlockInterval: " + resetBlockInterval);
	}

	public byte[] getFile(String name) throws IOException, FileFormatException {
		ListingEntry entry = listing.get(name);
		
		if (entry == null) {
			throw new FileNotFoundException();
		}
		
		
		int startBlock = (int) (entry.offset / 0x8000);
		int startOffset = (int) (entry.offset % 0x8000);
		int endBlock = (int) ((entry.offset + entry.length - 1) / 0x8000);
		
		final int length = (int) entry.length;
		
		byte[] block = getBlock(inputFile, startBlock);
		if (startBlock == endBlock) {
			return Arrays.copyOfRange(block, startOffset, startOffset + length);
		}

		byte[] data = new byte[length];
		int filled = block.length - startOffset;
		MemoryUtils.byteArrayCopy(data, 0, block, startOffset, filled);

		
		for (int i = startBlock+1; i < endBlock; i++) {
			block = getBlock(inputFile, i);
			MemoryUtils.byteArrayCopy(data, filled, block, 0, block.length);
			filled += block.length;
		}
		
		block = getBlock(inputFile, endBlock);
		MemoryUtils.byteArrayCopy(data, filled, block, 0, length - filled);
		
		return data;
	}

	private byte[] getBlock(RandomAccessFile input, int blockNo) throws IOException, FileFormatException {
		byte[] data = blockCache.get(blockNo);
		
		if (data != null) {
			return data;
		}
		
		int startBlock = (blockNo / resetBlockInterval) * resetBlockInterval;
		
		if (lastBlock >= startBlock && lastBlock <= blockNo - 1) {
			startBlock = lastBlock + 1;
		}
		
		for (int i = startBlock; i < blockNo; i++) {
			System.out.println("Extra: " + i);
			decodeBlock(input, i);
		}

		System.out.println("Decoding: " + blockNo);
		return decodeBlock(input, blockNo);
	}

	@SuppressWarnings("unused")
	private void decodeContent(RandomAccessFile input, String fname) throws IOException, FileFormatException {
		ListingEntry entry = listing.get(FILE_CONTENT);
		
		input.seek(contentOffset);
		OutputStream output = new BufferedOutputStream(new FileOutputStream(fname));
		final long maxBlock = resets.length;
		for (int i = 0; i < maxBlock ; i++) {
			byte[] out = decodeBlock(input, i);
			output.write(out);
			output.flush();
		}
		output.close();
	}

	private byte[] decodeBlock(RandomAccessFile input, int blockNumber) throws IOException, FileFormatException {
		if (blockNumber % resetBlockInterval == 0) {
			System.out.println("Reset: " + blockNumber);
			input.seek(contentOffset + resets[blockNumber]);
			decompressor.reset();
		} else if (lastBlock != -1 && lastBlock != blockNumber - 1) {
			throw new IllegalStateException("Incorrect block decoding order: " + lastBlock + " -> " + blockNumber);
		}

		int compBlockLen;
		int uncompBlockLen;
		if (blockNumber != resets.length - 1) {
			compBlockLen = (int) (resets[blockNumber+1] - resets[blockNumber]);
			uncompBlockLen = 0x8000;
		} else {
			compBlockLen = (int) (compressedLength - resets[blockNumber]);
			uncompBlockLen = (int) (uncompressedLength % 0x8000);
		}
		System.out.format("Decode %d %04x %04x%n", blockNumber, compBlockLen, uncompBlockLen);
		byte[] inp = new byte[compBlockLen];
		input.readFully(inp);

		byte[] block =  decompressor.decode(inp, uncompBlockLen);

		lastBlock = blockNumber;
		blockCache.add(blockNumber, block);
		return block;
	}


}
