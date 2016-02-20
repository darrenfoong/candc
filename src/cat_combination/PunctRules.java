package cat_combination;

import java.io.IOException;
import java.util.ArrayList;

import lexicon.Categories;
import lexicon.Category;
import lexicon.VarID;

public class PunctRules {
	RuleInstancesParams ruleInstancesParams;
	// settings determine which seen_rule instances are used
	RuleInstances rightPunctRuleInstances;
	RuleInstances leftPunctRuleInstances;
	RuleInstances leftPunctConjRuleInstances;

	/*
	 * needs categories in order to have access to pre-built type-changed
	 * categories for the commaTypeChange rules, and the canonical categories
	 * for rule instances
	 */
	Categories categories;

	/*
	 * get the references for these initially so that the hashMap in categories
	 * isn't being continually queried when applying the typeChange rules:
	 */
	private final Category SbS;
	private final Category SfS;
	private final Category SbNPbSbNP;
	private final Category SbNPfSbNP;
	private final Category NPbNP;

	public PunctRules(Categories categories,
			RuleInstancesParams ruleInstancesParams) throws IOException {
		this.ruleInstancesParams = ruleInstancesParams;

		if (ruleInstancesParams.getRightPunct()) {
			rightPunctRuleInstances = new RuleInstances(
					ruleInstancesParams.getDirectory()
					+ "/right_punct_rule_instances", categories);
		}
		if (ruleInstancesParams.getLeftPunct()) {
			leftPunctRuleInstances = new RuleInstances(
					ruleInstancesParams.getDirectory()
					+ "/left_punct_rule_instances", categories);
		}
		if (ruleInstancesParams.getLeftPunctConj()) {
			leftPunctConjRuleInstances = new RuleInstances(
					ruleInstancesParams.getDirectory()
					+ "/left_punct_conj_rule_instances", categories);
		}

		this.categories = categories;

		SbS = categories.getCategory("S\\S");
		SfS = categories.getCategory("S/S");
		SbNPbSbNP = categories.getCategory("(S\\NP)\\(S\\NP)");
		SbNPfSbNP = categories.getCategory("(S\\NP)/(S\\NP)");
		NPbNP = categories.getCategory("NP\\NP");
	}

	/*
	 * rightPunct and leftPunct the same - do some refactoring here
	 */
	public boolean rightPunct(SuperCategory leftSuperCat,
			SuperCategory rightSuperCat, ArrayList<SuperCategory> results) {
		if (!rightPunctAccept(leftSuperCat, rightSuperCat)) {
			return false;
		}
		/*
		 * we want to keep the CONJ flag if there is one on the left category;
		 * this would apply to cases like "and chips ," where the comma gets
		 * swallowed by the NP\NP[conj]
		 */
		short INHERITED_FLAGS = (short) (leftSuperCat.flags & SuperCategory.CONJ);
		SuperCategory resultSuperCat = SuperCategory.Punct(leftSuperCat.cat,
				(short) (SuperCategory.RIGHT_PUNCT | INHERITED_FLAGS),
				leftSuperCat, rightSuperCat, leftSuperCat);
		Rules.rightPunctCount++;

		results.add(resultSuperCat);
		return true;
	}

	public boolean leftPunct(SuperCategory leftSuperCat,
			SuperCategory rightSuperCat, ArrayList<SuperCategory> results) {
		if (!leftPunctAccept(leftSuperCat, rightSuperCat)) {
			return false;
		}
		/*
		 * we want to keep the CONJ flag if there is one on the right category;
		 * this would apply to cases like ", and chips" where the comma gets
		 * swallowed by the NP\NP[conj] (note this is different to a commaConj
		 * case)
		 */
		short INHERITED_FLAGS = (short) (rightSuperCat.flags & SuperCategory.CONJ);
		SuperCategory resultSuperCat = SuperCategory.Punct(rightSuperCat.cat,
				(short) (SuperCategory.LEFT_PUNCT | INHERITED_FLAGS),
				leftSuperCat, rightSuperCat, rightSuperCat);
		Rules.leftPunctCount++;
		results.add(resultSuperCat);
		return true;
	}

	public boolean leftPunctConj(SuperCategory leftSuperCat,
			SuperCategory rightSuperCat, ArrayList<SuperCategory> results) {
		if (!leftPunctConjAccept(leftSuperCat, rightSuperCat)) {
			return false;
		}
		/*
		 * aside from the flag on the SuperCategory, this is the same as the
		 * coordination rule in Combinators
		 */
		Category cat = new Category(rightSuperCat.cat, Category.BWD_SLASH,
				rightSuperCat.cat, VarID.NONE, (short) (0), (short) (0));
		Rules.leftPunctConjCount++;
		results.add(SuperCategory.Coordination(cat,
				(short) (SuperCategory.LEFT_PUNCT | SuperCategory.CONJ),
				leftSuperCat, rightSuperCat));
		return true;
	}

