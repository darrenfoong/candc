package cat_combination;

import io.Sentence;

import java.io.PrintWriter;

import lexicon.Categories;
import lexicon.Relation;
import lexicon.Relations;
import utils.Hash;

/*
 * this is the class for filled dependencies; very similar to
 * Dependency (for unfilled deps), but with a constant for the filler
 *
 * also has the linked list through the next field
 *
 * A nice OOP implementation would have a parent class which the two
 * dependency classes inherit from
 */

public class FilledDependency implements Comparable<FilledDependency> {
	final short relID;
	final short headIndex; // position of the "head" word in the sentence
	final short fillerIndex; // position of the filler word in the sentence
	final short unaryRuleID; // if dependency has been created thro' a unary
	// rule
	public final short conjFactor; // average divisor for multiple slot fillers
	// in max-recall decoder
	final short lrange; // if dependency has been created thro' the head-passing
	// mechanism

	public boolean correct = false;

	public FilledDependency next; // the linked list

	public FilledDependency(short relID, short headIndex, short fillerIndex,
			short unaryRuleID, short lrange) {
		this.relID = relID;
		this.headIndex = headIndex;
		this.fillerIndex = fillerIndex;
		this.unaryRuleID = unaryRuleID;
		this.conjFactor = 1;
		this.lrange = lrange;
		this.next = null;

		if (fillerIndex == 0) {
			throw new Error(
					"expecting a non-zero filler index when constructing the filled dependency!");
		}
	}

	public FilledDependency(short relID, short headIndex, short fillerIndex,
			short unaryRuleID, short lrange, short conjFactor,
			FilledDependency next) {
		this.relID = relID;
		this.headIndex = headIndex;
		this.fillerIndex = fillerIndex;
		this.unaryRuleID = unaryRuleID;
		this.conjFactor = conjFactor;
		this.lrange = lrange;
		this.next = next;

		if (fillerIndex == 0) {
			throw new Error(
					"expecting a non-zero filler index when constructing the filled dependency!");
		}
	}

	public FilledDependency(Dependency dep, short fillerIndex,
			short conjFactor, short lrange, FilledDependency next) {
		this.relID = dep.relID;
		this.headIndex = dep.headIndex;
		this.fillerIndex = fillerIndex;
		this.unaryRuleID = dep.unaryRuleID;
		this.conjFactor = conjFactor;
		this.lrange = (lrange != 0 ? lrange : dep.lrange);
		this.next = next;

		if (fillerIndex == 0) {
			throw new Error(
					"expecting a non-zero filler index when constructing the filled dependency!");
		}
	}

	/*
	 * creates a linked list of FilledDependencies from an unfilled dependency
	 * and a variable (which may have a number of fillers)
	 */
	public static FilledDependency fromUnfilled(Dependency dep, Variable var,
			short lrange, FilledDependency next) {
		short conjFactor = var.countFillers();

		for (int i = 0; i < var.fillers.length
				&& var.fillers[i] != Variable.SENTINEL; i++) {
			next = new FilledDependency(dep, var.fillers[i], conjFactor,
					lrange, next);
		}

		return next;
	}

	public void print(PrintWriter out) {
		out.println(headIndex + " " + relID + " " + fillerIndex + " "
				+ unaryRuleID);
	}

	public void printFull(PrintWriter out, Relations relations,
			Sentence sentence) {
		String head = sentence.words.get(headIndex - 1);
		String filler = sentence.words.get(fillerIndex - 1);
		Relation relation = relations.getRelation(relID);
		String stringCat = relation.category;
		short slot = relation.slot;
		out.println(head + "_" + headIndex + " " + stringCat + " " + slot + " "
				+ filler + "_" + fillerIndex + " " + unaryRuleID);
	}

	public void printFullJslot(PrintWriter out, Relations relations,
			Sentence sentence) {
		String head = sentence.words.get(headIndex - 1);
		String filler = sentence.words.get(fillerIndex - 1);
		Relation relation = relations.getRelation(relID);
		String stringCat = relation.category;
		short jslot = relation.jslot;
		out.println(head + "_" + headIndex + " " + stringCat + " " + jslot
				+ " " + filler + "_" + fillerIndex + " " + unaryRuleID);
	}

	public void printForTraining(PrintWriter out, Categories categories,
			Sentence sentence) {
		Relations relations = categories.dependencyRelations;
		Relation relation = relations.getRelation(relID);
		String stringCat = relation.category;
		short jslot = relation.jslot;

		String plainCatString = categories.getPlainString(stringCat);
		if (plainCatString == null) {
			throw new Error(
					"should have plain category string for all lexical categories! "
							+ stringCat);
		}

		out.println(headIndex + " " + plainCatString + " " + jslot + " "
				+ fillerIndex);
	}

	/*
	 * needed to implement the Comparable interface, which gets used to order
	 * the dependencies:
	 */
	@Override
	public int compareTo(FilledDependency other) {
		if (this.relID == other.relID) {
			if (this.headIndex == other.headIndex) {
				if (this.fillerIndex == other.fillerIndex) {
					return 0;
				} else if (this.fillerIndex < other.fillerIndex) {
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

	/*
	 * hashCode and equals needed for the HashSet in the OracleDepsDecoder; same
	 * hash as in the C&C depscore class except we ignore the unaryRuleID and
	 * lrange fields, since these are not part of the CCGbank dependencies that
	 * we use
	 */
	@Override
	public int hashCode() {
		Hash h = new Hash(headIndex);
		h.plusEqual(relID);
		h.plusEqual(fillerIndex);
		return (int) (h.value()); // int's go up to around 2 billion
	}

	@Override
	public boolean equals(Object other) {
		if ( other == null || getClass() != other.getClass() ) {
			return false;
		}

		FilledDependency cother = (FilledDependency) other;

		return relID == cother.relID
				&& headIndex == cother.headIndex
				&& fillerIndex == cother.fillerIndex;
	}
}
