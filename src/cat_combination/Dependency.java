package cat_combination;

import java.util.ArrayList;
import java.util.Collections;

import lexicon.Category;
import utils.Hash;

/*
 * this is the class for unfilled dependencies, with a var for the
 * filler; there is also a FilledDependency class which has a constant
 * (word index) as the filler
 *
 * individual dependencies are also part of a linked list structure
 * (through the "next" field); these linked lists appear on
 * SuperCategory objects
 *
 * the linked list is also sorted (as part of the link method), which
 * is useful if two linked lists need to be compared for equality;
 * note the compareTo method looks at the relID, headIndex and var
 */

public class Dependency implements Comparable<Dependency> {
	final short relID;
	final short headIndex; // position of the "head" word in the sentence
	final byte var; // varID associated with the filler
	final short unaryRuleID; // if dependency has been created thro' a unary
	// rule
	final short conjFactor; // average divisor for multiple slot fillers in
	// max-recall decoder
	final short lrange; // if dependency has been created thro' the head-passing
	// mechanism

	Dependency next; // the linked list

	public Dependency(short relID, short headIndex, byte var,
			short unaryRuleID, Dependency next) {
		this.relID = relID;
		this.headIndex = headIndex;
		this.var = var;
		this.unaryRuleID = unaryRuleID;
		this.conjFactor = 1;
		this.lrange = 0;
		this.next = next;

		if (headIndex == 0) {
			throw new Error(
					"expecting a non-zero head index when constructing the dependency!");
		}
	}

	public Dependency(short relID, short headIndex, byte var,
			short unaryRuleID, short lrange, Dependency next) {
		this.relID = relID;
		this.headIndex = headIndex;
		this.var = var;
		this.unaryRuleID = unaryRuleID;
		this.conjFactor = 1;
		this.lrange = lrange;
		this.next = next;

		if (headIndex == 0) {
			throw new Error(
					"expecting a non-zero head index when constructing the dependency!");
		}
	}

	public Dependency(Dependency other) {
		this.relID = other.relID;
		this.headIndex = other.headIndex;
		this.var = other.var;
		this.unaryRuleID = other.unaryRuleID;
		this.conjFactor = other.conjFactor;
		this.lrange = other.lrange;
		this.next = other.next;
	}

	/*
	 * this constructor is used in the clone method which is used in the
	 * UnaryRule SuperCategory constructor
	 */
	public Dependency(Dependency other, byte var, short unaryRuleID) {
		this.relID = other.relID;
		this.headIndex = other.headIndex;
		this.var = var;
		this.unaryRuleID = unaryRuleID;
		this.conjFactor = other.conjFactor;
		this.lrange = other.lrange;
		this.next = null;
	}

	/*
	 * note the arbitrary way in which the lrange variable is chosen; we never
	 * did work out a more motivated way of deciding when there are 2 options
	 * 
	 * we need the boolean at the end to distinguish this constructor from the
	 * one above with the same signature
	 */
	public Dependency(Dependency other, byte var, short lrange,
			boolean lrangeArg) {
		this.relID = other.relID;
		this.headIndex = other.headIndex;
		this.var = var;
		this.unaryRuleID = other.unaryRuleID;
		this.conjFactor = other.conjFactor;
		this.lrange = (lrange > other.lrange ? lrange : other.lrange);
		this.next = null;
	}

	/*
	 * this constructor multiplies two conjFactors together to get a new
	 * conjFactor
	 */
	Dependency(Dependency other, byte var, short lrange, short conjFactor) {
		this.relID = other.relID;
		this.headIndex = other.headIndex;
		this.var = var;
		this.unaryRuleID = other.unaryRuleID;
		this.conjFactor = (short) (other.conjFactor * conjFactor);
		this.lrange = (lrange > other.lrange ? lrange : other.lrange);
		this.next = null;
	}

	/*
	 * this constructor creates a new linked list by copying the linked list of
	 * other
	 */
	public Dependency(Dependency other, boolean linkedList) {
		this.relID = other.relID;
		this.headIndex = other.headIndex;
		this.var = other.var;
		this.unaryRuleID = other.unaryRuleID;
		this.conjFactor = other.conjFactor;
		this.lrange = other.lrange;
		this.next = (other.next != null ? new Dependency(other.next) : null);
	}

