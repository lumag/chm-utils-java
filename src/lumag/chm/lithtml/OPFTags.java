/**
 * 
 */
package lumag.chm.lithtml;

import java.util.HashMap;
import java.util.Map;

enum OPFTags implements ITag {
	__UNK,
	PACKAGE,
	DC_TITLE("dc:Title"),
	DC_CREATOR("dc:Creator"),
	__UNK3,
	__UNK4,
	__UNK5,
	__UNK6,
	__UNK7,
	__UNK8,
	__UNK9,
	__UNK10,
	__UNK11,
	__UNK12,
	__UNK13,
	__UNK14,
	MANIFEST,
	ITEM,
	SPINE,
	ITEMREF,
	METADATA,
	DC_METADATA("dc-metadata"),
	DC_SUBJECT("dc:Subject"),
	DC_DESCRIPTION("dc:Description"),
	DC_PUBLISHER("dc:Publisher"),
	DC_CONTRIBUTOR("dc:Contributor"),
	DC_DATE("dc:Date"),
	DC_TYPE("dc:Type"),
	DC_FORMAT("dc:Format"),
	DC_IDENTIFIER("dc:Identifier"),
	DC_SOURCE("dc:Source"),
	DC_LANGUAGE("dc:Language"),
	DC_RELATION("dc:Relation"),
	DC_COVERAGE("dc:Coverage"),
	DC_RIGHTS("dc:Rights"),
	X_METADATA("x-metadata"),
	META,
	TOURS,
	TOUR,
	SITE,
	GUIDE,
	REFERENCE,
	;
	
	private String text;
	private static final Map<Integer, String> commonAttributes;
	private static final String[] atts = {
		null,
		"href",
		"never-used",
		"guid",
		"minimum_level",
		"attr5", // FIXME
		"id",
		"href",
		"media-type",
		"fallback",
		"idref",
		"xmlns:dc",
		"xmlns:oebpackage",
		"role",
		"file-as",
		"event",
		"scheme",
		"title",
		"type",
		"unique-identifier",
		"name",
		"content",
		"xml:lang",
	};
	static {
		commonAttributes = new HashMap<Integer, String>();
		for (int i = 0; i < atts.length; i++) {
			commonAttributes.put(i, atts[i]);
		}
	}

	OPFTags(String s) {
		text = s;
	}

	OPFTags() {
		text = this.name().toLowerCase();
	}
	
	@Override
	public String toString() {
		return text;
	}

	public String getAttribute(int num) {
		return commonAttributes.get(num);
	}

}
