package lumag.crypto.ms;

import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherSpi;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

public final class MsDES extends CipherSpi {
	private static final int KEY_SIZE = 8;
	private static final int BLOCK_SIZE = 64;

	private static final byte[] PC1 = {
		56, 48, 40, 32, 24, 16,  8,  0,
		57, 49, 41, 33, 25, 17,  9,  1,
		58, 50, 42, 34, 26, 18, 10,  2,
		59, 51, 43, 35,
		
		62, 54, 46, 38, 30, 22, 14,  6,
		61, 53, 45, 37, 29, 21, 13,  5,
		60, 52, 44, 36, 28, 20, 12,  4,
		                27, 19, 11,  3
	};
	private static final byte[] PC2 = {
		13, 16, 10, 23,  0,  4,
		 2, 27, 14,  5, 20,  9,
		22, 18, 11,  3, 25,  7,
		15,  6, 26, 19, 12,  1,

		40, 51, 30, 36, 46, 54,
		29, 39, 50, 44, 32, 47,
		43, 48, 38, 55, 33, 52,
		45, 41, 49, 35, 28, 31
	};
	
	private static final int[] KEY_SHIFTS = {
		1, 1, 2, 2, 2, 2, 2, 2,
		1, 2, 2, 2, 2, 2, 2, 1
	};
	
