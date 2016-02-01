package model;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import lexicon.Categories;

public class FeaturesHashCode extends Features {
	private HashMap<Integer, Integer> featureCatHashCounts;
	private HashMap<Integer, Integer> featureCatHeadHashCounts;
	private HashMap<Integer, Integer> featureRuleHashCounts;
	private HashMap<Integer, Integer> featureRuleHeadHashCounts;
	private HashMap<Integer, Integer> featureRuleHeadHeadHashCounts;
	private HashMap<Integer, Integer> featureRuleHeadDistHashCounts;
	private HashMap<Integer, Integer> featureRuleRuleHeadHashCounts;
	private HashMap<Integer, Integer> featureRuleRuleHeadHeadHashCounts;

	public FeaturesHashCode(String featuresFile, String weightsFile, Weights weights, Categories categories) throws IOException {
		super(featuresFile, weightsFile, weights, categories, true);

		this.featureCatHashCounts = new HashMap<Integer, Integer>();
		this.featureCatHeadHashCounts = new HashMap<Integer, Integer>();
		this.featureRuleHashCounts = new HashMap<Integer, Integer>();
		this.featureRuleHeadHashCounts = new HashMap<Integer, Integer>();
		this.featureRuleHeadHeadHashCounts = new HashMap<Integer, Integer>();
		this.featureRuleHeadDistHashCounts = new HashMap<Integer, Integer>();
		this.featureRuleRuleHeadHashCounts = new HashMap<Integer, Integer>();
		this.featureRuleRuleHeadHeadHashCounts = new HashMap<Integer, Integer>();
	}

	public void countAllHashes() {
		countHashes(featureCatIDs, featureCatHashCounts);
		countHashes(featureCatHeadIDs, featureCatHeadHashCounts);
		countHashes(featureRuleIDs, featureRuleHashCounts);
		countHashes(featureRuleHeadIDs, featureRuleHeadHashCounts);
		countHashes(featureRuleHeadHeadIDs, featureRuleHeadHeadHashCounts);
		countHashes(featureRuleHeadDistIDs, featureRuleHeadDistHashCounts);
		countHashes(featureRuleRuleHeadIDs, featureRuleRuleHeadHashCounts);
		countHashes(featureRuleRuleHeadHeadIDs, featureRuleRuleHeadHeadHashCounts);
	}

	public void printAllHashCounts() {
		System.out.println("# featureCatHashCounts");
		printHashCounts(featureCatIDs, featureCatHashCounts);
		System.out.println("# featureCatHeadHashCounts");
		printHashCounts(featureCatHeadIDs, featureCatHeadHashCounts);
		System.out.println("# featureRuleHashCounts");
		printHashCounts(featureRuleIDs, featureRuleHashCounts);
		System.out.println("# featureRuleHeadHashCounts");
		printHashCounts(featureRuleHeadIDs, featureRuleHeadHashCounts);
		System.out.println("# featureRuleHeadHeadHashCounts");
		printHashCounts(featureRuleHeadHeadIDs, featureRuleHeadHeadHashCounts);
		System.out.println("# featureRuleHeadDistHashCounts");
		printHashCounts(featureRuleHeadDistIDs, featureRuleHeadDistHashCounts);
		System.out.println("# featureRuleRuleHeadHashCounts");
		printHashCounts(featureRuleRuleHeadIDs, featureRuleRuleHeadHashCounts);
		System.out.println("# featureRuleRuleHeadHeadHashCounts");
		printHashCounts(featureRuleRuleHeadHeadIDs, featureRuleRuleHeadHeadHashCounts);
	}

	private <T extends Feature<T>> void countHashes(FeatureIDs<T> featureIDs, HashMap<Integer, Integer> featureHashCounts) {
		for (Map.Entry<T, Integer> entry : featureIDs.getFeatureIDs().entrySet()) {
			Integer hashCount = featureHashCounts.get(entry.getKey().hashCode());
			if ( hashCount == null ) {
				hashCount = 0;
			}

			if ( hashCount > 1 ) {
				System.out.println(entry.getKey().hashCode() + " | " + entry.getKey());
			}
			featureHashCounts.put(entry.getKey().hashCode(), hashCount+1);
		}
	}

	private <T extends Feature<T>> void printHashCounts(FeatureIDs<T> featureIDs, HashMap<Integer, Integer> featureHashCounts) {
		int collidedBuckets = 0;
		int totalBuckets = 0;
		int maxCollisions = 0;
		for (Map.Entry<Integer, Integer> entry : featureHashCounts.entrySet()) {
			if ( entry.getValue() > 1 ) {
				if ( entry.getValue() > maxCollisions ) {
					maxCollisions = entry.getValue();
				}
				collidedBuckets++;
			}
			totalBuckets++;
		}
		if ( totalBuckets > 0 ) {
			System.out.println("# " + collidedBuckets + "/" + totalBuckets + " (" + ((double) (collidedBuckets*100)/(double) totalBuckets) + "%)" + " buckets with collisions");
			System.out.println("# Average per bucket: " + featureIDs.getFeatureIDs().size() + "/" + totalBuckets + " (" + ((double) featureIDs.getFeatureIDs().size()/(double) totalBuckets) + ")");
			System.out.println("# Max collisions in bucket: " + maxCollisions);
		} else {
			System.out.println("# " + collidedBuckets + "/" + totalBuckets + " buckets with collisions");
		}
	}
}
