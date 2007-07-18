package lumag.chm.lithtml;

public class OPFDecoder extends LitXMLDecoder {

	@Override
	protected ITag[] getTags() {
		return OPFTags.values();
	}

}
