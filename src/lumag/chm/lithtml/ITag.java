package lumag.chm.lithtml;

import java.util.Map;

interface ITag {
	Map<Integer, String> getCommonAttributes();
	Map<Integer, String> getAttributes();
}
