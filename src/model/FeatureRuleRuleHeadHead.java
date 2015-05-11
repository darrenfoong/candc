package model;

import io.Sentence;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import lexicon.Categories;
import lexicon.Category;
import utils.Hash;
import cat_combination.SuperCategory;
import cat_combination.Variable;

public class FeatureRuleRuleHeadHead implements Feature<FeatureRuleRuleHeadHead> {
	final short featureType;
	final Category leftGrandChildCat;
	final Category rightGrandChildCat;
	final Category childCat;
	final Category resultCat;
	final int leftHead;
	final int rightHead;
	final int hashCode;

	public FeatureRuleRuleHeadHead(short featureType, Category leftGrandChildCat, Category rightGrandChildCat, Category childCat, Category resultCat, int leftHead, int rightHead) {
		this.featureType = featureType;
		this.leftGrandChildCat = leftGrandChildCat;
		this.rightGrandChildCat = rightGrandChildCat;
		this.childCat = childCat;
		this.resultCat = resultCat;
		this.leftHead = leftHead;
		this.rightHead = rightHead;
		this.hashCode = genHashCode();
	}

	public static void readFeature(short featureType, String[] tokens, FeatureIDs<FeatureRuleRuleHeadHead> featureIDs, int ID, Categories categories) {
		Category leftGrandChildCat = categories.canonize(tokens[1]);
		Category rightGrandChildCat = categories.canonize(tokens[2]);
		Category childCat = categories.canonize(tokens[3]);
		Category resultCat = categories.canonize(tokens[4]);
		int leftHead = Integer.valueOf(tokens[5]);
		int rightHead = Integer.valueOf(tokens[6]);
		FeatureRuleRuleHeadHead feature = new FeatureRuleRuleHeadHead(featureType, leftGrandChildCat, rightGrandChildCat, childCat, resultCat, leftHead, rightHead);
		featureIDs.addFeature(feature, ID);
	}

	public static void collectFeatures(SuperCategory leftGrandChildSuperCat, SuperCategory rightGrandChildSuperCat, SuperCategory childSuperCat, SuperCategory resultSuperCat, short featureType, ArrayList<Integer> leftTokenIDs, ArrayList<Integer> rightTokenIDs, FeatureIDs<FeatureRuleRuleHeadHead> featureIDs, ArrayList<Integer> ids) {
		HashMap<FeatureRuleRuleHeadHead, Integer> featureIDsHashMap = featureIDs.getFeatureIDs();
		Variable leftGrandChildVar = leftGrandChildSuperCat.vars[leftGrandChildSuperCat.cat.var];
		Variable rightGrandChildVar = rightGrandChildSuperCat.vars[rightGrandChildSuperCat.cat.var];

		for (int i = 0; i < leftGrandChildVar.fillers.length && leftGrandChildVar.fillers[i] != Variable.SENTINEL; i++) {
			if (leftGrandChildVar.fillers[i] == 0) {
				continue;
			}

			for (int j = 0; j < rightGrandChildVar.fillers.length && rightGrandChildVar.fillers[j] != Variable.SENTINEL; j++) {
				if (rightGrandChildVar.fillers[j] == 0) {
					continue;
				}

				FeatureRuleRuleHeadHead feature = new FeatureRuleRuleHeadHead(featureType, leftGrandChildSuperCat.cat, rightGrandChildSuperCat.cat, childSuperCat.cat, resultSuperCat.cat, leftTokenIDs.get(leftGrandChildVar.fillers[i] - 1), rightTokenIDs.get(rightGrandChildVar.fillers[j] - 1));
				Integer id = featureIDsHashMap.get(feature);
				if (id != null) {
					ids.add(id);
				}
			}
		}
	}

