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
import io.Preface;
import io.Sentences;
import model.Lexicon;
import training.PrintForest;

public class PrintForests {
	public static void main(String[] args) {
		int MAX_WORDS = 250;
		int MAX_SUPERCATS = 1000000;
		boolean altMarkedup = false;
		boolean eisnerNormalForm = true;
		boolean newFeatures = false;
		boolean oracleFscore = false;
		// we only use the depsSumDecoder for printing forests;
		// F-score decoder fragments the charts much more

		String grammarDir = "grammar";
		String lexiconFile = "words_feats/wsj02-21.wordsPos";
		String featuresFile = "words_feats/wsj02-21.feats.1-22";

		RuleInstancesParams ruleInstancesParams = new RuleInstancesParams(true, false, false, false, false, false, grammarDir);

		boolean adaptiveSupertagging = false;
		double[] betas = { 0.0001, 0.0001, 0.0001, 0.01, 0.1 };
		/*
		 * true indicates beta values get smaller, and first value is betas[0];
		 * false is the opposite, and first value is betas[1] (which is what we
		 * use for PrintForests) betas[0] effectively isn't used by
		 * PrintForests, since if betas[1] fails to find a root then betas[0]
		 * will as well if betas[0] == betas[1])
		 */

		if ( args.length < 8 ) {
			System.err.println("PrintForests requires 8 arguments: <inputFile> <goldSupertagsFile> <outputFile> <logFile> <goldDepsFile> <rootCatsFile> <fromSentence> <toSentence>");
			return;
		}

		String inputFile = args[0];
		String goldSupertagsFile = args[1];
		String outputFile = args[2];
		String logFile = args[3];
		String goldDepsFile = args[4];
		String rootCatsFile = args[5];
		String fromSent = args[6];
		String toSent = args[7];

		System.setProperty("logFile", logFile);
		final Logger logger = LogManager.getLogger(PrintForests.class);

		int fromSentence = Integer.parseInt(fromSent);
		int toSentence = Integer.parseInt(toSent);

		Lexicon lexicon = null;
		ChartParser parser = null;
		OracleDepsSumDecoder oracleDecoder = null;

		try {
			lexicon = new Lexicon(lexiconFile);
			parser = new ChartParser(grammarDir, altMarkedup,
					eisnerNormalForm, MAX_WORDS, MAX_SUPERCATS,
					oracleFscore, adaptiveSupertagging, ruleInstancesParams,
					lexicon, featuresFile, null, newFeatures, false);
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
