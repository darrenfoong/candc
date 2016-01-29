package model;

import io.Sentence;

import java.util.ArrayList;
import java.util.HashMap;

import lexicon.Categories;
import lexicon.Category;
import utils.Hash;
import cat_combination.SuperCategory;
import cat_combination.Variable;

public class FeatureRuleHeadDist implements Feature<FeatureRuleHeadDist> {
	final short featureType;
	final Category leftCat;
	final Category rightCat;
	final Category resultCat;
	final int head;
	short distance;
	final int hashCode;

	public FeatureRuleHeadDist(short featureType, Category leftCat, Category rightCat, Category resultCat, int head, short distance) {
		this.featureType = featureType;
		this.leftCat = leftCat;
		this.rightCat = rightCat;
		this.resultCat = resultCat;
		this.head = head;
		this.distance = distance;
		this.hashCode = genHashCode();
	}

	public static void readFeature(short featureType, String[] tokens, FeatureIDs<FeatureRuleHeadDist> featureIDs, int ID, Categories categories) {
		Category leftCat = categories.canonize(tokens[1]);
		Category rightCat = categories.canonize(tokens[2]);
		Category resultCat = categories.canonize(tokens[3]);
		int head = Integer.parseInt(tokens[4]);
		short distance = Short.parseShort(tokens[5]);
		FeatureRuleHeadDist feature = new FeatureRuleHeadDist(featureType, leftCat, rightCat, resultCat, head, distance);
		featureIDs.addFeature(feature, ID);
	}

	public static void collectFeatures(SuperCategory leftSuperCat, SuperCategory rightSuperCat, SuperCategory resultSuperCat, short featureType, ArrayList<Integer> tokenIDs, ArrayList<String> postags, FeatureIDs<FeatureRuleHeadDist> featureIDs, ArrayList<Integer> ids) {
		HashMap<FeatureRuleHeadDist, Integer> featureIDsHashMap = featureIDs.getFeatureIDs();
		Variable var = resultSuperCat.vars[resultSuperCat.cat.var];
		Variable leftVar = leftSuperCat.vars[leftSuperCat.cat.var];
		Variable rightVar = rightSuperCat.vars[rightSuperCat.cat.var];

		// all combinations of head results and head arguments:
		for (int i = 0; i < var.fillers.length && var.fillers[i] != Variable.SENTINEL; i++) {
			if (var.fillers[i] == 0) {
				continue;
			}

			for (int j = 0; j < leftVar.fillers.length && leftVar.fillers[j] != Variable.SENTINEL; j++) {
				if (leftVar.fillers[j] == 0) {
					continue;
				}

				for (int k = 0; k < rightVar.fillers.length && rightVar.fillers[k] != Variable.SENTINEL; k++) {
					if (rightVar.fillers[k] == 0) {
						continue;
					}

					// one of the head arguments must be equal to the head result:
					if (var.fillers[i] != leftVar.fillers[j] && var.fillers[i] != rightVar.fillers[k]) {
						continue;
					}

					short distance = FeatureRuleHeadDist.calcDistance(featureType, leftVar.fillers[j], rightVar.fillers[k], postags);

					FeatureRuleHeadDist feature = new FeatureRuleHeadDist(featureType, leftSuperCat.cat, rightSuperCat.cat, resultSuperCat.cat, tokenIDs.get(var.fillers[i] - 1), distance);
					Integer id = featureIDsHashMap.get(feature);
					if (id != null) {
						ids.add(id);
					}
				}
			}
		}
	}

