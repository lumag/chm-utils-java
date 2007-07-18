/**
 * 
 */
package lumag.chm.lithtml;

class CustomTag implements ITag {
	private String name;
	private final ITag baseTag;

	CustomTag(String name, ITag baseTag) {
		this.name = name;
		this.baseTag = baseTag;
	}

	public String getAttribute(int num) {
		return baseTag.getAttribute(num);
	}

	@Override
	public String toString() {
		return name;
	}
	
	
}
