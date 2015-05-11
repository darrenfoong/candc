package lexicon;

import java.io.PrintWriter;

import utils.ByteWrapper;
import utils.Hash;

public class Category {
	public static final byte BWD_SLASH = 0;
	public static final byte FWD_SLASH = 1;

	// basic category (atom should be set to NONE for complex category):
	public final Atom atom;
	public final GrammaticalFeature feature;
	public byte var;

	public final short relID;
	public final short lrange;

	// complex category:
	public final byte slash;
	public final Category result;
	public final Category argument;

	/*
	 * unificationHash is used as a quick check to see if two categories cannot
	 * unify; equivalenceHash is used to see if two categories are equivalent in
	 * the chart; the only difference is that eHash() takes the grammatical
	 * feature on S into account
	 */
	private final Hash unificationHash;
	private final Hash equivalenceHash;

	// categories are limited to a maximum no. of arguments:
	public byte numArgs;

	// constructor for basic categories:
	public Category(Atom atom, GrammaticalFeature feature, byte var, short relID, short lrange) {
		this.atom = atom;
		this.feature = feature;
		this.var = var;
		this.relID = relID;
		this.lrange = lrange;
		this.slash = 0;
		this.result = null;
		this.argument = null;
		this.unificationHash = uHashBasic();
		this.equivalenceHash = eHashBasic();
		this.numArgs = 0;
		/*
		 * print(); System.out.println(" Num args: " + numArgs);
		 */
	}

	// constructor for complex categories:
	public Category(Category result, byte slash, Category argument, byte var, short relID, short lrange) {
		this.atom = new Atom(Atom.NONE);
		this.feature = new GrammaticalFeature(GrammaticalFeature.NONE);
		this.var = var;
		this.relID = relID;
		this.lrange = lrange;
		this.result = result;
		this.argument = argument;
		this.slash = slash;
		this.unificationHash = uHashComplex();
		this.equivalenceHash = eHashComplex();
		this.numArgs = countArgs();
		/*
		 * print(); System.out.println(" Num args: " + numArgs);
		 */
	}

	/*
	 * used for creating categories after rules applied and unification has
	 * provided the variable transTable and feat
	 */
	public Category(Category other, byte[] transTable, GrammaticalFeature feat) {
		this.atom = new Atom(other.atom);
		this.feature = ((other.feature.isVar() && !feat.isNone()) ? new GrammaticalFeature(feat) : new GrammaticalFeature(other.feature));
		this.var = transTable[other.var];
		this.relID = other.relID;
		this.lrange = other.lrange;
		this.slash = other.slash;
		this.result = ((other.result != null) ? new Category(other.result, transTable, feat) : null);
		this.argument = ((other.argument != null) ? new Category(other.argument, transTable, feat) : null);
		this.unificationHash = new Hash(other.unificationHash);
		this.equivalenceHash = ((result == null) ? eHashBasic() : eHashComplex());
		// need to call eHash method again since the feature may have changed
		// from other
		this.numArgs = other.numArgs;
		/*
		 * print(); System.out.println(" Num args: " + numArgs);
		 */
	}

	/*
	 * uses the constructor above; upper-case first letter since this is
	 * effectively a constructor; called TransVariable because it copies the
	 * other Category whilst translating its variables unsing the transTable
	 */
	public static Category TransVariable(Category other, byte[] transTable, GrammaticalFeature feat) {
		return new Category(other, transTable, feat);
	}

	private byte countArgs() {
		if (result != null) {
			return (byte) (result.countArgs() + 1);
		} else {
			return (byte) (0);
		}
	}

	/*
	 * gets the variables ordered according to the left-to-right category order
	 * e.g. (S{Z}\NP{Y})/NP{X} would get changed to: (S{X}\NP{Y})/NP{Z} (X=1,
	 * Y=2, Z=3 in VarID; 0 is reserved for the no variable case (NONE)) the "_"
	 * variable is treated like any other, eg: ((S{Z}\NP{Y})/NP{Z}){_} becomes:
	 * ((S{X}\NP{Y})/NP{Z}){W}
	 */
	public void reorderVariables(byte[] seenVariables, ByteWrapper order) {
		if (result != null) {
			result.reorderVariables(seenVariables, order);
			argument.reorderVariables(seenVariables, order);
		}

		if (var != VarID.NONE && seenVariables[var] == VarID.NONE) {
			seenVariables[var] = ++order.value;
		}

		var = seenVariables[var];
	}

