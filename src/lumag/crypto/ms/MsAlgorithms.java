package lumag.crypto.ms;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.Provider;

@SuppressWarnings("serial")
public final class MsAlgorithms extends Provider {
	private static final String NAME = "MsAlgorithms";
	private static final String version = "1.0";
	
	public MsAlgorithms() {
		super(NAME, Double.parseDouble(version), NAME + " v" + version +
				" implementing Microsoft cryptographic algorithms");

		AccessController.doPrivileged(new PrivilegedAction<Object>() {
			public Object run() {
				put("MessageDigest.MS-SHA-160", lumag.crypto.ms.MsSHA1.class.getName());
				put("Alg.Alias.MessageDigest.MS-SHA-1", "MS-SHA-160");
				put("Alg.Alias.MessageDigest.MS-SHA1", "MS-SHA-160");
				
				put("Cipher.MS-DES", lumag.crypto.ms.MsDES.class.getName());
				put("Alg.Alias.Cipher.MSDES", "MS-DES");
				return null;
			}
		});
	}
}
