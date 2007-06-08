/**
 * 
 */
package lumag.chm;

class ListingEntry {
	public final String name;
	public final int section;
	public final long offset;
	public final long length;

	public ListingEntry(final String name, final int section, final long offset, final long length) {
		this.name = name;
		this.section = section;
		this.offset = offset;
		this.length = length;
	}
	
	@Override
	public String toString() {
		return "File '" + name + "'" +
			" is at section " + section +
			" offset " + offset +
			" length " + length; 
	}
}