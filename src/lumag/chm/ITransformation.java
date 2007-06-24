package lumag.chm;

import java.io.IOException;
import java.util.Map;

interface ITransformation extends IDataStorage {
	void init(byte[] controlData, Map<String, byte[]> data, IDataStorage link) throws FileFormatException, IOException;
}