	// S-Boxes 1 through 8.
	private static final int[] SP1 = new int[] {
		0x02080800, 0x00080000, 0x02000002, 0x02080802,
		0x02000000, 0x00080802, 0x00080002, 0x02000002,
		0x00080802, 0x02080800, 0x02080000, 0x00000802,
		0x02000802, 0x02000000, 0x00000000, 0x00080002,
		0x00080000, 0x00000002, 0x02000800, 0x00080800,
		0x02080802, 0x02080000, 0x00000802, 0x02000800,
		0x00000002, 0x00000800, 0x00080800, 0x02080002,
		0x00000800, 0x02000802, 0x02080002, 0x00000000,
		0x00000000, 0x02080802, 0x02000800, 0x00080002,
		0x02080800, 0x00080000, 0x00000802, 0x02000800,
		0x02080002, 0x00000800, 0x00080800, 0x02000002,
		0x00080802, 0x00000002, 0x02000002, 0x02080000,
		0x02080802, 0x00080800, 0x02080000, 0x02000802,
		0x02000000, 0x00000802, 0x00080002, 0x00000000,
		0x00080000, 0x02000000, 0x02000802, 0x02080800,
		0x00000002, 0x02080002, 0x00000800, 0x00080802
	};
	private static final int[] SP2 = new int[] {
		0x40108010, 0x00000000, 0x00108000, 0x40100000,
		0x40000010, 0x00008010, 0x40008000, 0x00108000,
		0x00008000, 0x40100010, 0x00000010, 0x40008000,
		0x00100010, 0x40108000, 0x40100000, 0x00000010,
		0x00100000, 0x40008010, 0x40100010, 0x00008000,
		0x00108010, 0x40000000, 0x00000000, 0x00100010,
		0x40008010, 0x00108010, 0x40108000, 0x40000010,
		0x40000000, 0x00100000, 0x00008010, 0x40108010,
		0x00100010, 0x40108000, 0x40008000, 0x00108010,
		0x40108010, 0x00100010, 0x40000010, 0x00000000,
		0x40000000, 0x00008010, 0x00100000, 0x40100010,
		0x00008000, 0x40000000, 0x00108010, 0x40008010,
		0x40108000, 0x00008000, 0x00000000, 0x40000010,
		0x00000010, 0x40108010, 0x00108000, 0x40100000,
		0x40100010, 0x00100000, 0x00008010, 0x40008000,
		0x40008010, 0x00000010, 0x40100000, 0x00108000
	};
	private static final int[] SP3 = new int[] {
		0x04000001, 0x04040100, 0x00000100, 0x04000101,
		0x00040001, 0x04000000, 0x04000101, 0x00040100,
		0x04000100, 0x00040000, 0x04040000, 0x00000001,
		0x04040101, 0x00000101, 0x00000001, 0x04040001,
		0x00000000, 0x00040001, 0x04040100, 0x00000100,
		0x00000101, 0x04040101, 0x00040000, 0x04000001,
		0x04040001, 0x04000100, 0x00040101, 0x04040000,
		0x00040100, 0x00000000, 0x04000000, 0x00040101,
		0x04040100, 0x00000100, 0x00000001, 0x00040000,
		0x00000101, 0x00040001, 0x04040000, 0x04000101,
		0x00000000, 0x04040100, 0x00040100, 0x04040001,
		0x00040001, 0x04000000, 0x04040101, 0x00000001,
		0x00040101, 0x04000001, 0x04000000, 0x04040101,
		0x00040000, 0x04000100, 0x04000101, 0x00040100,
		0x04000100, 0x00000000, 0x04040001, 0x00000101,
		0x04000001, 0x00040101, 0x00000100, 0x04040000
	};
	private static final int[] SP4 = new int[] {
		0x00401008, 0x10001000, 0x00000008, 0x10401008,
		0x00000000, 0x10400000, 0x10001008, 0x00400008,
		0x10401000, 0x10000008, 0x10000000, 0x00001008,
		0x10000008, 0x00401008, 0x00400000, 0x10000000,
		0x10400008, 0x00401000, 0x00001000, 0x00000008,
		0x00401000, 0x10001008, 0x10400000, 0x00001000,
		0x00001008, 0x00000000, 0x00400008, 0x10401000,
		0x10001000, 0x10400008, 0x10401008, 0x00400000,
		0x10400008, 0x00001008, 0x00400000, 0x10000008,
		0x00401000, 0x10001000, 0x00000008, 0x10400000,
		0x10001008, 0x00000000, 0x00001000, 0x00400008,
		0x00000000, 0x10400008, 0x10401000, 0x00001000,
		0x10000000, 0x10401008, 0x00401008, 0x00400000,
		0x10401008, 0x00000008, 0x10001000, 0x00401008,
		0x00400008, 0x00401000, 0x10400000, 0x10001008,
		0x00001008, 0x10000000, 0x10000008, 0x10401000
	};
	private static final int[] SP5 = new int[] {
		0x08000000, 0x00010000, 0x00000400, 0x08010420,
		0x08010020, 0x08000400, 0x00010420, 0x08010000,
		0x00010000, 0x00000020, 0x08000020, 0x00010400,
		0x08000420, 0x08010020, 0x08010400, 0x00000000,
		0x00010400, 0x08000000, 0x00010020, 0x00000420,
		0x08000400, 0x00010420, 0x00000000, 0x08000020,
		0x00000020, 0x08000420, 0x08010420, 0x00010020,
		0x08010000, 0x00000400, 0x00000420, 0x08010400,
		0x08010400, 0x08000420, 0x00010020, 0x08010000,
		0x00010000, 0x00000020, 0x08000020, 0x08000400,
		0x08000000, 0x00010400, 0x08010420, 0x00000000,
		0x00010420, 0x08000000, 0x00000400, 0x00010020,
		0x08000420, 0x00000400, 0x00000000, 0x08010420,
		0x08010020, 0x08010400, 0x00000420, 0x00010000,
		0x00010400, 0x08010020, 0x08000400, 0x00000420,
		0x00000020, 0x00010420, 0x08010000, 0x08000020
	};
	private static final int[] SP6 = new int[] {
		0x80000040, 0x00200040, 0x00000000, 0x80202000,
		0x00200040, 0x00002000, 0x80002040, 0x00200000,
		0x00002040, 0x80202040, 0x00202000, 0x80000000,
		0x80002000, 0x80000040, 0x80200000, 0x00202040,
		0x00200000, 0x80002040, 0x80200040, 0x00000000,
		0x00002000, 0x00000040, 0x80202000, 0x80200040,
		0x80202040, 0x80200000, 0x80000000, 0x00002040,
		0x00000040, 0x00202000, 0x00202040, 0x80002000,
		0x00002040, 0x80000000, 0x80002000, 0x00202040,
		0x80202000, 0x00200040, 0x00000000, 0x80002000,
		0x80000000, 0x00002000, 0x80200040, 0x00200000,
		0x00200040, 0x80202040, 0x00202000, 0x00000040,
		0x80202040, 0x00202000, 0x00200000, 0x80002040,
		0x80000040, 0x80200000, 0x00202040, 0x00000000,
		0x00002000, 0x80000040, 0x80002040, 0x80202000,
		0x80200000, 0x00002040, 0x00000040, 0x80200040,
	};
	private static final int[] SP7 = new int[] {
		0x00004000, 0x00000200, 0x01000200, 0x01000004,
		0x01004204, 0x00004004, 0x00004200, 0x00000000,
		0x01000000, 0x01000204, 0x00000204, 0x01004000,
		0x00000004, 0x01004200, 0x01004000, 0x00000204,
		0x01000204, 0x00004000, 0x00004004, 0x01004204,
		0x00000000, 0x01000200, 0x01000004, 0x00004200,
		0x01004004, 0x00004204, 0x01004200, 0x00000004,
		0x00004204, 0x01004004, 0x00000200, 0x01000000,
		0x00004204, 0x01004000, 0x01004004, 0x00000204,
		0x00004000, 0x00000200, 0x01000000, 0x01004004,
		0x01000204, 0x00004204, 0x00004200, 0x00000000,
		0x00000200, 0x01000004, 0x00000004, 0x01000200,
		0x00000000, 0x01000204, 0x01000200, 0x00004200,
		0x00000204, 0x00004000, 0x01004204, 0x01000000,
		0x01004200, 0x00000004, 0x00004004, 0x01004204,
		0x01000004, 0x01004200, 0x01004000, 0x00004004,
	};
	private static final int[] SP8 = new int[] {
		0x20800080, 0x20820000, 0x00020080, 0x00000000,
		0x20020000, 0x00800080, 0x20800000, 0x20820080,
		0x00000080, 0x20000000, 0x00820000, 0x00020080,
		0x00820080, 0x20020080, 0x20000080, 0x20800000,
		0x00020000, 0x00820080, 0x00800080, 0x20020000,
		0x20820080, 0x20000080, 0x00000000, 0x00820000,
		0x20000000, 0x00800000, 0x20020080, 0x20800080,
		0x00800000, 0x00020000, 0x20820000, 0x00000080,
		0x00800000, 0x00020000, 0x20000080, 0x20820080,
		0x00020080, 0x20000000, 0x00000000, 0x00820000,
		0x20800080, 0x20020080, 0x20020000, 0x00800080,
		0x20820000, 0x00000080, 0x00800080, 0x20020000,
		0x20820080, 0x00800000, 0x20800000, 0x20000080,
		0x00820000, 0x00020080, 0x20020080, 0x20800000,
		0x00000080, 0x20820000, 0x00820080, 0x00000000,
		0x20000000, 0x20800080, 0x00020000, 0x00820080,
	};

