import io.Preface;
import io.Sentences;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import model.Lexicon;
import training.PrintForest;
import cat_combination.RuleInstancesParams;
import chart_parser.ChartParser;
import chart_parser.OracleDepsSumDecoder;

public class PrintForests {
	public static void main(String[] args) {
		int MAX_WORDS = 250;
		int MAX_SUPERCATS = 1000000;
		boolean altMarkedup = false;
		boolean eisnerNormalForm = true;
		boolean detailedOutput = false;
		boolean newFeatures = false;
		boolean oracleFscore = false;
		// we only use the depsSumDecoder for printing forests;
		// F-score decoder fragments the charts much more

		String grammarDir = "baseline_expts/grammar";
		String lexiconFile = "baseline_expts/working/lexicon/wsj02-21.wordsPos";
		String featuresFile = "baseline_expts/working/lexicon/wsj02-21.feats.1-22";

		RuleInstancesParams ruleInstancesParams = new RuleInstancesParams(true, false, false, false, false, false, grammarDir);

		boolean adaptiveSupertagging = false;
		double[] betas = { 0.01, 0.01, 0.1 };
		/*
		 * true indicates beta values get smaller, and first value is betas[0];
		 * false is the opposite, and first value is betas[1] (which is what we
		 * use for PrintForests) betas[0] effectively isn't used by
		 * PrintForests, since if betas[1] fails to find a root then betas[0]
		 * will as well if betas[0] == betas[1])
		 */

		int fromSentence = 32001;
		int toSentence = 40000;

		if ( args.length < 6 ) {
			System.err.println("PrintForests requires 6 arguments: <inputFile> <goldSupertagsFile> <outputFile> <logFile> <goldDepsFile> <rootCatsFile>");
			return;
		}

		String inputFile = args[0];
		String goldSupertagsFile = args[1];
		String outputFile = args[2];
		String logFile = args[3];
		String goldDepsFile = args[4];
		String rootCatsFile = args[5];

		Lexicon lexicon = null;

		try {
			lexicon = new Lexicon(lexiconFile);
		} catch (IOException e) {
			System.err.println(e);
			return;
		}

		ChartParser parser = null;

		try {
			parser = new ChartParser(grammarDir, altMarkedup,
					eisnerNormalForm, MAX_WORDS, MAX_SUPERCATS, detailedOutput,
					oracleFscore, adaptiveSupertagging, ruleInstancesParams,
					lexicon, featuresFile, null, newFeatures, false);
		} catch (IOException e) {
			System.err.println(e);
			return;
		}

		PrintForest forest = new PrintForest(parser.features);

		OracleDepsSumDecoder oracleDecoder = new OracleDepsSumDecoder(parser.categories, false);

		try ( BufferedReader in = new BufferedReader(new FileReader(inputFile));
			  BufferedReader gold = new BufferedReader(new FileReader(goldDepsFile));
			  BufferedReader stagsIn = new BufferedReader(new FileReader(goldSupertagsFile));
			  BufferedReader roots = new BufferedReader(new FileReader(rootCatsFile));
			  PrintWriter out = new PrintWriter(new FileWriter(outputFile));
			  PrintWriter log = new PrintWriter(new FileWriter(logFile)) ) {

			Preface.readPreface(in);
			Preface.readPreface(gold);
			Preface.readPreface(stagsIn);
			Preface.readPreface(roots);
			Preface.printPreface(out);

			out.println(parser.features.numFeatures);

			Sentences sentences = new Sentences(in, null, parser.categories, MAX_WORDS);
			sentences.skip(fromSentence - 1);

			for (int numSentence = fromSentence; numSentence <= toSentence && sentences.hasNext(); numSentence++) {
				System.out.println("Parsing sentence "+ numSentence);
				log.println("Parsing sentence "+ numSentence);

				parser.parseSentence(sentences.next(), log, betas);

				oracleDecoder.readDeps(gold, parser.categories);
				// ugly - passing parser.categories?
				oracleDecoder.readRootCat(roots, parser.categories);

				if (!parser.maxWordsExceeded && !parser.maxSuperCatsExceeded) {
					double maxGoldDeps = oracleDecoder.decode(parser.chart, parser.sentence);
					boolean checkRoot = true;
					oracleDecoder.markOracleDeps(parser.chart, maxGoldDeps, false, checkRoot);
					// if the chart is empty nothing gets printed (ie no new line)
					forest.print(out, parser.chart, parser.sentence);
				}
			}
		} catch (FileNotFoundException e) {
			System.err.println(e);
		} catch (IOException e) {
			System.err.println(e);
		}
	}
}