	public static void count(FeatureCounts<FeatureRuleHeadDist> featureRuleHeadDistCounts, Sentence sentence, short[] featureTypes, SuperCategory leftSuperCat, SuperCategory rightSuperCat, SuperCategory resultSuperCat) {
		Variable var = resultSuperCat.vars[resultSuperCat.cat.var];
		Variable leftVar = leftSuperCat.vars[leftSuperCat.cat.var];
		Variable rightVar = rightSuperCat.vars[rightSuperCat.cat.var];

		for (int i = 0; i < var.fillers.length && var.fillers[i] != Variable.SENTINEL; i++) {
			if (var.fillers[i] == 0) {
				continue;
			}

			int head = sentence.wordIDs.get(var.fillers[i] - 1);
			int pos = sentence.postagIDs.get(var.fillers[i] - 1);

			for (int j = 0; j < leftVar.fillers.length && leftVar.fillers[j] != Variable.SENTINEL; j++) {
				if (leftVar.fillers[j] == 0) {
					continue;
				}

				for (int k = 0; k < rightVar.fillers.length && rightVar.fillers[k] != Variable.SENTINEL; k++) {
					if (rightVar.fillers[k] == 0) {
						continue;
					}

					// one of the head arguments must be equal to the head result:
					if (var.fillers[i] != leftVar.fillers[j] && var.fillers[i] != rightVar.fillers[k]) {
						continue;
					}

					short distance;
					ArrayList<String> postags = sentence.postags;

					// rule + result head + distance:
					distance = FeatureRuleHeadDist.calcDistance(featureTypes[0], leftVar.fillers[j], rightVar.fillers[k], postags);
					featureRuleHeadDistCounts.addCount(new FeatureRuleHeadDist(featureTypes[0], leftSuperCat.cat, rightSuperCat.cat, resultSuperCat.cat, head, distance));
					featureRuleHeadDistCounts.addCount(new FeatureRuleHeadDist(featureTypes[1], leftSuperCat.cat, rightSuperCat.cat, resultSuperCat.cat, pos, distance));

					distance = FeatureRuleHeadDist.calcDistance(featureTypes[2], leftVar.fillers[j], rightVar.fillers[k], postags);
					featureRuleHeadDistCounts.addCount(new FeatureRuleHeadDist(featureTypes[2], leftSuperCat.cat, rightSuperCat.cat, resultSuperCat.cat, head, distance));
					featureRuleHeadDistCounts.addCount(new FeatureRuleHeadDist(featureTypes[3], leftSuperCat.cat, rightSuperCat.cat, resultSuperCat.cat, pos, distance));

					distance = FeatureRuleHeadDist.calcDistance(featureTypes[4], leftVar.fillers[j], rightVar.fillers[k], postags);
					featureRuleHeadDistCounts.addCount(new FeatureRuleHeadDist(featureTypes[4], leftSuperCat.cat, rightSuperCat.cat, resultSuperCat.cat, head, distance));
					featureRuleHeadDistCounts.addCount(new FeatureRuleHeadDist(featureTypes[5], leftSuperCat.cat, rightSuperCat.cat, resultSuperCat.cat, pos, distance));
				}
			}
		}
	}

	private int genHashCode() {
		Hash h = new Hash(featureType);
		h.plusEqual(leftCat.getEhash());
		h.plusEqual(rightCat.getEhash());
		h.plusEqual(resultCat.getEhash());
		h.plusEqual(head);
		h.plusEqual(distance);
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

		FeatureRuleHeadDist cother = (FeatureRuleHeadDist) other;
		return featureType == cother.featureType && leftCat.equals(cother.leftCat) && rightCat.equals(cother.rightCat) && resultCat.equals(cother.resultCat) && head == cother.head && distance == cother.distance;
	}

	public static short calcDistance(short featureType, int leftIndex, int rightIndex, ArrayList<String> postags) {
		// left and right indices start at 1 (i.e. leftmost word in sentence has
		// index 1)

		short numVerbs = 0;
		short numPunct = 0;

		if (featureType == Features.ruleBinaryWordDistAdj || featureType == Features.ruleBinaryPosDistAdj) {
			short numWords = (short) (rightIndex - leftIndex - 1);
			if (numWords > 2) {
				numWords = 2;
			}
			return numWords;
		}

		// finding the number of verbs *between* left and right (indexes already
		// off by 1)
		if (featureType == Features.ruleBinaryWordDistVerb || featureType == Features.ruleBinaryPosDistVerb) {
			for (int i = leftIndex; i < rightIndex - 1; i++) {
				String postag = postags.get(i);
				if (postag.charAt(0) == 'V' && numVerbs < 1) {
					numVerbs++;
				}
			}
			return numVerbs;
		}

		// finding the number of punctuation marks *between* left and right
		// (indexes already off by 1)
		if (featureType == Features.ruleBinaryWordDistPunct || featureType == Features.ruleBinaryPosDistPunct) {
			for (int i = leftIndex; i < rightIndex - 1; i++) {
				String postag = postags.get(i);
				if ((postag.charAt(0) == ',' || postag.charAt(0) == ':' || postag.charAt(0) == '.' || postag.charAt(0) == ';') && numPunct < 2) {
					numPunct++;
				}
			}
			return numPunct;
		}
		throw new Error("unexpected feature type in distance calculation");
	}

	@Override
	public FeatureRuleHeadDist canonize(Categories categories) {
		Category canonicalLeftCat = categories.canonize(leftCat);
		Category canonicalRightCat = categories.canonize(rightCat);
		Category canonicalResultCat = categories.canonize(resultCat);
		return new FeatureRuleHeadDist(featureType, canonicalLeftCat, canonicalRightCat, canonicalResultCat, head, distance);
	}

	@Override
	public String toString() {
		return featureType + " " + leftCat + " " + rightCat + " " + resultCat + " " + head + " " + distance;
	}
}
