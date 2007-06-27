package lumag.util.lzx;

import java.util.Arrays;

import lumag.chm.FileFormatException;

public class LZXDecompressor {
	private static final byte[] EXTRA_BITS = {
		0,  0,  0,  0,  1,  1,  2,  2,  3,  3,  4,  4,  5,  5,  6,  6,
		7,  7,  8,  8,  9,  9, 10, 10, 11, 11, 12, 12, 13, 13, 14, 14,
		15, 15, 16, 16, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17,
		17, 17, 17
	};
	private static final int[] POSITION_BASE = {
		0,       1,       2,      3,      4,      6,      8,     12,     16,     24,     32,       48,      64,      96,     128,     192,
		256,     384,     512,    768,   1024,   1536,   2048,   3072,   4096,   6144,   8192,    12288,   16384,   24576,   32768,   49152,
		65536,   98304,  131072, 196608, 262144, 393216, 524288, 655360, 786432, 917504, 1048576, 1179648, 1310720, 1441792, 1572864, 1703936,
		1835008, 1966080, 2097152
	};
	private static final int MIN_MATCH = 2;
//	private static final int MAX_MATCH = 257;
	private static final int MAX_CHARS = 256;
	private static final int NUM_SECONDARY_LENGTHS = 249;

	private static final int PRETREE_NUM_BITS = 6;
	private static final int PRETREE_NUM_ELEMENTS = 20;
	private static final int MAINTREE_NUM_BITS = 12;
//	private static final int MAINTREE_NUM_ELEMENTS = MAX_CHARS + 50 * 8;
	private static final int ALIGNED_NUM_BITS = 7;
	private static final int ALIGNED_NUM_ELEMENTS = 8;
	private static final int LENGTHTREE_NUM_BITS = 12;
	private static final int LENGTHREE_NUM_ELEMENTS = NUM_SECONDARY_LENGTHS;

	private static final int LZX_BLOCK_INVALID = -1;
	private static final int LZX_BLOCK_VERBATIM = 1;
	private static final int LZX_BLOCK_ALIGNED = 2;
	private static final int LZX_BLOCK_UNCOMPRESSED = 3;

	private static final int LONG_BITS = 64;
	private static final short NUM_PRIMARY_LENGTHS = 7;


	private final int windowSize;
	private final int positionSlots;
	private final int mainTreeElements;

	private final byte[] window;
	private int R0, R1, R2; // LRU offsets

	private boolean gotHeader;

	private long bitbuffer;
	private int bblen;

	private byte[] input;
	private int inputLength;
	private int inPos;

	private int windowPosition;

	@SuppressWarnings("unused")
	private boolean intel_started;
	@SuppressWarnings("unused")
	private long intel_filesize;

	private int blockType;
	private long blockLength;
	private long blockRemaining;

	private byte[] mainTreeLengths;
	private short[] mainTreeSymbols;
	private byte[] lengthTreeLengths;
	private short[] lengthTreeSymbols;
	private byte[] alignedTreeLengths;
	private short[] alignedTreeSymbols;

	public LZXDecompressor(int wnd) {
		if (wnd < (1 << 15) || wnd > (1 << 21)) {
			throw new IllegalArgumentException("Bad window size: " + wnd + " (" + log2(wnd) + ")");
		}

		windowSize = wnd;
		window = new byte[windowSize];

		if (wnd == (1 << 20)) {
			positionSlots = 42;
		} else if (wnd == (1 << 21)) {
			positionSlots = 50;
		} else {
			positionSlots = log2(wnd) << 1;
		}

		mainTreeElements = MAX_CHARS + (positionSlots << 3);

		reset();
	}

	private int log2(int value) {
		for (int bit = 1 << 31, res = 31; bit != 0; bit >>>= 1, res --) {
			if ((value & bit) != 0) {
				return res;
			}
		}

		return 0;
	}

	private long getBits(int n) throws FileFormatException {
		if (n == 0) {
			return 0;
		}

		ensureBits(n);
		
		if (inPos > inputLength) {
			throw new FileFormatException("Input buffer overrun");
		}

		long res = peekBits(n);

		removeBits(n);

		return res;
	}

