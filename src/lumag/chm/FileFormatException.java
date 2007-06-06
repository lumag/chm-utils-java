package lumag.chm;

@SuppressWarnings("serial")
public class FileFormatException extends Exception {

	public FileFormatException() {
		// Do nothing;
	}

	public FileFormatException(String message) {
		super(message);
	}

	public FileFormatException(Throwable cause) {
		super(cause);
	}

	public FileFormatException(String message, Throwable cause) {
		super(message, cause);
	}

}
