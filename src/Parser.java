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
import io.Params;
import io.Preface;
import io.Sentences;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import model.Lexicon;

public class Parser {
	public static void main(String[] args) {
		OptionParser optionParser = Params.getParserOptionParser();
		OptionSet options = null;

		try {
			options = optionParser.parse(args);
			if ( options.has("help") ) {
				optionParser.printHelpOn(System.out);
				return;
			}
		} catch ( OptionException e ) {
			System.err.println(e.getMessage());
			return;
		} catch ( IOException e ) {
			System.err.println(e);
			return;
		}

		int MAX_WORDS = (Integer) options.valueOf("maxWords");
		int MAX_SUPERCATS = (Integer) options.valueOf("maxSupercats");

		String grammarDir = (String) options.valueOf("grammarDir");
		String lexiconFile = (String) options.valueOf("lexiconFile");
		String featuresFile = (String) options.valueOf("featuresFile");

		RuleInstancesParams ruleInstancesParams = new RuleInstancesParams(true, false, false, false, false, false, grammarDir);

		boolean altMarkedup = (Boolean) options.valueOf("altMarkedup");
		boolean eisnerNormalForm = (Boolean) options.valueOf("eisnerNormalForm");
		boolean compactWeights = (Boolean) options.valueOf("compactWeights");
		boolean oracleFscore = (Boolean) options.valueOf("oracleFscore");
		boolean adaptiveSupertagging = (Boolean) options.valueOf("adaptiveSupertagging");
		double[] betas = Params.betasArray((String) options.valueOf("betas"));

		String inputFile = (String) options.valueOf("input");
		String outputFile = (String) options.valueOf("output");
		String logFile = (String) options.valueOf("log");
		String weightsFile = (String) options.valueOf("weights");
		int fromSentence = (Integer) options.valueOf("from");
		int toSentence = (Integer) options.valueOf("to");

		System.setProperty("logLevel", options.has("verbose") ? "trace" : "info");
		System.setProperty("logFile", logFile);
		final Logger logger = LogManager.getLogger(Parser.class);

		logger.info(Params.printOptions(options));

		Lexicon lexicon = null;
		ChartParser parser = null;

		try {
			lexicon = new Lexicon(lexiconFile);
			parser = new ChartParser(grammarDir, altMarkedup,
					eisnerNormalForm, MAX_WORDS, MAX_SUPERCATS,
					oracleFscore, adaptiveSupertagging, ruleInstancesParams,
					lexicon, featuresFile, weightsFile, false, compactWeights);
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
