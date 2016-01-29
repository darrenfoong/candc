package cat_combination;

import io.Sentence;

import java.util.ArrayList;

import lexicon.Category;
import lexicon.Relations;
import lexicon.TypeRaisedCategory;
import lexicon.VarID;
import utils.Hash;
import utils.IntWrapper;

/*
 * unfilledDeps is a linked list of Dependency objects, with each
 * dependency having a next reference; not very transparent, but same
 * implemenation as C&C so stick with it for now; same for filledDeps
 *
 * we also have a linked list of SuperCategories, using the next
 * field; this is used to implement the equivalent (Super-)Categories
 * in a packed chart; again not the most transparent, but stick with
 * the C&C implementation for now
 */

public class SuperCategory implements Comparable<SuperCategory> {
	public final Category cat;

	// linked list of dependencies waiting to be filled
	public Dependency unfilledDeps = null;

	// linked list of deps that were filled when children combined
	public FilledDependency filledDeps = null;

	/*
	 * used to record how the category was built (eg coordination) which then
	 * gets used by rule restrictions in the combinators
	 */
	public short flags;

	/*
	 * all the variables on this category; note that numVars and numActiveVars
	 * are both 1 more than the actual number of variables, since we assume a
	 * "NONE" Variable at the beginning; eg with X and Y variables, numVars = 3
	 * 
	 * the difference between numVars and numActiveVars is that numActiveVars
	 * comes from Unification (when applicable) which may have more variables
	 * than the resulting category (which is numVars);
	 */
	private final byte numVars;
	private final byte numActiveVars;
	public Variable[] vars;

	public final SuperCategory leftChild;
	public final SuperCategory rightChild;

	//linked list used to keep track of the equivalent SuperCategories in the packed chart
	public SuperCategory next = null;

	// used in the CategoryEquivalence hashMap
	private final Hash equivalenceHash;

	// all the scores and marked fields used by the various decoders:
	public double maxEquivScore;
	public SuperCategory maxEquivSuperCat = null;

	public double score = 0.0;
	public double inside = 0.0;
	public double outside = 0.0;
	public double disjInside = 0.0;

	// used to mark active nodes
	public int marker = 0;
	public int goldMarker = 0;

	public boolean marked = false;

	/*
	 * perhaps not great design having this here, but need it for the oracle
	 * F-score decoder when filling the outside field which records the number
	 * of dependencies (correct or incorrect, but need to ignore those not taken
	 * into account in the evaluation)
	 */
	private static IgnoreDepsEval ignoreDeps;

	public static void setIgnoreDepsEval(IgnoreDepsEval i) {
		ignoreDeps = i;
	}

	public static IgnoreDepsEval getIgnoreDepsEval() {
		return ignoreDeps;
	}

	/*
	 * used as a global counter to count the number of categories being produced
	 * for a sentence
	 */
	private static int numSuperCategories;

	public static void setNumSuperCategories(int n) {
		numSuperCategories = n;
	}

	public static int getNumSuperCategories() {
		return numSuperCategories;
	}

	public static void incrementNumSuperCategories() {
		numSuperCategories++;
		if (numSuperCategories % 50000 == 0) {
			System.out.println("numSuperCategories: " + numSuperCategories);
		}
	}

	public SuperCategory(short headIndex, Category cat, short flags) {
		this.cat = cat;
		this.unfilledDeps = Dependency.getDependencies(headIndex, cat, (short) (0)); // last argument is ruleID
		this.flags = flags;
		this.numVars = cat.getNumVars();
		this.numActiveVars = this.numVars; // same as numVars since lexical category
		this.vars = new Variable[this.numVars];

		for (int i = 0; i < numVars; i++) {
			vars[i] = new Variable();
		}

		this.leftChild = null;
		this.rightChild = null;

		if (cat.var != VarID.NONE) {
			vars[cat.var] = new Variable(headIndex);
		} else {
			throw new Error("shouldn't we have a variable on a lexical category?!");
		}

		equivalenceHash = equivalenceHash();
		incrementNumSuperCategories();

		outside = 0.0;
	}

