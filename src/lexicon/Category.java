package lexicon;

import utils.ByteWrapper;
import utils.Hash;

public class Category {
	public static final byte BWD_SLASH = 0;
	public static final byte FWD_SLASH = 1;

	public byte var;
	public final short relID;
	public final short lrange;

	private final Hash equivalenceHash;
	private final Hash unificationHash;

	// basic categories
	public final Atom atom;
	public final GrammaticalFeature feature;

	// complex categories
	public final byte slash;
	public final Category argument;
	public final Category result;
	public byte numArgs;

	// constructor for basic categories:
	public Category(Atom atom, GrammaticalFeature feature, byte var, short relID, short lrange) {
		this.var = var;
		this.relID = relID;
		this.lrange = lrange;

		this.atom = atom;
		this.feature = feature;

		this.slash = 0;
		this.argument = null;
		this.result = null;
		this.numArgs = 0;

		this.unificationHash = uHashBasic();
		this.equivalenceHash = eHashBasic();
	}

	// constructor for complex categories:
	public Category(Category result, byte slash, Category argument, byte var, short relID, short lrange) {
		this.var = var;
		this.relID = relID;
		this.lrange = lrange;

		this.atom = new Atom(Atom.NONE);
		this.feature = new GrammaticalFeature(GrammaticalFeature.NONE);

		this.slash = slash;
		this.argument = argument;
		this.result = result;
		this.numArgs = countArgs();

		this.unificationHash = uHashComplex();
		this.equivalenceHash = eHashComplex();
	}

	/*
	 * used for creating categories after rules applied and unification has
	 * provided the variable transTable and feat
	 */
	public Category(Category other, byte[] transTable, GrammaticalFeature feat) {
		this.var = transTable[other.var];
		this.relID = other.relID;
		this.lrange = other.lrange;

		this.atom = new Atom(other.atom);
		this.feature = (other.feature.isVar() && !feat.isNone()) ? new GrammaticalFeature(feat) : new GrammaticalFeature(other.feature);

		this.slash = other.slash;
		this.result = other.isComplex() ? new Category(other.result, transTable, feat) : null;
		this.argument = other.isComplex() ? new Category(other.argument, transTable, feat) : null;
		this.numArgs = other.numArgs;

		this.unificationHash = new Hash(other.unificationHash);
		this.equivalenceHash = isComplex() ? eHashComplex() : eHashBasic();
		// need to call eHash method again since the feature may have changed from other

	}

	/*
	 * uses the constructor above; upper-case first letter since this is
	 * effectively a constructor; called TransVariable because it copies the
	 * other Category whilst translating its variables using the transTable
	 */
	public static Category TransVariable(Category other, byte[] transTable, GrammaticalFeature feat) {
		return new Category(other, transTable, feat);
	}

	private byte countArgs() {
		if ( isBasic() ) {
			return (byte) 0;
		} else {
			return (byte) (result.countArgs() + 1);
		}
	}

	/*
	 * gets the variables ordered according to the left-to-right category order
	 * e.g. (S{Z}\NP{Y})/NP{X} would get changed to: (S{X}\NP{Y})/NP{Z} (X=1,
	 * Y=2, Z=3 in VarID; 0 is reserved for the no variable case (NONE)) the "_"
	 * variable is treated like any other, eg: ((S{Z}\NP{Y})/NP{Z}){_} becomes:
	 * ((S{X}\NP{Y})/NP{Z}){W}
	 */
	public void reorderVariables(byte[] seenVariables, ByteWrapper nextVarID) {
		if ( isComplex() ) {
			result.reorderVariables(seenVariables, nextVarID);
			argument.reorderVariables(seenVariables, nextVarID);
		}

		if ( var != VarID.NONE && seenVariables[var] == VarID.NONE ) {
			seenVariables[var] = ++nextVarID.value;
		}

		var = seenVariables[var];
	}

