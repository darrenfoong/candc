package io;

import java.util.List;
import java.util.Map;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

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

	public static double[] betasArray(String listString) {
		String[] list = listString.split(",");
		double[] array = new double[list.length];

		for ( int i = 0; i < list.length; i++ ) {
			array[i] = Double.parseDouble(list[i]);
		}

		return array;
	}

	public static String printOptions(OptionSet options) {
		StringBuilder outputBuilder = new StringBuilder("Parameters:\n");

		for ( Map.Entry<OptionSpec<?>, List<?>> entry: options.asMap().entrySet() ) {
			if ( !entry.getValue().isEmpty() ) {
				String optionString = entry.getKey().options().get(0);
				String argumentString = entry.getValue().get(0).toString();
				outputBuilder.append(optionString);
				outputBuilder.append(": ");
				outputBuilder.append(argumentString);
				outputBuilder.append("\n");
			}
		}

		return outputBuilder.toString();
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
		optionParser.accepts("printChartDeps").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
		optionParser.accepts("printChartFeatures").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
		optionParser.accepts("betas").withRequiredArg().ofType(String.class).defaultsTo("0.01,0.01,0.01,0.03,0.075");

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
		optionParser.accepts("betas").withRequiredArg().ofType(String.class).defaultsTo("0.0001");
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
		optionParser.accepts("depnn");
		optionParser.accepts("modelDir").requiredIf("depnn").withRequiredArg().ofType(String.class);
		optionParser.accepts("nnPosThres").requiredIf("depnn").withRequiredArg().ofType(Double.class).defaultsTo(0.5);
		optionParser.accepts("nnNegThres").requiredIf("depnn").withRequiredArg().ofType(Double.class).defaultsTo(0.5);
		optionParser.accepts("parallelUpdate").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
		optionParser.accepts("updateLogP").withRequiredArg().ofType(Boolean.class).defaultsTo(true);
		optionParser.accepts("updateDepNN").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
		optionParser.accepts("betas").withRequiredArg().ofType(String.class).defaultsTo("0.0001");
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
		optionParser.accepts("betas").withRequiredArg().ofType(String.class).defaultsTo("0.0001,0.001,0.01,0.03,0.075");

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
		optionParser.accepts("newFeatures").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
		optionParser.accepts("cubePruning").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
		optionParser.accepts("compactWeights").withRequiredArg().ofType(Boolean.class).defaultsTo(true);
		optionParser.accepts("depnn");
		optionParser.accepts("modelDir").requiredIf("depnn").withRequiredArg().ofType(String.class);
		optionParser.accepts("nnPosThres").requiredIf("depnn").withRequiredArg().ofType(Double.class).defaultsTo(0.5);
		optionParser.accepts("nnNegThres").requiredIf("depnn").withRequiredArg().ofType(Double.class).defaultsTo(0.5);
		optionParser.accepts("skimmer").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
		optionParser.accepts("printChartDeps").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
		optionParser.accepts("printChartFeatures").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
		optionParser.accepts("betas").withRequiredArg().ofType(String.class).defaultsTo("0.0001");
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
		optionParser.accepts("betas").withRequiredArg().ofType(String.class).defaultsTo("0.0001,0.0001,0.0001,0.01,0.1");

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
