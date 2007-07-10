/**
 * 
 */
package lumag.chm.lithtml;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


enum HtmlTags implements ITag {
	__UNK,
	__UNK1,
	__UNK2,
	A(
		0x0001, "href"),
	ACRONYM,
	ADDRESS,
	APPLET,
	AREA,
	B,
	BASE,
	BASEFONT,
	BDO,
	BGSOUND,
	BIG,
	BLINK,
	BLOCKQUOTE,
	BODY(
		0x07db, "link",
		0x07dd, "vlink",
		0x9392, "lang"),
	BR,
	BUTTON,
	CAPTION,
	CENTER,
	CITE,
	CODE,
	COL,
	COLGROUP,
	__UNK25,
	__UNK26,
	DD,
	DEL,
	DFN,
	DIR,
	DIV(
		0x804b, "style",
		0x83ea, "class",
		0x83eb, "id"),
	DL,
	DT,
	EM,
	EMBED,
	FIELDSET,
	FONT,
	FORM,
	FRAME,
	FRAMESET,
	__UNK41,
	H1(
		0x8049, "align",
		0x83eb, "id"),
	H2(
		0x83eb, "id"),
	H3,
	H4,
	H5,
	H6,
	HEAD,
	HR(
		0x8006, "width"),
	HTML(
		0x83eb, "id"),
	I,
	IFRAME,
	IMG(
		0x03ec, "src",
		0x03ed, "border",
		0x03ef, "hspace",
		0x8006, "width",
		0x8007, "height",
		0x804a, "align"),
	INPUT,
	INS,
	KBD,
	LABEL,
	LEGEND,
	LI,
	LINK(
		0x03ee, "href",
		0x03ef, "rel",
		0x03f1, "type"),
	LISTING,
	MAP,
	MARQUEE,
	MENU,
	META(
		0x03ea, "http-equiv",
		0x03eb, "content",
		0x8001, "name"),
	NEXTID,
	NOBR,
	NOEMBED,
	NOFRAMES,
	NOSCRIPT,
	OBJECT,
	OL,
	OPTION,
	P(
		0x8049, "align",
		0x804b, "style",
		0x83ea, "class"),
	PARAM,
	PLAINTEXT,
	PRE,
	Q,
	RP,
	RT,
	RUBY,
	S,
	SAMP,
	SCRIPT,
	SELECT,
	SMALL,
	SPAN(
		0x804b, "style",
		0x83ea, "class",
		0x9392, "lang"),
	STRIKE,
	STRONG,
	STYLE,
	SUB,
	SUP,
	TABLE,
	TBODY,
	TC,
	TD,
	TEXTAREA,
	TFOOT,
	TH,
	THEAD,
	TITLE,
	TR,
	TT,
	U,
	UL,
	VAR,
	WBR,
	;
	
//	private static final Map<Integer, String> commonAttributes;
//	static {
//		commonAttributes = new HashMap<Integer, String>();
//		commonAttributes.put(0x0001, "href");
//
//		commonAttributes.put(0x8001, "name");
//		commonAttributes.put(0x8006, "width");
//		commonAttributes.put(0x8007, "height");
//		commonAttributes.put(0x8010, "tabindex");
//		commonAttributes.put(0x8046, "title");
//		commonAttributes.put(0x8049, "align"); // warning: duplicate! "align". maybe one of them is horizontal-align?
//		commonAttributes.put(0x804a, "align");
//		commonAttributes.put(0x804b, "style");
//		commonAttributes.put(0x804d, "disabled");
//
//		commonAttributes.put(0x83ea, "class");
//		commonAttributes.put(0x83eb, "id");
//		commonAttributes.put(0x83fe, "datafld");
//		commonAttributes.put(0x83ff, "datasrc");
//
//		commonAttributes.put(0x8400, "dataformatas");
//		commonAttributes.put(0x87d6, "accesskey");
//
//		// class-related???
//		// FIXME: move them to correct tags!
//		commonAttributes.put(0x8bbb, "classid");
//		commonAttributes.put(0x8bbc, "data");
//		commonAttributes.put(0x8bbd, "palette");
//		commonAttributes.put(0x8bbe, "pluginspage");
//		commonAttributes.put(0x8bbf, "codebase");
//		commonAttributes.put(0x8bc0, "codetype");
//		commonAttributes.put(0x8bc1, "code");
//		commonAttributes.put(0x8bc2, "type");
//		commonAttributes.put(0x8bc3, "hidden");
//		commonAttributes.put(0x8bc5, "vspace");
//		commonAttributes.put(0x8bc6, "hspace");
//
//		// public
//		commonAttributes.put(0x938a, "background");
//		commonAttributes.put(0x938b, "color");
//		commonAttributes.put(0x938e, "nowrap");
//		commonAttributes.put(0x9392, "lang");
//		commonAttributes.put(0x9399, "clear");
//		commonAttributes.put(0x939a, "type");
//		commonAttributes.put(0x939b, "face");
//		commonAttributes.put(0x939c, "size"); // duplicate size
//		commonAttributes.put(0x93a3, "size");
//		commonAttributes.put(0x93a5, "bordercolor");
//		commonAttributes.put(0x93a6, "bordercolorlight");
//		commonAttributes.put(0x93a7, "bordercolordark");
//		commonAttributes.put(0x93a8, "valign");
//		commonAttributes.put(0x93ae, "topmargin");
//		commonAttributes.put(0x93af, "rightmargin");
//		commonAttributes.put(0x93b0, "bottommargin");
//		commonAttributes.put(0x93b1, "leftmargin");
//		commonAttributes.put(0x93b6, "bgproperties");
//		commonAttributes.put(0x93d8, "scroll");
//		commonAttributes.put(0x93ee, "value");
//		commonAttributes.put(0x93ed, "language");
//		commonAttributes.put(0x93fe, "dir");
//
//		// events
//		commonAttributes.put(0x9771, "onmouseover");
//		commonAttributes.put(0x9772, "onmouseout");
//		commonAttributes.put(0x9773, "onmousedown");
//		commonAttributes.put(0x9774, "onmouseup");
//		commonAttributes.put(0x9775, "onmousemove");
//		commonAttributes.put(0x9776, "onkeydown");
//		commonAttributes.put(0x9777, "onkeyup");
//		commonAttributes.put(0x9778, "onkeypress");
//		commonAttributes.put(0x9779, "onclick");
//		commonAttributes.put(0x977a, "ondblclick");
//		commonAttributes.put(0x977b, "onselect");
//		commonAttributes.put(0x977c, "onsubmit");
//		commonAttributes.put(0x977d, "onreset");
//		commonAttributes.put(0x977e, "onhelp");
//		commonAttributes.put(0x977f, "onfocus");
//		commonAttributes.put(0x9780, "onblur");
//		// 0x9781
//		// 0x9782
//		commonAttributes.put(0x9783, "onrowexit");
//		commonAttributes.put(0x9784, "onrowenter");
//		commonAttributes.put(0x9785, "onbounce");
//		commonAttributes.put(0x9786, "onbeforeupdate");
//		commonAttributes.put(0x9787, "onafterupdate");
//		// 0x9788
//		// 0x9789
//		commonAttributes.put(0x978a, "onreadystatechange");
//		commonAttributes.put(0x978b, "onfinish");
//		commonAttributes.put(0x978c, "onstart");
//		commonAttributes.put(0x978d, "onabort");
//		commonAttributes.put(0x978e, "onerror");
//		commonAttributes.put(0x978f, "onchange");
//		commonAttributes.put(0x9790, "onscroll");
//		commonAttributes.put(0x9791, "onload");
//		commonAttributes.put(0x9792, "onunload");
//		// 0x9793
//		commonAttributes.put(0x9794, "ondragstart");
//		commonAttributes.put(0x9795, "onresize");
//		commonAttributes.put(0x9796, "onselectstart");
//		commonAttributes.put(0x9797, "onerrorupdate");
//		commonAttributes.put(0x9798, "onbeforeunload");
//		commonAttributes.put(0x9799, "ondatasetchanged");
//		commonAttributes.put(0x979a, "ondataavailable");
//		commonAttributes.put(0x979b, "ondatasetcomplete");
//		commonAttributes.put(0x979c, "onfilterchange");
//		// 0x979d
//		// 0x979e
//		commonAttributes.put(0x979f, "onlosecapture");
//		commonAttributes.put(0x97a0, "onpropertychange");
//		// 0x97a1
//		commonAttributes.put(0x97a2, "ondrag");
//		commonAttributes.put(0x97a3, "ondragend");
//		commonAttributes.put(0x97a4, "ondragenter");
//		commonAttributes.put(0x97a5, "ondragover");
//		commonAttributes.put(0x97a6, "ondragleave");
//		commonAttributes.put(0x97a7, "ondrop");
//		commonAttributes.put(0x97a8, "oncut");
//		commonAttributes.put(0x97a9, "oncopy");
//		commonAttributes.put(0x97aa, "onpaste");
//		commonAttributes.put(0x97ab, "onbeforecut");
//		commonAttributes.put(0x97ac, "onbeforecopy");
//		commonAttributes.put(0x97ad, "onbeforepaste");
//		// 0x97ae
//		commonAttributes.put(0x97af, "onrowsdelete");
//		commonAttributes.put(0x97b0, "onrowsinserted");
//		commonAttributes.put(0x97b1, "oncellchange");
//		commonAttributes.put(0x97b2, "oncontextmenu");
//		commonAttributes.put(0x97b3, "onbeforeprint");
//		commonAttributes.put(0x97b4, "onafterprint");
//		commonAttributes.put(0x97b6, "onbeforeeditfocus");
//		// 0x97b7 -- ...
//
//		commonAttributes.put(0xfe0c, "bgcolor");
//	}
	
	private final Map<Integer, String> attributes = new HashMap<Integer, String>();
	private HtmlTags(Object... attrs) {
		if (attrs.length % 2 != 0) {
			throw new InternalError("Bad arguments array: " + Arrays.toString(attrs));
		}
		
		for (int i = 0; i < attrs.length; i+= 2) {
			attributes.put((Integer) attrs[i], (String) attrs[i+1]);
		}
	}

	@Override
	public String toString() {
		return name().toLowerCase();
	}

	public String getAttribute(int num) {
		return attributes.get(num);
//		return commonAttributes.get(num);
	}


}
