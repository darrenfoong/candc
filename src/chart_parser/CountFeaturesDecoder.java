package chart_parser;

import java.io.PrintWriter;
import java.util.Map;

import cat_combination.SuperCategory;
import io.Sentence;
import lexicon.Categories;
import model.Feature;
import model.FeatureCat;
import model.FeatureCatHead;
import model.FeatureCounts;
import model.FeatureIDs;
import model.FeatureRule;
import model.FeatureRuleHead;
import model.FeatureRuleHeadDist;
import model.FeatureRuleHeadHead;
import model.FeatureRuleRuleHead;
import model.FeatureRuleRuleHeadHead;
import model.Features;

public class CountFeaturesDecoder {
	private FeatureCounts<FeatureCat> featureCatCounts;
	private FeatureCounts<FeatureCatHead> featureCatHeadCounts;
	private FeatureCounts<FeatureRule> featureRuleCounts;
	private FeatureCounts<FeatureRuleHead> featureRuleHeadCounts;
	private FeatureCounts<FeatureRuleHeadHead> featureRuleHeadHeadCounts;
	private FeatureCounts<FeatureRuleHeadDist> featureRuleHeadDistCounts;
	private FeatureCounts<FeatureRuleRuleHead> featureRuleRuleHeadCounts;
	private FeatureCounts<FeatureRuleRuleHeadHead> featureRuleRuleHeadHeadCounts;

	public CountFeaturesDecoder(Categories categories) {
		this.featureCatCounts = new FeatureCounts<FeatureCat>(categories);
		this.featureCatHeadCounts = new FeatureCounts<FeatureCatHead>(categories);
		this.featureRuleCounts = new FeatureCounts<FeatureRule>(categories);
		this.featureRuleHeadCounts = new FeatureCounts<FeatureRuleHead>(categories);
		this.featureRuleHeadHeadCounts = new FeatureCounts<FeatureRuleHeadHead>(categories);
		this.featureRuleHeadDistCounts = new FeatureCounts<FeatureRuleHeadDist>(categories);
		this.featureRuleRuleHeadCounts = new FeatureCounts<FeatureRuleRuleHead>(categories);
		this.featureRuleRuleHeadHeadCounts = new FeatureCounts<FeatureRuleRuleHeadHead>(categories);
	}

	public boolean countFeatures(Chart chart, Sentence sentence) {
		Cell root = chart.root();

		if (root.isEmpty()) {
			return false;
		}

		for (SuperCategory superCat : root.getSuperCategories()) {
			FeatureCat.count(featureCatCounts, Features.catRoot, superCat);

			short[] featureTypes = {Features.catRootWord, Features.catRootPos};
			FeatureCatHead.count(featureCatHeadCounts, sentence, featureTypes, superCat);

			countFeaturesRecurse(superCat, sentence);
		}

		return true;
	}

