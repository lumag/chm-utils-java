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
		
		try {
			byte[] inbuf = new byte[0x8000 + 6144];
			input.read(inbuf);
	
			byte[] out = d.decode(inbuf, 0x8000);
			output.write(out);
		} finally {
			input.close();
			output.close();
		}
	}

}