	/*
	 * similar to the method above, but note this one does not set the var field
	 * on the category objects - it only keeps track of the new order in the
	 * seen array
	 */
	public void reorderVariables(byte[] transTable, byte[] seenVariables, ByteWrapper nextVarID) {
		if ( isComplex() ) {
			result.reorderVariables(transTable, seenVariables, nextVarID);
			argument.reorderVariables(transTable, seenVariables, nextVarID);
		}

		if (var != VarID.NONE && seenVariables[transTable[var]] == VarID.NONE) {
			seenVariables[transTable[var]] = ++nextVarID.value;
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
		if ( isBasic() ) {
			return (byte) (var + 1);
		} else {
			byte current = (byte) (var + 1);
			byte resNumVars = result.getNumVars();
			byte argNumVars = argument.getNumVars();

			byte max = current;
			if ( resNumVars > max ) { max = resNumVars; }
			if ( argNumVars > max ) { max = argNumVars; }

			return max;
		}
	}

	private Hash eHashBasic() {
		Hash h = new Hash(atom.value());

		if ( ( this.isS() && !this.feature.isFree() ) || !this.isS() ) {
			h.plusEqual(feature.value());
		}

		return h;
	}

	private Hash uHashBasic() {
		return new Hash(atom.value());
	}

	private Hash eHashComplex() {
		Hash h = new Hash(result.equivalenceHash);
		h.timesEqual(2);
		h.barEqual(slash);
		h.plusEqual(argument.equivalenceHash.value());
		return h;
	}

	private Hash uHashComplex() {
		Hash h = new Hash(result.unificationHash);
		h.timesEqual(2);
		h.barEqual(slash);
		h.plusEqual(argument.unificationHash.value());
		return h;
	}

	public long getEhash() {
		return equivalenceHash.value();
	}

	public long getUhash() {
		return unificationHash.value();
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

		Category cother = (Category) other;

		// not equal if hashes are different
		if ( this.equivalenceHash.value() != cother.equivalenceHash.value() ) {
			return false;
		}

		if ( isBasic() ) {
			// not equal if atoms are different
			if ( this.atom.value() != cother.atom.value() ) {
				return false;
			}

			// not equal if S's do not match, ignoring variables on S
			// e.g. S[X] = (S = S[_]), S[a] != S[b]
			if ( this.isS()
					&& ( this.feature.value() != cother.feature.value() )
					&& ( !(this.feature.isFree() && cother.feature.isFree()) ) ) {
				return false;
			}

			// not equal if non-S's (e.g. N and NP) do not match, considering variables
			// e.g. (N = N[_]) != N[num], (NP = NP[_]) != NP[nb]
			if ( !this.isS()
					&& ( this.feature.value() != cother.feature.value() ) ) {
				return false;
			}

			return true;
		} else {
			return this.argument.equals(cother.argument)
					&& this.result.equals(cother.result);
		}
	}

	public boolean equalsWithVars(Object other) {
		if ( other == null || getClass() != other.getClass() ) {
			return false;
		}

		Category cother = (Category) other;

		// not equal if hashes are different
		if ( this.equivalenceHash.value() != cother.equivalenceHash.value() ) {
			return false;
		}

		if ( isBasic() ) {
			return this.atom.value() == cother.atom.value()
					&& this.feature.value() == cother.feature.value()
					&& this.var == cother.var
					&& this.lrange == cother.lrange;
		} else {
			if ( this.var != cother.var ) {
				return false;
			}

			return this.argument.equals(cother.argument)
					&& this.result.equals(cother.result);
		}
	}

	@Override
	public String toString() {
		if ( isBasic() ) {
			return toStringNoOuterBrackets();
		} else {
			return "(" + toStringNoOuterBrackets() + ")";
		}
	}

	public String toStringNoOuterBrackets() {
		String output = "";

		if ( isBasic() ) {
			output += atom;
			if ( feature.value() != GrammaticalFeature.NONE ) {
				output += "[" + feature + "]";
			}
		} else {
			output += result;
			if ( isFwd() ) {
				output += "/";
			} else {
				output += "\\";
			}
			output += argument;
		}

		return output;
	}

	public String toStringNoVars() {
		if ( isBasic() ) {
			return toStringNoOuterBracketsNoVars();
		} else {
			return "(" + toStringNoOuterBracketsNoVars() + ")";
		}
	}

	public String toStringNoOuterBracketsNoVars() {
		String output = "";

		if ( isBasic() ) {
			output += atom;
			if ( feature.value() != GrammaticalFeature.NONE && feature.value() != GrammaticalFeature.X ) {
				output += "[" + feature + "]";
			}
		} else {
			output += result.toStringNoVars();
			if ( isFwd() ) {
				output += "/";
			} else {
				output += "\\";
			}
			output += argument.toStringNoVars();
		}

		return output;
	}

	public boolean isBasic() {
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