	private void ensureBits(int n) {
		if (n > LONG_BITS) {
			throw new IllegalArgumentException();
		}
		if (bblen < n && bblen + 16 > LONG_BITS) {
			throw new InternalError("Bitbuffer overflow: " + n + " " + bblen);
		}
		while (bblen < n) {
			// allow small overread with zeroes. Handled in readBits
			if (inPos < inputLength) {
				bitbuffer |= ((long) ( ((input[inPos+1] & 0xff) << 8) | input[inPos] & 0xff)) << (LONG_BITS - 16 - bblen);
			}

			inPos += 2;
			bblen += 16;
		}
	}

	private void removeBits(int n) {
		bitbuffer <<= n;
		bblen -= n;
	}

	private long peekBits(int n) {
		return bitbuffer >>> (LONG_BITS - n);
	}

	private short readHuffman(short[] symbols, byte[] lengths, int numBits) throws FileFormatException {
		ensureBits(16);
		short res = symbols[(int) peekBits(numBits)];
		if (res > lengths.length) {
			long bitMask = 1L << (LONG_BITS-numBits);
			do {
				bitMask >>= 1;
			res <<= 1;
			if (bitMask == 0) {
				throw new FileFormatException();
			}
			res |= ((bitbuffer & bitMask) != 0) ? 1 : 0;
			res = symbols[res];
			} while (res > lengths.length); 
		}
		removeBits(lengths[res]);
		return res;
	}

	private void readLengths(byte[] lengths, int first, int last) throws FileFormatException {
		byte[] preTreeLengths = new byte[PRETREE_NUM_ELEMENTS];
		for (int i = 0; i < PRETREE_NUM_ELEMENTS; i ++) {
			preTreeLengths[i] = (byte) getBits(4);
		}

		short[] preTreeSymbols = buildTree(preTreeLengths, PRETREE_NUM_BITS);

		for (int i = first; i < last;) {
			short sym = readHuffman(preTreeSymbols, preTreeLengths, PRETREE_NUM_BITS);
			if (sym == 17) {
				for (int j = (int) (getBits(4) + 4); j > 0; j--) {
					lengths [i++] = 0;
				}
			} else if (sym == 18) {
				for (int j = (int) (getBits(5) + 20); j > 0; j--) {
					lengths [i++] = 0;
				}
			} else if (sym == 19) {
				int j = (int) (getBits(1) + 4);
				sym = readHuffman(preTreeSymbols, preTreeLengths, PRETREE_NUM_BITS);
				sym = (short) (lengths[i] - sym);
				if (sym < 0) {
					sym += 17;
				}
				for (; j > 0; j--) {
					lengths [i++] = (byte) sym;
				}
			} else {
				sym = (short) (lengths[i] - sym);
				if (sym < 0) {
					sym += 17;
				}

				lengths [i++] = (byte) sym;
			}
		}
	}

	private short[] buildTree(byte[] lengths, int numBits) throws FileFormatException {
		final int numSyms = lengths.length;
		short[] result = new short[(1 << numBits) + (numSyms << 1)];

		byte bitNum = 1;
		int pos = 0;
		int tableMask = 1 << numBits;
		int bitMask = tableMask >> 1;

//		System.out.println("buildTree" + Arrays.toString(lengths));
		while (bitNum <= numBits) {
			for (short sym = 0; sym < numSyms; sym ++) {
				if (lengths[sym] == bitNum) {
					if (pos + bitMask > tableMask) {
						throw new FileFormatException("Table overrun! " + sym + " " + pos);
					}

					for (int fill = 0; fill < bitMask; fill ++, pos ++) {
						result[pos] = sym;
					}
				}
			}
			bitMask >>= 1;
			bitNum ++;
		}

		if (pos != tableMask) {
			int nextSymbol = tableMask >> 1;
		for (int sym = pos; sym < tableMask; sym ++) {
			result[sym] = 0;
		}

		pos <<= 16;
		tableMask <<= 16;
		bitMask = 1 << 15;

		while (bitNum <= 16) {
			for (short sym = 0; sym < numSyms; sym ++) {
				if (lengths[sym] == bitNum) {
					int leaf = pos >> 16;
			for (int fill = 0; fill < bitNum - numBits; fill ++) {
				if (result[leaf] == 0) {
					result[nextSymbol << 1] = 0;
					result[(nextSymbol << 1) + 1] = 0;
					result[leaf] = (short) nextSymbol ++;
				}
				leaf = result[leaf] << 1;
				if (((pos >> (15 - fill)) & 1) != 0) {
					leaf ++;
				}
			}
			result[leaf] = sym;
			pos += bitMask;
			if (pos > tableMask) {
				throw new FileFormatException("Table overflow");
			}
				}
			}
			bitMask >>= 1;
					bitNum ++;
		}
		}

		if (pos != tableMask) {
			for (short sym = 0; sym < numSyms; sym ++) {
				if (lengths[sym] != 0) {
					throw new FileFormatException("Erroneus table");
				}
			}
		}
		return result;
	}

