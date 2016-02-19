package cat_combination;

import io.Sentence;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import chart_parser.ChartParser;
import lexicon.Atom;
import lexicon.Categories;
import lexicon.Category;

public class Rules {
	public Combinators combinators;
	public UnaryRules unaryRules;
	public PunctRules punctRules;
	public RuleInstances allRuleInstances;
	/*
	 * unaryRules and PunctRules use categories to get particular Category
	 * objects, eg the NP Category for the N => NP rule, as well as the lists of
	 * type-raised categories (for Unary)
	 */

	// global counts for how many times the rules are used:
	public static int forwardAppCount;
	public static int backwardAppCount;
	public static int forwardCompCount;
	public static int genForwardCompCount;
	public static int backwardCompCount;
	public static int genBackwardCompCount;
	public static int backwardCrossCount;
	public static int genBackwardCrossCount;
	public static int appositionCount;
	public static int rightPunctCount;
	public static int leftPunctCount;
	public static int leftPunctConjCount;
	public static int leftCommaTCCount;
	public static int rightCommaTCCount;
	public static int conjCount;
	public static int funnyConjCount;

	/*
	 * these flags are calculated by the static ruleFlags method below and used
	 * to determine which rules can apply to two categories
	 */
	private static final int NONE = 0;
	private static final int FORWARD = 1;
	private static final int BACKWARD = 2;
	private static final int CONJ = 3;
	private static final int PERIOD = 4;
	private static final int COLON = 5;
	private static final int COMMA = 6;
	private static final int BRACKET = 7;
	private static final int NFLAGS = 8;

	public static final Logger logger = LogManager.getLogger(Rules.class);

	public Rules(boolean eisnerNormalForm, Categories categories, RuleInstancesParams ruleInstancesParams) {
		if (ruleInstancesParams.getAllRules()) {
			allRuleInstances = new RuleInstances(ruleInstancesParams.getDirectory() + "/all_rule_instances", categories);
		} else {
			allRuleInstances = null;
		}

		combinators = new Combinators(eisnerNormalForm, categories, ruleInstancesParams);
		unaryRules = new UnaryRules(categories);
		punctRules = new PunctRules(categories, ruleInstancesParams);
	}

