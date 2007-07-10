package lumag.chm.lithtml;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Writer;

import lumag.chm.lithtml.LitXMLDecoder.LitXMLType;
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
		new Manifest(reader);
		
		reader.close();

		reader = new BasicReader(new RandomAccessFile(args[0], "r"));
		Writer outputStream = new BufferedWriter(new FileWriter(args[1]));
		LitXMLDecoder decoder = new LitXMLDecoder();
		try {
			decoder.decode(reader, LitXMLType.HTML, new LitXMLTest(outputStream));
		} finally {
			outputStream.close();
		}
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