	public void reset() {
		R0 = R1 = R2 = 1;
		bitbuffer = 0;
		bblen = 0;
		windowPosition = 0;

		mainTreeLengths = new byte[mainTreeElements];
		lengthTreeLengths = new byte[NUM_SECONDARY_LENGTHS];
		alignedTreeLengths = new byte[ALIGNED_NUM_ELEMENTS];

		gotHeader = false;
		
		blockType = LZX_BLOCK_INVALID;
		blockLength = 0;
		blockRemaining = 0;

	}

	public byte[] decode(byte[] in, int outLength) throws FileFormatException {
		return decode(in, in.length, outLength, null);
	}

	public byte[] decode(byte[] in, int inLen, int outLength, int[] processed) throws FileFormatException {
		input = in;
		inputLength = inLen;
		inPos = 0;

		getBits(bblen % 16);
		if (!gotHeader) {
//			System.out.println("Reading header");
			intel_filesize = 0;

			long h = getBits(1);
			if (h != 0) {
				intel_filesize = (getBits(16) << 16) | getBits(16);
				throw new FileFormatException("Data transformation not yet supported");
			}
			gotHeader = true;
		}

		int toGo = outLength;

		while (toGo > 0) {
//			System.out.format("Left: %04x (%04x)%n", inputLength - inPos, toGo);
			if (blockRemaining == 0) {
				if (blockType == LZX_BLOCK_UNCOMPRESSED) {
					// restore bitstream
					if (blockLength % 2 == 1) {
						inPos ++;
					}
					bblen = 0;
					bitbuffer = 0;
				}
				blockType = (int) getBits(3);
				blockLength = blockRemaining = getBits(24);
//				System.out.format("block %d length 0x%x%n", blockType, blockLength);

				switch (blockType) {
				case LZX_BLOCK_UNCOMPRESSED:
//					System.out.println("Uncompressed");

					readUncompressedBlockHeader();

					break;
				case LZX_BLOCK_ALIGNED:
//					System.out.println("Aligned");

					readAlignedBlockHeader();
					
					break;
				case LZX_BLOCK_VERBATIM:
//					System.out.println("Verbatim");

					readVerbatimBlockHeader();

					break;
				default:
					throw new FileFormatException("Bad block type: " + blockType);
				}
			}

			long thisRun;

			while ((thisRun = blockRemaining) > 0 && toGo > 0) {
				if (thisRun > toGo) {
					thisRun = toGo;
				}
				toGo -= thisRun;
				blockRemaining -= thisRun;

				windowPosition &= windowSize - 1;
				if (windowPosition + thisRun > windowSize) {
					throw new FileFormatException("window overrun");
				}


				switch (blockType) {
				case LZX_BLOCK_VERBATIM:
				case LZX_BLOCK_ALIGNED:
					readVerbatimAlignedBlock(thisRun);
					break;
				case LZX_BLOCK_UNCOMPRESSED:
					readUncompressedBlock(thisRun);
					break;
				default:
					throw new FileFormatException("Bad block type: " + blockType);
				}
			}

		}
		
		if (bblen > 16) {
			inPos -= (bblen / 16) * 2;
			bblen %= 16;
		}

		if (inPos > inputLength) {
			bblen -= (inPos - inputLength) * 8;
			inPos = inputLength;
		}

		if (processed != null && processed.length > 0) {
			processed[0] = inPos;
		}

		int to = (windowPosition == 0? windowSize : windowPosition);

		return Arrays.copyOfRange(window, to - outLength, to);
	}
	
	private void readAlignedBlockHeader() throws FileFormatException {
		for (int i = 0; i < ALIGNED_NUM_ELEMENTS; i++) {
			alignedTreeLengths[i] = (byte) getBits(3);
		}

		alignedTreeSymbols = buildTree(alignedTreeLengths, ALIGNED_NUM_BITS);

		readVerbatimBlockHeader();
	}

