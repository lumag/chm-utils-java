package lumag.util;

public class MemoryUtils {

	public static void byteArrayCopy(byte[] out, int outOffset,
							   byte[] in,  int inOffset,
							   int len) {
		for (int i = 0; i < len; i++) {
			out[outOffset + i] = in[inOffset + i];
		}
	}

}
