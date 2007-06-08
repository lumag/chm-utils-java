package lumag.chm;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

public class LZXTest {

	public static void main(String[] args) throws Exception {
		LZXDecompressor d = new LZXDecompressor(1 << Integer.parseInt(args[0]));
		File in = new File(args[1]);
		RandomAccessFile input = new RandomAccessFile(in, "r");
		OutputStream output = new BufferedOutputStream(new FileOutputStream(args[2]));
		int reset = Integer.parseInt(args[3]);
		
		int[] processed = new int[1];
		long pos = 0;

		try {
			for (int i = 0; ; i++) {
				if (i % reset == 0) {
					d.reset();
				}
				input.seek(pos);
				System.out.println(i);
				byte[] inbuf = new byte[0x8000 + 6144];
				int read = input.read(inbuf);
				if (read < 0) {
					break;
				}
	
				byte[] out = d.decode(inbuf, read, 0x8000, processed);
				pos += processed[0];
				
				output.write(out);
			}
		} finally {
			input.close();
			output.close();
		}
	}

}