	public void countFeaturesRecurse(SuperCategory superCat, Sentence sentence) {
		if (superCat.maxEquivSuperCat != null) {
			return; // already been to this equivalence class
		}

		// just using this field to record that we've been to this node:
		superCat.maxEquivSuperCat = superCat;

		for (SuperCategory s = superCat; s != null; s = s.next) {
			if (s.leftChild != null) {
				if (s.rightChild != null) {
					// binary rule
					FeatureRule.count(featureRuleCounts, Features.ruleBinary, s.leftChild, s.rightChild, s);

					short[] featureTypes = {Features.ruleBinaryWord,
											Features.ruleBinaryPos};
					FeatureRuleHead.count(featureRuleHeadCounts, sentence, featureTypes, s.leftChild, s.rightChild, s);

					short[] featureTypes2 = {Features.ruleBinaryWordWord,
											Features.ruleBinaryWordPos,
											Features.ruleBinaryPosWord,
											Features.ruleBinaryPosPos};
					FeatureRuleHeadHead.count(featureRuleHeadHeadCounts, sentence, featureTypes2, s.leftChild, s.rightChild, s);

					short[] featureTypes3 = {Features.ruleBinaryWordDistAdj,
											Features.ruleBinaryPosDistAdj,
											Features.ruleBinaryWordDistPunct,
											Features.ruleBinaryPosDistPunct,
											Features.ruleBinaryWordDistVerb,
											Features.ruleBinaryPosDistVerb};
					FeatureRuleHeadDist.count(featureRuleHeadDistCounts, sentence, featureTypes3, s.leftChild, s.rightChild, s);

					// start gp feature
					if ( superCat.leftChild.leftChild != null ) {
						if ( superCat.leftChild.rightChild != null ) {
							if ( superCat.rightChild.leftChild != null ) {
								if ( superCat.rightChild.rightChild != null ) {
									// case 8: binary, binary, binary
									short[] featureTypes4 = {Features.ruleBinaryBinaryWordWord2,
															Features.ruleBinaryBinaryWordPos2,
															Features.ruleBinaryBinaryPosWord2,
															Features.ruleBinaryBinaryPosPos2};
									FeatureRuleHeadHead.count(featureRuleHeadHeadCounts, sentence, featureTypes4, s.leftChild.leftChild, s.rightChild.rightChild, s);

									short[] featureTypes5 = {Features.ruleRuleBinaryLeftWordWord,
															Features.ruleRuleBinaryLeftWordPos,
															Features.ruleRuleBinaryLeftPosWord,
															Features.ruleRuleBinaryLeftPosPos};
									FeatureRuleRuleHeadHead.count(featureRuleRuleHeadHeadCounts, sentence, featureTypes5, s.leftChild.leftChild, s.leftChild.rightChild, s.leftChild, s);

									short[] featureTypes6 = {Features.ruleRuleBinaryRightWordWord,
															Features.ruleRuleBinaryRightWordPos,
															Features.ruleRuleBinaryRightPosWord,
															Features.ruleRuleBinaryRightPosPos};
									FeatureRuleRuleHeadHead.count(featureRuleRuleHeadHeadCounts, sentence, featureTypes6, s.rightChild.leftChild, s.rightChild.rightChild, s.rightChild, s);
								} else {
									// case 7: binary, binary, unary
									short[] featureTypes4 = {Features.ruleBinaryUnaryWordWord2,
															Features.ruleBinaryUnaryWordPos2,
															Features.ruleBinaryUnaryPosWord2,
															Features.ruleBinaryUnaryPosPos2};
									FeatureRuleHeadHead.count(featureRuleHeadHeadCounts, sentence, featureTypes4, s.leftChild.leftChild, s.rightChild.leftChild, s);

									short[] featureTypes5 = {Features.ruleRuleBinaryLeftWordWord,
															Features.ruleRuleBinaryLeftWordPos,
															Features.ruleRuleBinaryLeftPosWord,
															Features.ruleRuleBinaryLeftPosPos};
									FeatureRuleRuleHeadHead.count(featureRuleRuleHeadHeadCounts, sentence, featureTypes5, s.leftChild.leftChild, s.leftChild.rightChild, s.leftChild, s);

									short[] featureTypes8 = {Features.ruleRuleUnaryRightWord,
															Features.ruleRuleUnaryRightPos};
									FeatureRuleRuleHead.count(featureRuleRuleHeadCounts, sentence, featureTypes8, s.rightChild.leftChild, s.rightChild, s);
								}
							} else {
								// case 3: binary, binary, null
								short[] featureTypes4 = {Features.ruleBinaryNullWordWord2,
														Features.ruleBinaryNullWordPos2,
														Features.ruleBinaryNullPosWord2,
														Features.ruleBinaryNullPosPos2};
								FeatureRuleHeadHead.count(featureRuleHeadHeadCounts, sentence, featureTypes4, s.leftChild.leftChild, s.rightChild, s);

								short[] featureTypes5 = {Features.ruleRuleBinaryLeftWordWord,
														Features.ruleRuleBinaryLeftWordPos,
														Features.ruleRuleBinaryLeftPosWord,
														Features.ruleRuleBinaryLeftPosPos};
								FeatureRuleRuleHeadHead.count(featureRuleRuleHeadHeadCounts, sentence, featureTypes5, s.leftChild.leftChild, s.leftChild.rightChild, s.leftChild, s);
							}
						} else {
							if ( superCat.rightChild.leftChild != null ) {
								if ( superCat.rightChild.rightChild != null ) {
									// case 6: binary, unary, binary
									short[] featureTypes4 = {Features.ruleUnaryBinaryWordWord2,
															Features.ruleUnaryBinaryWordPos2,
															Features.ruleUnaryBinaryPosWord2,
															Features.ruleUnaryBinaryPosPos2};
									FeatureRuleHeadHead.count(featureRuleHeadHeadCounts, sentence, featureTypes4, s.leftChild.leftChild, s.rightChild.rightChild, s);

									short[] featureTypes7 = {Features.ruleRuleUnaryLeftWord,
															Features.ruleRuleUnaryLeftPos};
									FeatureRuleRuleHead.count(featureRuleRuleHeadCounts, sentence, featureTypes7, s.leftChild.leftChild, s.leftChild, s);

									short[] featureTypes6 = {Features.ruleRuleBinaryRightWordWord,
															Features.ruleRuleBinaryRightWordPos,
															Features.ruleRuleBinaryRightPosWord,
															Features.ruleRuleBinaryRightPosPos};
									FeatureRuleRuleHeadHead.count(featureRuleRuleHeadHeadCounts, sentence, featureTypes6, s.rightChild.leftChild, s.rightChild.rightChild, s.rightChild, s);
								} else {
									// case 5: binary, unary, unary
									short[] featureTypes4 = {Features.ruleUnaryUnaryWordWord2,
															Features.ruleUnaryUnaryWordPos2,
															Features.ruleUnaryUnaryPosWord2,
															Features.ruleUnaryUnaryPosPos2};
									FeatureRuleHeadHead.count(featureRuleHeadHeadCounts, sentence, featureTypes4, s.leftChild.leftChild, s.rightChild.leftChild, s);

									short[] featureTypes7 = {Features.ruleRuleUnaryLeftWord,
															Features.ruleRuleUnaryLeftPos};
									FeatureRuleRuleHead.count(featureRuleRuleHeadCounts, sentence, featureTypes7, s.leftChild.leftChild, s.leftChild, s);

									short[] featureTypes8 = {Features.ruleRuleUnaryRightWord,
															Features.ruleRuleUnaryRightPos};
									FeatureRuleRuleHead.count(featureRuleRuleHeadCounts, sentence, featureTypes8, s.rightChild.leftChild, s.rightChild, s);
								}
							} else {
								// case 1: binary, unary, null
								short[] featureTypes4 = {Features.ruleUnaryNullWordWord2,
														Features.ruleUnaryNullWordPos2,
														Features.ruleUnaryNullPosWord2,
														Features.ruleUnaryNullPosPos2};
								FeatureRuleHeadHead.count(featureRuleHeadHeadCounts, sentence, featureTypes4, s.leftChild.leftChild, s.rightChild, s);

								short[] featureTypes7 = {Features.ruleRuleUnaryLeftWord,
														Features.ruleRuleUnaryLeftPos};
								FeatureRuleRuleHead.count(featureRuleRuleHeadCounts, sentence, featureTypes7, s.leftChild.leftChild, s.leftChild, s);
							}
						}
					} else {
						if ( superCat.rightChild.leftChild != null ) {
							if ( superCat.rightChild.rightChild != null ) {
								// case 4: binary, null, binary
								short[] featureTypes4 = {Features.ruleNullBinaryWordWord2,
														Features.ruleNullBinaryWordPos2,
														Features.ruleNullBinaryPosWord2,
														Features.ruleNullBinaryPosPos2};
								FeatureRuleHeadHead.count(featureRuleHeadHeadCounts, sentence, featureTypes4, s.leftChild, s.rightChild.rightChild, s);

								short[] featureTypes6 = {Features.ruleRuleBinaryRightWordWord,
														Features.ruleRuleBinaryRightWordPos,
														Features.ruleRuleBinaryRightPosWord,
														Features.ruleRuleBinaryRightPosPos};
								FeatureRuleRuleHeadHead.count(featureRuleRuleHeadHeadCounts, sentence, featureTypes6, s.rightChild.leftChild, s.rightChild.rightChild, s.rightChild, s);
							} else {
								// case 2: binary, null, unary
								short[] featureTypes4 = {Features.ruleNullUnaryWordWord2,
														Features.ruleNullUnaryWordPos2,
														Features.ruleNullUnaryPosWord2,
														Features.ruleNullUnaryPosPos2};
								FeatureRuleHeadHead.count(featureRuleHeadHeadCounts, sentence, featureTypes4, s.leftChild, s.rightChild.leftChild, s);	}

								short[] featureTypes8 = {Features.ruleRuleUnaryRightWord,
														Features.ruleRuleUnaryRightPos};
								FeatureRuleRuleHead.count(featureRuleRuleHeadCounts, sentence, featureTypes8, s.rightChild.leftChild, s.rightChild, s);
						}
					}
					// end gp feature

					countFeaturesRecurse(s.leftChild, sentence);
					countFeaturesRecurse(s.rightChild, sentence);
				} else {
					// unary rule
					FeatureRule.count(featureRuleCounts, Features.ruleUnary, s.leftChild, s.leftChild, s);

					short[] featureTypes = {Features.ruleUnaryWord,
											Features.ruleUnaryPos};
					FeatureRuleHead.count(featureRuleHeadCounts, sentence, featureTypes, s.leftChild, s.leftChild, s);

					// start gp feature
					if ( s.leftChild.leftChild!= null ) {
						if ( s.leftChild.rightChild != null ) {
							short[] featureTypes2 = {Features.ruleBinaryWordWord2,
													Features.ruleBinaryWordPos2,
													Features.ruleBinaryPosWord2,
													Features.ruleBinaryPosPos2};
							FeatureRuleHeadHead.count(featureRuleHeadHeadCounts, sentence, featureTypes2, s.leftChild.leftChild, s.leftChild.rightChild, s);
						} else {
							short[] featureTypes2 = {Features.ruleUnaryWord2,
													Features.ruleUnaryPos2};
							FeatureRuleHead.count(featureRuleHeadCounts, sentence, featureTypes2, s.leftChild.leftChild, s.leftChild.leftChild, s);
						}
					}
					// end gp feature

					countFeaturesRecurse(s.leftChild, sentence);
				}
			} else {
				// leaf
				FeatureCat.count(featureCatCounts, Features.catLex, s);

				short[] featureTypes = {Features.catLexWord, Features.catLexPos};
				FeatureCatHead.count(featureCatHeadCounts, sentence, featureTypes, s);
			}
		}
	}

