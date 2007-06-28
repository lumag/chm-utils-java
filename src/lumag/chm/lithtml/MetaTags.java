/**
 * 
 */
package lumag.chm.lithtml;

enum MetaTags {
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

	MetaTags(String s) {
		text = s;
	}

	MetaTags() {
		text = this.name().toLowerCase();
	}
	
	@Override
	public String toString() {
		return text;
	}
}