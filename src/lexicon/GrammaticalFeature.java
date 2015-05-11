package lexicon;

import java.io.PrintWriter;

public class GrammaticalFeature {
	/*
	 * essentially just a wrapper class around an integer byte value
	 * representing the grammatical feature
	 * 
	 * complete set of features in the grammar:
	 */
	final public static byte NONE = 0;
	final public static byte X = 1;
	final public static byte ADJ = 2;
	final public static byte AS = 3;
	final public static byte ASUP = 4;
	final public static byte B = 5;
	final public static byte BEM = 6;
	final public static byte DCL = 7;
	final public static byte EM = 8;
	final public static byte EXPL = 9;
	final public static byte FOR = 10;
	final public static byte FRG = 11;
	final public static byte INTJ = 12;
	final public static byte INV = 13;
	final public static byte NB = 14;
	final public static byte NG = 15;
	final public static byte NUM = 16;
	final public static byte POSS = 17;
	final public static byte PSS = 18;
	final public static byte PT = 19;
	final public static byte Q = 20;
	final public static byte QEM = 21;
	final public static byte THR = 22;
	final public static byte TO = 23;
	final public static byte WQ = 24;
	final public static short NUM_FEATURES = 25;

	final private byte feature;

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

	/*
	 * note it's important that the argument to the equals method is an Object,
	 * and not GrammaticalFeature, since this is the default signature we want
	 * to override:
	 */
	@Override
	public boolean equals(Object other) {
		return feature == ((GrammaticalFeature) (other)).feature;
	}

	/*
	 * returns byte version of a String feature
	 */
	static byte convert(String stringFeature) {
		byte byteFeature = NONE;

		if (stringFeature.isEmpty()) {
			throw new Error("cannot convert an empty string feature!");
		}

		switch (stringFeature.charAt(0)) {
		case 'X':
			byteFeature = X;
			break;
		case 'a':
			if (stringFeature.length() == 2) {
				byteFeature = AS;
			} else if (stringFeature.charAt(1) == 'd') {
				byteFeature = ADJ;
			} else {
				byteFeature = ASUP;
			}
			break;
		case 'b':
			if (stringFeature.length() == 1) {
				byteFeature = B;
			} else {
				byteFeature = BEM;
			}
			break;
		case 'd':
			byteFeature = DCL;
			break;
		case 'e':
			if (stringFeature.charAt(1) == 'm') {
				byteFeature = EM;
			} else {
				byteFeature = EXPL;
			}
			break;
		case 'f':
			if (stringFeature.charAt(1) == 'o') {
				byteFeature = FOR;
			} else {
				byteFeature = FRG;
			}
			break;
		case 'i':
			if (stringFeature.charAt(2) == 't') {
				byteFeature = INTJ;
			} else {
				byteFeature = INV;
			}
			break;
		case 'n':
			switch (stringFeature.charAt(1)) {
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
				throw new Error(
						"run out of grammatical features to check after 'n'!");
			}
			break;
		case 'p':
			switch (stringFeature.charAt(1)) {
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
				throw new Error(
						"run out of grammatical features to check after 'p'!");
			}
			break;
		case 'q':
			if (stringFeature.length() == 1) {
				byteFeature = Q;
			} else {
				byteFeature = QEM;
			}
			break;
		case 't':
			if (stringFeature.charAt(1) == 'h') {
				byteFeature = THR;
			} else {
				byteFeature = TO;
			}
			break;
		case 'w':
			byteFeature = WQ;
			break;
		default:
			throw new Error("run out of grammatical features to check!");
		}
		return byteFeature;
	}

	/*
	 * returns String representation of this feature
	 */
	public String convert() {
		String featureString = "";

		switch (feature) {
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
			throw new Error("run out of features to convert?!");
		}
		return featureString;
	}

	public void print(PrintWriter out) {
		out.print(convert());
	}

	public void printToErr() {
		System.err.print(convert());
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
