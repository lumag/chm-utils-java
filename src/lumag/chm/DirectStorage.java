package lumag.chm;

import java.io.IOException;
import java.io.RandomAccessFile;

class DirectStorage implements IDataStorage {
	private final long contentOffset;
	private final long contentLength;
	private final RandomAccessFile input;

	DirectStorage(RandomAccessFile input, long contentOffset,
			long contentLength) {
		this.input = input;
		this.contentOffset = contentOffset;
		this.contentLength = contentLength;
	}

	@Override
	public byte[] getData(long offset, int length) throws FileFormatException {
		if (offset + length > contentLength) {
			throw new FileFormatException("Read beyond the end of content");
		}

		try {
			input.seek(contentOffset + offset);
			byte[] data = new byte[length];
			input.readFully(data);
			return data;
		} catch (IOException e) {
			throw new FileFormatException(e);
		}
	}
}
