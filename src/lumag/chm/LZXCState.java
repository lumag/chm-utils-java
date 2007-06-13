/**
 * 
 */
package lumag.chm;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import lumag.util.FixedSizeCache;
import lumag.util.ReaderHelper;

class LZXCState {
	private static final byte[] HEADER_LZXC = {'L', 'Z', 'X', 'C'};

	private FixedSizeCache<Integer, byte[]> blockCache = new FixedSizeCache<Integer, byte[]>();
	private int lastBlock = -1;
	private long[] resets;
	private int resetBlockInterval;
	private int resetInterval;
	private int windowSize;
	private long uncompressedLength;
	private long compressedLength;
	private LZXDecompressor decompressor;
	private long contentOffset;
	
	void readResetTable(RandomAccessFile input) throws IOException, FileFormatException {
		int version = ReaderHelper.readDWord(input);
		System.out.println("ResetTable Version: " + version);
		if (version != 2) {
			throw new FileFormatException("ResetTable Version " + version + " is unsupported");
		}
		
		int resetNum = ReaderHelper.readDWord(input);
		System.out.println(resetNum + " entries in ResetTable");
		resets = new long[resetNum];

		int entrySize = ReaderHelper.readDWord(input);
		if (entrySize != 8) {
			throw new FileFormatException("Size of ResetTable entry isn't 8 (" + entrySize + ")");
		}
		
		int headerSize = ReaderHelper.readDWord(input);
		if (headerSize != 0x28) {
			throw new FileFormatException("Unsupported ResetTable header size: " + headerSize);
		}
		
		uncompressedLength = ReaderHelper.readQWord(input);
		compressedLength = ReaderHelper.readQWord(input);
		
		System.out.format("Content compression 0x%x -> 0x%x%n", uncompressedLength, compressedLength);
		
		long blockSize = ReaderHelper.readQWord(input);
		if (blockSize != 0x8000) {
			throw new FileFormatException("BlockSize isn't 0x8000: " + blockSize);
		}
		
		for (int i = 0; i < resetNum; i++) {
			resets[i] = ReaderHelper.readQWord(input);
		}
	}

	void readControlData(RandomAccessFile input) throws FileFormatException, IOException {
		int size = ReaderHelper.readDWord(input);
		if (size != 6) {
			throw new FileFormatException("Bad size of ControlData: " + size);
		}
		
		ReaderHelper.checkHeader(input, HEADER_LZXC);
		
		int version = ReaderHelper.readDWord(input);
		System.out.println("ControlData Version: " + version);
		if (version != 2) {
			throw new FileFormatException("ControlData Version " + version + " is unsupported");
		}
		
		resetInterval = ReaderHelper.readDWord(input);
		windowSize = ReaderHelper.readDWord(input);
		int cacheSize = ReaderHelper.readDWord(input);
		
		System.out.println("Resets parameters: " + resetInterval + ", " + windowSize + ", " + cacheSize);
		if (resetInterval % (windowSize / 2) != 0) {
			throw new FileFormatException("Unsupported reset interval value");
		}

		int unk = ReaderHelper.readDWord(input);
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

	byte[] getBlock(RandomAccessFile input, int blockNo) throws IOException, FileFormatException {
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
	private void decodeContent(RandomAccessFile input, ListingEntry entry, String fname) throws IOException, FileFormatException {
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

	public void setContentOffset(long offset) {
		this.contentOffset = offset;
	}

}