	private boolean rightPunctAccept(SuperCategory leftSuperCat,
			SuperCategory rightSuperCat) {
		/*
		 * leftSuperCat.cat.print(); System.out.print(" ");
		 * rightSuperCat.cat.print(); System.out.print(" "); if
		 * (rightPunctRuleInstances.contains(leftSuperCat.cat,
		 * rightSuperCat.cat)) System.out.println("found it"); else
		 * System.out.println("MISSING!");
		 */
		if (ruleInstancesParams.getRightPunct()) {
			return rightSuperCat.cat.isPunct()
					&& !leftSuperCat.coordinatedOrTypeRaised()
					&& rightPunctRuleInstances.contains(leftSuperCat.cat,
							rightSuperCat.cat);
		} else {
			return rightSuperCat.cat.isPunct()
					&& !leftSuperCat.coordinatedOrTypeRaised();
		}
	}

	private boolean leftPunctAccept(SuperCategory leftSuperCat,
			SuperCategory rightSuperCat) {
		/*
		 * leftSuperCat.cat.print(); System.out.print(" ");
		 * rightSuperCat.cat.print(); System.out.print(" "); if
		 * (leftPunctRuleInstances.contains(leftSuperCat.cat,
		 * rightSuperCat.cat)) System.out.println("found it"); else
		 * System.out.println("MISSING!");
		 */
		if (ruleInstancesParams.getLeftPunct()) {
			return leftSuperCat.cat.isPunct()
					&& !rightSuperCat.coordinatedOrTypeRaised()
					&& leftPunctRuleInstances.contains(leftSuperCat.cat,
							rightSuperCat.cat);
		} else {
			return leftSuperCat.cat.isPunct()
					&& !rightSuperCat.coordinatedOrTypeRaised();
		}
	}

	private boolean leftPunctConjAccept(SuperCategory left, SuperCategory right) {
		/*
		 * if (left.cat.isComma() || left.cat.isSemicolon()) { if
		 * (leftPunctConjRuleInstances.contains(left.cat, right.cat)) {
		 * System.out.print("LEFT PUNCT CONJ SUCCESS: "); left.cat.print();
		 * System.out.print(" "); right.cat.print(); System.out.println(); }
		 * else { System.out.print("LEFT PUNCT CONJ FAILURE: ");
		 * left.cat.print(); System.out.print(" "); right.cat.print();
		 * System.out.println(); } }
		 */
		if (ruleInstancesParams.getLeftPunctConj()) {
			return (left.cat.isComma() || left.cat.isSemicolon())
					&& leftPunctConjRuleInstances.contains(left.cat, right.cat);
		} else {
			return (left.cat.isComma() || left.cat.isSemicolon())
					&& !right.coordinatedOrTypeRaised() && !right.cat.isPunct();
		}

	}

	public boolean rightCommaTypeChange(SuperCategory leftSuperCat,
			SuperCategory rightSuperCat, ArrayList<SuperCategory> results) {
		if (!rightSuperCat.cat.isComma()) {
			return false;
		}

		Category cat = leftSuperCat.cat;
		short flags = SuperCategory.RIGHT_TC;
		if (cat.isNP()) { // 55: NP , -> S/S
			addSuperCat(SfS, leftSuperCat, rightSuperCat, leftSuperCat,
					(short) (55), flags, false, results);
			Rules.rightCommaTCCount++;
			return true;
		} else if (cat.isSdclfSdcl()) {
			// S[dcl]/S[dcl] , ->
			addSuperCat(SfS, leftSuperCat, rightSuperCat, leftSuperCat,
					(short) (50), flags, true, results);
			// 50: S/S
			addSuperCat(SbNPfSbNP, leftSuperCat, rightSuperCat, leftSuperCat,
					(short) (51), flags, true, results);
			// 51: (S\NP)/(S\NP)
			addSuperCat(SbNPbSbNP, leftSuperCat, rightSuperCat, leftSuperCat,
					(short) (52), flags, true, results);
			// 52: (S\NP)\(S\NP)
			addSuperCat(SbS, leftSuperCat, rightSuperCat, leftSuperCat,
					(short) (53), flags, true, results);
			// 53: S\S
			Rules.rightCommaTCCount++;
			return true;
		} else if (cat.isSdclbSdcl() && !leftSuperCat.coordinated()) {
			// 54:
			// S[dcl]\S[dcl]
			// , ->
			// S/S
			addSuperCat(SfS, leftSuperCat, rightSuperCat, leftSuperCat,
					(short) (54), flags, true, results);
			Rules.rightCommaTCCount++;
			return true;
		} else {
			return false;
		}
	}

	public boolean leftCommaTypeChange(SuperCategory leftSuperCat,
			SuperCategory rightSuperCat, ArrayList<SuperCategory> results) {
		if (!leftSuperCat.cat.isComma()) {
			return false;
		}

		Category cat = rightSuperCat.cat;
		short flags = SuperCategory.LEFT_TC;
		if (cat.isNP()) {
			// 56: , NP -> (S\NP)\(S\NP)
			addSuperCat(SbNPbSbNP, leftSuperCat, rightSuperCat, rightSuperCat,
					(short) (56), flags, false, results);
			Rules.leftCommaTCCount++;
			return true;
		} else {
			return false;
		}
	}

	private void addSuperCat(Category resultCat, SuperCategory leftSuperCat,
			SuperCategory rightSuperCat, SuperCategory headSuperCat,
			short ruleID, short flags, boolean replace,
			ArrayList<SuperCategory> results) {
		SuperCategory resultSuperCat = SuperCategory.TypeChanging(resultCat,
				flags, leftSuperCat, rightSuperCat, headSuperCat, replace,
				ruleID);
		results.add(resultSuperCat);
	}
}