	/*
	 * uses the constructor above; naming the method (with titlecaps) to make
	 * the use of the constructor more transparent
	 */
	public static SuperCategory Lexical(short headIndex, Category cat, short flags) {
		return new SuperCategory(headIndex, cat, flags);
	}

	/*
	 * sentence argument only used by numFilledDeps() and for the oracleFscore
	 * decoder - maybe there's a nicer solution
	 */
	public SuperCategory(Category cat, short flags, SuperCategory leftChild, SuperCategory rightChild, Unify unification, Sentence sentence) {
		this.cat = cat;
		this.flags = flags;
		this.numVars = unification.numVariables; // numActiveVars from Unification
		this.numActiveVars = cat.getNumVars(); // numVars from the resulting category
		this.vars = new Variable[this.numVars];
		vars[0] = new Variable(); // the other variables get created below
		this.leftChild = leftChild;
		this.rightChild = rightChild;
		this.next = null;

		// old sanity check from C&C; numActiveVars should never be greater than numVars (caught now below in the static call):
		if (numActiveVars > numVars) {
			throw new Error("numActiveVars > numVars!");
		}

		/*
		 * need to create new Variable objects based on the VarIDs present in
		 * the new Category (Unify never sees the Variables, only VarIDs)
		 * 
		 * note ignore i = 0, which corresponds to the "NONE" Variable
		 */
		for (int i = 1; i < numVars; i++) {
			// old sanity check from C&C:
			if (unification.old1[i] >= leftChild.numVars || unification.old2[i] >= rightChild.numVars) {
				throw new Error("attempt to access variables outside range");
			}

			// chains together any filler chains on unifying variables
			vars[i] = new Variable(leftChild.vars[unification.old1[i]], rightChild.vars[unification.old2[i]]);
		}

		ArrayList<Dependency> newUnfilledDeps = new ArrayList<Dependency>();

		for (Dependency dep = leftChild.unfilledDeps; dep != null; dep = dep.next) {
			byte var = unification.trans1[dep.var];

			if (var == VarID.NONE) {
				continue;
			}

			// CatID lrange = unify.lrange2[unify.old2[var]];
			short lrange = 0;

			if (vars[var].isFilled()) {
				filledDeps = FilledDependency.fromUnfilled(dep, vars[var], lrange, filledDeps);
			} else {
				/* 
				 * final boolean arg just differentiates constructors which
				 * otherwise would have same signature
				 */
				Dependency newDep = new Dependency(dep, var, lrange, true);
				newUnfilledDeps.add(newDep);
			}
		}

		for (Dependency dep = rightChild.unfilledDeps; dep != null; dep = dep.next) {
			byte var = unification.trans2[dep.var];
			if (var == VarID.NONE) {
				continue;
			}

			// CatID lrange = unify.lrange1[unify.old1[var]];
			short lrange = 0;

			if (vars[var].isFilled()) {
				filledDeps = FilledDependency.fromUnfilled(dep, vars[var], lrange, filledDeps);
			} else {
				/*
				 * final boolean arg just differentiates constructors which
				 * otherwise would have same signature
				 */
				Dependency newDep = new Dependency(dep, var, lrange, true);
				newUnfilledDeps.add(newDep);
			}
		}

		unfilledDeps = Dependency.link(newUnfilledDeps);

		equivalenceHash = equivalenceHash();
		incrementNumSuperCategories();
		/*
		 * this gets used by the oracleFscore decoder to split equivalence
		 * classes by the number of dependencies produced
		 */
		outside = leftChild.outside + rightChild.outside + numFilledDeps(sentence);
	}

	public static SuperCategory BinaryCombinator(Category cat, short flags, SuperCategory leftChild, SuperCategory rightChild, Unify unification, Sentence sentence) {
		/*
		 * there are some pathological cases where this appears to happen, so
		 * just rule these out as ungrammatical
		 */
		if (cat.getNumVars() > unification.numVariables) {
			return null;
		}

		return new SuperCategory(cat, flags, leftChild, rightChild, unification, sentence);
	}