	/*
	 * similar to the method above, but note this one does not set the var field
	 * on the category objects - it only keeps track of the new order in the
	 * seen array
	 */
	public void reorderVariables(byte[] transTable, byte[] seenVariables, ByteWrapper order) {
		if (isAtomic()) {
			if (var != VarID.NONE
					&& seenVariables[transTable[var]] == VarID.NONE) {
				seenVariables[transTable[var]] = ++order.value;
			}
		} else {
			result.reorderVariables(transTable, seenVariables, order);
			argument.reorderVariables(transTable, seenVariables, order);

			if (var != VarID.NONE
					&& seenVariables[transTable[var]] == VarID.NONE) {
				seenVariables[transTable[var]] = ++order.value;
			}
		}
	}

	/*
	 * recursively goes thro' category finding the highest value varID
	 * 
	 * note that the increment at the beginning means this returns 1 more than
	 * the actual number of variables; the reason is that we always assume a
	 * NONE variable; eg with X and Y variables, numVars = 3
	 */
	public byte getNumVars() {
		byte current = var;
		current++;

		if (this.isAtomic()) {
			return current;
		} else {
			byte max = result.getNumVars();
			byte maxArg = argument.getNumVars();

			if (max < maxArg) {
				max = maxArg;
			}

			if (max < current) {
				max = current;
			}

			return max;
		}
	}

	// easy for atomic categories: just use the atom value since these
	// are distinct for all basic categories
	private Hash uHashBasic() {
		return new Hash(atom.value());
	}

	// spread the complex cat hash values out beyond the basic values
	private Hash uHashComplex() {
		Hash h = new Hash(result.unificationHash.value());
		h.timesEqual(2);
		h.barEqual(slash);
		h.plusEqual(argument.unificationHash.value());
		return h;
	}

	/*
	 * ignore all features except on the S atom where we also ignore NONE and
	 * the variable X
	 */
	private Hash eHashBasic() {
		Hash h = uHashBasic();
		if ((isS() || isN() || isNP()) && !feature.isFree()) {
			h.plusEqual(feature.value());
		}
		return h;
	}

	private Hash eHashComplex() {
		Hash h = new Hash(result.equivalenceHash.value());
		h.plusEqual(argument.equivalenceHash.value());
		h.timesEqual(2);
		h.barEqual(slash);
		return h;
	}

	public long getUhash() {
		return unificationHash.value();
	}

	public long getEhash() {
		return equivalenceHash.value();
	}

	/*
	 * there are various equality functions defined for categories in C&C; this
	 * one is used by the SuperCategory equals() method
	 */
	public static boolean equal(Category cat1, Category cat2) {
		if (cat1.getEhash() != cat2.getEhash() || cat1.getUhash() != cat2.getUhash()) {
			return false;
		}

		return !unequal(cat1, cat2);
	}

	private static boolean unequal(Category cat1, Category cat2) {
		if (cat1.isAtomic()) {
			if (!cat2.isAtomic()) {
				return true;
			} else {
				// both atomic
				return cat1.atom.value() != cat2.atom.value()
						|| cat1.var != cat2.var
						|| cat1.feature.value() != cat2.feature.value()
						|| cat1.lrange != cat2.lrange;
			}
		} else {
			// cat1 complex
			if (cat2.isAtomic()) {
				return true;
			} else {
				// both complex
				if (cat1.var != cat2.var) {
					return true;
				}

				if (cat1.result.getEhash() != cat2.result.getEhash() || cat1.argument.getEhash() != cat2.argument.getEhash()) {
					return true;
				}

				return unequal(cat1.result, cat2.result)
						|| unequal(cat1.argument, cat2.argument);
			}
		}
	}

	// this one used by the equals method (used for Hash structures)
	public static boolean equalIgnoreVars2(Category cat1, Category cat2) {
		if (cat1.getEhash() != cat2.getEhash() || cat1.getUhash() != cat2.getUhash()) {
			return false;
		}

		return !unequalIgnoreVars2(cat1, cat2);
	}

	// note that S[X] is the same as S here, but not NP[nb] and NP,
	// nor are N and N[num]
	private static boolean unequalIgnoreVars2(Category cat1, Category cat2) {
		if (cat1.isAtomic()) {
			if (!cat2.isAtomic()) {
				return true;
			} else {
				// both atomic
				return cat1.atom.value() != cat2.atom.value()
						|| (cat1.isS()
								&& (!cat1.feature.isFree() || !cat2.feature.isFree())
								&& cat1.feature.value() != cat2.feature.value())
						|| (!cat1.isS()
								&& cat1.feature.value() != cat2.feature.value());
			}
		} else {
			// cat1 complex
			if (cat2.isAtomic()) {
				return true;
			} else {
				// both complex
				if (cat1.result.getEhash() != cat2.result.getEhash() || cat1.argument.getEhash() != cat2.argument.getEhash()) {
					return true;
				}

				return unequalIgnoreVars2(cat1.result, cat2.result)
						|| unequalIgnoreVars2(cat1.argument, cat2.argument);
			}
		}
	}

