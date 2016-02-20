import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cat_combination.RuleInstancesParams;
import chart_parser.ChartParserBeam;
import chart_parser.CountFeaturesDecoder;
import io.Preface;
import io.Sentences;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import model.Lexicon;

public class CountFeatures {
	public static void main(String[] args) {
		int MAX_WORDS = 150;
		int MAX_SUPERCATS = 500000;

		boolean altMarkedup = false;
		boolean eisnerNormalForm = true;
		boolean newFeatures = true;
		boolean cubePruning = false;

		String grammarDir = "grammar";
		String lexiconFile = "words_feats/wsj02-21.wordsPos";
		String featuresFile = "words_feats/wsj02-21.feats.1-22";

		RuleInstancesParams ruleInstancesParams = new RuleInstancesParams(true, false, false, false, false, false, grammarDir);

		double[] betas = { 0.0001 };

		int beamSize = 32;
		double beta = Double.NEGATIVE_INFINITY;

		OptionParser optionParser = new OptionParser();
		optionParser.accepts("help").forHelp();
		optionParser.accepts("verbose");
		optionParser.accepts("input").withRequiredArg().ofType(String.class).required();
		optionParser.accepts("output").withRequiredArg().ofType(String.class).required();
		optionParser.accepts("outputWeights").withRequiredArg().ofType(String.class).required();
		optionParser.accepts("log").withRequiredArg().ofType(String.class).required();
		optionParser.accepts("weights").withRequiredArg().ofType(String.class).required();
		optionParser.accepts("from").withRequiredArg().ofType(Integer.class).defaultsTo(1);
		optionParser.accepts("to").withRequiredArg().ofType(Integer.class).defaultsTo(Integer.MAX_VALUE);

		OptionSet options = null;

		try {
			options = optionParser.parse(args);
		} catch ( OptionException e ) {
			System.err.println(e.getMessage());
			return;
		}

		try {
			if ( options.has("help") ) {
				optionParser.printHelpOn(System.out);
				return;
			}
		} catch ( IOException e ) {
			System.err.println(e);
			return;
		}

		if ( options.has("verbose") ) {
			System.setProperty("logLevel", "trace");
		} else {
			System.setProperty("logLevel", "info");
		}

		String inputFile = (String) options.valueOf("input");
		String outputFile = (String) options.valueOf("output");
		String outputWeightsFile = (String) options.valueOf("outputWeights");
		String logFile = (String) options.valueOf("log");
		String weightsFile = (String) options.valueOf("weights");
		int fromSentence = (Integer) options.valueOf("from");
		int toSentence = (Integer) options.valueOf("to");

		System.setProperty("logFile", logFile);
		final Logger logger = LogManager.getLogger(CountFeatures.class);

		Lexicon lexicon = null;
		ChartParserBeam parser = null;

		try {
			lexicon = new Lexicon(lexiconFile);
			parser = new ChartParserBeam(grammarDir, altMarkedup,
					eisnerNormalForm, MAX_WORDS, MAX_SUPERCATS,
					ruleInstancesParams, lexicon, featuresFile, weightsFile,
					newFeatures, false, cubePruning, beamSize, beta);
		} catch ( IOException e ) {
			logger.error(e);
			return;
		}

		CountFeaturesDecoder countFeaturesDecoder = new CountFeaturesDecoder(parser.categories);

		try ( BufferedReader in = new BufferedReader(new FileReader(inputFile));
				PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));
				PrintWriter weights = new PrintWriter(new BufferedWriter(new FileWriter(outputWeightsFile))) ) {

			Preface.readPreface(in);
			Preface.printPreface(out);
			Preface.printPreface(weights);

			Sentences sentences = new Sentences(in, null, parser.categories, MAX_WORDS);
			sentences.skip(fromSentence - 1);

			for ( int numSentence = fromSentence; numSentence <= toSentence && sentences.hasNext(); numSentence++ ) {
				logger.info("Parsing sentence " + numSentence);

				parser.parseSentence(sentences.next(), betas);

				if ( !parser.maxWordsExceeded && !parser.maxSuperCatsExceeded ) {
					boolean success = countFeaturesDecoder.countFeatures(parser.chart, parser.sentence);

					if ( success ) {
						logger.info("Success.");
					} else {
						logger.info("No root category.");
					}
				}
			}

			countFeaturesDecoder.mergeAllFeatureCounts(parser.features);
			parser.features.print(out);
			parser.features.printWeights(parser.weights, weights);
		} catch ( FileNotFoundException e ) {
			logger.error(e);
		} catch ( IOException e ) {
			logger.error(e);
		}
	}
}
