package cat_combination;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.Sentence;
import lexicon.Category;
import lexicon.Relations;
import lexicon.TypeRaisedCategory;
import lexicon.VarID;
import utils.Hash;
import utils.IntWrapper;

public class SuperCategory {
	public final Category cat;

	public ArrayList<Dependency> unfilledDeps = null;
	public ArrayList<FilledDependency> filledDeps = null;

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

	public double logPScore = 0.0;
	public double depnnScore = 0.0;

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
	public static IgnoreDepsEval ignoreDeps;

	public static final Logger logger = LogManager.getLogger(SuperCategory.class);

	public static void setIgnoreDepsEval(IgnoreDepsEval i) {
		ignoreDeps = i;
	}

	public static IgnoreDepsEval getIgnoreDepsEval() {
		return ignoreDeps;
	}

	public SuperCategory(short headIndex, Category cat, short flags) {
		this.cat = cat;
		this.unfilledDeps = Dependency.getDependencies(headIndex, cat, (short) (0)); // last argument is ruleID
		this.filledDeps = new ArrayList<FilledDependency>();
		this.flags = flags;
		this.numVars = cat.getNumVars();
		this.numActiveVars = this.numVars; // same as numVars since lexical category
		this.vars = new Variable[this.numVars];

		for ( int i = 0; i < numVars; i++ ) {
			vars[i] = new Variable();
		}

		this.leftChild = null;
		this.rightChild = null;

		if ( cat.var != VarID.NONE ) {
			this.vars[cat.var] = new Variable(headIndex);
		} else {
			throw new Error("shouldn't we have a variable on a lexical category?!");
		}

		this.equivalenceHash = equivalenceHash();

		this.outside = 0.0;
	}

	public static SuperCategory Lexical(short headIndex, Category cat, short flags) {
		return new SuperCategory(headIndex, cat, flags);
	}

	/*
	 * sentence argument only used by numFilledDeps() and for the oracleFscore
	 * decoder - maybe there's a nicer solution
	 */
	public SuperCategory(Category cat,
			short flags,
			SuperCategory leftChild,
			SuperCategory rightChild,
			Unify unification,
			Sentence sentence) {
		this.cat = cat;
		this.unfilledDeps = new ArrayList<Dependency>();
		this.filledDeps = new ArrayList<FilledDependency>();
		this.flags = flags;
		this.numVars = unification.numVariables; // numActiveVars from Unification
		this.numActiveVars = cat.getNumVars(); // numVars from the resulting category
		this.vars = new Variable[this.numVars];
		this.leftChild = leftChild;
		this.rightChild = rightChild;
		this.next = null;

		// old sanity check from C&C; numActiveVars should never be greater than numVars (caught now below in the static call):
		if ( numActiveVars > numVars ) {
			throw new Error("numActiveVars > numVars!");
		}

		/*
		 * need to create new Variable objects based on the VarIDs present in
		 * the new Category (Unify never sees the Variables, only VarIDs)
		 * 
		 * note ignore i = 0, which corresponds to the "NONE" Variable
		 */
		this.vars[0] = new Variable(); // the other variables get created below

		for ( int i = 1; i < numVars; i++ ) {
			// old sanity check from C&C:
			if ( unification.old1[i] >= leftChild.numVars || unification.old2[i] >= rightChild.numVars ) {
				throw new Error("attempt to access variables outside range");
			}

			// chains together any filler chains on unifying variables
			vars[i] = new Variable(leftChild.vars[unification.old1[i]], rightChild.vars[unification.old2[i]]);
		}

		for ( Dependency dep : leftChild.unfilledDeps ) {
			byte var = unification.trans1[dep.var];

			if ( var == VarID.NONE ) {
				continue;
			}

			short lrange = 0;

			if ( vars[var].isFilled() ) {
				filledDeps = FilledDependency.fromUnfilled(dep, vars[var], lrange, filledDeps);
			} else {
				unfilledDeps.add(new Dependency(dep, var, lrange, true));
			}
		}

		for ( Dependency dep : rightChild.unfilledDeps ) {
			byte var = unification.trans2[dep.var];

			if ( var == VarID.NONE ) {
				continue;
			}

			short lrange = 0;

			if ( vars[var].isFilled() ) {
				filledDeps = FilledDependency.fromUnfilled(dep, vars[var], lrange, filledDeps);
			} else {
				unfilledDeps.add(new Dependency(dep, var, lrange, true));
			}
		}

		this.equivalenceHash = equivalenceHash();
		/*
		 * this gets used by the oracleFscore decoder to split equivalence
		 * classes by the number of dependencies produced
		 */
		this.outside = leftChild.outside + rightChild.outside + numFilledDeps(sentence);
	}

