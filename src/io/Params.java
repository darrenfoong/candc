package io;

import joptsimple.OptionParser;

public class Params {
	public static OptionParser getBaseOptionParser() {
		OptionParser optionParser = new OptionParser();
		optionParser.accepts("help").forHelp();
		optionParser.accepts("verbose");

		return optionParser;
	}

	public static void addDirs(OptionParser optionParser) {
		optionParser.accepts("grammarDir").withRequiredArg().ofType(String.class).defaultsTo("grammar");
		optionParser.accepts("lexiconFile").withRequiredArg().ofType(String.class).defaultsTo("words_feats/wsj02-21.wordsPos");
		optionParser.accepts("featuresFile").withRequiredArg().ofType(String.class).defaultsTo("words_feats/wsj02-21.feats.1-22");
	}

	public static void addFromTo(OptionParser optionParser) {
		optionParser.accepts("from").withRequiredArg().ofType(Integer.class).defaultsTo(1);
		optionParser.accepts("to").withRequiredArg().ofType(Integer.class).defaultsTo(Integer.MAX_VALUE);
	}

	public static void addBeamBeta(OptionParser optionParser) {
		optionParser.accepts("beamSize").withRequiredArg().ofType(Integer.class).defaultsTo(32);
		optionParser.accepts("beta").withRequiredArg().ofType(Double.class).defaultsTo(Double.NEGATIVE_INFINITY);
	}

	public static OptionParser getOracleParserOptionParser() {
		OptionParser optionParser = getBaseOptionParser();

		optionParser.accepts("maxWords").withRequiredArg().ofType(Integer.class).defaultsTo(250);
		optionParser.accepts("maxSupercats").withRequiredArg().ofType(Integer.class).defaultsTo(2000000);

		optionParser.accepts("grammarDir").withRequiredArg().ofType(String.class).defaultsTo("grammar");

		optionParser.accepts("altMarkedup").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
		optionParser.accepts("eisnerNormalForm").withRequiredArg().ofType(Boolean.class).defaultsTo(true);
		optionParser.accepts("adaptiveSupertagging").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
		optionParser.accepts("depsSumDecoder").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
		optionParser.accepts("extractRuleInstances").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
		optionParser.accepts("oracleRuleInstancesFile").withRequiredArg().ofType(String.class).defaultsTo("");
		optionParser.accepts("training").withRequiredArg().ofType(Boolean.class).defaultsTo(true);

		optionParser.accepts("input").withRequiredArg().ofType(String.class).required();
		optionParser.accepts("goldSupertags").withRequiredArg().ofType(String.class).defaultsTo("null");
		optionParser.accepts("output").withRequiredArg().ofType(String.class).required();
		optionParser.accepts("log").withRequiredArg().ofType(String.class).required();
		optionParser.accepts("goldDeps").withRequiredArg().ofType(String.class).required();
		optionParser.accepts("rootCats").withRequiredArg().ofType(String.class).defaultsTo("null");
		addFromTo(optionParser);

		return optionParser;
	}

	public static OptionParser getCountFeaturesOptionParser() {
		OptionParser optionParser = getBaseOptionParser();

		optionParser.accepts("maxWords").withRequiredArg().ofType(Integer.class).defaultsTo(150);
		optionParser.accepts("maxSupercats").withRequiredArg().ofType(Integer.class).defaultsTo(500000);

		addDirs(optionParser);

		optionParser.accepts("altMarkedup").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
		optionParser.accepts("eisnerNormalForm").withRequiredArg().ofType(Boolean.class).defaultsTo(true);
		optionParser.accepts("newFeatures").withRequiredArg().ofType(Boolean.class).defaultsTo(true);
		optionParser.accepts("cubePruning").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
		addBeamBeta(optionParser);

		optionParser.accepts("input").withRequiredArg().ofType(String.class).required();
		optionParser.accepts("output").withRequiredArg().ofType(String.class).required();
		optionParser.accepts("outputWeights").withRequiredArg().ofType(String.class).required();
		optionParser.accepts("log").withRequiredArg().ofType(String.class).required();
		optionParser.accepts("weights").withRequiredArg().ofType(String.class).required();
		addFromTo(optionParser);

		return optionParser;
	}

	public static OptionParser getTrainParserBeamOptionParser() {
		OptionParser optionParser = getBaseOptionParser();

		optionParser.accepts("maxWords").withRequiredArg().ofType(Integer.class).defaultsTo(150);
		optionParser.accepts("maxSupercats").withRequiredArg().ofType(Integer.class).defaultsTo(500000);

		addDirs(optionParser);

		optionParser.accepts("altMarkedup").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
		optionParser.accepts("eisnerNormalForm").withRequiredArg().ofType(Boolean.class).defaultsTo(true);
		optionParser.accepts("newFeatures").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
		optionParser.accepts("cubePruning").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
		optionParser.accepts("parallelUpdate").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
		optionParser.accepts("updateLogP").withRequiredArg().ofType(Boolean.class).defaultsTo(true);
		addBeamBeta(optionParser);

		optionParser.accepts("input").withRequiredArg().ofType(String.class).required();
		optionParser.accepts("outputWeights").withRequiredArg().ofType(String.class).required();
		optionParser.accepts("log").withRequiredArg().ofType(String.class).required();
		optionParser.accepts("weights").withRequiredArg().ofType(String.class).required();
		optionParser.accepts("goldDeps").withRequiredArg().ofType(String.class).required();
		optionParser.accepts("rootCats").withRequiredArg().ofType(String.class).required();
		optionParser.accepts("numIterations").withRequiredArg().ofType(Integer.class).required();
		addFromTo(optionParser);

		return optionParser;
	}

