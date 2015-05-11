package model;

import io.Preface;
import io.Sentence;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import lexicon.Categories;
import cat_combination.SuperCategory;

public class Features {
	// FeatureCat
	final public static short catRoot = 1;
	final public static short catLex = 4;

	// FeatureCatHead
	final public static short catRootWord = 2;
	final public static short catRootPos = 3;
	final public static short catLexWord = 5;
	final public static short catLexPos = 6;

	// FeatureRule
	final public static short ruleBinary = 7;
	final public static short ruleUnary = 8;

	// FeatureRuleHead
	final public static short ruleBinaryWord = 9;
	final public static short ruleBinaryPos = 10;
	final public static short ruleUnaryWord = 11;
	final public static short ruleUnaryPos = 12;

	// FeatureRuleHeadHead
	final public static short ruleBinaryWordWord = 13;
	final public static short ruleBinaryWordPos = 14;
	final public static short ruleBinaryPosWord = 15;
	final public static short ruleBinaryPosPos = 16;

	// FeatureRuleHeadDist
	final public static short ruleBinaryWordDistAdj = 17; // adjacency distance measure
	final public static short ruleBinaryWordDistPunct = 18; // punctuation distance measure
	final public static short ruleBinaryWordDistVerb = 19; // verb distance measure
	final public static short ruleBinaryPosDistAdj = 20;
	final public static short ruleBinaryPosDistPunct = 21;
	final public static short ruleBinaryPosDistVerb = 22;

	// FeatureRuleHead
	// unary 1
	final public static short ruleUnaryWord2 = 300;
	final public static short ruleUnaryPos2 = 301;

	// FeatureRuleHeadHead
	// unary 1
	final public static short ruleBinaryWordWord2 = 310;
	final public static short ruleBinaryWordPos2 = 311;
	final public static short ruleBinaryPosWord2 = 312;
	final public static short ruleBinaryPosPos2 = 313;

	// binary 1
	final public static short ruleUnaryUnaryWordWord2 = 320;
	final public static short ruleUnaryUnaryWordPos2 = 321;
	final public static short ruleUnaryUnaryPosWord2 = 322;
	final public static short ruleUnaryUnaryPosPos2 = 323;

	final public static short ruleNullUnaryWordWord2 = 3200;
	final public static short ruleNullUnaryWordPos2 = 3210;
	final public static short ruleNullUnaryPosWord2 = 3220;
	final public static short ruleNullUnaryPosPos2 = 3230;

	final public static short ruleUnaryNullWordWord2 = 3201;
	final public static short ruleUnaryNullWordPos2 = 3211;
	final public static short ruleUnaryNullPosWord2 = 3221;
	final public static short ruleUnaryNullPosPos2 = 3231;

	final public static short ruleUnaryBinaryWordWord2 = 330;
	final public static short ruleUnaryBinaryWordPos2 = 331;
	final public static short ruleUnaryBinaryPosWord2 = 332;
	final public static short ruleUnaryBinaryPosPos2 = 333;

	final public static short ruleBinaryUnaryWordWord2 = 340;
	final public static short ruleBinaryUnaryWordPos2 = 341;
	final public static short ruleBinaryUnaryPosWord2 = 342;
	final public static short ruleBinaryUnaryPosPos2 = 343;

	final public static short ruleBinaryBinaryWordWord2 = 350;
	final public static short ruleBinaryBinaryWordPos2 = 351;
	final public static short ruleBinaryBinaryPosWord2 = 352;
	final public static short ruleBinaryBinaryPosPos2 = 353;

	final public static short ruleNullBinaryWordWord2 = 3500;
	final public static short ruleNullBinaryWordPos2 = 3510;
	final public static short ruleNullBinaryPosWord2 = 3520;
	final public static short ruleNullBinaryPosPos2 = 3530;

	final public static short ruleBinaryNullWordWord2 = 3501;
	final public static short ruleBinaryNullWordPos2 = 3511;
	final public static short ruleBinaryNullPosWord2 = 3521;
	final public static short ruleBinaryNullPosPos2 = 3531;

	final public static short ruleRuleUnaryLeftWord = 4010;
	final public static short ruleRuleUnaryLeftPos = 4020;

	final public static short ruleRuleUnaryRightWord = 4011;
	final public static short ruleRuleUnaryRightPos = 4021;

	final public static short ruleRuleBinaryLeftWordWord = 4110;
	final public static short ruleRuleBinaryLeftWordPos = 4120;
	final public static short ruleRuleBinaryLeftPosWord = 4130;
	final public static short ruleRuleBinaryLeftPosPos= 4140;
	
	final public static short ruleRuleBinaryRightWordWord = 4111;
	final public static short ruleRuleBinaryRightWordPos = 4121;
	final public static short ruleRuleBinaryRightPosWord = 4131;
	final public static short ruleRuleBinaryRightPosPos= 4141;

	public FeatureIDs<FeatureCat> featureCatIDs;
	public FeatureIDs<FeatureCatHead> featureCatHeadIDs;
	public FeatureIDs<FeatureRule> featureRuleIDs;
	public FeatureIDs<FeatureRuleHead> featureRuleHeadIDs;
	public FeatureIDs<FeatureRuleHeadHead> featureRuleHeadHeadIDs;
	public FeatureIDs<FeatureRuleHeadDist> featureRuleHeadDistIDs;
	public FeatureIDs<FeatureRuleRuleHead> featureRuleRuleHeadIDs;
	public FeatureIDs<FeatureRuleRuleHeadHead> featureRuleRuleHeadHeadIDs;