	public static SuperCategory BinaryCombinator(Category cat, short flags, SuperCategory leftChild, SuperCategory rightChild, Unify unification, Sentence sentence) {
		/*
		 * there are some pathological cases where this appears to happen, so
		 * just rule these out as ungrammatical
		 */
		if ( cat.getNumVars() > unification.numVariables ) {
			return null;
		}

		return new SuperCategory(cat, flags, leftChild, rightChild, unification, sentence);
	}

	public SuperCategory(Category cat,
			short flags,
			SuperCategory leftChild,
			SuperCategory rightChild) {
		this.cat = cat;
		this.unfilledDeps = new ArrayList<Dependency>(rightChild.unfilledDeps);
		this.filledDeps = new ArrayList<FilledDependency>();
		this.flags = flags;
		this.numVars = rightChild.numVars;
		this.numActiveVars = rightChild.numActiveVars;
		this.vars = new Variable[this.numVars];
		this.leftChild = leftChild;
		this.rightChild = rightChild;
		this.next = null;

		// old sanity check from C&C; numActiveVars should never be greater than numVars:
		if ( numActiveVars > numVars ) {
			throw new Error("numActiveVars > numVars!");
		}

		vars[0] = new Variable(); // the other variables get created below
		// copying Variables from non-conj category
		for ( int i = 1; i < numVars; i++ ) {
			vars[i] = new Variable(rightChild.vars[i]);
		}

		// need this check? just makes sure that conj cat has a head
		if ( !leftChild.vars[1].isFilled() ) {
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
		if ( var.isFilled() ) { // shouldn't this always be the case?
			// this is the "head" on the conj cat, eg "and"
			short head = leftChild.vars[1].getFiller();
			unfilledDeps.add(new Dependency(Relations.conj1, head, cat.argument.var, (short) (0)));
			// unfilledDeps is already set to those from rightChild; these become the next field
		}

		this.equivalenceHash = equivalenceHash();
		/*
		 * this gets used by the oracleFscore decoder to split equivalence
		 * classes by the number of dependencies produced
		 */
		this.outside = leftChild.outside + rightChild.outside;
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
	public SuperCategory(Category cat,
			short flags,
			SuperCategory leftSuperCat,
			SuperCategory rightSuperCat,
			SuperCategory variablesSuperCat) {
		this.cat = cat;
		this.unfilledDeps = new ArrayList<Dependency>(variablesSuperCat.unfilledDeps);
		this.filledDeps = new ArrayList<FilledDependency>();
		this.flags = flags;
		this.numVars = variablesSuperCat.numVars;
		this.numActiveVars = variablesSuperCat.numActiveVars;
		this.vars = variablesSuperCat.vars;
		this.leftChild = leftSuperCat;
		this.rightChild = rightSuperCat;
		this.equivalenceHash = variablesSuperCat.equivalenceHash;
		this.next = null;

		// old sanity check from C&C; numActiveVars should never be greater than numVars:
		if ( numActiveVars > numVars ) {
			throw new Error("numActiveVars > numVars!");
		}

		/*
		 * this gets used by the oracleFscore decoder to split equivalence
		 * classes by the number of dependencies produced
		 */
		this.outside = leftChild.outside + rightChild.outside;
	}

	public static SuperCategory Punct(Category cat, short flags, SuperCategory leftSuperCat, SuperCategory rightSuperCat, SuperCategory variablesSuperCat) {
		return new SuperCategory(cat, flags, leftSuperCat, rightSuperCat, variablesSuperCat);
	}

	public SuperCategory(Category cat,
			short flags,
			SuperCategory leftSuperCat,
			SuperCategory rightSuperCat,
			SuperCategory headSuperCat,
			boolean replace,
			short ruleID) {
		this.cat = cat;
		this.unfilledDeps = new ArrayList<Dependency>();
		this.filledDeps = new ArrayList<FilledDependency>();
		this.flags = flags;
		this.numVars = cat.getNumVars();
		this.numActiveVars = numVars;
		vars = new Variable[numVars];

		for ( int i = 0; i < numVars; i++ ) {
			vars[i] = new Variable();
		}

		this.leftChild = leftSuperCat;
		this.rightChild = rightSuperCat;
		this.next = null;

		// old sanity check from C&C; numActiveVars should never be greater than numVars:
		if ( numActiveVars > numVars ) {
			throw new Error("numActiveVars > numVars!");
		}

		Variable outerVariable = headSuperCat.vars[headSuperCat.cat.var];

		if ( replace ) {
			/*
			 * added this (not in the C&C code): (gets cases like
			 * "references would survive unamended", where there is a dep
			 * between references and unamended)
			 */
			if ( cat.isSbNPbSbNP() ) {
				unfilledDeps = Dependency.clone(headSuperCat.cat.argument.var, cat.argument.argument.var, ruleID, headSuperCat.unfilledDeps);
			} else if ( headSuperCat.cat.argument != null ) {
				unfilledDeps = Dependency.clone(headSuperCat.cat.argument.var, cat.argument.var, ruleID, headSuperCat.unfilledDeps);
			}
		} else {
			unfilledDeps = Dependency.getDependencies(outerVariable, cat, ruleID);
		}

		if ( cat.var != VarID.NONE ) {
			vars[cat.var] = outerVariable;
		}

		this.equivalenceHash = equivalenceHash();
		/*
		 * this gets used by the oracleFscore decoder to split equivalence
		 * classes by the number of dependencies produced
		 */
		this.outside = leftChild.outside;
		if ( rightChild != null ) {
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

	public SuperCategory(TypeRaisedCategory trCat,
			short flags,
			SuperCategory leftSuperCat) {
		this.cat = trCat.cat;
		this.unfilledDeps = new ArrayList<Dependency>();
		this.filledDeps = new ArrayList<FilledDependency>();
		this.flags = flags;
		this.numVars = cat.getNumVars();
		this.numActiveVars = numVars;
		this.vars = new Variable[numVars];

		for ( int i = 0; i < numVars; i++ ) {
			vars[i] = new Variable();
		}

		this.leftChild = leftSuperCat;
		this.rightChild = null;
		this.next = null;

		// note can't be at the end of the defn because there is a return statement earlier!
		this.outside = leftChild.outside;

		// old sanity check from C&C; numActiveVars should never be greater than numVars:
		if ( numActiveVars > numVars ) {
			throw new Error("numActiveVars > numVars!");
		}

		// grab the Variable corresponding to the lexical item:
		if ( trCat.lexVar != VarID.NONE ) {
			vars[trCat.lexVar] = leftSuperCat.vars[leftSuperCat.cat.var];
		} else {
			// added this - not in C&C
			throw new Error("should always be a lexical item with a tr cat!");
		}

		if ( trCat.depVar == VarID.NONE ) { // no dependency in original category
			equivalenceHash = equivalenceHash();
			return;
			// TODO: did we forget to increment numSuperCategories++ here?
		}

		unfilledDeps.clear();

		/*
		 * essentially copying dependencies over from the original
		 * non-type-raised category (could be more than one if eg two APs have
		 * been coordinated), and replacing the var with trCat.depVar
		 */
		for ( Dependency dep : leftSuperCat.unfilledDeps ) {
			// boolean argument distinguishes the constructor from another with same signature
			unfilledDeps.add(new Dependency(dep, trCat.depVar, dep.lrange, true));
		}

		this.equivalenceHash = equivalenceHash();
	}

	public static SuperCategory TypeRaising(TypeRaisedCategory trCat, short flags, SuperCategory leftSuperCat) {
		return new SuperCategory(trCat, flags, leftSuperCat);
	}

	/*
	 * Apposition only called on NP and S[dcl] cases
	 */
	public SuperCategory(short flags,
			SuperCategory leftSuperCat,
			SuperCategory rightSuperCat) {
		this.cat = leftSuperCat.cat;
		this.unfilledDeps = new ArrayList<Dependency>();
		this.filledDeps = new ArrayList<FilledDependency>();
		this.flags = flags;
		this.numVars = 2;
		this.numActiveVars = numVars;
		this.vars = new Variable[numVars];

		for ( int i = 0; i < numVars; i++ ) {
			vars[i] = new Variable();
		}

		// calling this constructor directly is a little ugly (?)
		vars[1] = new Variable(leftSuperCat.vars[1], rightSuperCat.vars[1]);

		this.leftChild = leftSuperCat;
		this.rightChild = rightSuperCat;
		this.next = null;

		this.equivalenceHash = equivalenceHash();
		/*
		 * this gets used by the oracleFscore decoder to split equivalence
		 * classes by the number of dependencies produced
		 */
		this.outside = leftChild.outside + rightChild.outside;
	}

	public static SuperCategory Apposition(short flags, SuperCategory leftSuperCat, SuperCategory rightSuperCat) {
		return new SuperCategory(flags, leftSuperCat, rightSuperCat);
	}

	public int numFilledDeps(Sentence sentence) {
		if ( ignoreDeps == null ) {
			return 0;
		}

		int numDeps = 0;
		for ( FilledDependency dep : filledDeps ) {
			if ( !ignoreDeps.ignoreDependency(dep, sentence) ) {
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

		for ( Dependency dep : unfilledDeps ) {
			h.plusEqual(dep.relID);
		}

		for ( int i = 1; i < numActiveVars; i++ ) {
			// numActiveVars is always 1 more than the real number
			for ( int j = 1; j != Variable.NUM_FILLERS; j++ ) {
				// note the start index is 1
				h.plusEqual(vars[i].fillers[j]);
			}
		}

		return h;
	}

	public long getEhash() {
		return equivalenceHash.value();
	}

	@Override
	public int hashCode() {
		return (int) (equivalenceHash.value());
	}

	@Override
	public boolean equals(Object other) {
		if ( other == null || getClass() != other.getClass() ) {
			return false;
		}

		SuperCategory cother = (SuperCategory) other;

		if ( this.equivalenceHash.value() != cother.equivalenceHash.value() ) {
			return false;
		}

		if ( this.numActiveVars != cother.numActiveVars ) {
			return false;
		}

		for ( int i = 1; i < this.numActiveVars; i++ ) {
			if ( !this.vars[i].equals(cother.vars[i]) ) {
				return false;
			}
		}

		// ensures cats created via unary rules get their own equiv classes
		if ( this.unary() != cother.unary() ) {
			return false;
		}

		if ( !this.cat.equalsWithVars(cother.cat) ) {
			return false;
		}

		if ( this.unfilledDeps.size() != cother.unfilledDeps.size() ) {
			return false;
		}

		Collections.sort(this.unfilledDeps);
		Collections.sort(cother.unfilledDeps);

		for ( int i = 0; i < this.unfilledDeps.size(); i++ ) {
			if ( !this.unfilledDeps.get(i).equals(cother.unfilledDeps.get(i)) ) {
				return false;
			}
		}

		return true;
	}

	public int compareToScore(SuperCategory other) {
		return Double.compare(other.score, this.score);
	}

	public static Comparator<SuperCategory> scoreComparator() {
		return new Comparator<SuperCategory>() {
			@Override
			public int compare(SuperCategory superCat1, SuperCategory superCat2) {
				return Double.compare(superCat2.score, superCat1.score);
			}
		};
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
		if ( leftChild != null && !leftChild.isActive() ) {
			leftChild.markActiveDisj(numDisjNodes);
		}

		if ( rightChild != null && !rightChild.isActive() ) {
			rightChild.markActiveDisj(numDisjNodes);
		}

		if ( next != null ) {
			next.markActiveConj(numDisjNodes);
		}
	}

	public void markActiveConj(IntWrapper numDisjNodes) {
		markActive();
		if ( leftChild != null && !leftChild.isActive() ) {
			leftChild.markActiveDisj(numDisjNodes);
		}

		if ( rightChild != null && !rightChild.isActive() ) {
			rightChild.markActiveDisj(numDisjNodes);
		}

		if ( next != null ) {
			next.markActiveConj(numDisjNodes);
		}
	}

	public void printFilledDeps(Relations relations) {
		logger.info("printing deps:");
		for ( FilledDependency dep : filledDeps ) {
			logger.info(dep);
		}
	}

	public int numEquivNodes() {
		return next != null ? 1 + next.numEquivNodes() : 1;
	}

	/*
	 * these are the same flags from C&C (with LEX renamed UNARY_TC)
	 */
	protected static final short CONJ = (short) (1 << 0);
	protected static final short TR = (short) (1 << 1);
	protected static final short UNARY_TC = (short) (1 << 2);

	protected static final short FWD_APP = (short) (1 << 3);
	protected static final short BWD_APP = (short) (1 << 4);
	protected static final short FWD_COMP = (short) (1 << 5);
	protected static final short BWD_COMP = (short) (1 << 6);
	protected static final short BWD_CROSS = (short) (1 << 7);

	protected static final short RECURSIVE = (short) (1 << 8);

	protected static final short FUNNY_CONJ = (short) (1 << 9);

	protected static final short LEFT_PUNCT = (short) (1 << 10);
	protected static final short RIGHT_PUNCT = (short) (1 << 11);

	protected static final short LEFT_TC = (short) (1 << 12);
	protected static final short RIGHT_TC = (short) (1 << 13);

	protected static final short APPO = (short) (1 << 14);
	protected static final short GEN_MISC = (short) (1 << 15);

	protected static final short CONJ_TR = (short) (CONJ | TR);
	protected static final short TR_UNARY = (short) (TR | UNARY_TC);
	protected static final short GEN_RULES = (short) (CONJ | TR | UNARY_TC | FWD_APP | BWD_APP | FWD_COMP | BWD_COMP | BWD_CROSS);

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
