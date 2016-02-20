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

		if ( args.length < 6 ) {
			System.err.println("Parser requires 6 arguments: <inputFile> <outputFile> <logFile> <weightsFile> <fromSentence> <toSentence>");
			return;
		}

		String inputFile = args[0];
		String outputFile = args[1];
		String logFile = args[2];
		String weightsFile = args[3];
		String fromSent = args[4];
		String toSent = args[5];

		System.setProperty("logFile", logFile);
		final Logger logger = LogManager.getLogger(Parser.class);

		int fromSentence = Integer.parseInt(fromSent);
		int toSentence = Integer.parseInt(toSent);

		Lexicon lexicon = null;

		try {
			lexicon = new Lexicon(lexiconFile);
		} catch ( IOException e ) {
			logger.error(e);
			return;
		}

		ChartParser parser = null;

		try {
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