	public boolean combine(SuperCategory leftSuperCat, SuperCategory rightSuperCat, ArrayList<SuperCategory> results,  Sentence sentence) {
		logger.trace("trying to combine two cats: ");
		logger.trace(leftSuperCat.cat);
		logger.trace(rightSuperCat.cat);

		if (allRuleInstances != null && !allRuleInstances.contains(leftSuperCat.cat, rightSuperCat.cat)) {
			/*
			 * PrintWriter systemOut = new PrintWriter(System.out, true);
			 * System.out.println("MISSING THESE: ");
			 * leftSuperCat.cat.print(systemOut); systemOut.flush();
			 * System.out.print(" "); rightSuperCat.cat.print(systemOut);
			 * systemOut.flush(); System.out.println();
			 */
			return false;
			// haven't got this to work yet - because of Category.equals, we need NP[nb] in the rules file
		}

		int leftFlags = ruleFlags(leftSuperCat.cat);
		int rightFlags = ruleFlags(rightSuperCat.cat);

		// not sure this is doing much, and may not be worth having
		// the extra field in Category; printing out categories only
		// appears to give cats with arguments up to 6
		if (leftSuperCat.cat.numArgs > 5 || rightSuperCat.cat.numArgs > 5) {
			return false;
		}

		/*
		 * this was the equivalent from C&C, using nvars: if(sc1->nvars > 9 ||
		 * sc2->nvars > 9){ ++rule_nvars; return false;
		 */

		/*
		 * each combination of left and right flags values results in a unique
		 * value for ruleFlagsPair:
		 */
		int ruleFlagsPair = leftFlags * NFLAGS + rightFlags;
		switch (ruleFlagsPair) {
			/*
			 * two atomic categories, neither punctuation nor conj:
			 */
			case NONE * NFLAGS + NONE:
				return combinators.apposition(leftSuperCat, rightSuperCat, results);
			/*
			 * atomic left, complex backslash right:
			 */
			case NONE * NFLAGS + BACKWARD:
				return combinators.backwardApplication(leftSuperCat, rightSuperCat, results, sentence);
			/*
			 * atomic or complex left, punct (not comma) right:
			 */
			case NONE * NFLAGS + PERIOD: // fall through
			case NONE * NFLAGS + COLON: // |
			case NONE * NFLAGS + BRACKET: // |
			case FORWARD * NFLAGS + PERIOD: // |
			case FORWARD * NFLAGS + COLON: // |
			case FORWARD * NFLAGS + BRACKET: // |
			case BACKWARD * NFLAGS + PERIOD: // |
			case BACKWARD * NFLAGS + COLON: // |
			case BACKWARD * NFLAGS + BRACKET: // |
				return punctRules.rightPunct(leftSuperCat, rightSuperCat, results);
			/*
			 * atomic or complex left, comma right:
			 */
			case NONE * NFLAGS + COMMA: // fall through
			case FORWARD * NFLAGS + COMMA: // |
			case BACKWARD * NFLAGS + COMMA: // |
				boolean typeChangeResult = punctRules.rightCommaTypeChange(leftSuperCat, rightSuperCat, results);
				boolean punctResult = punctRules.rightPunct(leftSuperCat, rightSuperCat, results);
				return typeChangeResult || punctResult;
			/*
			 * complex forward slash left, atomic right conj case is for
			 * conj/conj conj
			 */
			case FORWARD * NFLAGS + NONE: // fall through
			case FORWARD * NFLAGS + CONJ: // |
				return combinators.forwardApplication(leftSuperCat, rightSuperCat, results, sentence);
			/*
			 * complex left and right, both forward slash:
			 */
			case FORWARD * NFLAGS + FORWARD:
				if (combinators.forwardComposition(leftSuperCat, rightSuperCat, results, sentence)) {
					return true;
				} else {
					return combinators.forwardApplication(leftSuperCat, rightSuperCat, results, sentence);
				}
			/*
			 * complex left and right, forward slash left, backslash right:
			 */
			case FORWARD * NFLAGS + BACKWARD:
				if (combinators.backwardApplication(leftSuperCat, rightSuperCat, results, sentence)) {
					return true;
				} else if (combinators.forwardApplication(leftSuperCat, rightSuperCat, results, sentence)) {
					return true;
				} else {
					return combinators.backwardCrossComposition(leftSuperCat, rightSuperCat, results, sentence);
				}
			/*
			 * complex left and right, both backslash
			 */
			case BACKWARD * NFLAGS + BACKWARD:
				if (combinators.backwardApplication(leftSuperCat, rightSuperCat, results, sentence)) {
					return true;
				} else {
					return combinators.backwardComposition(leftSuperCat, rightSuperCat, results, sentence);
				}
			/*
			 * conj left, atomic right
			 */
			case CONJ * NFLAGS + NONE:
				boolean conjResult = combinators.coordination(leftSuperCat, rightSuperCat, results);
				boolean funnyConjResult = combinators.funnyConj(leftSuperCat, rightSuperCat, results);
				return conjResult || funnyConjResult;
			/*
			 * conj left, complex right; backwardApplication captures conj
			 * conj\conj - this was a bug in C&C since the 2nd case never gets
			 * called; fixed here by checking for conj\conj in
			 * coordinationReject
			 */
			case CONJ * NFLAGS + FORWARD: // fall through
			case CONJ * NFLAGS + BACKWARD: // |
				if (combinators.coordination(leftSuperCat, rightSuperCat, results)) {
					return true;
				} else {
					return combinators.backwardApplication(leftSuperCat, rightSuperCat, results, sentence);
				}
			/*
			 * colon or bracket left, atomic or complex right leftPunctConj case
			 * is for the semicolon (following C&C)
			 */
			case COLON * NFLAGS + NONE: // fall through
			case COLON * NFLAGS + FORWARD: // |
			case COLON * NFLAGS + BACKWARD: // |
			case BRACKET * NFLAGS + NONE: // |
			case BRACKET * NFLAGS + FORWARD: // |
			case BRACKET * NFLAGS + BACKWARD: // |
			case PERIOD * NFLAGS + NONE: // | // added these period cases since
			case PERIOD * NFLAGS + FORWARD: // | // we have more punct rules now
			case PERIOD * NFLAGS + BACKWARD: // |
				punctResult = punctRules.leftPunct(leftSuperCat, rightSuperCat, results);
				conjResult = punctRules.leftPunctConj(leftSuperCat, rightSuperCat, results);
				return punctResult || conjResult;
			/*
			 * comma left, atomic or complex right
			 */
			case COMMA * NFLAGS + NONE: // fall through
			case COMMA * NFLAGS + FORWARD: // |
			case COMMA * NFLAGS + BACKWARD: // |
				typeChangeResult = punctRules.leftCommaTypeChange(leftSuperCat, rightSuperCat, results);
				punctResult = punctRules.leftPunct(leftSuperCat, rightSuperCat, results);
				conjResult = punctRules.leftPunctConj(leftSuperCat, rightSuperCat, results);
				return typeChangeResult || punctResult || conjResult;
			default:
				return false;
		}
	}

	public void typeChange(ArrayList<SuperCategory> leftSuperCats, ArrayList<SuperCategory> results) {
		unaryRules.typeChange(leftSuperCats, results);
	}

	public void typeRaise(ArrayList<SuperCategory> leftSuperCats, ArrayList<SuperCategory> results) {
		unaryRules.typeRaise(leftSuperCats, results);
	}

	/*
	 * categories, for the purposes of deciding which rules to apply, are
	 * either: complex with forward slash complex with backward slash atomic
	 * conj atomic and one of 4 punctuation types atomic and none of the above
	 */
	private static int ruleFlags(Category cat) {
		switch (cat.atom.value()) {
			case Atom.NONE:
				if (cat.isFwd()) {
					return FORWARD;
				} else {
					return BACKWARD;
				}
			case Atom.CONJ:
				return CONJ;
			case Atom.PERIOD:
				return PERIOD;
			case Atom.COLON: // fall through
			case Atom.SEMICOLON:
				return COLON;
			case Atom.COMMA:
				return COMMA;
			case Atom.LQU: // fall through
			case Atom.RQU: // |
			case Atom.LRB: // |
			case Atom.RRB:
				return BRACKET;
			default:
				return NONE;
		}
	}
}