	public SuperCategory(Category cat, short flags, SuperCategory leftChild, SuperCategory rightChild) {
		this.cat = cat;
		this.unfilledDeps = rightChild.unfilledDeps;
		this.filledDeps = null;
		this.flags = flags;
		this.numVars = rightChild.numVars;
		this.numActiveVars = rightChild.numActiveVars;
		this.vars = new Variable[this.numVars];
		vars[0] = new Variable(); // the other variables get created below
		this.leftChild = leftChild;
		this.rightChild = rightChild;
		this.next = null;

		// old sanity check from C&C; numActiveVars should never be greater than numVars:
		if (numActiveVars > numVars) {
			throw new Error("numActiveVars > numVars!");
		}

		// copying Variables from non-conj category
		for (int i = 1; i < numVars; i++) {
			vars[i] = new Variable(rightChild.vars[i]);
		}

		// need this check? just makes sure that conj cat has a head
		if (!leftChild.vars[1].isFilled()) {
			throw new Error("no head on conj cat!");
		}

		/*
		 * here we create the new dependency for the coordination; eg
		 * "and chips" will create a conj dependency; note that the var Variable
		 * will be an unfilled chain case, with "chips" already there as a
		 * filler (but not at index 0, which is UNFILLED); hence when
		 * "and chips" combines with "fish" then two dependencies will be
		 * created
		 */
		// "outer" variable on the non-conj cat
		Variable var = rightChild.vars[rightChild.cat.var];
		if (var.isFilled()) { // shouldn't this always be the case?
			// this is the "head" on the conj cat, eg "and"
			short head = leftChild.vars[1].getFiller(); 
			unfilledDeps = new Dependency(Relations.conj1, head, cat.argument.var, (short) (0), unfilledDeps);
			// unfilledDeps is already set to those from rightChild; these become the next field
		}

		equivalenceHash = equivalenceHash();
		incrementNumSuperCategories();
		/*
		 * this gets used by the oracleFscore decoder to split equivalence
		 * classes by the number of dependencies produced
		 */
		outside = leftChild.outside + rightChild.outside;
	}

	public static SuperCategory Coordination(Category cat, short flags, SuperCategory leftSuperCat, SuperCategory rightSuperCat) {
		return new SuperCategory(cat, flags, leftSuperCat, rightSuperCat);
	}

	/*
	 * note we use some of the same objects from variablesSuperCat, eg
	 * unfilledDeps, vars (ie don't make copies); this is fine here since we
	 * always make copies when changes are made to the object; eg see the
	 * creation of FilledDeps above
	 */
	public SuperCategory(Category cat, short flags, SuperCategory leftSuperCat, SuperCategory rightSuperCat, SuperCategory variablesSuperCat) {
		this.cat = cat;
		unfilledDeps = variablesSuperCat.unfilledDeps;
		filledDeps = null;
		this.flags = flags;
		numVars = variablesSuperCat.numVars;
		numActiveVars = variablesSuperCat.numActiveVars;
		vars = variablesSuperCat.vars;
		leftChild = leftSuperCat;
		rightChild = rightSuperCat;
		equivalenceHash = variablesSuperCat.equivalenceHash;
		next = null;

		// old sanity check from C&C; numActiveVars should never be greater than numVars:
		if (numActiveVars > numVars) {
			throw new Error("numActiveVars > numVars!");
		}

		incrementNumSuperCategories();
		/*
		 * this gets used by the oracleFscore decoder to split equivalence
		 * classes by the number of dependencies produced
		 */
		outside = leftChild.outside + rightChild.outside;
	}

	public static SuperCategory Punct(Category cat, short flags, SuperCategory leftSuperCat, SuperCategory rightSuperCat, SuperCategory variablesSuperCat) {
		return new SuperCategory(cat, flags, leftSuperCat, rightSuperCat, variablesSuperCat);
	}