	public int numFeatures;

	public static boolean newFeatures;

	public Features(String featuresFile, Categories categories, boolean newFeatures) throws IOException {
		this.featureCatIDs = new FeatureIDs<FeatureCat>();
		this.featureCatHeadIDs = new FeatureIDs<FeatureCatHead>();
		this.featureRuleIDs = new FeatureIDs<FeatureRule>();
		this.featureRuleHeadIDs = new FeatureIDs<FeatureRuleHead>();
		this.featureRuleHeadHeadIDs = new FeatureIDs<FeatureRuleHeadHead>();
		this.featureRuleHeadDistIDs = new FeatureIDs<FeatureRuleHeadDist>();
		this.featureRuleRuleHeadIDs = new FeatureIDs<FeatureRuleRuleHead>();
		this.featureRuleRuleHeadHeadIDs = new FeatureIDs<FeatureRuleRuleHeadHead>();

		readFeatures(featuresFile, categories);

		Features.newFeatures = newFeatures;
	}

	public Features(String featuresFile, String weightsFile, Weights weights, Categories categories, boolean newFeatures) throws IOException {
		this.featureCatIDs = new FeatureIDs<FeatureCat>();
		this.featureCatHeadIDs = new FeatureIDs<FeatureCatHead>();
		this.featureRuleIDs = new FeatureIDs<FeatureRule>();
		this.featureRuleHeadIDs = new FeatureIDs<FeatureRuleHead>();
		this.featureRuleHeadHeadIDs = new FeatureIDs<FeatureRuleHeadHead>();
		this.featureRuleHeadDistIDs = new FeatureIDs<FeatureRuleHeadDist>();
		this.featureRuleRuleHeadIDs = new FeatureIDs<FeatureRuleRuleHead>();
		this.featureRuleRuleHeadHeadIDs = new FeatureIDs<FeatureRuleRuleHeadHead>();

		readFeaturesWeights(featuresFile, weightsFile, weights, categories);

		Features.newFeatures = newFeatures;
	}

	private void readFeatures(String featuresFile, Categories categories) throws IOException {
		BufferedReader featuresIn = new BufferedReader(new FileReader(featuresFile));

		Preface.readPreface(featuresIn);

		String featuresLine = null;
		int ID = 1;

		while ((featuresLine = featuresIn.readLine()) != null) {
			readFeature(featuresLine, ID, categories);
			ID++;
		}

		numFeatures = ID;
		// ID starts at 1 (because of logp)
		// and gets incremented after each feature has been read
		System.out.println("Total number of features read in: " + numFeatures);
	}

	private void readFeaturesWeights(String featuresFile, String weightsFile, Weights weights, Categories categories) throws IOException {
		ArrayList<Double> weightsList = new ArrayList<Double>();

		BufferedReader featuresIn = new BufferedReader(new FileReader(featuresFile));
		BufferedReader weightsIn = new BufferedReader(new FileReader(weightsFile));

		Preface.readPreface(featuresIn);
		Preface.readPreface(weightsIn);

		String featuresLine = null;

		// first line in weights is for logp
		String weightsLine = weightsIn.readLine();
		weightsList.add(Double.valueOf(weightsLine));
		int ID = 1;

		while ((featuresLine = featuresIn.readLine()) != null) {
			weightsLine = weightsIn.readLine();
			double weight = Double.valueOf(weightsLine);

			if ( weight == 0.0 ) {
				continue;
			} else {
				weightsList.add(weight);
			}

			readFeature(featuresLine, ID, categories);
			ID++;
		}

		double[] weightsListArray = new double[weightsList.size()];

		for ( int i = 0; i < weightsList.size(); i++ ) {
			weightsListArray[i] = weightsList.get(i);
		}

		weights.setWeights(weightsListArray);

		numFeatures = ID;
		// ID starts at 1 (because of logp)
		// and gets incremented after each feature has been read
		System.out.println("Total number of features read in: " + numFeatures);
	}

