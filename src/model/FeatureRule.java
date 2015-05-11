package model;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import lexicon.Categories;
import lexicon.Category;
import utils.Hash;
import cat_combination.SuperCategory;

public class FeatureRule implements Feature<FeatureRule> {
	final short featureType;
	final Category leftCat;
	final Category rightCat;
	final Category resultCat;
	final int hashCode;

	public FeatureRule(short featureType, Category leftCat, Category rightCat, Category resultCat) {
		this.featureType = featureType;
		this.leftCat = leftCat;
		this.rightCat = rightCat;
		this.resultCat = resultCat;
		this.hashCode = genHashCode();
	}

	public static void readFeature(short featureType, String[] tokens, FeatureIDs<FeatureRule> featureIDs, int ID, Categories categories) {
		Category leftCat = categories.canonize(tokens[1]);
		Category rightCat = categories.canonize(tokens[2]);
		Category resultCat = categories.canonize(tokens[3]);
		FeatureRule feature = new FeatureRule(featureType, leftCat, rightCat, resultCat);
		featureIDs.addFeature(feature, ID);
	}

	public static void collectFeatures(SuperCategory leftSuperCat, SuperCategory rightSuperCat, SuperCategory resultSuperCat, short featureType, FeatureIDs<FeatureRule> featureIDs, ArrayList<Integer> ids) {
		HashMap<FeatureRule, Integer> featureIDsHashMap = featureIDs.getFeatureIDs();
		FeatureRule feature = new FeatureRule(featureType, leftSuperCat.cat, rightSuperCat.cat, resultSuperCat.cat);
		Integer id = featureIDsHashMap.get(feature);
		if (id != null) {
			ids.add(id);
		}
	}

	public static void count(FeatureCounts<FeatureRule> featureRuleCounts, short featureType, SuperCategory leftSuperCat, SuperCategory rightSuperCat, SuperCategory resultSuperCat) {
		featureRuleCounts.addCount(new FeatureRule(featureType, leftSuperCat.cat, rightSuperCat.cat, resultSuperCat.cat));
	}

	private int genHashCode() {
		Hash h = new Hash(featureType);
		h.plusEqual(leftCat.getEhash());
		h.plusEqual(rightCat.getEhash());
		h.plusEqual(resultCat.getEhash());
		return (int) (h.value());
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object other) {
		return featureType == ((FeatureRule) (other)).featureType && leftCat.equals(((FeatureRule) (other)).leftCat) && rightCat.equals(((FeatureRule) (other)).rightCat) && resultCat.equals(((FeatureRule) (other)).resultCat);
	}

	@Override
	public FeatureRule canonize(Categories categories) {
		Category canonicalLeftCat = categories.canonize(leftCat);
		Category canonicalRightCat = categories.canonize(rightCat);
		Category canonicalResultCat = categories.canonize(resultCat);
		return new FeatureRule(featureType, canonicalLeftCat, canonicalRightCat, canonicalResultCat);
	}

	@Override
	public void print(PrintWriter out) {
		out.print(featureType + " ");
		leftCat.print(out);
		out.print(" ");
		rightCat.print(out);
		out.print(" ");
		resultCat.print(out);
	}

	@Override
	public String toString() {
		return featureType + " " + leftCat.toStringNoOuterBrackets() + " " + rightCat.toStringNoOuterBrackets() + " " + resultCat.toStringNoOuterBrackets();
	}
}