	public static OptionParser getParserOptionParser() {
		OptionParser optionParser = getBaseOptionParser();

		optionParser.accepts("maxWords").withRequiredArg().ofType(Integer.class).defaultsTo(250);
		optionParser.accepts("maxSupercats").withRequiredArg().ofType(Integer.class).defaultsTo(300000);

		addDirs(optionParser);

		optionParser.accepts("altMarkedup").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
		optionParser.accepts("eisnerNormalForm").withRequiredArg().ofType(Boolean.class).defaultsTo(true);
		optionParser.accepts("compactWeights").withRequiredArg().ofType(Boolean.class).defaultsTo(true);
		optionParser.accepts("oracleFscore").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
		optionParser.accepts("adaptiveSupertagging").withRequiredArg().ofType(Boolean.class).defaultsTo(false);

		optionParser.accepts("input").withRequiredArg().ofType(String.class).required();
		optionParser.accepts("output").withRequiredArg().ofType(String.class).required();
		optionParser.accepts("log").withRequiredArg().ofType(String.class).required();
		optionParser.accepts("weights").withRequiredArg().ofType(String.class).required();
		addFromTo(optionParser);

		return optionParser;
	}

	public static OptionParser getParserBeamOptionParser() {
		OptionParser optionParser = getBaseOptionParser();

		optionParser.accepts("maxWords").withRequiredArg().ofType(Integer.class).defaultsTo(150);
		optionParser.accepts("maxSupercats").withRequiredArg().ofType(Integer.class).defaultsTo(500000);

		addDirs(optionParser);

		optionParser.accepts("altMarkedup").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
		optionParser.accepts("eisnerNormalForm").withRequiredArg().ofType(Boolean.class).defaultsTo(true);
		optionParser.accepts("newFeatures").withRequiredArg().ofType(Boolean.class).defaultsTo(true);
		optionParser.accepts("cubePruning").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
		optionParser.accepts("compactWeights").withRequiredArg().ofType(Boolean.class).defaultsTo(true);
		optionParser.accepts("skimmer").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
		optionParser.accepts("printChartDeps").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
		addBeamBeta(optionParser);

		optionParser.accepts("input").withRequiredArg().ofType(String.class).required();
		optionParser.accepts("output").withRequiredArg().ofType(String.class).required();
		optionParser.accepts("log").withRequiredArg().ofType(String.class).required();
		optionParser.accepts("weights").withRequiredArg().ofType(String.class).required();
		addFromTo(optionParser);

		return optionParser;
	}

	public static OptionParser getPrintForestsOptionParser() {
		OptionParser optionParser = getBaseOptionParser();

		optionParser.accepts("maxWords").withRequiredArg().ofType(Integer.class).defaultsTo(250);
		optionParser.accepts("maxSupercats").withRequiredArg().ofType(Integer.class).defaultsTo(1000000);

		addDirs(optionParser);

		optionParser.accepts("altMarkedup").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
		optionParser.accepts("eisnerNormalForm").withRequiredArg().ofType(Boolean.class).defaultsTo(true);
		optionParser.accepts("oracleFscore").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
		optionParser.accepts("adaptiveSupertagging").withRequiredArg().ofType(Boolean.class).defaultsTo(false);

		optionParser.accepts("input").withRequiredArg().ofType(String.class).required();
		optionParser.accepts("goldSupertags").withRequiredArg().ofType(String.class).required();
		optionParser.accepts("output").withRequiredArg().ofType(String.class).required();
		optionParser.accepts("log").withRequiredArg().ofType(String.class).required();
		optionParser.accepts("goldDeps").withRequiredArg().ofType(String.class).required();
		optionParser.accepts("rootCats").withRequiredArg().ofType(String.class).required();
		addFromTo(optionParser);

		return optionParser;
	}

	public static OptionParser getTrainViterbiOptionParser() {
		OptionParser optionParser = getBaseOptionParser();

		optionParser.accepts("forest").withRequiredArg().ofType(String.class).required();
		optionParser.accepts("weights").withRequiredArg().ofType(String.class).required();
		optionParser.accepts("log").withRequiredArg().ofType(String.class).required();
		optionParser.accepts("numIterations").withRequiredArg().ofType(Integer.class).required();
		addFromTo(optionParser);

		return optionParser;
	}

	public static OptionParser getTrainLogLinearOptionParser() {
		OptionParser optionParser = getTrainViterbiOptionParser();

		optionParser.accepts("learningRate").withRequiredArg().ofType(Double.class).defaultsTo(0.5);

		return optionParser;
	}
}
