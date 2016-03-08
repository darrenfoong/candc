package cat_combination;

import java.io.PrintWriter;
import java.util.ArrayList;

import io.Sentence;
import lexicon.Categories;
import lexicon.Relation;
import lexicon.Relations;
import utils.Hash;

/*
 * this is the class for filled dependencies; very similar to
 * Dependency (for unfilled deps), but with a constant for the filler*
 *
 */

public class FilledDependency implements Comparable<FilledDependency> {
	protected final short relID;
	protected final short headIndex; // position of the "head" word in the sentence
	protected final short fillerIndex; // position of the filler word in the sentence
	protected final short unaryRuleID; // if dependency has been created thro' a unary rule
	protected final short lrange; // if dependency has been created thro' the head-passing mechanism

	public final short conjFactor; // average divisor for multiple slot fillers in max-recall decoder

	public FilledDependency(short relID,
			short headIndex,
			short fillerIndex,
			short unaryRuleID,
			short lrange) {
		this.relID = relID;
		this.headIndex = headIndex;
		this.fillerIndex = fillerIndex;
		this.unaryRuleID = unaryRuleID;
		this.conjFactor = 1;
		this.lrange = lrange;

		if ( fillerIndex == 0 ) {
			throw new Error("expecting a non-zero filler index when constructing the filled dependency!");
		}
	}

	public FilledDependency(Dependency dep,
			short fillerIndex,
			short conjFactor,
			short lrange) {
		this.relID = dep.relID;
		this.headIndex = dep.headIndex;
		this.fillerIndex = fillerIndex;
		this.unaryRuleID = dep.unaryRuleID;
		this.conjFactor = conjFactor;
		this.lrange = (lrange != 0) ? lrange : dep.lrange;

		if ( fillerIndex == 0 ) {
			throw new Error("expecting a non-zero filler index when constructing the filled dependency!");
		}
	}

	public static ArrayList<FilledDependency> fromUnfilled(Dependency dep, Variable var, short lrange, ArrayList<FilledDependency> deps) {
		short conjFactor = var.countFillers();

		for ( int i = 0; i < var.fillers.length && var.fillers[i] != Variable.SENTINEL; i++ ) {
			deps.add(new FilledDependency(dep, var.fillers[i], conjFactor, lrange));
		}

		return deps;
	}

	@Override
	public String toString() {
		return headIndex + " " + relID + " " + fillerIndex + " " + unaryRuleID;
	}

	public String[] getAttributes(Relations relations, Sentence sentence) {
		String[] output = new String[7];
		Relation relation = relations.getRelation(relID);
		output[0] = sentence.words.get(headIndex - 1);
		output[1] = relation.category;
		output[2] = sentence.words.get(fillerIndex - 1);
		output[3] = String.valueOf(relation.jslot);
		output[4] = String.valueOf(Math.abs(fillerIndex - headIndex));
		output[5] = sentence.postags.get(headIndex);
		output[6] = sentence.postags.get(fillerIndex);

		return output;
	}

	public void printFull(PrintWriter out, Relations relations, Sentence sentence) {
		String head = sentence.words.get(headIndex - 1);
		String filler = sentence.words.get(fillerIndex - 1);
		Relation relation = relations.getRelation(relID);
		String stringCat = relation.category;
		short slot = relation.slot;
		out.println(head + "_" + headIndex + " " + stringCat + " " + slot + " " + filler + "_" + fillerIndex + " " + unaryRuleID);
	}

	public void printFullJslot(PrintWriter out, Relations relations, Sentence sentence) {
		String head = sentence.words.get(headIndex - 1);
		String filler = sentence.words.get(fillerIndex - 1);
		Relation relation = relations.getRelation(relID);
		String stringCat = relation.category;
		short jslot = relation.jslot;
		out.println(head + "_" + headIndex + " " + stringCat + " " + jslot + " " + filler + "_" + fillerIndex + " " + unaryRuleID);
	}

	public void printForTraining(PrintWriter out, Categories categories, Sentence sentence) {
		Relations relations = categories.dependencyRelations;
		Relation relation = relations.getRelation(relID);
		String stringCat = relation.category;
		short jslot = relation.jslot;

		String plainCatString = categories.getPlainString(stringCat);
		if ( plainCatString == null ) {
			throw new Error("should have plain category string for all lexical categories! " + stringCat);
		}

		out.println(headIndex + " " + plainCatString + " " + jslot + " " + fillerIndex);
	}

	@Override
	public int compareTo(FilledDependency other) {
		int compare;
		if ( (compare = Short.compare(this.relID, other.relID)) != 0 ) { return compare; }
		if ( (compare = Short.compare(this.headIndex, other.headIndex)) != 0 ) { return compare; }
		if ( (compare = Short.compare(this.fillerIndex, other.fillerIndex)) != 0 ) { return compare; }

		return 0;
	}

	@Override
	public int hashCode() {
		/*
		 * ignore the unaryRuleID and lrange fields, since these are not part of the
		 * CCGbank dependencies that we use
		 */
		Hash h = new Hash(relID);
		h.plusEqual(headIndex);
		h.plusEqual(fillerIndex);
		return (int) (h.value());
	}

	@Override
	public boolean equals(Object other) {
		if ( other == null || getClass() != other.getClass() ) {
			return false;
		}

		FilledDependency cother = (FilledDependency) other;

		return compareTo(cother) == 0;
	}
}
