package lumag.chm;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import lumag.util.BasicReader;
import lumag.util.FixedSizeCache;

public class LZXCTransformation implements ITransformation {

	private static final String RESET_TABLE = "ResetTable";
	private static final byte[] LZXC_SIGNATURE = new byte[]{'L', 'Z', 'X', 'C'};
	private FixedSizeCache<Integer, byte[]> blockCache = new FixedSizeCache<Integer, byte[]>();
	private int lastBlock = -1;
	private long[] resets;
	private int resetBlockInterval;
	private int resetInterval;
	private int windowSize;
	private long uncompressedLength;
	private long compressedLength;
	private LZXDecompressor decompressor;
	private IDataStorage parent;
	
	public void init(CommonReader reader, IDataStorage prev,
			String guid, byte[] controlData, Map<String, byte[]> data) throws FileFormatException, IOException {
		this.parent = prev;
		processControlData(controlData);

		if (!data.containsKey(RESET_TABLE)) {
			throw new FileFormatException("No resets table present!");
		}
		processResetTable(data.get(RESET_TABLE));

		decompressor = new LZXDecompressor(windowSize);
	}

	private void processControlData(byte[] controlData)
			throws FileFormatException, IOException {
		// FIXME: make this check dependant on the GUID
		if (controlData.length != 6*4 && controlData.length != 7*4) {
			throw new FileFormatException("Bad size of ControlData: " + controlData.length);
		}
		BasicReader reader = new BasicReader(controlData);
		
		byte[] signature = reader.read(4);
		
		if (!Arrays.equals(signature, LZXC_SIGNATURE)) {
			throw new FileFormatException("Bad control data header");
		}

		int version = reader.readDWord();
		System.out.println("ControlcontrolData Version: " + version);
		// FIXME: make this check dependant on the GUID
		if (version != 2 && version != 3) {
			throw new FileFormatException("ControlcontrolData Version " + version + " is unsupported");
		}

		resetInterval = reader.readDWord();
		windowSize = reader.readDWord();
		int cacheSize = reader.readDWord();

		System.out.println("Resets parameters: " + resetInterval + ", " + windowSize + ", " + cacheSize);
		if (resetInterval % (windowSize / 2) != 0) {
			throw new FileFormatException("Unsupported reset interval value");
		}

		int unk = reader.readDWord();
		if (unk != 0) {
			System.out.println("Warning: unknown element expected to be zero: " + unk);
		}

		if (version == 2 || version == 3) {
			resetInterval *= 0x8000;
			windowSize *= 0x8000;
		}
		resetBlockInterval = resetInterval / (windowSize / 2) * cacheSize;
		System.out.println("ResetBlockInterval: " + resetBlockInterval);
	}
	
	private void processResetTable(byte[] data) throws FileFormatException, IOException {
		if (data.length == 0) {
			System.out.println("Empty reset table");
			return;
		}
		BasicReader reader = new BasicReader(data);

		int version = reader.readDWord();
		System.out.println("ResetTable Version: " + version);
		// FIXME: make this check dependant on the GUID
		if (version != 2 && version != 3) {
			throw new FileFormatException("ResetTable Version " + version + " is unsupported");
		}
		
		int resetNum = reader.readDWord();
		System.out.println(resetNum + " entries in ResetTable");
		resets = new long[resetNum];

		int entrySize = reader.readDWord();
		if (entrySize != 8) {
			throw new FileFormatException("Size of ResetTable entry isn't 8 (" + entrySize + ")");
		}
		
		int headerSize = reader.readDWord();
		if (headerSize != 0x28) {
			throw new FileFormatException("Unsupported ResetTable header size: " + headerSize);
		}
		
		uncompressedLength = reader.readQWord();
		compressedLength = reader.readQWord();
		
		System.out.format("Content compression 0x%x -> 0x%x%n", uncompressedLength, compressedLength);
		
		long blockSize = reader.readQWord();
		if (blockSize != 0x8000) {
			throw new FileFormatException("BlockSize isn't 0x8000: " + blockSize);
		}
		
		for (int i = 0; i < resetNum; i++) {
			resets[i] = reader.readQWord();
		}
	}

	private byte[] getBlock(int blockNo) throws FileFormatException {
		byte[] data = blockCache.get(blockNo);
		
		if (data != null) {
			return data;
		}
		
		int startBlock = (blockNo / resetBlockInterval) * resetBlockInterval;
		
		if (lastBlock >= startBlock && lastBlock <= blockNo - 1) {
			startBlock = lastBlock + 1;
		}
		
		for (int i = startBlock; i < blockNo; i++) {
//			System.out.println("Extra: " + i);
			decodeBlock(i);
		}

//		System.out.println("Decoding: " + blockNo);
		return decodeBlock(blockNo);
	}

//	@SuppressWarnings("unused")
//	private void decodeContent(RandomAccessFile input, ListingEntry entry, String fname) throws IOException, FileFormatException {
//		input.seek(contentOffset);
//		OutputStream output = new BufferedOutputStream(new FileOutputStream(fname));
//		final long maxBlock = resets.length;
//		for (int i = 0; i < maxBlock ; i++) {
//			byte[] out = decodeBlock(input, i);
//			output.write(out);
//			output.flush();
//		}
//		output.close();
//	}
//
	private byte[] decodeBlock(int blockNumber) throws FileFormatException {
		if (blockNumber % resetBlockInterval == 0) {
//			System.out.println("Reset: " + blockNumber);
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
//		System.out.format("Decode %d %04x %04x%n", blockNumber, compBlockLen, uncompBlockLen);
		byte[] inp = parent.getData(resets[blockNumber], compBlockLen);

		byte[] block =  decompressor.decode(inp, uncompBlockLen);

		lastBlock = blockNumber;
		blockCache.add(blockNumber, block);
		return block;
	}

	public byte[] getData(long offset, int length) throws FileFormatException {
		int startBlock = (int) (offset / 0x8000);
		int startOffset = (int) (offset % 0x8000);
		int endBlock = (int) ((offset + length - 1) / 0x8000);
		
		byte[] block = getBlock(startBlock);
		if (startBlock == endBlock) {
			return Arrays.copyOfRange(block, startOffset, startOffset + length);
		}

		byte[] data = new byte[length];
		int filled = block.length - startOffset;
		System.arraycopy(block, startOffset, data, 0, filled);

		
		for (int i = startBlock+1; i < endBlock; i++) {
			block = getBlock(i);
			System.arraycopy(block, 0, data, filled, block.length);
			filled += block.length;
		}
		
		block = getBlock(endBlock);
		System.arraycopy(block, 0, data, filled, length - filled);
		
		return data;
	}

}
