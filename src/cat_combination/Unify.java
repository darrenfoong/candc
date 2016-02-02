package cat_combination;

import java.util.Arrays;

import lexicon.Category;
import lexicon.GrammaticalFeature;
import lexicon.VarID;
import utils.ByteWrapper;

/*
 * unifies features on S as well as head variables, eg S[dcl] and
 * S[X], but doesn't check to make sure that X matches the same
 * feature each time; ignores a case such as S[X]/(S[X]/S[X])
 * combining with S[dcl]/S[ng] (this will succeed and X will get one
 * of dcl or ng); also note that S and S[dcl] don't unify
 */
public class Unify {
	/*
	 * table which stores matching variables during unification
	 */
	byte[][] heads;

	/*
	 * features on S get unified, eg S[X] and S[dcl]; but dealing with features
	 * is much easier because there is only ever one variable for a whole
	 * category (X); the feature field stores the resulting feature after
	 * unification
	 */
	public GrammaticalFeature feature;

	byte numVariables = 0;

	public byte[] trans1;
	public byte[] trans2;
	byte[] old1;
	byte[] old2;

	public ByteWrapper order; // used by the reordering methods in Category
	public byte[] seen; // used by the reordering methods in Category

	short lrange1[];
	short lrange2[];

	public Unify() {
		heads = new byte[VarID.NUM_VARS][VarID.NUM_VARS];

		trans1 = new byte[VarID.NUM_VARS];
		trans2 = new byte[VarID.NUM_VARS];
		old1 = new byte[VarID.NUM_VARS];
		old2 = new byte[VarID.NUM_VARS];

		order = new ByteWrapper((byte) (0));
		seen = new byte[VarID.NUM_VARS];
		for (int i = 0; i < seen.length; i++) {
			seen[i] = VarID.NONE;
		}

		lrange1 = new short[VarID.NUM_VARS];
		lrange2 = new short[VarID.NUM_VARS];
	}

	/*
	 * a single unify object is created as part of a single rules object which
	 * is a member of the parser object; hence we need to clear the unify object
	 * for each new category pair
	 * 
	 * clear2 is only called when the unification has succeeded
	 */
	private void clear1() {
		for (byte[] row : heads) {
			// need this for multidimensional array
			Arrays.fill(row, VarID.NONE);
		}
		Arrays.fill(lrange1, (short) (0));
		Arrays.fill(lrange2, (short) (0));
		feature = new GrammaticalFeature(GrammaticalFeature.NONE);
		Arrays.fill(seen, VarID.NONE);
		order.value = (byte) (0);
	}

	private void clear2() {
		Arrays.fill(trans1, (byte) (0));
		Arrays.fill(trans2, (byte) (0));
		Arrays.fill(old1, (byte) (0));
		Arrays.fill(old2, (byte) (0));
	}

	/*
	 * this is the outer unify call which does a quick check, clears some of the
	 * member variables ready for new matchings, and then calls the recursive
	 * unify method; if the unification is successful, the
	 * newVarsForUnifiedPairs method is called which produces a new set of
	 * variables
	 */
	boolean unify(Category cat1, Category cat2) {
		// quick check first on the hash values
		if (cat1.getUhash() != cat2.getUhash()) {
			return false;
		}

		clear1();

		if (unifyRecursive(cat1, cat2)) {
			newVarsForUnifiedPairs();
			return true;
		} else {
			return false;
		}
	}

	/*
	 * this is the unify method which recursively checks the argument and result
	 * of a complex category (unless the category is atomic), updating the heads
	 * table and feature variable as it goes along
	 */
	private boolean unifyRecursive(Category cat1, Category cat2) {
		// the atomic case:
		if (cat1.isBasic()) {
			if (!cat2.isBasic()) {
				return false;
			}
			/*
			 * we want features on S to unify, eg S[X] and S[dcl], but ignore
			 * all features on NP ([nb]) and N (eg [num])
			 */
			if (!cat1.isS()) {
				// do nothing
			} else if (!cat1.hasFeatureVar()) { // cat1 has constant or no
				// feature
				if (!cat2.hasFeatureVar()) { // cat2 has constant or no feature
					if (!cat1.feature.equals(cat2.feature))
					{
						return false; // feature clash (one of the features
						// could be NONE and still clash)
					}
				} else {
					feature = cat1.feature; // cat1 is constant or NONE and cat2
					// is variable
				}
			} else {
				feature = cat2.feature; // cat1 has variable feature so take
				// cat2 feature whatever it is
			}
		} else {
			if (cat2.isBasic())
			{
				return false; // cat1 is not atomic, cat2 is
			}
			/*
			 * both cat1 and cat2 are complex
			 * 
			 * do we need this extra check here, since we already did the check
			 * at the top?
			 * 
			 * note that C&C also has an extra check on the flags, which we
			 * don't have (since chosen not to implement the flags on the
			 * categories (so far))
			 */
			if (cat1.result.getUhash() != cat2.result.getUhash()
					|| cat1.argument.getUhash() != cat2.argument.getUhash()) {
				return false;
			}

			if (!unifyRecursive(cat1.result, cat2.result)
					|| !unifyRecursive(cat1.argument, cat2.argument)) {
				return false;
			}
		}

		heads[cat1.var][cat2.var] = 1;

		/*
		 * comment from C&C: // assumes that we don't have the case //
		 * ((S\NP{X*})/NP{X+})
		 * 
		 * ignore the lrange for now: what do the lrange cases do?
		 * lrange1[c1->var] |= c1->lrange; lrange2[c2->var] |= c2->lrange;
		 */

		return true;
	}