	// hashCode and equals used by the canonicalCats hashSet in Categories
	@Override
	public int hashCode() {
		return (int) (equivalenceHash.value());
	}

	// equalIgnoreVars2 *does* take the features on NPs into account,
	// so N[num] != N (updated 10/6/14)
	@Override
	public boolean equals(Object other) {
		return equalIgnoreVars2(this, (Category) (other));
		// return equalIgnoreVars(this, (Category)(other));
	}

	@Override
	public String toString() {
		String output = "";
		if (isAtomic()) {
			output += atom.toString();
			if (feature.value() != GrammaticalFeature.NONE) {
				output += "[";
				output += feature.convert();
				output += "]";
			}
		} else {
			output += "(";
			output += result.toString();
			if (isFwd()) {
				output += "/";
			} else {
				output += "\\";
			}
			output += argument.toString();
			output += ")";
		}

		return output;
	}

	public String toStringNoOuterBrackets() {
		String output = "";
		if (isAtomic()) {
			output += atom.toString();
			if (feature.value() != GrammaticalFeature.NONE) {
				output += "[";
				output += feature.convert();
				output += "]";
			}
		} else {
			output += result.toString();
			if (isFwd()) {
				output += "/";
			} else {
				output += "\\";
			}
			output += argument.toString();
		}

		return output;
	}

	public void print(PrintWriter out) {
		if (isAtomic()) {
			atom.print(out);
			if (feature.value() != GrammaticalFeature.NONE) {
				out.print("[");
				feature.print(out);
				out.print("]");
			}
		} else {
			out.print("(");
			result.print(out);
			if (isFwd()) {
				out.print("/");
			} else {
				out.print("\\");
			}
			argument.print(out);
			out.print(")");
		}
	}

	public void printToErr() {
		if (isAtomic()) {
			atom.printToErr();
			if (feature.value() != GrammaticalFeature.NONE) {
				System.err.print("[");
				feature.printToErr();
				System.err.print("]");
			}
		} else {
			System.err.print("(");
			result.printToErr();
			if (isFwd()) {
				System.err.print("/");
			} else {
				System.err.print("\\");
			}
			argument.printToErr();
			System.err.print(")");
		}
	}

	public void printNoOuterBrackets(PrintWriter out, boolean outer) {
		if (isAtomic()) {
			atom.print(out);
			if (feature.value() != GrammaticalFeature.NONE
					&& feature.value() != GrammaticalFeature.X) {
				out.print("[");
				feature.print(out);
				out.print("]");
			}
		} else {
			if (!outer) {
				out.print("(");
			}
			result.printNoOuterBrackets(out, false);
			if (isFwd()) {
				out.print("/");
			} else {
				out.print("\\");
			}
			argument.printNoOuterBrackets(out, false);
			if (!outer) {
				out.print(")");
			}
		}
	}

	/*
	 * remaining methods are a large set of observers checking for various
	 * properties and types
	 */
	public boolean isAtomic() {
		return atom.value() != Atom.NONE;
	}

	public boolean isComplex() {
		return atom.value() == Atom.NONE;
	}

	public boolean isFwd() {
		return isComplex() && slash == FWD_SLASH;
	}

	public boolean isBwd() {
		return isComplex() && slash == BWD_SLASH;
	}

	public boolean notFwd() {
		return !isFwd();
	}

	public boolean notBwd() {
		return !isBwd();
	}

	public boolean isNP() {
		return atom.isNP();
	}

	public boolean isN() {
		return atom.isN();
	}

	public boolean isNorNP() {
		return atom.isNorNP();
	}

	public boolean isPP() {
		return atom.isPP();
	}

	public boolean isConj() {
		return atom.isConj();
	}

	public boolean isS() {
		return atom.isS();
	}

	public boolean isComma() {
		return atom.isComma();
	}

	public boolean isCommaOrPeriod() {
		return atom.isCommaOrPeriod();
	}

	public boolean isPeriod() {
		return atom.isPeriod();
	}

