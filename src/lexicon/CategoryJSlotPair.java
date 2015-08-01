package lexicon;

import utils.Hash;

/*
 * keys for one of the hashMaps in Relations
 */
public class CategoryJSlotPair {
	public String catString;
	public short slot;

	public CategoryJSlotPair(String catString, short slot) {
		this.catString = catString;
		this.slot = slot;
	}

	@Override
	public int hashCode() {
		Hash h = new Hash(catString.hashCode());
		h.plusEqual(slot);
		return (int) (h.value()); // int's go up to around 2 billion
	}

	@Override
	public boolean equals(Object other) {
		if ( other == null ) {
			return false;
		}

		return catString.equals(((CategoryJSlotPair) (other)).catString)
				&& slot == ((CategoryJSlotPair) (other)).slot;
	}
}
