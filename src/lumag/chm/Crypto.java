package lumag.chm;

import java.io.RandomAccessFile;
import java.security.Key;
import java.security.MessageDigest;
import java.security.Security;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import lumag.crypto.ms.MsAlgorithms;
import lumag.util.CipherAdapter;


public class Crypto {
	static {
		Security.addProvider(new MsAlgorithms());
	}

	public static void main(String[] args) throws Exception {
		RandomAccessFile file;
		byte[] buf;

		MessageDigest md = MessageDigest.getInstance("MS-SHA-1", "MsAlgorithms");
		
		int len;
		
		file = new RandomAccessFile("test_lit/meta", "r");
		len = (int) ((file.length() + 2 + 63) &~63); 
		buf = new byte[len];
		buf[0] = 0;
		buf[1] = 0;
		file.readFully(buf, 2, (int) file.length());
		file.close();
		
		md.update(buf);
		
		file = new RandomAccessFile("test_lit/DRMStorage/DRMSource", "r");

		len = (int) ((file.length() + 0 + 63) &~63); 
		buf = new byte[len];
		file.readFully(buf, 0, (int) file.length());
		file.close();
		
		md.update(buf);
		
		byte[] digest = md.digest();
		byte[] btempkey = new byte[8];
		for (int i = 0; i < digest.length; i++) {
			btempkey[i%8] ^= digest[i];
		}
		for (int i = 0; i < 8; i++) {
			System.out.format("%02x", btempkey[i]);
		}
		System.out.println();
		
		Key tempkey = new SecretKeySpec(btempkey, "DES");

		file = new RandomAccessFile("test_lit/DRMStorage/DRMSealed", "r");
		buf = new byte[(int) file.length()];
		file.readFully(buf);
		file.close();

		byte[] out;

		CipherAdapter decoder;
		try {
			Cipher des = Cipher.getInstance("MS-DES", "MsAlgorithms");
			decoder = CipherAdapter.newAdapter(des);
		} catch (SecurityException e) {
			decoder = CipherAdapter.newMsDesAdapter();
		}

		decoder.init(Cipher.DECRYPT_MODE, tempkey);
		
		out = decoder.doFinal(buf);

		System.out.println(Arrays.toString(out));
		
		byte[] bkey = Arrays.copyOfRange(out, 1, 9);
		Key key = new SecretKeySpec(bkey, "DES");
		decoder.init(Cipher.DECRYPT_MODE, key);

		file = new RandomAccessFile("test_lit/__DataSpace/Storage/EbEncryptOnlyDS/Content", "r");
		buf = new byte[(int) file.length()];
		file.readFully(buf);
		file.close();

		out = decoder.doFinal(buf);
		System.out.println(new String(out));
		
		file = new RandomAccessFile("test_lit/__DataSpace/Storage/EbEncryptDS/Content", "r");
		buf = new byte[(int) file.length()];
		file.readFully(buf);
		file.close();

		out = decoder.doFinal(buf);
		file = new RandomAccessFile("test_lit/__Content", "rw");
		file.write(out);
		file.close();
	}

}