	public boolean isColon() {
		return atom.isColon();
	}

	public boolean isSemicolon() {
		return atom.isSemicolon();
	}

	public boolean isSemiOrColon() {
		return atom.isSemiOrColon();
	}

	public boolean isLRB() {
		return atom.isLRB();
	}

	public boolean isRRB() {
		return atom.isRRB();
	}

	public boolean isLQU() {
		return atom.isLQU();
	}

	public boolean isRQU() {
		return atom.isRQU();
	}

	public boolean isPunct() {
		return atom.isPunct();
	}

	public boolean hasNone() {
		return feature.isNone();
	}

	public boolean hasFeatureVar() {
		return feature.isVar();
	}

	public boolean isFree() {
		return feature.isFree();
	}

	public boolean hasAdj() {
		return feature.isAdj();
	}

	public boolean hasPss() {
		return feature.isPss();
	}

	public boolean hasTo() {
		return feature.isTo();
	}

	public boolean hasNg() {
		return feature.isNg();
	}

	public boolean hasDcl() {
		return feature.isDcl();
	}

	public boolean hasB() {
		return feature.isB();
	}

	public boolean isSbNP() {
		return isBwd() && argument.isNP() && result.isS();
	}

	public boolean isSfNP() {
		return isFwd() && argument.isNP() && result.isS();
	}

	public boolean isSdclbNP() {
		return isSbNP() && result.hasDcl();
	}

	public boolean isSdclfNP() {
		return isSfNP() && result.hasDcl();
	}

	public boolean isStobNP() {
		return isSbNP() && result.hasTo();
	}

	public boolean isSbS() {
		return isBwd() && argument.isS() && result.isS();
	}

	public boolean isSbPP() {
		return isBwd() && argument.isPP() && result.isS();
	}

	public boolean isNPbNP() {
		return isBwd() && argument.isNP() && result.isNP();
	}

	public boolean isNbN() {
		return isBwd() && argument.isN() && result.isN();
	}

	public boolean isSfS() {
		return isFwd() && argument.isS() && result.isS();
	}

	public boolean isNfN() {
		return isFwd() && argument.isN() && result.isN();
	}

	public boolean isNfNbNfN() {
		return isBwd() && argument.isNfN() && result.isNfN();
	}

	public boolean isNPfNPbNP() {
		return isFwd() && argument.isNPbNP() && result.isNP();
	}

	public boolean isSbNPbSbNP() {
		return isBwd() && argument.isSbNP() && result.isSbNP();
	}

	public boolean isVPbVPbVPbVP() {
		return isBwd() && argument.isSbNPbSbNP() && result.isSbNPbSbNP();
	}

	public boolean isSbNPfSbNP() {
		return isFwd() && argument.isSbNP() && result.isSbNP();
	}

	public boolean isSdcl() {
		return isS() && hasDcl();
	}

	public boolean isSb() {
		return isS() && hasB();
	}

	public boolean isSdclbNPfSdcl() {
		return isFwd() && argument.isSdcl() && result.isSdclbNP();
	}

	public boolean isSdclbNPfS() {
		return isFwd() && argument.isS() && result.isSdclbNP();
	}

	public boolean isSdclbNPfNP() {
		return isFwd() && argument.isNP() && result.isSdclbNP();
	}

	public boolean isSdclbNPfPP() {
		return isFwd() && argument.isPP() && result.isSdclbNP();
	}

	public boolean isSdclbSbNP() {
		return isBwd() && argument.isSbNP() && result.isSdcl();
	}

	public boolean isSdclbSdcl() {
		return isBwd() && argument.isSdcl() && result.isSdcl();
	}

	public boolean isSdclfSdcl() {
		return isFwd() && argument.isSdcl() && result.isSdcl();
	}

	public boolean isSdclbSdclbNP() {
		return isBwd() && argument.isNP() && result.isSdclbSdcl();
	}

	public boolean isStobNPfNP() {
		return isFwd() && argument.isNP() && result.isStobNP();
	}

	public boolean isNPbNPfSdclbNP() {
		return isFwd() && result.isNPbNP() && argument.isSdclbNP();
	}

	public boolean isPPbPP() {
		return isBwd() && argument.isPP() && result.isPP();
	}

	public boolean isAP() {
		return isSbNP() && result.hasAdj();
	}

	public boolean isConjbConj() {
		return isBwd() && argument.isConj() && result.isConj();
	}

	public boolean isArgNorNP() {
		return argument.isN() || argument.isNP();
	}
}
