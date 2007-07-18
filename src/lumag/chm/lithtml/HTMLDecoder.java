package lumag.chm.lithtml;

public class HTMLDecoder extends LitXMLDecoder {

	@Override
	protected ITag[] getTags() {
		return HtmlTags.values();
	}

}
