package lumag.chm;

interface IDataStorage {
	byte[] getData(long offset, int length) throws FileFormatException;
}