	public static void count(FeatureCounts<FeatureRuleRuleHeadHead> featureRuleHeadHeadCounts, Sentence sentence, short[] featureTypes, SuperCategory leftGrandChildSuperCat, SuperCategory rightGrandChildSuperCat, SuperCategory childSuperCat, SuperCategory resultSuperCat) {
		Variable leftGrandChildVar = leftGrandChildSuperCat.vars[leftGrandChildSuperCat.cat.var];
		Variable rightGrandChildVar = rightGrandChildSuperCat.vars[rightGrandChildSuperCat.cat.var];

		for (int i = 0; i < leftGrandChildVar.fillers.length && leftGrandChildVar.fillers[i] != Variable.SENTINEL; i++) {
			if (leftGrandChildVar.fillers[i] == 0) {
				continue;
			}

			for (int j = 0; j < rightGrandChildVar.fillers.length && rightGrandChildVar.fillers[j] != Variable.SENTINEL; j++) {
				if (rightGrandChildVar.fillers[j] == 0) {
					continue;
				}

				int leftHead = sentence.wordIDs.get(leftGrandChildVar.fillers[i] - 1);
				int rightHead = sentence.wordIDs.get(rightGrandChildVar.fillers[j] - 1);
				int leftPos = sentence.postagIDs.get(leftGrandChildVar.fillers[i] - 1);
				int rightPos = sentence.postagIDs.get(rightGrandChildVar.fillers[j] - 1);

				// rule + argument heads:
				featureRuleHeadHeadCounts.addCount(new FeatureRuleRuleHeadHead(featureTypes[0], leftGrandChildSuperCat.cat, rightGrandChildSuperCat.cat, childSuperCat.cat, resultSuperCat.cat, leftHead, rightHead));
				featureRuleHeadHeadCounts.addCount(new FeatureRuleRuleHeadHead(featureTypes[1], leftGrandChildSuperCat.cat, rightGrandChildSuperCat.cat, childSuperCat.cat, resultSuperCat.cat, leftHead, rightPos));
				featureRuleHeadHeadCounts.addCount(new FeatureRuleRuleHeadHead(featureTypes[2], leftGrandChildSuperCat.cat, rightGrandChildSuperCat.cat, childSuperCat.cat, resultSuperCat.cat, leftPos, rightHead));
				featureRuleHeadHeadCounts.addCount(new FeatureRuleRuleHeadHead(featureTypes[3], leftGrandChildSuperCat.cat, rightGrandChildSuperCat.cat, childSuperCat.cat, resultSuperCat.cat, leftPos, rightPos));
			}
		}
	}

	private int genHashCode(){
		Hash h = new Hash(featureType);
		h.plusEqual(leftGrandChildCat.getEhash());
		h.plusEqual(rightGrandChildCat.getEhash());
		h.plusEqual(childCat.getEhash());
		h.plusEqual(resultCat.getEhash());
		h.plusEqual(leftHead);
		h.plusEqual(rightHead);
		return (int) (h.value());
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object other) {
		return featureType == ((FeatureRuleRuleHeadHead) (other)).featureType && leftGrandChildCat.equals(((FeatureRuleRuleHeadHead) (other)).leftGrandChildCat) && rightGrandChildCat.equals(((FeatureRuleRuleHeadHead) (other)).rightGrandChildCat) && childCat.equals(((FeatureRuleRuleHeadHead) (other)).childCat) && resultCat.equals(((FeatureRuleRuleHeadHead) (other)).resultCat) && leftHead == ((FeatureRuleRuleHeadHead) (other)).leftHead && rightHead == ((FeatureRuleRuleHeadHead) (other)).rightHead;
	}

	@Override
	public FeatureRuleRuleHeadHead canonize(Categories categories) {
		Category canonicalLeftGrandChildCat = categories.canonize(leftGrandChildCat);
		Category canonicalRightGrandChildCat = categories.canonize(rightGrandChildCat);
		Category canonicalChildCat = categories.canonize(childCat);
		Category canonicalResultCat = categories.canonize(resultCat);
		return new FeatureRuleRuleHeadHead(featureType, canonicalLeftGrandChildCat, canonicalRightGrandChildCat, canonicalChildCat, canonicalResultCat, leftHead, rightHead);
	}

	@Override
	public void print(PrintWriter out) {
		out.print(featureType + " ");
		leftGrandChildCat.print(out);
		out.print(" ");
		rightGrandChildCat.print(out);
		out.print(" ");
		childCat.print(out);
		out.print(" ");
		resultCat.print(out);
		out.print(" " + leftHead);
		out.print(" " + rightHead);
	}

	@Override
	public String toString() {
		return featureType + " " + leftGrandChildCat.toStringNoOuterBrackets() + " " + rightGrandChildCat.toStringNoOuterBrackets() + " " + childCat.toStringNoOuterBrackets() + " " + resultCat.toStringNoOuterBrackets() + " " + leftHead + " " + rightHead;
	}
}
