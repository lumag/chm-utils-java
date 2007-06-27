package lumag.chm;

import java.io.IOException;
import java.util.Map;

interface ITransformation extends IDataStorage {
	void init(CommonReader reader, IDataStorage parent, String guid, byte[] controlData, Map<String, byte[]> data) throws FileFormatException, IOException;
}

