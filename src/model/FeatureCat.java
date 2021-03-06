package model;

import java.util.ArrayList;
import java.util.HashMap;

import cat_combination.SuperCategory;
import lexicon.Categories;
import lexicon.Category;
import utils.Hash;

public class FeatureCat implements Feature<FeatureCat> {
	final short featureType;
	final Category cat;
	final int hashCode;

	public FeatureCat(short featureType, Category cat) {
		this.featureType = featureType;
		this.cat = cat;
		this.hashCode = genHashCode();
	}

	public static void readFeature(short featureType, String[] tokens, FeatureIDs<FeatureCat> featureIDs, int ID, Categories categories) {
		Category cat = categories.canonize(tokens[1]);
		FeatureCat feature = new FeatureCat(featureType, cat);
		featureIDs.addFeature(feature, ID);
	}

	public static void collectFeatures(SuperCategory superCat, short featureType, FeatureIDs<FeatureCat> featureIDs, ArrayList<Integer> ids) {
		HashMap<FeatureCat, Integer> featureIDsHashMap = featureIDs.getFeatureIDs();
		FeatureCat feature = new FeatureCat(featureType, superCat.cat);
		Integer id = featureIDsHashMap.get(feature);
		if (id != null) {
			ids.add(id);
		}
	}

	public static void count(FeatureCounts<FeatureCat> featureCatCounts, short featureType, SuperCategory superCat) {
		featureCatCounts.addCount(new FeatureCat(featureType, superCat.cat));
	}

	private int genHashCode() {
		Hash h = new Hash(featureType);
		h.plusEqual(cat.getEhash());
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

		FeatureCat cother = (FeatureCat) other;

		return featureType == cother.featureType && cat.equals(cother.cat);
	}

	@Override
	public FeatureCat canonize(Categories categories) {
		Category canonicalCat = categories.canonize(cat);
		return new FeatureCat(featureType, canonicalCat);
	}

	@Override
	public String toString() {
		return featureType + " " + cat;
	}
}