	private long[] enKeys = new long[16];
	private long[] deKeys = new long[16];
	private int workingMode;
	private byte[] buffer = new byte[BLOCK_SIZE/8];
	private int bPos;

	@Override
	protected byte[] engineDoFinal(byte[] input, int inputOffset, int inputLength)
			throws IllegalBlockSizeException, BadPaddingException {
		byte[] out = engineUpdate(input, inputOffset, inputLength);

		try {
			engineDoFinal(new byte[0], 0, 0, new byte[0], 0);
		} catch (ShortBufferException e) {
			InternalError ie = new InternalError("Buffer too short???");
			ie.initCause(e);
			throw ie;
		}

		return out;
	}

	@Override
	protected int engineDoFinal(byte[] input, int inputOffset, int inputLength,
			byte[] output, int outputOffset) throws ShortBufferException,
			IllegalBlockSizeException, BadPaddingException {
		
		int res = engineUpdate(input, inputOffset, inputLength, output, outputOffset);

		if (bPos != 0) {
			if (workingMode == Cipher.ENCRYPT_MODE) {
				throw new BadPaddingException();
			}
			throw new IllegalBlockSizeException();
		}

		return res;
	}

	@Override
	protected int engineGetBlockSize() {
		return BLOCK_SIZE/8;
	}

	@Override
	protected byte[] engineGetIV() {
		return null;
	}

	@Override
	protected int engineGetOutputSize(int inputLength) {
		return (bPos + inputLength) / (BLOCK_SIZE/8) * (BLOCK_SIZE/8);
	}

	@Override
	protected AlgorithmParameters engineGetParameters() {
		return null;
	}