	public SuperCategory(Category cat, short flags, SuperCategory leftSuperCat, SuperCategory rightSuperCat, SuperCategory headSuperCat, boolean replace, short ruleID) {
		this.cat = cat;
		unfilledDeps = null;
		filledDeps = null;
		this.flags = flags;
		numVars = cat.getNumVars();
		numActiveVars = numVars;
		vars = new Variable[numVars];
		for (int i = 0; i < numVars; i++) {
			vars[i] = new Variable();
		}
		leftChild = leftSuperCat;
		rightChild = rightSuperCat;
		next = null;

		// old sanity check from C&C; numActiveVars should never be greater than numVars:
		if (numActiveVars > numVars) {
			throw new Error("numActiveVars > numVars!");
		}

		Variable outerVariable = headSuperCat.vars[headSuperCat.cat.var];

		if (replace) {
			/*
			 * added this (not in the C&C code): (gets cases like
			 * "references would survive unamended", where there is a dep
			 * between references and unamended)
			 */
			if (cat.isSbNPbSbNP()) {
				unfilledDeps = Dependency.clone(headSuperCat.cat.argument.var, cat.argument.argument.var, ruleID, headSuperCat.unfilledDeps);
			} else if (headSuperCat.cat.argument != null) {
				unfilledDeps = Dependency.clone(headSuperCat.cat.argument.var, cat.argument.var, ruleID, headSuperCat.unfilledDeps);
			}
		} else {
			unfilledDeps = Dependency.getDependencies(outerVariable, cat, ruleID);
		}

		if (cat.var != VarID.NONE) {
			vars[cat.var] = outerVariable;
		}

		equivalenceHash = equivalenceHash();
		incrementNumSuperCategories();
		/*
		 * this gets used by the oracleFscore decoder to split equivalence
		 * classes by the number of dependencies produced
		 */
		outside = leftChild.outside;
		if (rightChild != null) {
			outside += rightChild.outside;
		}
	}

	/*
	 * TypeChanging is used for both unary and right comma type-changing rules,
	 * unlike in C&C where there are two constructors; the only difference is
	 * that in the binary case there is a rightSuperCat (so just pass in null
	 * for unary); this also combines the TypeChange and Special constructors
	 * from C&C (Special was only used for left comma type-changing)
	 */
	public static SuperCategory TypeChanging(Category cat, short flags, SuperCategory leftSuperCat, SuperCategory rightSuperCat, SuperCategory headSuperCat, boolean replace, short ruleID) {
		return new SuperCategory(cat, flags, leftSuperCat, rightSuperCat, headSuperCat, replace, ruleID);
	}

	public SuperCategory(TypeRaisedCategory trCat, short flags, SuperCategory leftSuperCat) {
		cat = trCat.cat;
		unfilledDeps = null;
		filledDeps = null;
		this.flags = flags;
		numVars = cat.getNumVars();
		numActiveVars = numVars;
		vars = new Variable[numVars];
		for (int i = 0; i < numVars; i++) {
			vars[i] = new Variable();
		}
		leftChild = leftSuperCat;
		rightChild = null;
		next = null;

		// note can't be at the end of the defn because there is a return statement earlier!
		outside = leftChild.outside; 

		// old sanity check from C&C; numActiveVars should never be greater than numVars:
		if (numActiveVars > numVars) {
			throw new Error("numActiveVars > numVars!");
		}

		// grab the Variable corresponding to the lexical item:
		if (trCat.lexVar != VarID.NONE) {
			vars[trCat.lexVar] = leftSuperCat.vars[leftSuperCat.cat.var];
		} else {
			// added this - not in C&C
			throw new Error("should always be a lexical item with a tr cat!");
		}

		if (trCat.depVar == VarID.NONE) { // no dependency in original category
			equivalenceHash = equivalenceHash();
			return;
			// TODO: did we forget to increment numSuperCategories++ here?
		}

		ArrayList<Dependency> newDeps = new ArrayList<Dependency>();
		/*
		 * essentially copying dependencies over from the original
		 * non-type-raised category (could be more than one if eg two APs have
		 * been coordinated), and replacing the var with trCat.depVar
		 */
		for (Dependency dep = leftSuperCat.unfilledDeps; dep != null; dep = dep.next) {
			// boolean argument distinguishes the constructor from another with same signature
			Dependency newDep = new Dependency(dep, trCat.depVar, dep.lrange, true);
			newDeps.add(newDep);
		}
		unfilledDeps = Dependency.link(newDeps);

		equivalenceHash = equivalenceHash();
		incrementNumSuperCategories();
	}

