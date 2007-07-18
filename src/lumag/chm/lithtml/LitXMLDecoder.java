package lumag.chm.lithtml;

import java.io.IOException;

import lumag.util.BasicReader;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public abstract class LitXMLDecoder {
	protected abstract ITag[] getTags();

	void decode(BasicReader reader, ContentHandler handler) throws IOException, SAXException {
		State state = new State(handler, getTags());

		state.start();

		while (!reader.isEOF()) {
			int ucs32 = reader.readUtf8Char();
//			System.err.format("%16s: %d%n", state, ucs32);
			state.process(ucs32);
		}
		
		state.finish();
	}

}
