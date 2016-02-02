import io.Preface;
import io.Sentences;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import model.Lexicon;
import cat_combination.RuleInstancesParams;
import chart_parser.ChartParserBeam;
import chart_parser.CountFeaturesDecoder;

public class CountFeatures {
	public static void main(String[] args) {
		int MAX_WORDS = 250;
		int MAX_SUPERCATS = 50000;

		boolean altMarkedup = false;
		boolean eisnerNormalForm = true;
		boolean detailedOutput = false;
		boolean newFeatures = false;
		boolean cubePruning = false;

		String grammarDir = "data/baseline_expts/grammar";
		String lexiconFile = "data/baseline_expts/working/lexicon/wsj02-21.wordsPos";
		String featuresFile = "data/baseline_expts/working/lexicon/wsj02-21.feats.1-22";

		RuleInstancesParams ruleInstancesParams = new RuleInstancesParams(true, false, false, false, false, false, grammarDir);

		double[] betas = { 0.0001 };

		int beamSize = 32;
		double beta = Double.NEGATIVE_INFINITY;

		if ( args.length < 7 ) {
			System.err.println("CountFeatures requires 7 arguments: <inputFile> <outputFile> <outputWeightsFile> <logFile> <weightsFile> <fromSentence> <toSentence>");
			return;
		}

		String inputFile = args[0];
		String outputFile = args[1];
		String outputWeightsFile = args[2];
		String logFile = args[3];
		String weightsFile = args[4];
		String fromSent = args[5];
		String toSent = args[6];

		int fromSentence = Integer.parseInt(fromSent);
		int toSentence = Integer.parseInt(toSent);

		Lexicon lexicon = null;

		try {
			lexicon = new Lexicon(lexiconFile);
		} catch ( IOException e ) {
			System.err.println(e);
			return;
		}

		ChartParserBeam parser = null;

		try {
			parser = new ChartParserBeam(grammarDir, altMarkedup,
					eisnerNormalForm, MAX_WORDS, MAX_SUPERCATS, detailedOutput,
					ruleInstancesParams, lexicon, featuresFile, weightsFile,
					newFeatures, false, cubePruning, beamSize, beta);
		} catch ( IOException e ) {
			System.err.println(e);
			return;
		}

		CountFeaturesDecoder countFeaturesDecoder = new CountFeaturesDecoder(parser.categories);

		try ( BufferedReader in = new BufferedReader(new FileReader(inputFile));
				PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));
				PrintWriter log = new PrintWriter(new BufferedWriter(new FileWriter(logFile)));
				PrintWriter weights = new PrintWriter(new BufferedWriter(new FileWriter(outputWeightsFile))) ) {

			Preface.readPreface(in);
			Preface.printPreface(out);
			Preface.printPreface(weights);

			Sentences sentences = new Sentences(in, null, parser.categories, MAX_WORDS);
			sentences.skip(fromSentence - 1);

			for ( int numSentence = fromSentence; numSentence <= toSentence && sentences.hasNext(); numSentence++ ) {
				System.out.println("Parsing sentence " + numSentence);
				log.println("Parsing sentence " + numSentence);

				parser.parseSentence(sentences.next(), log, betas);

				if ( !parser.maxWordsExceeded && !parser.maxSuperCatsExceeded ) {
					boolean success = countFeaturesDecoder.countFeatures(parser.chart, parser.sentence);

					if ( success ) {
						System.out.println("Success.");
						log.println("Success.");
					} else {
						System.out.println("No root category.");
						log.println("No root category.");
					}
				}
			}

			countFeaturesDecoder.mergeAllFeatureCounts(parser.features);
			parser.features.print(out);
			parser.features.printWeights(parser.weights, weights);
		} catch ( FileNotFoundException e ) {
			System.err.println(e);
		} catch ( IOException e ) {
			System.err.println(e);
		}
	}
}