	public static SuperCategory TypeRaising(TypeRaisedCategory trCat, short flags, SuperCategory leftSuperCat) {
		return new SuperCategory(trCat, flags, leftSuperCat);
	}

	/*
	 * Apposition only called on NP and S[dcl] cases
	 */
	public SuperCategory(short flags, SuperCategory leftSuperCat, SuperCategory rightSuperCat) {
		cat = leftSuperCat.cat;
		unfilledDeps = null;
		filledDeps = null;
		this.flags = flags;
		numVars = 2; // ugly?
		numActiveVars = numVars;
		vars = new Variable[numVars];
		for (int i = 0; i < numVars; i++) {
			vars[i] = new Variable();
		}
		leftChild = leftSuperCat;
		rightChild = rightSuperCat;
		equivalenceHash = equivalenceHash();
		next = null;

		// calling this constructor directly is a little ugly (?)
		vars[1] = new Variable(leftSuperCat.vars[1], rightSuperCat.vars[1]);

		incrementNumSuperCategories();
		/*
		 * this gets used by the oracleFscore decoder to split equivalence
		 * classes by the number of dependencies produced
		 */
		outside = leftChild.outside + rightChild.outside;
	}

	public static SuperCategory Apposition(short flags, SuperCategory leftSuperCat, SuperCategory rightSuperCat) {
		return new SuperCategory(flags, leftSuperCat, rightSuperCat);
	}

	public int numFilledDeps(Sentence sentence) {
		if (ignoreDeps == null) {
			return 0;
		}

		int numDeps = 0;
		for (FilledDependency dep = filledDeps; dep != null; dep = dep.next) {
			if (!ignoreDeps.ignoreDependency(dep, sentence)) {
				numDeps++;
			}
		}
		return numDeps;
	}

	/*
	 * categories are equivalent if they have the same syntactic type (got thro'
	 * the category's ehash) and have the same unfilled dependencies (got thro'
	 * both the relation and the variables)
	 */
	private Hash equivalenceHash() {
		Hash h = new Hash(cat.getEhash());
		for (Dependency dep = unfilledDeps; dep != null; dep = dep.next) {
			h.plusEqual(dep.relID);
		}
		for (int i = 1; i < numActiveVars; i++) {
			// numActiveVars is always 1 more than the real number
			for (int j = 1; j != Variable.NUM_FILLERS; j++) {
				// note the start index is 1
				h.plusEqual(vars[i].fillers[j]);
			}
		}
		return h;
	}

	public long getEhash() {
		return equivalenceHash.value();
	}

	/*
	 * designed for the equivalence check in EquivKey; ie what SuperCategory
	 * objects are equivalent w.r.t. DP in the chart
	 */
	public static boolean equal(SuperCategory superCategory1, SuperCategory superCategory2) {
		if (superCategory1.numActiveVars != superCategory2.numActiveVars) {
			return false;
		}

		for (int i = 1; i < superCategory1.numActiveVars; i++) {
			if (!Variable.equal(superCategory1.vars[i], superCategory2.vars[i])) {
				return false;
			}
		}

		// ensures cats created via unary rules get their own equiv classes
		if (superCategory1.unary() != superCategory2.unary()) {
			return false;
		}

		if (!Category.equal(superCategory1.cat, superCategory2.cat)) {
			return false;
		}

		Dependency dep1 = superCategory1.unfilledDeps;
		Dependency dep2 = superCategory2.unfilledDeps;
		while (dep1 != null && dep2 != null) {
			if (!Dependency.equal(dep1, dep2)) {
				return false;
			}

			dep1 = dep1.next;
			dep2 = dep2.next;
		}
		if (dep1 != null || dep2 != null) {
			return false;
		}

		return true;
	}

