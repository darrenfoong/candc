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

public class FeatureRuleHead implements Feature<FeatureRuleHead> {
	final short featureType;
	final Category leftCat;
	final Category rightCat;
	final Category resultCat;
	final int head;
	final int hashCode;

	public FeatureRuleHead(short featureType, Category leftCat, Category rightCat, Category resultCat, int head) {
		this.featureType = featureType;
		this.leftCat = leftCat;
		this.rightCat = rightCat;
		this.resultCat = resultCat;
		this.head = head;
		this.hashCode = genHashCode();
	}

	public static void readFeature(short featureType, String[] tokens, FeatureIDs<FeatureRuleHead> featureIDs, int ID, Categories categories) {
		Category leftCat = categories.canonize(tokens[1]);
		Category rightCat = categories.canonize(tokens[2]);
		Category resultCat = categories.canonize(tokens[3]);
		int head = Integer.valueOf(tokens[4]);
		FeatureRuleHead feature = new FeatureRuleHead(featureType, leftCat, rightCat, resultCat, head);
		featureIDs.addFeature(feature, ID);
	}

	public static void collectFeatures(SuperCategory leftSuperCat, SuperCategory rightSuperCat, SuperCategory resultSuperCat, short featureType, ArrayList<Integer> tokenIDs, FeatureIDs<FeatureRuleHead> featureIDs, ArrayList<Integer> ids){
		HashMap<FeatureRuleHead, Integer> featureIDsHashMap = featureIDs.getFeatureIDs();
		Variable var = resultSuperCat.vars[resultSuperCat.cat.var];
		for (int i = 0; i < var.fillers.length && var.fillers[i] != Variable.SENTINEL; i++) {
			if (var.fillers[i] == 0) {
				continue;
			}

			FeatureRuleHead feature = new FeatureRuleHead(featureType, leftSuperCat.cat, rightSuperCat.cat, resultSuperCat.cat, tokenIDs.get(var.fillers[i] - 1));
			Integer id = featureIDsHashMap.get(feature);
			if (id != null) {
				ids.add(id);
			}
		}
	}

	public static void count(FeatureCounts<FeatureRuleHead> featureRuleHeadCounts, Sentence sentence, short[] featureTypes, SuperCategory leftSuperCat, SuperCategory rightSuperCat, SuperCategory resultSuperCat) {
		Variable var = resultSuperCat.vars[resultSuperCat.cat.var];
		for (int i = 0; i < var.fillers.length && var.fillers[i] != Variable.SENTINEL; i++) {
			if (var.fillers[i] == 0) {
				continue;
			}

			int head = sentence.wordIDs.get(var.fillers[i] - 1);
			int pos = sentence.postagIDs.get(var.fillers[i] - 1);

			featureRuleHeadCounts.addCount(new FeatureRuleHead(featureTypes[0], leftSuperCat.cat, rightSuperCat.cat, resultSuperCat.cat, head));
			featureRuleHeadCounts.addCount(new FeatureRuleHead(featureTypes[1], leftSuperCat.cat, rightSuperCat.cat, resultSuperCat.cat, pos));
		}
	}

	private int genHashCode() {
		Hash h = new Hash(featureType);
		h.plusEqual(leftCat.getEhash());
		h.plusEqual(rightCat.getEhash());
		h.plusEqual(resultCat.getEhash());
		h.plusEqual(head);
		return (int) (h.value());
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object other) {
		return featureType == ((FeatureRuleHead) (other)).featureType && leftCat.equals(((FeatureRuleHead) (other)).leftCat) && rightCat.equals(((FeatureRuleHead) (other)).rightCat) && resultCat.equals(((FeatureRuleHead) (other)).resultCat) && head == ((FeatureRuleHead) (other)).head;
	}

	@Override
	public FeatureRuleHead canonize(Categories categories) {
		Category canonicalLeftCat = categories.canonize(leftCat);
		Category canonicalRightCat = categories.canonize(rightCat);
		Category canonicalResultCat = categories.canonize(resultCat);
		return new FeatureRuleHead(featureType, canonicalLeftCat, canonicalRightCat, canonicalResultCat, head);
	}

	@Override
	public void print(PrintWriter out) {
		out.print(featureType + " ");
		leftCat.print(out);
		out.print(" ");
		rightCat.print(out);
		out.print(" ");
		resultCat.print(out);
		out.print(" " + head);
	}

	@Override
	public String toString() {
		return featureType + " " + leftCat.toStringNoOuterBrackets() + " " + rightCat.toStringNoOuterBrackets() + " " + resultCat.toStringNoOuterBrackets() + " " + head;
	}
}
