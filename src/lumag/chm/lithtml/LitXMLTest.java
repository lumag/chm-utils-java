package lumag.chm.lithtml;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Writer;

import lumag.util.BasicReader;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class LitXMLTest extends DefaultHandler {

	private final Appendable output;
	private boolean tagOpened;

	public LitXMLTest(Appendable output) {
		this.output = output;
	}

	public static void main(String[] args) throws Exception {
		BasicReader reader;
		reader = new BasicReader(new RandomAccessFile("test_lit/manifest", "r"));
		Manifest manifest = new Manifest(reader);
		reader.close();

		String file = args[0];

		String out = manifest.find(file);
		if (out == null) {
			out = "test.html";
		}
		System.out.format("Decoding %s to %s%n", file, out);

		String name;

		LitXMLDecoder decoder;
		if (file.equals("meta")) {
			name = "test_lit/meta";
			decoder = new OPFDecoder();
		} else {
			name = "test_lit/data/" + file + "/content";
			decoder = new HTMLDecoder();
		}
		reader = new BasicReader(new RandomAccessFile(name , "r"));
		new File("out").mkdir();
		Writer outputStream = new BufferedWriter(new FileWriter("out/" + out));
		try {
			decoder.decode(reader, new LitXMLTest(outputStream));
		} finally {
			outputStream.close();
		}
		reader.close();
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		try {
			if (tagOpened) {
				tagOpened = false;
				output.append('>');
			}
			output.append(new String(ch, start, length));
		} catch (IOException e) {
			throw new SAXException(e);
		}
	}

	@Override
	public void startElement(String uri, String localName, String name,
			Attributes attributes) throws SAXException {
		try {
			if (tagOpened) {
				tagOpened = false;
				output.append('>');
			}
			output.append('<');
			output.append(name);
			int length = attributes.getLength();
			for (int i = 0; i < length; i++) {
				output.append(' ');
				output.append(attributes.getQName(i));
				output.append("=\"");
				output.append(attributes.getValue(i));
				output.append('"');
			}
			tagOpened = true;
		} catch (IOException e) {
			throw new SAXException(e);
		}
	}

	@Override
	public void endElement(String uri, String localName, String name)
			throws SAXException {
		try {
			if (tagOpened) {
				tagOpened = false;
				output.append(" />");
			} else {
				output.append("</");
				output.append(name);
				output.append('>');
			}
		} catch (IOException e) {
			throw new SAXException(e);
		}
	}

}