	@Override
	protected void engineInit(int opmode, Key key, SecureRandom random)
			throws InvalidKeyException {
		this.workingMode = opmode;
		byte[] kb;
//		if (key instanceof DESKeySpec) {
//			DESKeySpec dk = (DESKeySpec) key;
//			kb = dk.getKey();
//		} else
		if (key instanceof SecretKeySpec) {
			SecretKeySpec sks = (SecretKeySpec) key;
			kb = sks.getEncoded();
		} else {
			throw new InvalidKeyException("Unsupported key class: " + key.getClass().getName());
		}
		
		init(kb);
	}
	
	@Override
	protected void engineInit(int opmode, Key key, AlgorithmParameterSpec params,
			SecureRandom random) throws InvalidKeyException,
			InvalidAlgorithmParameterException {
		engineInit(opmode, key, random);
	}

	@Override
	protected void engineInit(int opmode, Key key, AlgorithmParameters params,
			SecureRandom random) throws InvalidKeyException,
			InvalidAlgorithmParameterException {
		engineInit(opmode, key, random);
	}

	@Override
	protected void engineSetMode(String mode) throws NoSuchAlgorithmException {
		throw new NoSuchAlgorithmException("Mode " + mode + " isn't supported");
	}

	@Override
	protected void engineSetPadding(String pad) throws NoSuchPaddingException {
		throw new NoSuchPaddingException("Padding " + pad + " isn't supported");
	}

	@Override
	protected byte[] engineUpdate(byte[] input, int inputOffset, int inputLength) {
		byte[] out = new byte[engineGetOutputSize(inputLength)];
		int res;
		try {
			res = engineUpdate(input, inputOffset, inputLength, out, 0);
		} catch (ShortBufferException e) {
			InternalError ie = new InternalError("Buffer too short???");
			ie.initCause(e);
			throw ie;
		}
		if (res != out.length) {
			throw new InternalError("incorrect output length"); 
		}
		return out;
	}

	@Override
	protected int engineUpdate(byte[] input, int inputOffset, int inputLength, byte[] output,
			int outputOffset) throws ShortBufferException {
		if (output.length - outputOffset < (bPos + inputLength) / (BLOCK_SIZE/8) * (BLOCK_SIZE/8)) {
			throw new ShortBufferException("Buufer is too short");
		}
		if (inputLength + bPos < buffer.length) {
			System.arraycopy(input, inputOffset, buffer, bPos, inputLength);
			bPos += inputLength;
			return 0;
		}
		
		long[] keys;
		if (workingMode == Cipher.ENCRYPT_MODE) {
			keys = enKeys;
		} else {
			keys = deKeys;
		}

		int inOff = inputOffset;
		int inLen = inputLength;
		int outOff = outputOffset;
		System.arraycopy(input, inOff, buffer, bPos, buffer.length - bPos);
		inOff += buffer.length - bPos;
		inLen -= buffer.length - bPos;
		des(buffer, 0, output, outOff, keys);
		bPos = 0;
		outOff += BLOCK_SIZE/8;
		
		while (inLen >= BLOCK_SIZE/8) {
			des(input, inOff, output, outOff, keys);
			inOff += BLOCK_SIZE/8;
			inLen -= BLOCK_SIZE/8;
			outOff += BLOCK_SIZE/8;
		}
		
		if (inLen != 0) {
			System.arraycopy(input, inOff, buffer, 0, inLen);
			bPos += inLen;
		}

		return outOff - outputOffset;
	}

	private void init(byte[] kb) throws InvalidKeyException {
		if (kb == null || kb.length != KEY_SIZE) {
			throw new InvalidKeyException("DES keys must be 8 bytes long");
		}
		
		long pc1key = 0;
		
		for (int i = 0; i < 56; i++) {
			int bit = PC1[i];
			if ((kb[bit >>> 3] & (0x80 >>> (bit & 7))) != 0) {
				pc1key |= 1L << (55 - i);
			}
		}
		
		int c = (int) (pc1key >>> 28);
		int d = (int) pc1key & ((1 << 28) - 1);

		for (int i = 0; i < 16; i++) {
			
			int shift = KEY_SHIFTS[i];
			c = ((c << shift) | (c >>> (28 - shift))) & ((1 << 28) - 1);
			d = ((d << shift) | (d >>> (28 - shift))) & ((1 << 28) - 1);
			long cd = ((long) c << 28) | d;

			long ki = 0;

			for (int j = 0; j < 48; j++) {
				if ((cd & (1L << (55 - PC2[j]))) != 0) {
					ki |= 1L << (47 - j);
				}
			}
			
			ki =
				((ki & (0xFCL << 40)) <<  14) |
				((ki & (0xFCL << 28)) <<  18) |
				((ki & (0xFCL << 16)) <<  22) |
				((ki & (0xFCL <<  4)) <<  26) |
				((ki & (0x3FL << 36)) >>> 12) |
				((ki & (0x3FL << 24)) >>>  8) |
				((ki & (0x3FL << 12)) >>>  4) |
				((ki & (0x3FL      ))       ) |
				0
				;

//			System.out.format("%16x%n", ki);
			
			enKeys[i] = ki;
			deKeys[15 - i] = ki;
		}

	}