	private void readFeature(String featuresLine, int ID, Categories categories) {
		String[] tokens = featuresLine.split("\\s");
		short featureType = Short.valueOf(tokens[0]);

		switch (featureType) {
			case Features.catRoot: // fall through
			case Features.catLex: // |
				if (tokens.length != 3) {
					throw new Error("catRoot and catLex features should have 3 fields!");
				}
				FeatureCat.readFeature(featureType, tokens, featureCatIDs, ID, categories);
				break;
			case Features.catRootWord: // fall through
			case Features.catRootPos: // |
			case Features.catLexWord: // |
			case Features.catLexPos: // |
				if (tokens.length != 4) {
					throw new Error("catRootWord and catLexWord features should have 4 fields!");
				}
				FeatureCatHead.readFeature(featureType, tokens, featureCatHeadIDs, ID, categories);
				break;
			case Features.ruleBinary: // fall through
			case Features.ruleUnary: // |
				if (tokens.length != 5) {
					throw new Error("ruleBinary and ruleUnary features should have 5 fields!");
				}
				FeatureRule.readFeature(featureType, tokens, featureRuleIDs, ID, categories);
				break;
			case Features.ruleBinaryWord: // fall through
			case Features.ruleBinaryPos: // |
			case Features.ruleUnaryWord: // |
			case Features.ruleUnaryPos: // |
				if (tokens.length != 6) {
					throw new Error("ruleBinaryWord and ruleUnaryWord features should have 6 fields!");
				}
				FeatureRuleHead.readFeature(featureType, tokens, featureRuleHeadIDs, ID, categories);
				break;
			case Features.ruleBinaryWordWord: // fall through
			case Features.ruleBinaryWordPos: // |
			case Features.ruleBinaryPosWord: // |
			case Features.ruleBinaryPosPos: // |
				if (tokens.length != 7) {
					throw new Error("ruleBinaryWordWord features should have 7 fields!");
				}
				FeatureRuleHeadHead.readFeature(featureType, tokens, featureRuleHeadHeadIDs, ID, categories);
				break;
			case Features.ruleBinaryWordDistAdj: // fall through
			case Features.ruleBinaryWordDistPunct: // |
			case Features.ruleBinaryWordDistVerb: // |
			case Features.ruleBinaryPosDistAdj: // |
			case Features.ruleBinaryPosDistPunct: // |
			case Features.ruleBinaryPosDistVerb: // |
				if (tokens.length != 7) {
					throw new Error("ruleBinaryWordDist features should have 7 fields!");
				}
				FeatureRuleHeadDist.readFeature(featureType, tokens, featureRuleHeadDistIDs, ID, categories);
				break;
			case Features.ruleUnaryWord2: // fall through
			case Features.ruleUnaryPos2: // |
				if (tokens.length != 6) {
					throw new Error("features should have 6 fields!");
				}
				FeatureRuleHead.readFeature(featureType, tokens, featureRuleHeadIDs, ID, categories);
				break;
			case Features.ruleBinaryWordWord2: // fall through
			case Features.ruleBinaryWordPos2: // |
			case Features.ruleBinaryPosWord2: // |
			case Features.ruleBinaryPosPos2: // |
			case Features.ruleUnaryUnaryWordWord2: // |
			case Features.ruleUnaryUnaryWordPos2 : // |
			case Features.ruleUnaryUnaryPosWord2: // |
			case Features.ruleUnaryUnaryPosPos2: // |
			case Features.ruleNullUnaryWordWord2: // |
			case Features.ruleNullUnaryWordPos2 : // |
			case Features.ruleNullUnaryPosWord2: // |
			case Features.ruleNullUnaryPosPos2: // |
			case Features.ruleUnaryNullWordWord2: // |
			case Features.ruleUnaryNullWordPos2 : // |
			case Features.ruleUnaryNullPosWord2: // |
			case Features.ruleUnaryNullPosPos2: // |
			case Features.ruleUnaryBinaryWordWord2: // |
			case Features.ruleUnaryBinaryWordPos2: // |
			case Features.ruleUnaryBinaryPosWord2: // |
			case Features.ruleUnaryBinaryPosPos2: // |
			case Features.ruleBinaryUnaryWordWord2: // |
			case Features.ruleBinaryUnaryWordPos2: // |
			case Features.ruleBinaryUnaryPosWord2: // |
			case Features.ruleBinaryUnaryPosPos2: // |
			case Features.ruleBinaryBinaryWordWord2: // |
			case Features.ruleBinaryBinaryWordPos2: // |
			case Features.ruleBinaryBinaryPosWord2: // |
			case Features.ruleBinaryBinaryPosPos2: // |
			case Features.ruleNullBinaryWordWord2: // |
			case Features.ruleNullBinaryWordPos2: // |
			case Features.ruleNullBinaryPosWord2: // |
			case Features.ruleNullBinaryPosPos2: // |
			case Features.ruleBinaryNullWordWord2: // |
			case Features.ruleBinaryNullWordPos2: // |
			case Features.ruleBinaryNullPosWord2: // |
			case Features.ruleBinaryNullPosPos2: // |
				if (tokens.length != 7) {
					throw new Error("features should have 7 fields!");
				}
				FeatureRuleHeadHead.readFeature(featureType, tokens, featureRuleHeadHeadIDs, ID, categories);
				break;
			case Features.ruleRuleUnaryLeftWord:
			case Features.ruleRuleUnaryLeftPos:
			case Features.ruleRuleUnaryRightWord:
			case Features.ruleRuleUnaryRightPos:
				if (tokens.length != 6) {
					throw new Error("features should have 6 fields!");
				}
				FeatureRuleRuleHead.readFeature(featureType, tokens, featureRuleRuleHeadIDs, ID, categories);
				break;
			case Features.ruleRuleBinaryLeftWordWord:
			case Features.ruleRuleBinaryLeftWordPos:
			case Features.ruleRuleBinaryLeftPosWord:
			case Features.ruleRuleBinaryLeftPosPos:
			case Features.ruleRuleBinaryRightWordWord:
			case Features.ruleRuleBinaryRightWordPos:
			case Features.ruleRuleBinaryRightPosWord:
			case Features.ruleRuleBinaryRightPosPos:
				if (tokens.length != 8) {
					throw new Error("features should have 8 fields!");
				}
				FeatureRuleRuleHeadHead.readFeature(featureType, tokens, featureRuleRuleHeadHeadIDs, ID, categories);
				break;
			default:
				throw new Error("run out of feature types!");
		}
	}

