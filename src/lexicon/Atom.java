package lexicon;

import java.io.PrintWriter;

public class Atom {
	/*
	 * essentially just a wrapper class around an integer byte value
	 * representing the atomic category
	 * 
	 * the complete set of atomic categories in the grammar:
	 */
	public static final byte NONE = 0;
	public static final byte N = 1;
	public static final byte NP = 2;
	public static final byte S = 3;
	public static final byte PP = 4;
	public static final byte CONJ = 5;
	public static final byte PERIOD = 6;
	public static final byte COLON = 7;
	public static final byte SEMICOLON = 8;
	public static final byte COMMA = 9;
	public static final byte LQU = 10;
	public static final byte RQU = 11;
	public static final byte LRB = 12;
	public static final byte RRB = 13;
	public static final short NUM_ATOMS = 14;

	private final byte atom;

	public Atom(byte atom) {
		this.atom = atom;
	}

	public Atom(Atom other) {
		this.atom = other.atom;
	}

	public Atom(String stringAtom) {
		this.atom = convert(stringAtom);
	}

	public byte value() {
		return atom;
	}

	private static byte convert(String stringAtom) {
		if ( stringAtom.isEmpty() ) {
			throw new IllegalArgumentException("Cannot convert an empty string atom.");
		}

		byte byteAtom = NONE;

		switch ( stringAtom.charAt(0) ) {
			case ':':
				byteAtom = COLON;
				break;
			case ',':
				byteAtom = COMMA;
				break;
			case 'c':
				byteAtom = CONJ;
				break;
			case 'L':
				if ( stringAtom.charAt(1) == 'R' ) {
					byteAtom = LRB;
				} else {
					byteAtom = LQU;
				}
				break;
			case 'N':
				if ( stringAtom.length() == 1 ) {
					byteAtom = N;
				} else {
					byteAtom = NP;
				}
				break;
			case '.':
				byteAtom = PERIOD;
				break;
			case 'P':
				byteAtom = PP;
				break;
			case 'R':
				if ( stringAtom.charAt(1) == 'R' ) {
					byteAtom = RRB;
				} else {
					byteAtom = RQU;
				}
				break;
			case 'S':
				byteAtom = S;
				break;
			case ';':
				byteAtom = SEMICOLON;
				break;
			default:
				throw new IllegalArgumentException("Invalid string atom.");
		}

		return byteAtom;
	}

	@Override
	public String toString() {
		String atomString = "";

		switch (atom) {
			case NONE:
				atomString = "";
				break;
			case N:
				atomString = "N";
				break;
			case NP:
				atomString = "NP";
				break;
			case S:
				atomString = "S";
				break;
			case PP:
				atomString = "PP";
				break;
			case CONJ:
				atomString = "conj";
				break;
			case PERIOD:
				atomString = ".";
				break;
			case COLON:
				atomString = ":";
				break;
			case SEMICOLON:
				atomString = ";";
				break;
			case COMMA:
				atomString = ",";
				break;
			case LQU:
				atomString = "LQU";
				break;
			case RQU:
				atomString = "RQU";
				break;
			case LRB:
				atomString = "LRB";
				break;
			case RRB:
				atomString = "RRB";
				break;
			default:
				throw new Error("Invalid atom.");
		}

		return atomString;
	}

	public void print(PrintWriter out) {
		out.print(this.toString());
	}

	public void printToErr() {
		System.err.print(this.toString());
	}

	public boolean isN() {
		return atom == N;
	}

	public boolean isNP() {
		return atom == NP;
	}

	public boolean isNorNP() {
		return atom == NP || atom == N;
	}

	public boolean isPP() {
		return atom == PP;
	}

	public boolean isConj() {
		return atom == CONJ;
	}

	public boolean isS() {
		return atom == S;
	}

	public boolean isComma() {
		return atom == COMMA;
	}

	public boolean isPeriod() {
		return atom == PERIOD;
	}

	public boolean isCommaOrPeriod() {
		return atom == COMMA || atom == PERIOD;
	}

	public boolean isSemicolon() {
		return atom == SEMICOLON;
	}

	public boolean isColon() {
		return atom == COLON;
	}

	public boolean isSemiOrColon() {
		return atom == COLON || atom == SEMICOLON;
	}

	public boolean isLRB() {
		return atom == LRB;
	}

	public boolean isRRB() {
		return atom == RRB;
	}

	public boolean isLQU() {
		return atom == LQU;
	}

	public boolean isRQU() {
		return atom == RQU;
	}

	public boolean isPunct() {
		return atom >= PERIOD && atom <= RRB;
	}
}
