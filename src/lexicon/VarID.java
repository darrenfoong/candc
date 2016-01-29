package lexicon;

public class VarID implements Comparable<VarID> {
	/*
	 * NOT USING VarID objects now (29 Jan 2014), rather using byte values
	 * directly, but still taking the (static) byte values from this class and
	 * using the (static) convert method
	 * 
	 * essentially just a wrapper class around an integer byte value
	 * representing the variable
	 * 
	 * (at least) the complete set of variable symbols seen in the markedup file
	 */
	public static final byte NONE = 0;
	public static final byte X = 1;
	public static final byte Y = 2;
	public static final byte Z = 3;
	public static final byte W = 4;
	public static final byte V = 5;
	public static final byte U = 6;
	public static final byte T = 7;
	public static final byte R = 8;
	public static final byte Q = 9;
	public static final byte A = 10;
	public static final byte B = 11;
	public static final byte C = 12;
	public static final byte D = 13;
	public static final byte E = 14;
	public static final byte F = 15;
	public static final short NUM_VARS = 16;

	private final byte varID;

	public VarID() {
		this.varID = NONE;
	}

	public VarID(byte varID) {
		this.varID = varID;
	}

	public VarID(VarID other) {
		this.varID = other.varID;
	}

	public VarID(String varID) {
		this.varID = convert(varID);
	}

	public byte value() {
		return varID;
	}

	@Override
	public boolean equals(Object other) {
		if ( other == null || getClass() != other.getClass() ) {
			return false;
		}

		VarID cother = (VarID) other;

		return compareTo(cother) == 0;
	}

	@Override
	public int hashCode() {
		return varID;
	}

	@Override
	public int compareTo(VarID other) {
		return Byte.compare(this.varID, other.varID);
	}

	/*
	 * returns byte representation of String variable note that "_" is for
	 * lexical filled variables and shares the same VarID as "X" so the markedup
	 * file cannot use the "X" variable
	 */
	public static byte convert(String stringVarID) {
		if ( stringVarID.isEmpty() ) {
			throw new IllegalArgumentException("Cannot convert an empty string VarID.");
		}

		if ( stringVarID.equals("+") ) {
			return NONE;
		} else if ( stringVarID.equals("X") || stringVarID.equals("_") ) {
			return X;
		} else if ( stringVarID.equals("Y") ) {
			return Y;
		} else if ( stringVarID.equals("Z") ) {
			return Z;
		} else if ( stringVarID.equals("W") ) {
			return W;
		} else if ( stringVarID.equals("V") ) {
			return V;
		} else if ( stringVarID.equals("U") ) {
			return U;
		} else if ( stringVarID.equals("T") ) {
			return T;
		} else if ( stringVarID.equals("R") ) {
			return R;
		} else if ( stringVarID.equals("Q") ) {
			return Q;
		} else if ( stringVarID.equals("A") ) {
			return A;
		} else if ( stringVarID.equals("B") ) {
			return B;
		} else if ( stringVarID.equals("C") ) {
			return C;
		} else if ( stringVarID.equals("D") ) {
			return D;
		} else if ( stringVarID.equals("E") ) {
			return E;
		} else if ( stringVarID.equals("F") ) {
			return F;
		} else {
			throw new IllegalArgumentException("Invalid string VarID.");
		}
	}

	@Override
	public String toString() {
		String varIDString = "";

		switch ( varID ) {
			case NONE:
				varIDString = "";
				break;
			case X:
				varIDString = "X";
				break;
			case Y:
				varIDString = "Y";
				break;
			case Z:
				varIDString = "Z";
				break;
			case W:
				varIDString = "W";
				break;
			case V:
				varIDString = "V";
				break;
			case U:
				varIDString = "U";
				break;
			case T:
				varIDString = "T";
				break;
			case R:
				varIDString = "R";
				break;
			case Q:
				varIDString = "Q";
				break;
			case A:
				varIDString = "A";
				break;
			case B:
				varIDString = "B";
				break;
			case C:
				varIDString = "C";
				break;
			case D:
				varIDString = "D";
				break;
			case E:
				varIDString = "E";
				break;
			case F:
				varIDString = "F";
				break;
			default:
				throw new Error("Invalid VarID.");
		}

		return varIDString;
	}
}
