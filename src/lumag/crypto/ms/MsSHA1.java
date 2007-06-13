package lumag.crypto.ms;

import java.security.DigestException;
import java.security.MessageDigestSpi;
import java.util.Arrays;

public final class MsSHA1 extends MessageDigestSpi implements Cloneable {
	private static final int DIGEST_LENGTH = 160;
	private static final int BLOCK_SIZE = 512;
	private int[] state;
	
	private long count;
	
	private byte[] buffer = new byte[BLOCK_SIZE / 8];
	int bPos;
	
	public MsSHA1() {
		engineReset();
	}
	
	@Override
	protected byte[] engineDigest() {
		byte[] result = new byte[DIGEST_LENGTH/8];

		try {
			engineDigest(result, 0, result.length);
		} catch (DigestException e) {
			Error err = new InternalError();
			err.initCause(e);
			throw err;
		}

		return result;
	}

	@Override
	protected int engineDigest(byte[] buf, int offset, int len) throws DigestException {
		if (len < DIGEST_LENGTH / 8) {
			throw new DigestException("Buffer is too short for digest");
		}

		buffer[bPos ++] = (byte) 0x80;

		if (bPos >= BLOCK_SIZE /8 - 8) {
			Arrays.fill(buffer, bPos, BLOCK_SIZE/8, (byte) 0);
			sha1(buffer, 0);
			bPos = 0;
		}

		Arrays.fill(buffer, bPos, BLOCK_SIZE/8 - 8, (byte) 0);

		final long bits = count * 8;
		for (int i = 7; i >= 0; i--) {
			buffer[BLOCK_SIZE/8 - i - 1] = (byte) (bits >>> (i * 8)); 
		}

		sha1(buffer, 0);

		for (int i = 0; i < DIGEST_LENGTH/8; i++) {
			buf[offset + i] = (byte) (state[i/4] >>> (8 * (3 - (i % 4))));
		}
		
		engineReset();
		
		return DIGEST_LENGTH/8;
	}

	@Override
	protected int engineGetDigestLength() {
		return DIGEST_LENGTH/8;
	}

	@Override
	protected void engineReset() {
		count = 0;
		
		state = new int[5];

		state[0] = 0x32107654;
		state[1] = 0x23016745;
		state[2] = 0xc4e680a2;
        state[3] = 0xdc679823;
        state[4] = 0xd0857a34;
	}

	// 512-bit block = 64 bytes = 16 ints 
	private void sha1(byte[] block, int start) {
		int[] w = new int[80];
		
		int temp;
		
		int[] wrk = state.clone();
		
		for (int i = 0; i < 16; i++) {
			w[i] =	(block[start + i * 4 + 0] & 0xff) << 24 |
					(block[start + i * 4 + 1] & 0xff) << 16 |
					(block[start + i * 4 + 2] & 0xff) << 8  |
					(block[start + i * 4 + 3] & 0xff) << 0  ;
		}
		for (int i = 16; i < 80; i++) {
			temp = w[i-3] ^ w[i-8] ^ w[i-14] ^ w[i-16];
			w[i] = temp << 1 | temp >>> 31; 
		}

		int value = 0x5a827999;
		for (int i = 0; i < 20; i++) {
			if (i == 3 || i == 10 || i == 15) {
				withParity(w, wrk, i, value);
			} else if (i == 6) {
				withExtra(w, wrk, i, value);
			} else {
				withCh(w, wrk, i, value);
			}
		}
		

		value = 0x6ed9eba1;
		for (int i = 20; i < 40; i++) {
			if (i == 26) {
				withCh(w, wrk, i, value);
			} else if (i == 31) {
				withMaj(w, wrk, i, value);
			} else {
				withParity(w, wrk, i, value);
			}
		}
		
		value = 0x8f1bbcdc;
		for (int i = 40; i < 60; i++) {
			if (i == 42) {
				withExtra(w, wrk, i, value);
			} else if (i == 51) {
				withParity(w, wrk, i, value);
			} else {
				withMaj(w, wrk, i, value);
			}
		}


		value = 0xca62c1d6;
		for (int i = 60; i < 80; i++) {
			if (i == 68) {
				withCh(w, wrk, i, value);
			} else {
				withParity(w, wrk, i, value);				
			}
		}

		for (int i = 0; i < 5; i++) {
			state[i] += wrk[i];
		}
	}