	/*
	 * makes sure we're not trying to unify two constant variables
	 */
	public boolean unify(SuperCategory leftSuperCat, SuperCategory rightSuperCat) {
		for (int i = 0; i < numVariables; i++) {
			if (leftSuperCat.vars[old1[i]].isFilled()
					&& rightSuperCat.vars[old2[i]].isFilled()) {
				return false;
			}
		}
		return true;
	}

	/*
	 * iterates over the pairs of unified variables assigning a new variable to
	 * each unified pair
	 * 
	 * trans1 maps from old variables in cat1 to new ones trans2 maps from old
	 * variables in cat2 to new ones old1 maps from new variables to old ones in
	 * cat1 old2 maps from new variables to old ones in cat2
	 * 
	 * appears to assume that we never have a variable in cat1 matching two
	 * different variables in cat2; if this does happen then the second match
	 * overrides the first
	 * 
	 * eg never have: (N{X}/N{X})/(N{X}/N{X}) combining with N{X}/N{Y} are we
	 * guaranteed this from the markedup file? Should we be looking for such
	 * cases and failing unification?
	 */
	void newVarsForUnifiedPairs() {
		clear2();
		numVariables = 1;
		for (byte i = 0; i < VarID.NUM_VARS; i++) {
			for (byte j = 0; j < VarID.NUM_VARS; j++) {
				if (heads[i][j] != 0) {
					trans1[i] = numVariables;
					trans2[j] = numVariables;
					old1[numVariables] = i;
					old2[numVariables] = j;

					numVariables++;

					if (numVariables > VarID.NUM_VARS) {
						throw new Error(
								"too many variables created during unification");
					}
				}
			}
		}
	}

	/*
	 * just sees if var is in the translation table (ie took part in the
	 * unification), and if not it adds it (by translating into the next
	 * available variable)
	 */
	private void addVar(byte var, byte[] transTable, byte transTableOld[]) {
		// does this first case ever occur? every category shld have a variable?
		if (var != VarID.NONE && transTable[var] == VarID.NONE) {
			if (numVariables >= VarID.NUM_VARS) {
				throw new Error(
						"no variable ids left to add all variables from result category");
			}

			transTable[var] = numVariables;
			transTableOld[numVariables] = var;
			numVariables++;
		}
	}

	public void addVar1(byte var1) {
		addVar(var1, trans1, old1);
	}

	public void addVar2(byte var2) {
		addVar(var2, trans2, old2);
	}

	/*
	 * just calls addVar recursively
	 */
	private void addVars(Category cat, byte[] transTable, byte transTableOld[]) {
		addVar(cat.var, transTable, transTableOld);
		if (cat.argument != null) {
			addVars(cat.argument, transTable, transTableOld);
			addVars(cat.result, transTable, transTableOld);
		}
	}

	/*
	 * checking if cat has any variables not yet seen in trans1 and old1; which
	 * one of addVars1 and addVars2 gets called depends on whether the cat
	 * argument is part of a larger category containing cat1 or cat2 (the
	 * unified categories)
	 */
	public void addVars1(Category cat) {
		addVars(cat, trans1, old1);
	}

	// checking if cat has any variables not yet seen in trans2 and old2
	public void addVars2(Category cat) {
		addVars(cat, trans2, old2);
	}

	public void reorderVariables(SuperCategory superCat1,
			SuperCategory superCat2) {
		/*
		 * need to pick up variables which are not in the categories making up
		 * the result but which have constant values and hence are needed to
		 * create dependencies
		 */
		for (int i = 1; i < VarID.NUM_VARS; i++) { // i starts at 1 (ignoring
			// NONE at 0 index)
			if (seen[i] == VarID.NONE) {
				if (superCat1.vars[old1[i]].isFilled()
						|| superCat2.vars[old2[i]].isFilled()) {
					seen[i] = ++order.value;
				}
			}
		}
		numVariables = (byte) (order.value + 1);

		byte[] tmp1 = Arrays.copyOf(old1, old1.length);
		byte[] tmp2 = Arrays.copyOf(old2, old2.length);

		Arrays.fill(old1, (byte) (0));
		Arrays.fill(old2, (byte) (0));

		for (int i = 1; i < VarID.NUM_VARS; i++) { // i starts at 1 (ignoring
			// NONE at 0 index)
			if (seen[trans1[i]] == VarID.NONE) {
				trans1[i] = VarID.NONE;
			} else {
				trans1[i] = seen[trans1[i]];
			}

			if (seen[trans2[i]] == VarID.NONE) {
				trans2[i] = VarID.NONE;
			} else {
				trans2[i] = seen[trans2[i]];
			}

			if (seen[i] == VarID.NONE) {
				old1[VarID.NONE] = tmp1[i];
			} else {
				old1[seen[i]] = tmp1[i];
			}

			if (seen[i] == VarID.NONE) {
				old2[VarID.NONE] = tmp2[i];
			} else {
				old2[seen[i]] = tmp2[i];
			}
		}
	}
}
