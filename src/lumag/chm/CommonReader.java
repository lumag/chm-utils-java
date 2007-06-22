package lumag.chm;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import lumag.util.ReaderHelper;

abstract class CommonReader {

	private static final String LZXC_GUID = "{7FC28940-9D31-11D0-9B27-00A0C91E9C7C}";
	private static final int GUID_LENGTH = 38;
	private static final byte[] HEADER_FILE = {'I', 'T', 'S', 'F'};
	private static final byte[] HEADER_FILE_SECTION = {(byte) 0xfe, 0x01, 0x00, 0x00};
	private static final int SECTION_FILE_SIZE = 0;
	protected static final int SECTION_INDEX = 1;

	private static final String CONTENT_UNCOMPRESSED = "Uncompressed";
	private static final String FILE_NAME_LIST = "::DataSpace/NameList";
	private static final String FILE_TRANSFORM_LIST = "::DataSpace/Storage/%s/Transform/List"; 
	private static final String FILE_CONTROL_DATA = "::DataSpace/Storage/%s/ControlData";
	private static final String FILE_CONTENT = "::DataSpace/Storage/%s/Content";
	private static final String FILE_TRANSFORM_INSTANCE_DATA = "::DataSpace/Storage/%s/Transform/%s/InstanceData/";
	private static final String LZXC_BAD_GUID = new String(
			new byte[] {'{', 0, '7', 0, 'F', 0, 'C', 0, '2', 0, '8', 0, '9', 0, '4', 0, '0', 0, '-', 0,
						'9', 0, 'D', 0, '3', 0, '1', 0, '-', 0, '1', 0, '1', 0, 'D', 0, '0', 0}); 

	private class Content {
		private final String name;
		private IDataStorage reader;

		public Content(String name,
				IDataStorage reader) {
			this.name = name;
			this.reader = reader;
		}
		
		@Override
		public String toString() {
			return name;
		}
		
		public String getName() {
			return name;
		}
	}

	@SuppressWarnings("unused")
	private long fileSize;
	protected long dataOffset;

	private long[] sectionOffsets;
	private long[] sectionLengths;
	
	private Content[] content;

	private Map<String, ListingEntry> listing = new LinkedHashMap<String, ListingEntry>();

	protected void readFormatHeader(RandomAccessFile input) throws IOException, FileFormatException {
		ReaderHelper.checkHeader(input, HEADER_FILE);
		
		int version = ReaderHelper.readDWord(input);
		System.out.println("ITSF Version: " + version);
		if (version != 4 && version != 3 && version != 2) {
			throw new FileFormatException("ITSF Version " + version + " is unsupported");
		}

		int headerLen = ReaderHelper.readDWord(input);
//		System.out.println("Header len: " + headerLen);
		if ((version == 2 && headerLen != 0x58) || 
			(version == 3 && headerLen != 0x60) ||
			(version == 4 && headerLen != 0x20)) {
			throw new FileFormatException("bad section length");
		}
		
		ReaderHelper.readDWord(input); // 1
		
		if (version == 4) {
			dataOffset = ReaderHelper.readQWord(input);
		}
		
		ReaderHelper.readDWord(input); // time
		
		ReaderHelper.readDWord(input); // LCID
		
		if (version == 2 || version == 3) {
			ReaderHelper.readGUID(input);
			ReaderHelper.readGUID(input);
		
		
			readSectionTable(input, 2);
		}
		
		if (version == 2) {
			dataOffset = sectionOffsets[SECTION_INDEX] + sectionLengths[SECTION_INDEX];
		} else if (version == 3) {
			dataOffset = ReaderHelper.readQWord(input);
		}
	}

	protected void readSectionTable(RandomAccessFile input, int size) throws IOException {
		sectionOffsets = new long[size];
		sectionLengths = new long[size];
		
		for (int i = 0; i < size; i++) {
			sectionOffsets[i] = ReaderHelper.readQWord(input);
			sectionLengths[i] = ReaderHelper.readQWord(input);
		}
		
		System.out.println("Offsets: " + Arrays.toString(sectionOffsets));
		System.out.println("Lengths: " + Arrays.toString(sectionLengths));
	}

	protected long getSectionOffset(int section) {
		return sectionOffsets[section];
	}

	protected long getSectionLengths(int section) {
		return sectionLengths[section];
	}

	protected void readFileSizeSection(RandomAccessFile input) throws FileFormatException, IOException {
		input.seek(sectionOffsets[SECTION_FILE_SIZE]);
		ReaderHelper.checkHeader(input, HEADER_FILE_SECTION);
	
		if (sectionLengths[SECTION_FILE_SIZE] < 0x18) {
			throw new FileFormatException("FileSize section is too small");
		} else if (sectionLengths[SECTION_FILE_SIZE] > 0x18) {
			System.out.format("Warning: extra %d bytes at the end of FileSize section%n", sectionLengths[SECTION_FILE_SIZE] - 0x18);
		}
	
		int unk = ReaderHelper.readDWord(input); // mostly 0. One file with 1
		if (unk != 0) {
			System.out.println("Warning: unknown element expected to be zero: " + unk);
		}
		fileSize = ReaderHelper.readQWord(input);
		System.out.println("Expected file size: " + fileSize);
	
		ReaderHelper.readDWord(input); // 0
		ReaderHelper.readDWord(input); // 0
	
	}

