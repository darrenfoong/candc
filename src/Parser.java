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
import chart_parser.ChartParser;
import chart_parser.ViterbiDecoder;
import io.Preface;
import io.Sentences;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import model.Lexicon;

public class Parser {
	public static void main(String[] args) {
		int MAX_WORDS = 250;
		int MAX_SUPERCATS = 300000;

		boolean altMarkedup = false;
		boolean eisnerNormalForm = true;
		boolean newFeatures = false;
		boolean compactWeights = true;
		boolean oracleFscore = false;

		String grammarDir = "grammar";
		String lexiconFile = "words_feats/wsj02-21.wordsPos";
		String featuresFile = "words_feats/wsj02-21.feats.1-22";

		RuleInstancesParams ruleInstancesParams = new RuleInstancesParams(true, false, false, false, false, false, grammarDir);

		boolean adaptiveSupertagging = false;
		double[] betas = { 0.0001, 0.001, 0.01, 0.03, 0.075 };

		OptionParser optionParser = new OptionParser();
		optionParser.accepts("help").forHelp();
		optionParser.accepts("verbose");
		optionParser.accepts("input").withRequiredArg().ofType(String.class).required();
		optionParser.accepts("output").withRequiredArg().ofType(String.class).required();
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
		String logFile = (String) options.valueOf("log");
		String weightsFile = (String) options.valueOf("weights");
		int fromSentence = (Integer) options.valueOf("from");
		int toSentence = (Integer) options.valueOf("to");

		System.setProperty("logFile", logFile);
		final Logger logger = LogManager.getLogger(Parser.class);

		Lexicon lexicon = null;
		ChartParser parser = null;

		try {
			lexicon = new Lexicon(lexiconFile);
			parser = new ChartParser(grammarDir, altMarkedup,
					eisnerNormalForm, MAX_WORDS, MAX_SUPERCATS,
					oracleFscore, adaptiveSupertagging, ruleInstancesParams,
					lexicon, featuresFile, weightsFile, newFeatures, compactWeights);
		} catch ( IOException e ) {
			logger.error(e);
			return;
		}

		ViterbiDecoder viterbiDecoder = new ViterbiDecoder();

		try ( BufferedReader in = new BufferedReader(new FileReader(inputFile));
				PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile))) ) {

			Preface.readPreface(in);
			Preface.printPreface(out);

			Sentences sentences = new Sentences(in, null, parser.categories, MAX_WORDS);
			sentences.skip(fromSentence - 1);

			for ( int numSentence = fromSentence; numSentence <= toSentence && sentences.hasNext(); numSentence++ ) {
				logger.info("Parsing sentence " + numSentence);

				parser.parseSentence(sentences.next(),  betas);

				if ( !parser.maxWordsExceeded && !parser.maxSuperCatsExceeded ) {
					boolean success = parser.calcScores();

					if ( success ) {
						viterbiDecoder.decode(parser.chart, parser.sentence);
						viterbiDecoder.print(out, parser.categories.dependencyRelations, parser.sentence);

						parser.sentence.printC_line(out);
					} else {
						logger.info("No root category.");
					}
				}

				out.println();
			}
		} catch ( FileNotFoundException e ) {
			logger.error(e);
		} catch ( IOException e ) {
			logger.error(e);
		}
	}
}