	private void des(byte[] input, int inputOffset, byte[] output, int outputOffset, long[] keys) {
		long word = 0;

		for (int i = 0; i < 8; i++) {
			word |= ((long) (input[inputOffset + i] & 0xff)) << (56 - 8 * i); 
		}
		
		word = performRotate(word, 4, 0x0f0f0f0f);
		word = performRotate(word, 16, 0x0000ffff);
		word = (word >>> 32) | (word << 32);
		word = performRotate(word, 2, 0x33333333);
		word = performRotate(word, 8, 0x00ff00ff);
		word = (word >>> 32) | (word << 32);
		word = performRotate(word, 1, 0x55555555);

		long leftt = (word >>> 32);
		leftt = (leftt << 1) | (leftt >>> 31);
		long right =  word & ((1L << 32) - 1);
		right = (right << 1) | (right >>> 31);
//		word =  (leftt << 32) | (right & 0xffffffffL);
		leftt &= (1L << 32) - 1;
		right &= (1L << 32) - 1;

		
		for (int round = 0; round < 8; round ++) {
			long temp, sSubst;

			temp = ((right >>> 4) << 32) |
					(right << 60) | right;
			
			temp ^= keys[round * 2];

			sSubst = 0;
			sSubst |= SP8[(int) (temp      ) & 0x3F];
            sSubst |= SP6[(int) (temp >>  8) & 0x3F];
            sSubst |= SP4[(int) (temp >> 16) & 0x3F];
            sSubst |= SP2[(int) (temp >> 24) & 0x3F];
			sSubst |= SP7[(int) (temp >> 32) & 0x3F];
			sSubst |= SP5[(int) (temp >> 40) & 0x3F];
			sSubst |= SP3[(int) (temp >> 48) & 0x3F];
			sSubst |= SP1[(int) (temp >> 56) & 0x3F];
			sSubst &= 0xFFFFFFFFL;
			
			leftt ^= sSubst; 

			temp = ((leftt >>> 4) << 32) |
			(leftt << 60) | leftt;

			temp ^= keys[round * 2 + 1];

			sSubst = 0;
			sSubst |= SP8[(int) (temp      ) & 0x3F];
			sSubst |= SP6[(int) (temp >>  8) & 0x3F];
			sSubst |= SP4[(int) (temp >> 16) & 0x3F];
			sSubst |= SP2[(int) (temp >> 24) & 0x3F];
			sSubst |= SP7[(int) (temp >> 32) & 0x3F];
			sSubst |= SP5[(int) (temp >> 40) & 0x3F];
			sSubst |= SP3[(int) (temp >> 48) & 0x3F];
			sSubst |= SP1[(int) (temp >> 56) & 0x3F];
			sSubst &= 0xFFFFFFFFL;

			right ^= sSubst; 

		}
		

		leftt = (leftt << 31) | (leftt >>> 1);
		right = (right << 31) | (right >>> 1);
		word =  (right << 32) | (leftt & 0xffffffffL);

		word = performRotate(word, 1, 0x55555555);
		word = (word >>> 32) | (word << 32);
		word = performRotate(word, 8, 0x00ff00ff);
		word = performRotate(word, 2, 0x33333333);
		word = (word >>> 32) | (word << 32);
		word = performRotate(word, 16, 0x0000ffff);
		word = performRotate(word, 4, 0x0f0f0f0f);
		
		for (int i = 0; i < 8; i++) {
			output[outputOffset + i] = (byte) (word >>> (56 - 8 * i));
		}
	}

	private long performRotate(long word, int shift, int mask) {
		long temp =  (
				    (word >>> (32 + shift)) ^ word
					 ) & mask & ((1L << 32) -1 );

		return word ^ (temp * (1L + (1L << (32 + shift))));
	}	


}
