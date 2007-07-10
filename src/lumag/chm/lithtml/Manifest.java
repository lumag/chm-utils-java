package lumag.chm.lithtml;

import java.io.IOException;
import java.util.Map;

import lumag.util.BasicReader;

class Manifest {
	private class Entry {
		private final String id;
		private final String href;
		private final String contentType;

		public Entry(String id, String href, String contentType) {
			this.id = id;
			this.href = href;
			this.contentType = contentType;
		}
	}
	
	Map<String, Entry> map;
	
	Manifest(BasicReader reader) throws IOException {
		
		// unknown
		/*short unk = */reader.readWord();
//		System.out.format("Unknown: %04x%n", unk);
		
		for (int section = 0; section < 4; section ++) {
			int numEntries = reader.readDWord();

			for (int i = 0; i < numEntries; i++) {
				reader.readDWord();
				String id = reader.readString();
				String name = reader.readString();
				String type = reader.readString();
				reader.readByte();
				
				map.put(id, new Entry(id, name, type));
			}
		}
	}
}
