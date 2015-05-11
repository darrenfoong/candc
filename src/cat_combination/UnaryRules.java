package cat_combination;

import java.util.ArrayList;

import lexicon.Categories;
import lexicon.Category;
import lexicon.GrammaticalFeature;
import lexicon.TypeRaisedCategory;

public class UnaryRules {
	/*
	 * needs a reference to a Categories object in order to have access to
	 * pre-built type-raised and type-changed categories
	 */
	Categories categories;

	/*
	 * get the references for these initially so that the hashMap in categories
	 * isn't being continually queried when applying the typeChange rules
	 */
	private final Category NP;
	private final Category NPfNPbNP;
	private final Category NPbNP;
	private final Category NbN;
	private final Category SbS;
	private final Category SfS;
	private final Category SbNPbSbNP;
	private final Category SbNPfSbNP;

	public UnaryRules(Categories categories) {
		this.categories = categories;

		NP = categories.getCategory("NP");
		NPfNPbNP = categories.getCategory("NP/(NP\\NP)");
		NPbNP = categories.getCategory("NP\\NP");
		NbN = categories.getCategory("N\\N");
		SbS = categories.getCategory("S\\S");
		SfS = categories.getCategory("S/S");
		SbNPbSbNP = categories.getCategory("(S\\NP)\\(S\\NP)");
		SbNPfSbNP = categories.getCategory("(S\\NP)/(S\\NP)");
	}

	public void typeRaise(ArrayList<SuperCategory> superCats,
			ArrayList<SuperCategory> results) {
		for (SuperCategory superCat : superCats) {
			Category cat = superCat.cat;
			ArrayList<TypeRaisedCategory> trCats = null;

			if (cat.isNP()) {
				trCats = categories.trNPs;
			} else if (cat.isAP()) {
				trCats = categories.trAPs;
			} else if (cat.isPP()) {
				trCats = categories.trPPs;
			} else if (cat.isStobNP()) {
				trCats = categories.trVP_TOs;
			}

			if (trCats != null) {
				for (TypeRaisedCategory trCat : trCats) {
					SuperCategory resultSuperCat = SuperCategory.TypeRaising(
							trCat, SuperCategory.TR, superCat);
					results.add(resultSuperCat);
				}
			}
		}
	}

	/*
	 * using the same RuleIDs from C&C
	 */
	public void typeChange(ArrayList<SuperCategory> superCats,
			ArrayList<SuperCategory> results) {
		for (SuperCategory superCat : superCats) {
			Category cat = superCat.cat;

			if (cat.isN()) {
				addSuperCat(NP, superCat, (short) (1), false, results);
			} else if (cat.isNP()) {
				addSuperCat(NPfNPbNP, superCat, (short) (11), false, results);
			} else if (cat.isSdclfNP()) {
				addSuperCat(NPbNP, superCat, (short) (10), true, results);
			} else if (cat.isStobNPfNP()) {
				addSuperCat(NPbNP, superCat, (short) (19), true, results);
			} else if (cat.isSdcl()) { // 21: S[dcl] -> NP\NP; 22: S[dcl] -> S\S
				addSuperCat(NPbNP, superCat, (short) (21), false, results);
				addSuperCat(SbS, superCat, (short) (22), false, results);
			} else if (cat.isSbNP()) {
				switch (cat.result.feature.value()) {
				case GrammaticalFeature.DCL: // 12: S[dcl]\NP -> NP\NP
					addSuperCat(NPbNP, superCat, (short) (12), true, results);
					break;
				case GrammaticalFeature.PSS: // S[pss]\NP ->
					addSuperCat(NPbNP, superCat, (short) (2), true, results); // 2:
					// NP\NP
					addSuperCat(SbNPbSbNP, superCat, (short) (17), false,
							results); // 17: (S\NP)\(S\NP)
					addSuperCat(SfS, superCat, (short) (13), false, results); // 13:
					// S/S
					break;
				case GrammaticalFeature.NG: // S[ng]\NP ->
					addSuperCat(NPbNP, superCat, (short) (3), true, results); // 3:
					// NP\NP
					addSuperCat(SbNPbSbNP, superCat, (short) (4), true, results); // 4:
					// (S\NP)\(S\NP)
					addSuperCat(SfS, superCat, (short) (5), false, results); // 5:
					// S/S
					addSuperCat(SbNPfSbNP, superCat, (short) (18), false,
							results); // 18: (S\NP)/(S\NP)
					addSuperCat(SbS, superCat, (short) (16), false, results); // 16:
					// S\S
					addSuperCat(NP, superCat, (short) (20), false, results); // 20:
					// NP
					break;
				case GrammaticalFeature.ADJ: // S[adj]\NP ->
					addSuperCat(NPbNP, superCat, (short) (6), true, results); // 6:
					// NP\NP
					addSuperCat(SbNPbSbNP, superCat, (short) (93), true,
							results); // 93: (S\NP)\(S\NP)
					addSuperCat(SfS, superCat, (short) (15), false, results); // 15:
					// S/S
					break;
				case GrammaticalFeature.TO: // S[to]\NP ->
					addSuperCat(NPbNP, superCat, (short) (7), true, results); // 7:
					// NP\NP
					addSuperCat(SbNPbSbNP, superCat, (short) (8), true, results); // 8:
					// (S\NP)\(S\NP)
					addSuperCat(NbN, superCat, (short) (9), true, results); // 9:
					// N\N
					addSuperCat(SfS, superCat, (short) (14), false, results); // 14:
					// S/S
				default:
					continue;
				}
			} else {
				continue;
			}
		}
	}

	private void addSuperCat(Category resultCat, SuperCategory leftSuperCat,
			short ruleID, boolean replace, ArrayList<SuperCategory> results) {
		// null argument is for the rightSuperCat
		SuperCategory resultSuperCat = SuperCategory.TypeChanging(resultCat,
				SuperCategory.UNARY_TC, leftSuperCat, null, leftSuperCat,
				replace, ruleID);
		results.add(resultSuperCat);
	}
}