	public void collectLeafFeatures(SuperCategory superCat, Sentence sentence, ArrayList<Integer> featureIDs) {
		FeatureCat.collectFeatures(superCat, catLex, featureCatIDs, featureIDs);
		FeatureCatHead.collectFeatures(superCat, catLexWord, sentence.wordIDs, featureCatHeadIDs, featureIDs);
		FeatureCatHead.collectFeatures(superCat, catLexPos, sentence.postagIDs, featureCatHeadIDs, featureIDs);
	}

	public void collectUnaryFeatures(SuperCategory superCat, Sentence sentence, ArrayList<Integer> featureIDs) {
		FeatureRule.collectFeatures(superCat.leftChild, superCat.leftChild, superCat, ruleUnary, featureRuleIDs, featureIDs);
		FeatureRuleHead.collectFeatures(superCat.leftChild, superCat.leftChild, superCat, ruleUnaryWord, sentence.wordIDs, featureRuleHeadIDs, featureIDs);
		FeatureRuleHead.collectFeatures(superCat.leftChild, superCat.leftChild, superCat, ruleUnaryPos, sentence.postagIDs, featureRuleHeadIDs, featureIDs);

		if ( Features.newFeatures ) {
			if ( superCat.leftChild.leftChild!= null ) {
				if ( superCat.leftChild.rightChild != null ) {
					FeatureRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.leftChild.rightChild, superCat, ruleBinaryWordWord2, sentence.wordIDs, sentence.wordIDs, featureRuleHeadHeadIDs, featureIDs);
					FeatureRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.leftChild.rightChild, superCat, ruleBinaryWordPos2, sentence.wordIDs, sentence.postagIDs, featureRuleHeadHeadIDs, featureIDs);
					FeatureRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.leftChild.rightChild, superCat, ruleBinaryPosWord2, sentence.postagIDs, sentence.wordIDs, featureRuleHeadHeadIDs, featureIDs);
					FeatureRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.leftChild.rightChild, superCat, ruleBinaryPosPos2, sentence.postagIDs, sentence.postagIDs, featureRuleHeadHeadIDs, featureIDs);
	
					FeatureRuleRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.leftChild.rightChild, superCat.leftChild, superCat, ruleRuleBinaryLeftWordWord, sentence.wordIDs, sentence.wordIDs, featureRuleRuleHeadHeadIDs, featureIDs);
					FeatureRuleRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.leftChild.rightChild, superCat.leftChild, superCat, ruleRuleBinaryLeftWordPos, sentence.wordIDs, sentence.postagIDs, featureRuleRuleHeadHeadIDs, featureIDs);
					FeatureRuleRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.leftChild.rightChild, superCat.leftChild, superCat, ruleRuleBinaryLeftPosWord, sentence.postagIDs, sentence.wordIDs, featureRuleRuleHeadHeadIDs, featureIDs);
					FeatureRuleRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.leftChild.rightChild, superCat.leftChild, superCat, ruleRuleBinaryLeftPosPos, sentence.postagIDs, sentence.postagIDs, featureRuleRuleHeadHeadIDs, featureIDs);
	
				} else {
					FeatureRuleHead.collectFeatures(superCat.leftChild.leftChild, superCat.leftChild.leftChild, superCat, ruleUnaryWord2, sentence.wordIDs, featureRuleHeadIDs, featureIDs);
					FeatureRuleHead.collectFeatures(superCat.leftChild.leftChild, superCat.leftChild.leftChild, superCat, ruleUnaryPos2, sentence.postagIDs, featureRuleHeadIDs, featureIDs);
	
					FeatureRuleRuleHead.collectFeatures(superCat.leftChild.leftChild, superCat.leftChild, superCat, ruleRuleUnaryLeftWord, sentence.wordIDs, featureRuleRuleHeadIDs, featureIDs);
					FeatureRuleRuleHead.collectFeatures(superCat.leftChild.leftChild, superCat.leftChild, superCat, ruleRuleUnaryLeftPos, sentence.postagIDs, featureRuleRuleHeadIDs, featureIDs);
				}
			}
		}
	}

	public void collectBinaryFeatures(SuperCategory superCat, Sentence sentence, ArrayList<Integer> featureIDs) {
		// just the rule:
		FeatureRule.collectFeatures(superCat.leftChild, superCat.rightChild, superCat, ruleBinary, featureRuleIDs, featureIDs);

		// rule + result head:
		FeatureRuleHead.collectFeatures(superCat.leftChild, superCat.rightChild, superCat, ruleBinaryWord, sentence.wordIDs, featureRuleHeadIDs, featureIDs);
		FeatureRuleHead.collectFeatures(superCat.leftChild, superCat.rightChild, superCat, ruleBinaryPos, sentence.postagIDs, featureRuleHeadIDs, featureIDs);

		// rule + combining heads:
		FeatureRuleHeadHead.collectFeatures(superCat.leftChild, superCat.rightChild, superCat, ruleBinaryWordWord, sentence.wordIDs, sentence.wordIDs, featureRuleHeadHeadIDs, featureIDs);
		FeatureRuleHeadHead.collectFeatures(superCat.leftChild, superCat.rightChild, superCat, ruleBinaryWordPos, sentence.wordIDs, sentence.postagIDs, featureRuleHeadHeadIDs, featureIDs);
		FeatureRuleHeadHead.collectFeatures(superCat.leftChild, superCat.rightChild, superCat, ruleBinaryPosWord, sentence.postagIDs, sentence.wordIDs, featureRuleHeadHeadIDs, featureIDs);
		FeatureRuleHeadHead.collectFeatures(superCat.leftChild, superCat.rightChild, superCat, ruleBinaryPosPos, sentence.postagIDs, sentence.postagIDs, featureRuleHeadHeadIDs, featureIDs);

		// rule + result head + distance:
		FeatureRuleHeadDist.collectFeatures(superCat.leftChild, superCat.rightChild, superCat, ruleBinaryWordDistAdj, sentence.wordIDs, sentence.postags, featureRuleHeadDistIDs, featureIDs);
		FeatureRuleHeadDist.collectFeatures(superCat.leftChild, superCat.rightChild, superCat, ruleBinaryWordDistPunct, sentence.wordIDs, sentence.postags, featureRuleHeadDistIDs, featureIDs);
		FeatureRuleHeadDist.collectFeatures(superCat.leftChild, superCat.rightChild, superCat, ruleBinaryWordDistVerb, sentence.wordIDs, sentence.postags, featureRuleHeadDistIDs, featureIDs);
		FeatureRuleHeadDist.collectFeatures(superCat.leftChild, superCat.rightChild, superCat, ruleBinaryPosDistAdj, sentence.wordIDs, sentence.postags, featureRuleHeadDistIDs, featureIDs);
		FeatureRuleHeadDist.collectFeatures(superCat.leftChild, superCat.rightChild, superCat, ruleBinaryPosDistPunct, sentence.wordIDs, sentence.postags, featureRuleHeadDistIDs, featureIDs);
		FeatureRuleHeadDist.collectFeatures(superCat.leftChild, superCat.rightChild, superCat, ruleBinaryPosDistVerb, sentence.wordIDs, sentence.postags, featureRuleHeadDistIDs, featureIDs);

		if ( Features.newFeatures ) {
			if ( superCat.leftChild.leftChild != null ) {
				if ( superCat.leftChild.rightChild != null ) {
					if ( superCat.rightChild.leftChild != null ) {
						if ( superCat.rightChild.rightChild != null ) {
							// case 8: binary, binary, binary
							FeatureRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.rightChild.rightChild, superCat, ruleBinaryBinaryWordWord2, sentence.wordIDs, sentence.wordIDs, featureRuleHeadHeadIDs, featureIDs);
							FeatureRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.rightChild.rightChild, superCat, ruleBinaryBinaryWordPos2, sentence.wordIDs, sentence.postagIDs, featureRuleHeadHeadIDs, featureIDs);
							FeatureRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.rightChild.rightChild, superCat, ruleBinaryBinaryPosWord2, sentence.postagIDs, sentence.wordIDs, featureRuleHeadHeadIDs, featureIDs);
							FeatureRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.rightChild.rightChild, superCat, ruleBinaryBinaryPosPos2, sentence.postagIDs, sentence.postagIDs, featureRuleHeadHeadIDs, featureIDs);
	
							FeatureRuleRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.leftChild.rightChild, superCat.leftChild, superCat, ruleRuleBinaryLeftWordWord, sentence.wordIDs, sentence.wordIDs, featureRuleRuleHeadHeadIDs, featureIDs);
							FeatureRuleRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.leftChild.rightChild, superCat.leftChild, superCat, ruleRuleBinaryLeftWordPos, sentence.wordIDs, sentence.postagIDs, featureRuleRuleHeadHeadIDs, featureIDs);
							FeatureRuleRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.leftChild.rightChild, superCat.leftChild, superCat, ruleRuleBinaryLeftPosWord, sentence.postagIDs, sentence.wordIDs, featureRuleRuleHeadHeadIDs, featureIDs);
							FeatureRuleRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.leftChild.rightChild, superCat.leftChild, superCat, ruleRuleBinaryLeftPosPos, sentence.postagIDs, sentence.postagIDs, featureRuleRuleHeadHeadIDs, featureIDs);
	
							FeatureRuleRuleHeadHead.collectFeatures(superCat.rightChild.leftChild, superCat.rightChild.rightChild, superCat.rightChild, superCat, ruleRuleBinaryRightWordWord, sentence.wordIDs, sentence.wordIDs, featureRuleRuleHeadHeadIDs, featureIDs);
							FeatureRuleRuleHeadHead.collectFeatures(superCat.rightChild.leftChild, superCat.rightChild.rightChild, superCat.rightChild, superCat, ruleRuleBinaryRightWordPos, sentence.wordIDs, sentence.postagIDs, featureRuleRuleHeadHeadIDs, featureIDs);
							FeatureRuleRuleHeadHead.collectFeatures(superCat.rightChild.leftChild, superCat.rightChild.rightChild, superCat.rightChild, superCat, ruleRuleBinaryRightPosWord, sentence.postagIDs, sentence.wordIDs, featureRuleRuleHeadHeadIDs, featureIDs);
							FeatureRuleRuleHeadHead.collectFeatures(superCat.rightChild.leftChild, superCat.rightChild.rightChild, superCat.rightChild, superCat, ruleRuleBinaryRightPosPos, sentence.postagIDs, sentence.postagIDs, featureRuleRuleHeadHeadIDs, featureIDs);
						} else {
							// case 7: binary, binary, unary
							FeatureRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.rightChild.leftChild, superCat, ruleBinaryUnaryWordWord2, sentence.wordIDs, sentence.wordIDs, featureRuleHeadHeadIDs, featureIDs);
							FeatureRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.rightChild.leftChild, superCat, ruleBinaryUnaryWordPos2, sentence.wordIDs, sentence.postagIDs, featureRuleHeadHeadIDs, featureIDs);
							FeatureRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.rightChild.leftChild, superCat, ruleBinaryUnaryPosWord2, sentence.postagIDs, sentence.wordIDs, featureRuleHeadHeadIDs, featureIDs);
							FeatureRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.rightChild.leftChild, superCat, ruleBinaryUnaryPosPos2, sentence.postagIDs, sentence.postagIDs, featureRuleHeadHeadIDs, featureIDs);
	
							FeatureRuleRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.leftChild.rightChild, superCat.leftChild, superCat, ruleRuleBinaryLeftWordWord, sentence.wordIDs, sentence.wordIDs, featureRuleRuleHeadHeadIDs, featureIDs);
							FeatureRuleRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.leftChild.rightChild, superCat.leftChild, superCat, ruleRuleBinaryLeftWordPos, sentence.wordIDs, sentence.postagIDs, featureRuleRuleHeadHeadIDs, featureIDs);
							FeatureRuleRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.leftChild.rightChild, superCat.leftChild, superCat, ruleRuleBinaryLeftPosWord, sentence.postagIDs, sentence.wordIDs, featureRuleRuleHeadHeadIDs, featureIDs);
							FeatureRuleRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.leftChild.rightChild, superCat.leftChild, superCat, ruleRuleBinaryLeftPosPos, sentence.postagIDs, sentence.postagIDs, featureRuleRuleHeadHeadIDs, featureIDs);
	
							FeatureRuleRuleHead.collectFeatures(superCat.rightChild.leftChild, superCat.rightChild, superCat, ruleRuleUnaryRightWord, sentence.wordIDs, featureRuleRuleHeadIDs, featureIDs);
							FeatureRuleRuleHead.collectFeatures(superCat.rightChild.leftChild, superCat.rightChild, superCat, ruleRuleUnaryRightPos, sentence.postagIDs, featureRuleRuleHeadIDs, featureIDs);
						}
					} else {
						// case 3: binary, binary, null
						FeatureRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.rightChild, superCat, ruleBinaryNullWordWord2, sentence.wordIDs, sentence.wordIDs, featureRuleHeadHeadIDs, featureIDs);
						FeatureRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.rightChild, superCat, ruleBinaryNullWordPos2, sentence.wordIDs, sentence.postagIDs, featureRuleHeadHeadIDs, featureIDs);
						FeatureRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.rightChild, superCat, ruleBinaryNullPosWord2, sentence.postagIDs, sentence.wordIDs, featureRuleHeadHeadIDs, featureIDs);
						FeatureRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.rightChild, superCat, ruleBinaryNullPosPos2, sentence.postagIDs, sentence.postagIDs, featureRuleHeadHeadIDs, featureIDs);
	
						FeatureRuleRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.leftChild.rightChild, superCat.leftChild, superCat, ruleRuleBinaryLeftWordWord, sentence.wordIDs, sentence.wordIDs, featureRuleRuleHeadHeadIDs, featureIDs);
						FeatureRuleRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.leftChild.rightChild, superCat.leftChild, superCat, ruleRuleBinaryLeftWordPos, sentence.wordIDs, sentence.postagIDs, featureRuleRuleHeadHeadIDs, featureIDs);
						FeatureRuleRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.leftChild.rightChild, superCat.leftChild, superCat, ruleRuleBinaryLeftPosWord, sentence.postagIDs, sentence.wordIDs, featureRuleRuleHeadHeadIDs, featureIDs);
						FeatureRuleRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.leftChild.rightChild, superCat.leftChild, superCat, ruleRuleBinaryLeftPosPos, sentence.postagIDs, sentence.postagIDs, featureRuleRuleHeadHeadIDs, featureIDs);
					}
				} else {
					if ( superCat.rightChild.leftChild != null ) {
						if ( superCat.rightChild.rightChild != null ) {
							// case 6: binary, unary, binary
							FeatureRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.rightChild.rightChild, superCat, ruleUnaryBinaryWordWord2, sentence.wordIDs, sentence.wordIDs, featureRuleHeadHeadIDs, featureIDs);
							FeatureRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.rightChild.rightChild, superCat, ruleUnaryBinaryWordPos2, sentence.wordIDs, sentence.postagIDs, featureRuleHeadHeadIDs, featureIDs);
							FeatureRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.rightChild.rightChild, superCat, ruleUnaryBinaryPosWord2, sentence.postagIDs, sentence.wordIDs, featureRuleHeadHeadIDs, featureIDs);
							FeatureRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.rightChild.rightChild, superCat, ruleUnaryBinaryPosPos2, sentence.postagIDs, sentence.postagIDs, featureRuleHeadHeadIDs, featureIDs);
	
							FeatureRuleRuleHead.collectFeatures(superCat.leftChild.leftChild, superCat.leftChild, superCat, ruleRuleUnaryLeftWord, sentence.wordIDs, featureRuleRuleHeadIDs, featureIDs);
							FeatureRuleRuleHead.collectFeatures(superCat.leftChild.leftChild, superCat.leftChild, superCat, ruleRuleUnaryLeftPos, sentence.postagIDs, featureRuleRuleHeadIDs, featureIDs);
	
							FeatureRuleRuleHeadHead.collectFeatures(superCat.rightChild.leftChild, superCat.rightChild.rightChild, superCat.rightChild, superCat, ruleRuleBinaryRightWordWord, sentence.wordIDs, sentence.wordIDs, featureRuleRuleHeadHeadIDs, featureIDs);
							FeatureRuleRuleHeadHead.collectFeatures(superCat.rightChild.leftChild, superCat.rightChild.rightChild, superCat.rightChild, superCat, ruleRuleBinaryRightWordPos, sentence.wordIDs, sentence.postagIDs, featureRuleRuleHeadHeadIDs, featureIDs);
							FeatureRuleRuleHeadHead.collectFeatures(superCat.rightChild.leftChild, superCat.rightChild.rightChild, superCat.rightChild, superCat, ruleRuleBinaryRightPosWord, sentence.postagIDs, sentence.wordIDs, featureRuleRuleHeadHeadIDs, featureIDs);
							FeatureRuleRuleHeadHead.collectFeatures(superCat.rightChild.leftChild, superCat.rightChild.rightChild, superCat.rightChild, superCat, ruleRuleBinaryRightPosPos, sentence.postagIDs, sentence.postagIDs, featureRuleRuleHeadHeadIDs, featureIDs);
						} else {
							// case 5: binary, unary, unary
							FeatureRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.rightChild.leftChild, superCat, ruleUnaryUnaryWordWord2, sentence.wordIDs, sentence.wordIDs, featureRuleHeadHeadIDs, featureIDs);
							FeatureRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.rightChild.leftChild, superCat, ruleUnaryUnaryWordPos2, sentence.wordIDs, sentence.postagIDs, featureRuleHeadHeadIDs, featureIDs);
							FeatureRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.rightChild.leftChild, superCat, ruleUnaryUnaryPosWord2, sentence.postagIDs, sentence.wordIDs, featureRuleHeadHeadIDs, featureIDs);
							FeatureRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.rightChild.leftChild, superCat, ruleUnaryUnaryPosPos2, sentence.postagIDs, sentence.postagIDs, featureRuleHeadHeadIDs, featureIDs);
	
							FeatureRuleRuleHead.collectFeatures(superCat.leftChild.leftChild, superCat.leftChild, superCat, ruleRuleUnaryLeftWord, sentence.wordIDs, featureRuleRuleHeadIDs, featureIDs);
							FeatureRuleRuleHead.collectFeatures(superCat.leftChild.leftChild, superCat.leftChild, superCat, ruleRuleUnaryLeftPos, sentence.postagIDs, featureRuleRuleHeadIDs, featureIDs);
	
							FeatureRuleRuleHead.collectFeatures(superCat.rightChild.leftChild, superCat.rightChild, superCat, ruleRuleUnaryRightWord, sentence.wordIDs, featureRuleRuleHeadIDs, featureIDs);
							FeatureRuleRuleHead.collectFeatures(superCat.rightChild.leftChild, superCat.rightChild, superCat, ruleRuleUnaryRightPos, sentence.postagIDs, featureRuleRuleHeadIDs, featureIDs);
						}
					} else {
						// case 1: binary, unary, null
						FeatureRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.rightChild, superCat, ruleUnaryNullWordWord2, sentence.wordIDs, sentence.wordIDs, featureRuleHeadHeadIDs, featureIDs);
						FeatureRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.rightChild, superCat, ruleUnaryNullWordPos2, sentence.wordIDs, sentence.postagIDs, featureRuleHeadHeadIDs, featureIDs);
						FeatureRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.rightChild, superCat, ruleUnaryNullPosWord2, sentence.postagIDs, sentence.wordIDs, featureRuleHeadHeadIDs, featureIDs);
						FeatureRuleHeadHead.collectFeatures(superCat.leftChild.leftChild, superCat.rightChild, superCat, ruleUnaryNullPosPos2, sentence.postagIDs, sentence.postagIDs, featureRuleHeadHeadIDs, featureIDs);
	
						FeatureRuleRuleHead.collectFeatures(superCat.leftChild.leftChild, superCat.leftChild, superCat, ruleRuleUnaryLeftWord, sentence.wordIDs, featureRuleRuleHeadIDs, featureIDs);
						FeatureRuleRuleHead.collectFeatures(superCat.leftChild.leftChild, superCat.leftChild, superCat, ruleRuleUnaryLeftPos, sentence.postagIDs, featureRuleRuleHeadIDs, featureIDs);
					}
				}
			} else {
				if ( superCat.rightChild.leftChild != null ) {
					if ( superCat.rightChild.rightChild != null ) {
						// case 4: binary, null, binary
						FeatureRuleHeadHead.collectFeatures(superCat.leftChild, superCat.rightChild.rightChild, superCat, ruleNullBinaryWordWord2, sentence.wordIDs, sentence.wordIDs, featureRuleHeadHeadIDs, featureIDs);
						FeatureRuleHeadHead.collectFeatures(superCat.leftChild, superCat.rightChild.rightChild, superCat, ruleNullBinaryWordPos2, sentence.wordIDs, sentence.postagIDs, featureRuleHeadHeadIDs, featureIDs);
						FeatureRuleHeadHead.collectFeatures(superCat.leftChild, superCat.rightChild.rightChild, superCat, ruleNullBinaryPosWord2, sentence.postagIDs, sentence.wordIDs, featureRuleHeadHeadIDs, featureIDs);
						FeatureRuleHeadHead.collectFeatures(superCat.leftChild, superCat.rightChild.rightChild, superCat, ruleNullBinaryPosPos2, sentence.postagIDs, sentence.postagIDs, featureRuleHeadHeadIDs, featureIDs);
	
						FeatureRuleRuleHeadHead.collectFeatures(superCat.rightChild.leftChild, superCat.rightChild.rightChild, superCat.rightChild, superCat, ruleRuleBinaryRightWordWord, sentence.wordIDs, sentence.wordIDs, featureRuleRuleHeadHeadIDs, featureIDs);
						FeatureRuleRuleHeadHead.collectFeatures(superCat.rightChild.leftChild, superCat.rightChild.rightChild, superCat.rightChild, superCat, ruleRuleBinaryRightWordPos, sentence.wordIDs, sentence.postagIDs, featureRuleRuleHeadHeadIDs, featureIDs);
						FeatureRuleRuleHeadHead.collectFeatures(superCat.rightChild.leftChild, superCat.rightChild.rightChild, superCat.rightChild, superCat, ruleRuleBinaryRightPosWord, sentence.postagIDs, sentence.wordIDs, featureRuleRuleHeadHeadIDs, featureIDs);
						FeatureRuleRuleHeadHead.collectFeatures(superCat.rightChild.leftChild, superCat.rightChild.rightChild, superCat.rightChild, superCat, ruleRuleBinaryRightPosPos, sentence.postagIDs, sentence.postagIDs, featureRuleRuleHeadHeadIDs, featureIDs);
					} else {
						// case 2: binary, null, unary
						FeatureRuleHeadHead.collectFeatures(superCat.leftChild, superCat.rightChild.leftChild, superCat, ruleNullUnaryWordWord2, sentence.wordIDs, sentence.wordIDs, featureRuleHeadHeadIDs, featureIDs);
						FeatureRuleHeadHead.collectFeatures(superCat.leftChild, superCat.rightChild.leftChild, superCat, ruleNullUnaryWordPos2, sentence.wordIDs, sentence.postagIDs, featureRuleHeadHeadIDs, featureIDs);
						FeatureRuleHeadHead.collectFeatures(superCat.leftChild, superCat.rightChild.leftChild, superCat, ruleNullUnaryPosWord2, sentence.postagIDs, sentence.wordIDs, featureRuleHeadHeadIDs, featureIDs);
						FeatureRuleHeadHead.collectFeatures(superCat.leftChild, superCat.rightChild.leftChild, superCat, ruleNullUnaryPosPos2, sentence.postagIDs, sentence.postagIDs, featureRuleHeadHeadIDs, featureIDs);
	
						FeatureRuleRuleHead.collectFeatures(superCat.rightChild.leftChild, superCat.rightChild, superCat, ruleRuleUnaryRightWord, sentence.wordIDs, featureRuleRuleHeadIDs, featureIDs);
						FeatureRuleRuleHead.collectFeatures(superCat.rightChild.leftChild, superCat.rightChild, superCat, ruleRuleUnaryRightPos, sentence.postagIDs, featureRuleRuleHeadIDs, featureIDs);
					}
				}
			}
		}
	}

	public void collectRootFeatures(SuperCategory superCat, Sentence sentence, ArrayList<Integer> featureIDs) {
		FeatureCat.collectFeatures(superCat, catRoot, featureCatIDs, featureIDs);
		FeatureCatHead.collectFeatures(superCat, catRootWord, sentence.wordIDs, featureCatHeadIDs, featureIDs);
		FeatureCatHead.collectFeatures(superCat, catRootPos, sentence.postagIDs, featureCatHeadIDs, featureIDs);
	}

	public void print(PrintWriter out) {
		featureCatIDs.print(out);
		featureCatHeadIDs.print(out);
		featureRuleIDs.print(out);
		featureRuleHeadIDs.print(out);
		featureRuleHeadHeadIDs.print(out);
		featureRuleHeadDistIDs.print(out);
		featureRuleRuleHeadIDs.print(out);
		featureRuleRuleHeadHeadIDs.print(out);
	}

	public void printWeights(Weights weights, PrintWriter out) {
		featureCatIDs.printWeights(weights, out);
		featureCatHeadIDs.printWeights(weights, out);
		featureRuleIDs.printWeights(weights, out);
		featureRuleHeadIDs.printWeights(weights, out);
		featureRuleHeadHeadIDs.printWeights(weights, out);
		featureRuleHeadDistIDs.printWeights(weights, out);
		featureRuleRuleHeadIDs.printWeights(weights, out);
		featureRuleRuleHeadHeadIDs.printWeights(weights, out);
	}
}