	public void mergeAllFeatureCounts(Features features) {
		mergeFeatureCounts(featureCatCounts, features.featureCatIDs);
		mergeFeatureCounts(featureCatHeadCounts, features.featureCatHeadIDs);
		mergeFeatureCounts(featureRuleCounts, features.featureRuleIDs);
		mergeFeatureCounts(featureRuleHeadCounts, features.featureRuleHeadIDs);
		mergeFeatureCounts(featureRuleHeadHeadCounts, features.featureRuleHeadHeadIDs);
		mergeFeatureCounts(featureRuleHeadDistCounts, features.featureRuleHeadDistIDs);
		mergeFeatureCounts(featureRuleRuleHeadCounts, features.featureRuleRuleHeadIDs);
		mergeFeatureCounts(featureRuleRuleHeadHeadCounts, features.featureRuleRuleHeadHeadIDs);
	}

	public <T extends Feature<T>> void mergeFeatureCounts(FeatureCounts<T> featureCounts, FeatureIDs<T> featureIDs) {
		for (Map.Entry<T, Integer> entry : featureCounts.getFeatureCounts().entrySet()) {
			T feature = entry.getKey();
			if ( !featureIDs.getFeatureIDs().containsKey(feature) ) {
				featureIDs.addFeature(entry.getKey(), -2);
			}
		}
	}

	public void printFeatureCounts(PrintWriter out) {
		featureCatCounts.print(out);
		featureCatHeadCounts.print(out);
		featureRuleCounts.print(out);
		featureRuleHeadCounts.print(out);
		featureRuleHeadHeadCounts.print(out);
		featureRuleHeadDistCounts.print(out);
		featureRuleRuleHeadCounts.print(out);
		featureRuleRuleHeadHeadCounts.print(out);
	}
}
