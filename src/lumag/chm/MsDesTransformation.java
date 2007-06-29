package lumag.chm;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.Arrays;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import lumag.crypto.ms.MsAlgorithms;
import lumag.util.CipherAdapter;

class MsDesTransformation implements ITransformation {
	static {
		Security.addProvider(new MsAlgorithms());
	}

	private CipherAdapter decoder;
	private IDataStorage prev;


	public void init(CommonReader reader, IDataStorage parent,
			String guid, byte[] controlData, Map<String, byte[]> data) throws FileFormatException, IOException {

		this.prev = parent;
		try {

			try {
				Cipher des = Cipher.getInstance("MS-DES", "MsAlgorithms");
				decoder = CipherAdapter.newAdapter(des);
			} catch (SecurityException e) {
				decoder = CipherAdapter.newMsDesAdapter();
			}

			byte[] btempkey = generateTempKey(reader);

			Key tempkey = new SecretKeySpec(btempkey, "DES");
			decoder.init(Cipher.DECRYPT_MODE, tempkey);

			byte[] out;
			out = decoder.doFinal(reader.getFile("/DRMStorage/DRMSealed"));

			System.out.println(Arrays.toString(out));
			
			byte[] bkey = Arrays.copyOfRange(out, 1, 9);
			Key key = new SecretKeySpec(bkey, "DES");
			decoder.init(Cipher.DECRYPT_MODE, key);

		} catch (GeneralSecurityException e) {
			throw new FileFormatException(e);
		}
		
	}

	private byte[] generateTempKey(CommonReader reader)
			throws NoSuchAlgorithmException, NoSuchProviderException,
			IOException, FileFormatException {
		MessageDigest md = MessageDigest.getInstance("MS-SHA-1", "MsAlgorithms");
		byte[] buf;
		int len;
		md.update(new byte[2]); // padding
		buf = reader.getFile("/meta");
		md.update(buf);
		len = (buf.length + 2) % 64;
		if (len != 0) {
			md.update(new byte[64 - len]);
		}
		buf = reader.getFile("/DRMStorage/DRMSource");
		md.update(buf);
		len = buf.length % 64;
		if (len != 0) {
			md.update(new byte[64 - len]);
		}
		
		byte[] digest = md.digest();
		byte[] btempkey = new byte[8];
		for (int i = 0; i < digest.length; i++) {
			btempkey[i%8] ^= digest[i];
		}
		return btempkey;
	}

	public byte[] getData(long offset, int length) throws FileFormatException {
		long dataOffset = offset &~7;
		int dataLength = (length + (int) (offset - dataOffset) + 7) &~7;
		byte[] data = prev.getData(dataOffset, dataLength);
		try {
			data = decoder.doFinal(data);
		} catch (GeneralSecurityException e) {
			throw new FileFormatException(e);
		}
		
		if (dataOffset == offset && dataLength == length) {
			return data;
		}
		return Arrays.copyOfRange(data, (int) (offset - dataOffset), (int) (offset - dataOffset + length));
	}

}