	protected void readListingEntries(RandomAccessFile input, long endPos) throws IOException, FileFormatException {
			while (input.getFilePointer() < endPos) {
				String name = ReaderHelper.readString(input);
				int section = (int) ReaderHelper.readCWord(input);
				long offset = ReaderHelper.readCWord(input);
				long len = ReaderHelper.readCWord(input);
				listing.put(name, new ListingEntry(name, section, offset, len));
	//			System.out.println(name);
			}
		}

	protected void readContentData(RandomAccessFile input) throws IOException, FileFormatException {
		ListingEntry nameList = listing.get(FILE_NAME_LIST);
	
		input.seek(dataOffset + nameList.offset);
	
		short len = ReaderHelper.readWord(input);
		if (len * 2 != nameList.length) {
			throw new FileFormatException("Incorrect " + FILE_NAME_LIST + " length");
		}
		
		short entries = ReaderHelper.readWord(input);
//		if (entries != 2) {
//			System.out.println("Warning: more than two compression sections");
//		}
		
		content = new Content[entries];
		
		for (int i = 0; i < entries; i++) {
			short nameLen = ReaderHelper.readWord(input);
			char[] name = new char[nameLen];
			for (int j = 0; j < nameLen; j++) {
				name[j] = (char) ReaderHelper.readWord(input);
			}
			ReaderHelper.readWord(input); // terminal zero
			String sName = new String(name);
			IDataStorage data;
			if (i == 0) {
				data = new DirectStorage(input, dataOffset, fileSize - dataOffset);
			} else {
				Formatter fmt = new Formatter();
				fmt.format(FILE_CONTENT, sName);
				ListingEntry entry = listing.get(fmt.toString());
				fmt.close();
				
				if (entry == null) {
					throw new FileFormatException("No Content for " + sName);
				}

				data = new DirectStorage(input, dataOffset + entry.offset, entry.length);
			}
			content[i] = new Content(sName, data);
		}
		System.out.println(Arrays.toString(content));
		
		for (Content cnt: content) {
			String name = cnt.getName();
			if (CONTENT_UNCOMPRESSED.equals(name)) {
				continue;
			}
			
			Formatter fmt;

			fmt = new Formatter();
			fmt.format(FILE_CONTROL_DATA, name);
			byte[] controlData = getFile(fmt.toString());
			fmt.close();

			int controlDataOffset = 0;
			
			fmt = new Formatter();
			fmt.format(FILE_TRANSFORM_LIST, name);
			byte[] transforms = getFile(fmt.toString());
			fmt.close();
			
			// FIXME: check that for .lit it's still 38!
			for (int i = 0; i < transforms.length / GUID_LENGTH; i++) {
				int cdSize = ReaderHelper.getDWord(controlData, controlDataOffset);
				controlDataOffset += 4;
				if (cdSize > controlData.length - controlDataOffset) {
					throw new FileFormatException("Bad transformation control data");
				}
				int newControlDataOffset = controlDataOffset + cdSize*4;
				byte[] cd = Arrays.copyOfRange(controlData, controlDataOffset, newControlDataOffset);
				
				String guid = new String(transforms, i * GUID_LENGTH, GUID_LENGTH);
				if (LZXC_BAD_GUID.equals(guid)) {
					guid = LZXC_GUID;
				}

				Map<String, byte[]> files = new HashMap<String, byte[]>();
				fmt = new Formatter();
				fmt.format(FILE_TRANSFORM_INSTANCE_DATA, name, guid);
				String prefix = fmt.toString();
				int prefixLen = prefix.length();
				fmt.close();

				for (ListingEntry entry: listing.values()) {
					if (entry.name.startsWith(prefix)) {
						files.put(entry.name.substring(prefixLen), getFile(entry));
					}
				}

				// FIXME: Select correct transformation based on the GUID
				ITransformation transform = new LZXCTransformation();
				transform.init(cd, files , cnt.reader);
				cnt.reader = transform;
				
				controlDataOffset = newControlDataOffset;
			}

			if (controlData.length != controlDataOffset) {
				throw new FileFormatException("Extra data at the end of transformation control data");
			}
		}
	}
	
	public ListingEntry getFileEntry(String name) {
		return listing.get(name);
	}

	public Collection<ListingEntry> getFiles() {
		return Collections.unmodifiableCollection(listing.values());
	}
	
	public byte[] getFile(String name) throws IOException, FileFormatException {
		ListingEntry entry = getFileEntry(name);
		
		if (entry == null) {
			throw new FileNotFoundException();
		}

		return getFile(entry);
	}

	private byte[] getFile(ListingEntry entry) throws FileFormatException {
		return content[entry.section].reader.getData(entry.offset, (int) entry.length);
	}

	@SuppressWarnings("unused")
	protected void dump(String path) throws IOException, FileFormatException {
		File parent = new File(path);
		parent.mkdirs();
		for (ListingEntry entry: getFiles()) {
			File f = new File(parent, entry.name);
			System.out.println(entry.name + ": " + entry.section + " @ " + entry.offset + " = " + entry.length);
			f.getParentFile().mkdirs();
			if (entry.name.charAt(entry.name.length() - 1) == '/') {
				f.mkdir();
			} else {
				byte[] data = getFile(entry.name);
				OutputStream output = new BufferedOutputStream(new FileOutputStream(f));
				output.write(data);
				output.close();
			}
		}
	}

}
