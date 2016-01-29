package model;

import io.Sentence;

import java.util.ArrayList;
import java.util.HashMap;

import lexicon.Categories;
import lexicon.Category;
import utils.Hash;
import cat_combination.SuperCategory;
import cat_combination.Variable;

public class FeatureCatHead implements Feature<FeatureCatHead> {
	final short featureType;
	final Category cat;
	final int head;
	final int hashCode;

	public FeatureCatHead(short featureType, Category cat, int head) {
		this.featureType = featureType;
		this.cat = cat;
		this.head = head;
		this.hashCode = genHashCode();
	}

	public static void readFeature(short featureType, String[] tokens, FeatureIDs<FeatureCatHead> featureIDs, int ID, Categories categories) {
		Category cat = categories.canonize(tokens[1]);
		int head = Integer.parseInt(tokens[2]);
		FeatureCatHead feature = new FeatureCatHead(featureType, cat, head);
		featureIDs.addFeature(feature, ID);
	}

	public static void collectFeatures(SuperCategory superCat, short featureType, ArrayList<Integer> tokenIDs, FeatureIDs<FeatureCatHead> featureIDs, ArrayList<Integer> ids) {
		HashMap<FeatureCatHead, Integer> featureIDsHashMap = featureIDs.getFeatureIDs();
		Variable var = superCat.vars[superCat.cat.var];
		for (int i = 0; i < var.fillers.length && var.fillers[i] != Variable.SENTINEL; i++) {
			if (var.fillers[i] == 0) {
				continue;
			}

			FeatureCatHead feature = new FeatureCatHead(featureType, superCat.cat, tokenIDs.get(var.fillers[i] - 1));
			Integer id = featureIDsHashMap.get(feature);
			if (id != null) {
				ids.add(id);
			}
		}
	}

	public static void count(FeatureCounts<FeatureCatHead> featureCatHeadCounts, Sentence sentence, short[] featureTypes, SuperCategory superCat) {
		Variable var = superCat.vars[superCat.cat.var];
		for (int i = 0; i < var.fillers.length && var.fillers[i] != Variable.SENTINEL; i++) {
			if (var.fillers[i] == 0) {
				continue;
			}

			int head = sentence.wordIDs.get(var.fillers[i] - 1);
			int pos = sentence.postagIDs.get(var.fillers[i] - 1);

			featureCatHeadCounts.addCount(new FeatureCatHead(featureTypes[0], superCat.cat, head));
			featureCatHeadCounts.addCount(new FeatureCatHead(featureTypes[1], superCat.cat, pos));
		}
	}

	private int genHashCode() {
		Hash h = new Hash(featureType);
		h.plusEqual(cat.getEhash());
		h.plusEqual(head);
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

		FeatureCatHead cother = (FeatureCatHead) other;
		return featureType == cother.featureType && cat.equals(cother.cat) && head == cother.head;
	}

	@Override
	public FeatureCatHead canonize(Categories categories) {
		Category canonicalCat = categories.canonize(cat);
		return new FeatureCatHead(featureType, canonicalCat, head);
	}

	@Override
	public String toString() {
		return featureType + " " + cat + " " + head;
	}
}
