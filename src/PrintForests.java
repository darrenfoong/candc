import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cat_combination.RuleInstancesParams;
import chart_parser.ChartParser;
import chart_parser.OracleDepsSumDecoder;
import io.Params;
import io.Preface;
import io.Sentences;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import model.Lexicon;
import training.PrintForest;

public class PrintForests {
	public static void main(String[] args) {
		double[] betas = { 0.0001, 0.0001, 0.0001, 0.01, 0.1 };

		OptionParser optionParser = Params.getPrintForestsOptionParser();
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
		boolean oracleFscore = (Boolean) options.valueOf("oracleFscore");
		boolean adaptiveSupertagging = (Boolean) options.valueOf("adaptiveSupertagging");

		String inputFile = (String) options.valueOf("input");
		String goldSupertagsFile = (String) options.valueOf("goldSupertags");
		String outputFile = (String) options.valueOf("output");
		String logFile = (String) options.valueOf("log");
		String goldDepsFile = (String) options.valueOf("goldDeps");
		String rootCatsFile = (String) options.valueOf("rootCats");
		int fromSentence = (Integer) options.valueOf("from");
		int toSentence = (Integer) options.valueOf("to");

		System.setProperty("logLevel", options.has("verbose") ? "trace" : "info");
		System.setProperty("logFile", logFile);
		final Logger logger = LogManager.getLogger(PrintForests.class);

		Lexicon lexicon = null;
		ChartParser parser = null;
		OracleDepsSumDecoder oracleDecoder = null;

		try {
			lexicon = new Lexicon(lexiconFile);
			parser = new ChartParser(grammarDir, altMarkedup,
					eisnerNormalForm, MAX_WORDS, MAX_SUPERCATS,
					oracleFscore, adaptiveSupertagging, ruleInstancesParams,
					lexicon, featuresFile, null, false, false);
			oracleDecoder = new OracleDepsSumDecoder(parser.categories, false);
		} catch ( IOException e ) {
			logger.error(e);
			return;
		}

		PrintForest forest = new PrintForest(parser.features);

		try ( BufferedReader in = new BufferedReader(new FileReader(inputFile));
				BufferedReader gold = new BufferedReader(new FileReader(goldDepsFile));
				BufferedReader stagsIn = new BufferedReader(new FileReader(goldSupertagsFile));
				BufferedReader roots = new BufferedReader(new FileReader(rootCatsFile));
				PrintWriter out = new PrintWriter(new FileWriter(outputFile)) ) {

			Preface.readPreface(in);
			Preface.readPreface(gold);
			Preface.readPreface(stagsIn);
			Preface.readPreface(roots);
			Preface.printPreface(out);

			out.println(parser.features.numFeatures);

			Sentences sentences = new Sentences(in, null, parser.categories, MAX_WORDS);
			sentences.skip(fromSentence - 1);

			for ( int numSentence = fromSentence; numSentence <= toSentence && sentences.hasNext(); numSentence++ ) {
				logger.info("Parsing sentence " + numSentence);

				parser.parseSentence(sentences.next(), betas);

				oracleDecoder.readDeps(gold, parser.categories);
				// ugly - passing parser.categories?
				oracleDecoder.readRootCat(roots, parser.categories);

				if ( !parser.maxWordsExceeded && !parser.maxSuperCatsExceeded ) {
					double maxGoldDeps = oracleDecoder.decode(parser.chart, parser.sentence);
					boolean checkRoot = true;
					oracleDecoder.markOracleDeps(parser.chart, maxGoldDeps, false, checkRoot);
					// if the chart is empty nothing gets printed (ie no new line)
					forest.print(out, parser.chart, parser.sentence);
				}
			}
		} catch ( FileNotFoundException e ) {
			logger.error(e);
		} catch ( IOException e ) {
			logger.error(e);
		}
	}
}