	private void withExtra(int[] w, int[] wrk, int i, int value) {
		wrk[(4 + 80 - i) % 5] += (wrk[(0 + 80 - i) % 5] << 5 | wrk[(0 + 80 - i) % 5] >>> 27) +
					((wrk[(1 + 80 - i) % 5] + wrk[(2 + 80 - i) % 5]) ^ wrk[(2 + 80 - i) % 5])
					+ w[i] + value;
		wrk[(1 + 80 - i) % 5] = wrk[(1 + 80 - i) % 5] << 30 | wrk[(1 + 80 - i) % 5] >>> 2;
	}

	private void withMaj(int[] w, int[] wrk, int i, int value) {
		wrk[(4 + 80 - i) % 5] += (wrk[(0 + 80 - i) % 5] << 5 | wrk[(0 + 80 - i) % 5] >>> 27) +
					((wrk[(1 + 80 - i) % 5] & wrk[(2 + 80 - i) % 5]) | (wrk[(1 + 80 - i) % 5] & wrk[(3 + 80 - i) % 5]) | (wrk[(2 + 80 - i) % 5] & wrk[(3 + 80 - i) % 5]))
					+ w[i] + value;
		wrk[(1 + 80 - i) % 5] = wrk[(1 + 80 - i) % 5] << 30 | wrk[(1 + 80 - i) % 5] >>> 2;
	}

	private void withParity(int[] w, int[] wrk, int i, int value) {
		wrk[(4 + 80 - i) % 5] += (wrk[(0 + 80 - i) % 5] << 5 | wrk[(0 + 80 - i) % 5] >>> 27) +
					(wrk[(1 + 80 - i) % 5] ^ wrk[(2 + 80 - i) % 5] ^ wrk[(3 + 80 - i) % 5])
					+ w[i] + value;
		wrk[(1 + 80 - i) % 5] = wrk[(1 + 80 - i) % 5] << 30 | wrk[(1 + 80 - i) % 5] >>> 2;
	}

	private void withCh(int[] w, int[] wrk, int i, int value) {
		wrk[(4 + 80 - i) % 5] += (wrk[(0 + 80 - i) % 5] << 5 | wrk[(0 + 80 - i) % 5] >>> 27) +
					((wrk[(1 + 80 - i) % 5] & wrk[(2 + 80 - i) % 5]) | (~wrk[(1 + 80 - i) % 5] & wrk[(3 + 80 - i) % 5]))
					+ w[i] + value;
		wrk[(1 + 80 - i) % 5] = wrk[(1 + 80 - i) % 5] << 30 | wrk[(1 + 80 - i) % 5] >>> 2;
	}

	
	@Override
	protected void engineUpdate(byte[] block, int offset, int length) {
		count += length;
		int len = length;
		int off = offset;

		if (bPos != 0) {
			int tempLen = (BLOCK_SIZE / 8) - bPos;

			if (tempLen < len) {
				System.arraycopy(block, off, buffer, bPos, len);
				return;
			}

			System.arraycopy(block, off, buffer, bPos, tempLen);
			sha1(buffer, 0);
			Arrays.fill(buffer, (byte)0);
			bPos = 0;

			off += tempLen;
			len -= tempLen;
			
		}
		
		while (len >= (BLOCK_SIZE / 8)) {
			sha1(block, off);
			off += (BLOCK_SIZE / 8);
			len -= (BLOCK_SIZE / 8);
		}
		
		if (len != 0) {
			System.arraycopy(block, off, buffer, 0, len);
			bPos += len;
		}
	}
	

	@Override
	protected void engineUpdate(byte input) {
		buffer[bPos++] = input;
		if (bPos == BLOCK_SIZE / 8) {
			sha1(buffer, 0);
			Arrays.fill(buffer, (byte)0);
			bPos = 0;
		}
	}

}