	public void markActive() {
		marker = 1;
	}

	public boolean isActive() {
		return marker != 0;
	}

	public void markActiveDisj(IntWrapper numDisjNodes) {
		numDisjNodes.value++;

		markActive();
		if (leftChild != null && !leftChild.isActive()) {
			leftChild.markActiveDisj(numDisjNodes);
		}

		if (rightChild != null && !rightChild.isActive()) {
			rightChild.markActiveDisj(numDisjNodes);
		}

		if (next != null) {
			next.markActiveConj(numDisjNodes);
		}
	}

	public void markActiveConj(IntWrapper numDisjNodes) {
		markActive();
		if (leftChild != null && !leftChild.isActive()) {
			leftChild.markActiveDisj(numDisjNodes);
		}

		if (rightChild != null && !rightChild.isActive()) {
			rightChild.markActiveDisj(numDisjNodes);
		}

		if (next != null) {
			next.markActiveConj(numDisjNodes);
		}
	}

	public void printFilledDeps(Relations relations) {
		System.out.println("printing deps:");
		for (FilledDependency dep = filledDeps; dep != null; dep = dep.next) {
			System.out.println(dep);
		}
	}

	public int numEquivNodes() {
		return next != null ? 1 + next.numEquivNodes() : 1;
	}

	// used by the beam search decoder in Cell
	// x.score < y.score <=> x > y
	@Override
	public int compareTo(SuperCategory other) {
		return Double.compare(other.score, this.score);
	}

	@Override
	public boolean equals(Object other) {
		if ( other == null || getClass() != other.getClass() ) {
			return false;
		}

		SuperCategory cother = (SuperCategory) other;

		return compareTo(cother) == 0;
	}

	@Override
	public int hashCode() {
		return (int) getEhash();
	}

	/*
	 * these are the same flags from C&C (with LEX renamed UNARY_TC)
	 */
	protected static short CONJ = (short) (1 << 0);
	protected static short TR = (short) (1 << 1);
	protected static short UNARY_TC = (short) (1 << 2);

	protected static short FWD_APP = (short) (1 << 3);
	protected static short BWD_APP = (short) (1 << 4);
	protected static short FWD_COMP = (short) (1 << 5);
	protected static short BWD_COMP = (short) (1 << 6);
	protected static short BWD_CROSS = (short) (1 << 7);

	protected static short RECURSIVE = (short) (1 << 8);

	protected static short FUNNY_CONJ = (short) (1 << 9);

	protected static short LEFT_PUNCT = (short) (1 << 10);
	protected static short RIGHT_PUNCT = (short) (1 << 11);

	protected static short LEFT_TC = (short) (1 << 12);
	protected static short RIGHT_TC = (short) (1 << 13);

	protected static short APPO = (short) (1 << 14);
	protected static short GEN_MISC = (short) (1 << 15);

	protected static short CONJ_TR = (short) (CONJ | TR);
	protected static short TR_UNARY = (short) (TR | UNARY_TC);
	protected static short GEN_RULES = (short) (CONJ | TR | UNARY_TC | FWD_APP | BWD_APP | FWD_COMP | BWD_COMP | BWD_CROSS);

	public boolean coordinated() {
		return (flags & CONJ) != 0;
	}

	public boolean typeRaised() {
		return (flags & TR) != 0;
	}

	public boolean unary() {
		return (flags & TR_UNARY) != 0;
	}

	boolean coordinatedOrTypeRaised() {
		return (flags & CONJ_TR) != 0;
	}

	boolean forwardComp() {
		return (flags & FWD_COMP) != 0;
	}

	boolean backwardComp() {
		return (flags & BWD_COMP) != 0;
	}

	boolean backwardCrossComp() {
		return (flags & BWD_CROSS) != 0;
	}
}
