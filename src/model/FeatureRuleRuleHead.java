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

public class FeatureRuleRuleHead implements Feature<FeatureRuleRuleHead> {
	final short featureType;
	final Category grandChildCat;
	final Category childCat;
	final Category resultCat;
	final int head;
	final int hashCode;

	public FeatureRuleRuleHead(short featureType, Category grandChildCat, Category childCat, Category resultCat, int head) {
		this.featureType = featureType;
		this.grandChildCat = grandChildCat;
		this.childCat = childCat;
		this.resultCat = resultCat;
		this.head = head;
		this.hashCode = genHashCode();
	}

	public static void readFeature(short featureType, String[] tokens, FeatureIDs<FeatureRuleRuleHead> featureIDs, int ID, Categories categories) {
		Category grandChildCat = categories.canonize(tokens[1]);
		Category childCat = categories.canonize(tokens[2]);
		Category resultCat = categories.canonize(tokens[3]);
		int head = Integer.valueOf(tokens[4]);
		FeatureRuleRuleHead feature = new FeatureRuleRuleHead(featureType, grandChildCat, childCat, resultCat, head);
		featureIDs.addFeature(feature, ID);
	}

	public static void collectFeatures(SuperCategory grandChildSuperCat, SuperCategory childSuperCat, SuperCategory resultSuperCat, short featureType, ArrayList<Integer> tokenIDs, FeatureIDs<FeatureRuleRuleHead> featureIDs, ArrayList<Integer> ids){
		HashMap<FeatureRuleRuleHead, Integer> featureIDsHashMap = featureIDs.getFeatureIDs();
		Variable var = resultSuperCat.vars[resultSuperCat.cat.var];
		for (int i = 0; i < var.fillers.length && var.fillers[i] != Variable.SENTINEL; i++) {
			if (var.fillers[i] == 0) {
				continue;
			}

			FeatureRuleRuleHead feature = new FeatureRuleRuleHead(featureType, grandChildSuperCat.cat, childSuperCat.cat, resultSuperCat.cat, tokenIDs.get(var.fillers[i] - 1));
			Integer id = featureIDsHashMap.get(feature);
			if (id != null) {
				ids.add(id);
			}
		}
	}

	public static void count(FeatureCounts<FeatureRuleRuleHead> featureRuleHeadCounts, Sentence sentence, short[] featureTypes, SuperCategory grandChildSuperCat, SuperCategory childSuperCat, SuperCategory resultSuperCat) {
		Variable var = resultSuperCat.vars[resultSuperCat.cat.var];
		for (int i = 0; i < var.fillers.length && var.fillers[i] != Variable.SENTINEL; i++) {
			if (var.fillers[i] == 0) {
				continue;
			}

			int head = sentence.wordIDs.get(var.fillers[i] - 1);
			int pos = sentence.postagIDs.get(var.fillers[i] - 1);

			featureRuleHeadCounts.addCount(new FeatureRuleRuleHead(featureTypes[0], grandChildSuperCat.cat, childSuperCat.cat, resultSuperCat.cat, head));
			featureRuleHeadCounts.addCount(new FeatureRuleRuleHead(featureTypes[1], grandChildSuperCat.cat, childSuperCat.cat, resultSuperCat.cat, pos));
		}
	}

	private int genHashCode() {
		Hash h = new Hash(featureType);
		h.plusEqual(grandChildCat.getEhash());
		h.plusEqual(childCat.getEhash());
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
		if ( other == null ) {
			return false;
		}

		return featureType == ((FeatureRuleRuleHead) (other)).featureType && grandChildCat.equals(((FeatureRuleRuleHead) (other)).grandChildCat) && childCat.equals(((FeatureRuleRuleHead) (other)).childCat) && resultCat.equals(((FeatureRuleRuleHead) (other)).resultCat) && head == ((FeatureRuleRuleHead) (other)).head;
	}

	@Override
	public FeatureRuleRuleHead canonize(Categories categories) {
		Category canonicalGrandChildCat = categories.canonize(grandChildCat);
		Category canonicalChildCat = categories.canonize(childCat);
		Category canonicalResultCat = categories.canonize(resultCat);
		return new FeatureRuleRuleHead(featureType, canonicalGrandChildCat, canonicalChildCat, canonicalResultCat, head);
	}

	@Override
	public void print(PrintWriter out) {
		out.print(featureType + " ");
		grandChildCat.print(out);
		out.print(" ");
		childCat.print(out);
		out.print(" ");
		resultCat.print(out);
		out.print(" " + head);
	}

	@Override
	public String toString() {
		return featureType + " " + grandChildCat.toStringNoOuterBrackets() + " " + childCat.toStringNoOuterBrackets() + " " + resultCat.toStringNoOuterBrackets() + " " + head;
	}
}