	/*
	 * goes through the Category object collecting all the relations, creating
	 * dependencies for each one
	 */
	static private void get(short headIndex, Category cat, short ruleID,
			ArrayList<Dependency> resultDeps) {
		if (cat.relID != 0) {
			resultDeps.add(new Dependency(cat.relID, headIndex, cat.var,
					ruleID, null));
		}

		if (cat.result != null) {
			get(headIndex, cat.result, ruleID, resultDeps);
			get(headIndex, cat.argument, ruleID, resultDeps);
		}
	}

	/*
	 * goes through the Category object collecting all the relations, creating
	 * dependencies for each one, and for each headIndex on the Variable object
	 */
	static private void get(Variable variable, Category cat, short ruleID,
			ArrayList<Dependency> resultDeps) {
		if (cat.relID != 0) {
			for (short filler : variable.fillers) {
				if (filler == Variable.SENTINEL) {
					break;
				} else if (filler != 0)
				{
					resultDeps.add(new Dependency(cat.relID, filler, cat.var,
							ruleID, null));
					// } else // note this check isn't in C&C:
					// throw new
					// Error("trying to create dependencies with an unfilled variable?!");
				}
			}
		}
		if (cat.result != null) {
			get(variable, cat.result, ruleID, resultDeps);
			get(variable, cat.argument, ruleID, resultDeps);
		}
	}

	static public Dependency getDependencies(short headIndex, Category cat,
			short ruleID) {
		if (cat.isAtomic()) {
			if (cat.relID != 0) {
				return new Dependency(cat.relID, headIndex, cat.var, ruleID,
						null);
			} else {
				return null;
			}
		} else {
			ArrayList<Dependency> deps = new ArrayList<Dependency>();
			get(headIndex, cat, ruleID, deps);

			switch (deps.size()) {
			case 0:
				return null;
			case 1:
				return deps.get(0);
			default:
				return link(deps);
			}
		}
	}

	static public Dependency getDependencies(Variable var, Category cat,
			short ruleID) {
		ArrayList<Dependency> deps = new ArrayList<Dependency>();
		get(var, cat, ruleID, deps);

		switch (deps.size()) {
		case 0:
			return null;
		case 1:
			return deps.get(0);
		default:
			return link(deps);
		}
	}

	/*
	 * used in the SuperCategory equal() method which is used for the DP
	 * equivalence check in the chart
	 */
	public static boolean equal(Dependency dep1, Dependency dep2) {
		return dep1.relID == dep2.relID && dep1.headIndex == dep2.headIndex
				&& dep1.var == dep2.var && dep1.lrange == dep2.lrange
				&& dep1.unaryRuleID == dep2.unaryRuleID;
	}

	@Override
	public int compareTo(Dependency other) {
		if (this.relID == other.relID) {
			if (this.headIndex == other.headIndex) {
				if (this.var == other.var) {
					return 0;
				} else if (this.var < other.var) {
					return -1;
				} else {
					return 1;
				}
			} else if (this.headIndex < other.headIndex) {
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

	@Override
	public boolean equals(Object other) {
		if ( other == null || getClass() != other.getClass() ) {
			return false;
		}

		Dependency cother = (Dependency) other;

		return compareTo(cother) == 0;
	}

	@Override
	public int hashCode() {
		Hash h = new Hash(relID);
		h.plusEqual(headIndex);
		h.plusEqual(var);
		h.plusEqual(unaryRuleID);
		h.plusEqual(conjFactor);
		h.plusEqual(lrange);
		return (int) (h.value());
	}
	
	static public Dependency link(ArrayList<Dependency> deps) {
		if (deps.isEmpty()) {
			return null;
		}

		/*
		 * sort the deps so that it's easy to see if 2 linked lists of deps are
		 * the same; eg this is done in the packed chart equivalence check if
		 * the unfilled dependencies are part of that check
		 * 
		 * sorting is done using the compareTo relation defined above
		 */
		Collections.sort(deps);

		for (int i = 0; i < deps.size() - 1; i++) {
			(deps.get(i)).next = deps.get(i + 1);
		}

		return deps.get(0);
	}

	static public Dependency clone(byte from, byte to, short ruleID,
			Dependency source) {
		ArrayList<Dependency> deps = new ArrayList<Dependency>();

		Dependency d = source;
		while (d != null) {
			if (d.var == from) {
				deps.add(new Dependency(d, to, ruleID));
			}
			d = d.next;
		}
		switch (deps.size()) {
		case 0:
			return null;
		case 1:
			return deps.get(0);
		default:
			return link(deps);
		}
	}
}
