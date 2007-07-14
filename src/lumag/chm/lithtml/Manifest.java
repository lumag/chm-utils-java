package lumag.chm.lithtml;

import java.io.IOException;
import java.util.HashMap;
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
		
		@Override
		public int hashCode() {
			return id.hashCode();
		}
		
		@Override
		public boolean equals(Object o) {
			if (o instanceof Entry) {
				Entry e = (Entry) o;
				return id.equals(e.id) && href.equals(e.href) && contentType.equals(e.contentType);
			}
			return false;
		}
	}
	
	Map<String, Entry> map = new HashMap<String, Entry>();
	
	Manifest(BasicReader reader) throws IOException {
		
		// unknown
		/*short unk = */reader.readWord();
//		System.out.format("Unknown: %04x%n", unk);
		
		for (int section = 0; section < 4; section ++) {
			int numEntries = reader.readDWord();

			for (int i = 0; i < numEntries; i++) {
				reader.readDWord();
				String id = reader.readLongString();
				String name = reader.readLongString();
				String type = reader.readLongString();
				reader.readByte();
				
				map.put(id, new Entry(id, name, type));
			}
		}
	}

	public String find(String id) {
		if (!map.containsKey(id)) {
			return null;
		}
		return map.get(id).href;
	}
}