	private void readVerbatimBlockHeader() throws FileFormatException {
		readLengths(mainTreeLengths, 0, 256);
		readLengths(mainTreeLengths, 256, mainTreeElements);
		mainTreeSymbols = buildTree(mainTreeLengths, MAINTREE_NUM_BITS);

		if (mainTreeLengths[0xE8] != 0) {
			intel_started = true;
		}

		readLengths(lengthTreeLengths, 0, LENGTHREE_NUM_ELEMENTS);
		lengthTreeSymbols = buildTree(lengthTreeLengths, LENGTHTREE_NUM_BITS);
	}

	private void readUncompressedBlockHeader() throws FileFormatException {
		if (bblen > 16) {
			// FIXME
			throw new UnsupportedOperationException("Can't handle uncompressed block with too many chars read");
		}

		getBits(bblen%16);
		
		R0 = readQword();
		R1 = readQword();
		R2 = readQword();

		intel_started = true;
	}

	private void readVerbatimAlignedBlock(long length) throws FileFormatException {
		long remaining = length;

		while (remaining > 0) {
			short element = readHuffman(mainTreeSymbols, mainTreeLengths, MAINTREE_NUM_BITS);
			if (element < MAX_CHARS) {
				window[windowPosition++] = (byte) element;
				remaining --;
			} else {
				element -= MAX_CHARS;

				short matchLength = (short) (element & NUM_PRIMARY_LENGTHS);
				if (matchLength == NUM_PRIMARY_LENGTHS) {
					matchLength += readHuffman(lengthTreeSymbols, lengthTreeLengths, LENGTHTREE_NUM_BITS);
				}
				matchLength += MIN_MATCH;

				int matchOffset = element >> 3;

				if (matchOffset == 0) {
					matchOffset = R0;
				} else if (matchOffset == 1) {
					matchOffset = R1;
					R1 = R0;
					R0 = matchOffset;
				} else if (matchOffset == 2) {
					matchOffset = R2;
					R2 = R0;
					R0 = matchOffset;
				} else {
					if (blockType == LZX_BLOCK_VERBATIM) {
						long verbatimBits;
						if (matchOffset == 3) {
							verbatimBits = 0;
						} else {
							byte extra = EXTRA_BITS[matchOffset];
							verbatimBits = getBits(extra);
						}
						matchOffset = (int) (POSITION_BASE[matchOffset] - 2 + verbatimBits);
					} else {// ALIGNED
						byte extra = EXTRA_BITS[matchOffset];
						long verbatimBits;
						long alignedBits;
						if (extra > 3) {
							verbatimBits = getBits(extra - 3) << 3;
							alignedBits = readHuffman(alignedTreeSymbols, alignedTreeLengths, ALIGNED_NUM_BITS);
						} else if (extra == 3) {
							verbatimBits = 0;
							alignedBits = readHuffman(alignedTreeSymbols, alignedTreeLengths, ALIGNED_NUM_BITS);
						} else if (extra > 0) {
							verbatimBits = getBits(extra);
							alignedBits = 0;
						} else { // extra == 0
							verbatimBits = 0;
							alignedBits = 0;
						}
						matchOffset = (int) (POSITION_BASE[matchOffset] - 2 + verbatimBits + alignedBits);
					}

					R2 = R1;
					R1 = R0;
					R0 = matchOffset;
				}

				int src = windowPosition - matchOffset;
				int dest = windowPosition;
				windowPosition += matchLength;
				remaining -= matchLength;
				if (windowPosition > windowSize) {
					throw new FileFormatException("Illegal data");
				}

				if (src < 0 && matchLength > 0) {
					int tempLen = (matchLength + src > 0)? -src : matchLength;
					// don't use System.arraycopy here!
					for (int i = 0; i < tempLen; i++) {
						window[dest] = window[windowSize + src];
						dest ++;
						src ++;
					}
					matchLength -= tempLen;
				}

				// don't use System.arraycopy here!
				for (int i = 0; i < matchLength; i++) {
					window[dest] = window[src];
					dest ++;
					src ++;
				}
			}
		}
	}

	private void readUncompressedBlock(long length) throws FileFormatException {
		if (windowPosition + length > windowSize) {
			throw new FileFormatException("Illegal data");
		}

		System.arraycopy(input, inPos, window, windowPosition, (int) length);

		windowPosition += length;
		inPos += length;
	}

	private int readQword() {
		int ret = 0;
		for (int i = 0; i < 4; i++) {
			ret |= (input[inPos] & 0xff) << (i * 8);   
			inPos ++;
		}
		return ret;
	}
}
