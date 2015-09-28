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

public class FeatureRuleHeadHead implements Feature<FeatureRuleHeadHead> {
	final short featureType;
	final Category leftCat;
	final Category rightCat;
	final Category resultCat;
	final int leftHead;
	final int rightHead;
	final int hashCode;

	public FeatureRuleHeadHead(short featureType, Category leftCat, Category rightCat, Category resultCat, int leftHead, int rightHead) {
		this.featureType = featureType;
		this.leftCat = leftCat;
		this.rightCat = rightCat;
		this.resultCat = resultCat;
		this.leftHead = leftHead;
		this.rightHead = rightHead;
		this.hashCode = genHashCode();
	}

	public static void readFeature(short featureType, String[] tokens, FeatureIDs<FeatureRuleHeadHead> featureIDs, int ID, Categories categories) {
		Category leftCat = categories.canonize(tokens[1]);
		Category rightCat = categories.canonize(tokens[2]);
		Category resultCat = categories.canonize(tokens[3]);
		int leftHead = Integer.parseInt(tokens[4]);
		int rightHead = Integer.parseInt(tokens[5]);
		FeatureRuleHeadHead feature = new FeatureRuleHeadHead(featureType, leftCat, rightCat, resultCat, leftHead, rightHead);
		featureIDs.addFeature(feature, ID);
	}

	public static void collectFeatures(SuperCategory leftSuperCat, SuperCategory rightSuperCat, SuperCategory resultSuperCat, short featureType, ArrayList<Integer> leftTokenIDs, ArrayList<Integer> rightTokenIDs, FeatureIDs<FeatureRuleHeadHead> featureIDs, ArrayList<Integer> ids) {
		HashMap<FeatureRuleHeadHead, Integer> featureIDsHashMap = featureIDs.getFeatureIDs();
		Variable leftVar = leftSuperCat.vars[leftSuperCat.cat.var];
		Variable rightVar = rightSuperCat.vars[rightSuperCat.cat.var];

		for (int i = 0; i < leftVar.fillers.length && leftVar.fillers[i] != Variable.SENTINEL; i++) {
			if (leftVar.fillers[i] == 0) {
				continue;
			}

			for (int j = 0; j < rightVar.fillers.length && rightVar.fillers[j] != Variable.SENTINEL; j++) {
				if (rightVar.fillers[j] == 0) {
					continue;
				}

				FeatureRuleHeadHead feature = new FeatureRuleHeadHead(featureType, leftSuperCat.cat, rightSuperCat.cat, resultSuperCat.cat, leftTokenIDs.get(leftVar.fillers[i] - 1), rightTokenIDs.get(rightVar.fillers[j] - 1));
				Integer id = featureIDsHashMap.get(feature);
				if (id != null) {
					ids.add(id);
				}
			}
		}
	}

	public static void count(FeatureCounts<FeatureRuleHeadHead> featureRuleHeadHeadCounts, Sentence sentence, short[] featureTypes, SuperCategory leftSuperCat, SuperCategory rightSuperCat, SuperCategory resultSuperCat) {
		Variable leftVar = leftSuperCat.vars[leftSuperCat.cat.var];
		Variable rightVar = rightSuperCat.vars[rightSuperCat.cat.var];

		for (int i = 0; i < leftVar.fillers.length && leftVar.fillers[i] != Variable.SENTINEL; i++) {
			if (leftVar.fillers[i] == 0) {
				continue;
			}

			for (int j = 0; j < rightVar.fillers.length && rightVar.fillers[j] != Variable.SENTINEL; j++) {
				if (rightVar.fillers[j] == 0) {
					continue;
				}

				int leftHead = sentence.wordIDs.get(leftVar.fillers[i] - 1);
				int rightHead = sentence.wordIDs.get(rightVar.fillers[j] - 1);
				int leftPos = sentence.postagIDs.get(leftVar.fillers[i] - 1);
				int rightPos = sentence.postagIDs.get(rightVar.fillers[j] - 1);

				// rule + argument heads:
				featureRuleHeadHeadCounts.addCount(new FeatureRuleHeadHead(featureTypes[0], leftSuperCat.cat, rightSuperCat.cat, resultSuperCat.cat, leftHead, rightHead));
				featureRuleHeadHeadCounts.addCount(new FeatureRuleHeadHead(featureTypes[1], leftSuperCat.cat, rightSuperCat.cat, resultSuperCat.cat, leftHead, rightPos));
				featureRuleHeadHeadCounts.addCount(new FeatureRuleHeadHead(featureTypes[2], leftSuperCat.cat, rightSuperCat.cat, resultSuperCat.cat, leftPos, rightHead));
				featureRuleHeadHeadCounts.addCount(new FeatureRuleHeadHead(featureTypes[3], leftSuperCat.cat, rightSuperCat.cat, resultSuperCat.cat, leftPos, rightPos));
			}
		}
	}

	private int genHashCode() {
		Hash h = new Hash(featureType);
		h.plusEqual(leftCat.getEhash());
		h.plusEqual(rightCat.getEhash());
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
		if ( other == null || getClass() != other.getClass() ) {
			return false;
		}

		FeatureRuleHeadHead cother = (FeatureRuleHeadHead) other;
		return featureType == cother.featureType && leftCat.equals(cother.leftCat) && rightCat.equals(cother.rightCat) && resultCat.equals(cother.resultCat) && leftHead == cother.leftHead && rightHead == cother.rightHead;
	}

	@Override
	public FeatureRuleHeadHead canonize(Categories categories) {
		Category canonicalLeftCat = categories.canonize(leftCat);
		Category canonicalRightCat = categories.canonize(rightCat);
		Category canonicalResultCat = categories.canonize(resultCat);
		return new FeatureRuleHeadHead(featureType, canonicalLeftCat, canonicalRightCat, canonicalResultCat, leftHead, rightHead);
	}

	@Override
	public void print(PrintWriter out) {
		out.print(featureType + " ");
		leftCat.print(out);
		out.print(" ");
		rightCat.print(out);
		out.print(" ");
		resultCat.print(out);
		out.print(" " + leftHead);
		out.print(" " + rightHead);
	}

	@Override
	public String toString() {
		return featureType + " " + leftCat.toStringNoOuterBrackets() + " " + rightCat.toStringNoOuterBrackets() + " " + resultCat.toStringNoOuterBrackets() + " " + leftHead + " " + rightHead;
	}
}
