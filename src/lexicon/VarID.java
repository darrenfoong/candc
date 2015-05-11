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
	final public static byte NONE = 0;
	final static byte X = 1;
	final static byte Y = 2;
	final static byte Z = 3;
	final static byte W = 4;
	final static byte V = 5;
	final static byte U = 6;
	final static byte T = 7;
	final static byte R = 8;
	final static byte Q = 9;
	final static byte A = 10;
	final static byte B = 11;
	final static byte C = 12;
	final static byte D = 13;
	final static byte E = 14;
	final static byte F = 15;
	final public static short NUM_VARS = 16;

	final private byte varID;

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

	/*
	 * returns byte representation of String variable note that "_" is for
	 * lexical filled variables and shares the same VarID as "X" so the markedup
	 * file cannot use the "X" variable
	 */
	static public byte convert(String stringVarID) {
		if (stringVarID.isEmpty()) {
			throw new Error("cannot convert an empty string atom!");
		}

		if (stringVarID.equals("+")) {
			return NONE;
		} else if (stringVarID.equals("X") || stringVarID.equals("_")) {
			return X;
		} else if (stringVarID.equals("Y")) {
			return Y;
		} else if (stringVarID.equals("Z")) {
			return Z;
		} else if (stringVarID.equals("W")) {
			return W;
		} else if (stringVarID.equals("V")) {
			return V;
		} else if (stringVarID.equals("U")) {
			return U;
		} else if (stringVarID.equals("T")) {
			return T;
		} else if (stringVarID.equals("R")) {
			return R;
		} else if (stringVarID.equals("Q")) {
			return Q;
		} else if (stringVarID.equals("A")) {
			return A;
		} else if (stringVarID.equals("B")) {
			return B;
		} else if (stringVarID.equals("C")) {
			return C;
		} else if (stringVarID.equals("D")) {
			return D;
		} else if (stringVarID.equals("E")) {
			return E;
		} else if (stringVarID.equals("F")) {
			return F;
		} else {
			throw new Error("run out of varIDs to try!" + stringVarID
					+ "buffer");
		}
	}

	/*
	 * returns String representation of byte variable
	 */
	public String convert() {
		String varIDString = "";

		switch (varID) {
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
			throw new Error("run out of varIDs to convert?!");
		}
		return varIDString;
	}

	@Override
	public int compareTo(VarID other) {
		if (this.varID == other.varID) {
			return 0;
		} else if (this.varID < other.varID) {
			return -1;
		} else {
			return 1;
		}
	}

	public void print() {
		System.out.print(convert());
	}
}
