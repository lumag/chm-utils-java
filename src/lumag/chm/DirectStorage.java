package lumag.chm;

import java.io.IOException;

import lumag.util.BasicReader;

class DirectStorage implements IDataStorage {
	private final long contentOffset;
	private final long contentLength;
	private final BasicReader reader;

	DirectStorage(BasicReader reader, long contentOffset,
			long contentLength) {
		this.reader = reader;
		this.contentOffset = contentOffset;
		this.contentLength = contentLength;
	}

	public byte[] getData(long offset, int length) throws FileFormatException {
		if (offset + length > contentLength) {
			throw new FileFormatException("Read beyond the end of content");
		}

		try {
			reader.seek(contentOffset + offset);
			return reader.read(length);
		} catch (IOException e) {
			throw new FileFormatException(e);
		}
	}
}
