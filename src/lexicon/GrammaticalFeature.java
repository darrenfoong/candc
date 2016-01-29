package lexicon;

import java.io.PrintWriter;

public class GrammaticalFeature {
	/*
	 * essentially just a wrapper class around an integer byte value
	 * representing the grammatical feature
	 * 
	 * complete set of features in the grammar:
	 */
	public static final byte NONE = 0;
	public static final byte X = 1;
	public static final byte ADJ = 2;
	public static final byte AS = 3;
	public static final byte ASUP = 4;
	public static final byte B = 5;
	public static final byte BEM = 6;
	public static final byte DCL = 7;
	public static final byte EM = 8;
	public static final byte EXPL = 9;
	public static final byte FOR = 10;
	public static final byte FRG = 11;
	public static final byte INTJ = 12;
	public static final byte INV = 13;
	public static final byte NB = 14;
	public static final byte NG = 15;
	public static final byte NUM = 16;
	public static final byte POSS = 17;
	public static final byte PSS = 18;
	public static final byte PT = 19;
	public static final byte Q = 20;
	public static final byte QEM = 21;
	public static final byte THR = 22;
	public static final byte TO = 23;
	public static final byte WQ = 24;
	public static final short NUM_FEATURES = 25;

	private final byte feature;

	public GrammaticalFeature(byte feature) {
		this.feature = feature;
	}

	public GrammaticalFeature(GrammaticalFeature other) {
		this.feature = other.feature;
	}

	public GrammaticalFeature(String feature) {
		this.feature = convert(feature);
	}

	public byte value() {
		return feature;
	}

	@Override
	public boolean equals(Object other) {
		if ( other == null || getClass() != other.getClass() ) {
			return false;
		}

		GrammaticalFeature cother = (GrammaticalFeature) other;

		return feature == cother.feature;
	}

	@Override
	public int hashCode() {
		return feature;
	}

	private static byte convert(String stringFeature) {
		if ( stringFeature.isEmpty() ) {
			throw new IllegalArgumentException("Cannot convert an empty string feature.");
		}

		byte byteFeature = NONE;

		switch ( stringFeature.charAt(0) ) {
			case 'X':
				byteFeature = X;
				break;
			case 'a':
				if ( stringFeature.length() == 2 ) {
					byteFeature = AS;
				} else if ( stringFeature.charAt(1) == 'd' ) {
					byteFeature = ADJ;
				} else {
					byteFeature = ASUP;
				}
				break;
			case 'b':
				if ( stringFeature.length() == 1 ) {
					byteFeature = B;
				} else {
					byteFeature = BEM;
				}
				break;
			case 'd':
				byteFeature = DCL;
				break;
			case 'e':
				if ( stringFeature.charAt(1) == 'm' ) {
					byteFeature = EM;
				} else {
					byteFeature = EXPL;
				}
				break;
			case 'f':
				if ( stringFeature.charAt(1) == 'o' ) {
					byteFeature = FOR;
				} else {
					byteFeature = FRG;
				}
				break;
			case 'i':
				if ( stringFeature.charAt(2) == 't' ) {
					byteFeature = INTJ;
				} else {
					byteFeature = INV;
				}
				break;
			case 'n':
				switch ( stringFeature.charAt(1) ) {
					case 'b':
						byteFeature = NB;
						break;
					case 'g':
						byteFeature = NG;
						break;
					case 'u':
						byteFeature = NUM;
						break;
					default:
						throw new IllegalArgumentException("Invalid string feature.");
				}
				break;
			case 'p':
				switch ( stringFeature.charAt(1) ) {
					case 'o':
						byteFeature = POSS;
						break;
					case 's':
						byteFeature = PSS;
						break;
					case 't':
						byteFeature = PT;
						break;
					default:
						throw new IllegalArgumentException("Invalid string feature.");
				}
				break;
			case 'q':
				if ( stringFeature.length() == 1 ) {
					byteFeature = Q;
				} else {
					byteFeature = QEM;
				}
				break;
			case 't':
				if ( stringFeature.charAt(1) == 'h' ) {
					byteFeature = THR;
				} else {
					byteFeature = TO;
				}
				break;
			case 'w':
				byteFeature = WQ;
				break;
			default:
				throw new IllegalArgumentException("Invalid string feature.");
		}

		return byteFeature;
	}

	@Override
	public String toString() {
		String featureString = "";

		switch ( feature ) {
			case NONE:
				featureString = "";
				break;
			case X:
				featureString = "X";
				break;
			case ADJ:
				featureString = "adj";
				break;
			case AS:
				featureString = "as";
				break;
			case ASUP:
				featureString = "asup";
				break;
			case B:
				featureString = "b";
				break;
			case BEM:
				featureString = "bem";
				break;
			case DCL:
				featureString = "dcl";
				break;
			case EM:
				featureString = "em";
				break;
			case EXPL:
				featureString = "expl";
				break;
			case FOR:
				featureString = "for";
				break;
			case FRG:
				featureString = "frg";
				break;
			case INTJ:
				featureString = "intj";
				break;
			case INV:
				featureString = "inv";
				break;
			case NB:
				featureString = "nb";
				break;
			case NG:
				featureString = "ng";
				break;
			case NUM:
				featureString = "num";
				break;
			case POSS:
				featureString = "poss";
				break;
			case PSS:
				featureString = "pss";
				break;
			case PT:
				featureString = "pt";
				break;
			case Q:
				featureString = "q";
				break;
			case QEM:
				featureString = "qem";
				break;
			case THR:
				featureString = "thr";
				break;
			case TO:
				featureString = "to";
				break;
			case WQ:
				featureString = "wq";
				break;
			default:
				throw new Error("Invalid feature.");
		}

		return featureString;
	}

	public void print(PrintWriter out) {
		out.print(this.toString());
	}

	public void printToErr() {
		System.err.print(this.toString());
	}

	public boolean isNone() {
		return feature == NONE;
	}

	public boolean isVar() {
		return feature == X;
	}

	public boolean isFree() {
		return feature <= X;
	}

	public boolean isAdj() {
		return feature == ADJ;
	}

	public boolean isPss() {
		return feature == PSS;
	}

	public boolean isTo() {
		return feature == TO;
	}

	public boolean isNg() {
		return feature == NG;
	}

	public boolean isDcl() {
		return feature == DCL;
	}

	public boolean isB() {
		return feature == B;
	}
}
