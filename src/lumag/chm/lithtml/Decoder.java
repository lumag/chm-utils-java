/**
 * 
 */
package lumag.chm.lithtml;

import org.xml.sax.SAXException;

enum Decoder implements Constants {
	TEXT {
		@Override
		protected Decoder process(State state, int ucs32) throws SAXException {
			if (ucs32 == 0) {
				String s = state.getString();
				if (s.length() != 0) {
					state.text(s);
				}

				return FLAG;
			}
			
			state.readUtf8Char(ucs32);

			return this;
		}
	},
	FLAG {
		@Override
		protected Decoder process(State state, int ucs32) {
			state.setFlags(ucs32);
			if ((state.getFlags() & ~0xf) != 0) {
				System.err.print("FLAG 0x" + Integer.toHexString((state.getFlags() & ~0xf)));
			}
			return TAG_NAME;
		}
	},
	TAG_NAME {
		@Override
		protected Decoder process(State state, int ucs32) throws SAXException {
			if (ucs32 == 0) {
				state.endElement();
				state.startString();
				return TEXT;
			}
			
			if (ucs32 == 0x8000){
				return TAG_CUSTOM_LEN; 
			}

			state.openElement(ucs32);
			return ATTR_NAME;
		}
	},
	TAG_CUSTOM_LEN {
		@Override
		protected Decoder process(State state, int ucs32) {
			state.startString(ucs32 - 1);
			return TAG_CUSTOM;
		}
	},
	TAG_CUSTOM {
		@Override
		protected Decoder process(State state, int ucs32) {
			if (state.readUtf8Char(ucs32)) {
				state.openElement(state.getString());
				return ATTR_NAME;
			}
			return this;
		}
	},
	ATTR_NAME {
		@Override
		protected Decoder process(State state, int ucs32) throws SAXException {
			if (ucs32 == 0) {
				state.startElement();
				if ((state.getFlags() & FLAGS_CLOSE) != 0) {
					state.endElement();
				}
				state.startString();
				return TEXT;
			}

			if (ucs32 == 0x8000){
				return ATTR_CUSTOM_LEN;
			}

			String name = state.getAttribute(ucs32);
			if (name == null) {
				name = "_0x" + Integer.toHexString(ucs32) + "_";
			}
			state.addAttribute(name);

			return ATTR_VALUE_LEN;
		}
	},
	ATTR_CUSTOM_LEN {
		@Override
		protected Decoder process(State state, int ucs32) {
			state.startString(ucs32 - 1);
			return ATTR_CUSTOM;
		}
	},
	ATTR_CUSTOM {
		@Override
		protected Decoder process(State state, int ucs32) {
			if (state.readUtf8Char(ucs32)) {
				state.addAttribute(state.getString());
				return ATTR_VALUE_LEN;
			}
			return this;
		}
	},
	ATTR_VALUE_LEN {
		@Override
		protected Decoder process(State state, int ucs32) {
			if (ucs32 == 0xffff) {
				return ATTR_VALUE_NUMBER;
			}
			if (ucs32 != 1) {
				state.startString(ucs32 - 1);
				return ATTR_NAME;
			}
			return ATTR_VALUE;
		}
	},
	ATTR_VALUE {
		@Override
		protected Decoder process(State state, int ucs32) {
			if (state.readUtf8Char(ucs32)) {
				state.setAttributeValue(state.getString());
				return ATTR_NAME;
			}
			return this;
		}
	},
	ATTR_VALUE_NUMBER {
		@Override
		protected Decoder process(State state, int ucs32) {
			state.setAttributeValue(Integer.toString(ucs32));
			return ATTR_NAME;
		}
	},
	;

	protected abstract Decoder process(State state, int ucs32) throws SAXException;
}