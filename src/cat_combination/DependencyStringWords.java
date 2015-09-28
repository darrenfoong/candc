package cat_combination;

import utils.Hash;

// used by the oracle decoder and IgnoreDepsEval class which stores
// which dependencies are ignored by the evaluate script; we have to
// store heads and fillers as strings, not sentence indices

public class DependencyStringWords implements Comparable<DependencyStringWords> {
	final short relID;
	final String head;
	final String filler;
	final short unaryRuleID;

	public DependencyStringWords(short relID, String head, String filler,
			short unaryRuleID) {
		this.relID = relID;
		this.head = head;
		this.filler = filler;
		this.unaryRuleID = unaryRuleID;
	}

	@Override
	public int hashCode() {
		Hash h = new Hash((head.hashCode()));
		h.plusEqual(relID);
		h.plusEqual(unaryRuleID);
		h.plusEqual((filler.hashCode()));
		return (int) (h.value());
	}

	@Override
	public boolean equals(Object other) {
		if ( other == null || getClass() != other.getClass() ) {
			return false;
		}

		DependencyStringWords cother = (DependencyStringWords) other;

		return head.equals(cother.head)
				&& filler.equals(cother.filler)
				&& relID == cother.relID
				&& unaryRuleID == cother.unaryRuleID;
	}

	// not sure this is even needed for the HashSet?!
	@Override
	public int compareTo(DependencyStringWords other) {
		if (this.relID == other.relID) {
			if (this.head.equals(other.head)) {
				if (this.filler.equals(other.filler)) {
					if (this.unaryRuleID == other.unaryRuleID) {
						return 0;
					} else if (this.unaryRuleID < other.unaryRuleID) {
						return -1;
					} else {
						return 1;
					}
				} else if (this.filler.compareTo(other.filler) < 0) {
					return -1;
				} else {
					return 1;
				}
			} else if (this.head.compareTo(other.head) < 0) {
				return -1;
			} else {
				return 1;
			}
		} else if (this.relID < other.relID) {
			return -1;
		} else {
			return 1;
		}
	}
}
