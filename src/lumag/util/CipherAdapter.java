package lumag.util;

import java.security.InvalidKeyException;
import java.security.Key;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

import lumag.crypto.ms.MsDesAdapter;

public abstract class CipherAdapter {
	public abstract void init(int opmode, Key key)
				throws InvalidKeyException;
	
	public abstract byte[] doFinal(byte[] data) throws IllegalBlockSizeException, BadPaddingException;
	
	public static CipherAdapter newAdapter(final Cipher cpi) {
		return new CipherAdapter() {
			@Override
			public void init(int opmode, Key key)
					throws InvalidKeyException {
				cpi.init(opmode, key);
			}

			@Override
			public byte[] doFinal(byte[] data) throws IllegalBlockSizeException, BadPaddingException {
				return cpi.doFinal(data);
			}
		};
	}
	
	public static CipherAdapter newMsDesAdapter() {
		return new MsDesAdapter();
	}
}
