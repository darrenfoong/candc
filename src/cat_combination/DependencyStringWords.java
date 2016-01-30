package cat_combination;

import utils.Hash;

// used by the oracle decoder and IgnoreDepsEval class which stores
// which dependencies are ignored by the evaluate script; we have to
// store heads and fillers as strings, not sentence indices

public class DependencyStringWords implements Comparable<DependencyStringWords> {
	private final short relID;
	private final String head;
	private final String filler;
	private final short unaryRuleID;

	public DependencyStringWords(short relID, String head, String filler, short unaryRuleID) {
		this.relID = relID;
		this.head = head;
		this.filler = filler;
		this.unaryRuleID = unaryRuleID;
	}

	@Override
	public int compareTo(DependencyStringWords other) {
		int compare;
		if ( (compare = Short.compare(this.relID, other.relID)) != 0 ) { return compare; }
		if ( (compare = this.head.compareTo(other.head)) != 0 ) { return compare; }
		if ( (compare = this.filler.compareTo(other.filler)) != 0 ) { return compare; }
		if ( (compare = Short.compare(this.unaryRuleID, other.unaryRuleID)) != 0 ) { return compare; }

		return 0;
	}

	@Override
	public int hashCode() {
		Hash h = new Hash(relID);
		h.plusEqual(head.hashCode());
		h.plusEqual(filler.hashCode());
		h.plusEqual(unaryRuleID);
		return (int) (h.value());
	}

	@Override
	public boolean equals(Object other) {
		if ( other == null || getClass() != other.getClass() ) {
			return false;
		}

		DependencyStringWords cother = (DependencyStringWords) other;

		return compareTo(cother) == 0;
	}
}
