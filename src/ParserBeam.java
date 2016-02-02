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

public class ParserBeam {
	public static void main(String[] args) {
		int MAX_WORDS = 150;
		int MAX_SUPERCATS = 500000;

		boolean altMarkedup = false;
		boolean eisnerNormalForm = true;
		boolean detailedOutput = false;
		boolean newFeatures = false;
		boolean compactWeights = true;
		boolean cubePruning = false;

		boolean printChartDeps = false;

		String grammarDir = "data/baseline_expts/grammar";
		String lexiconFile = "data/baseline_expts/working/lexicon/wsj02-21.wordsPos";
		String featuresFile = "data/baseline_expts/working/lexicon/wsj02-21.feats.1-22";

		RuleInstancesParams ruleInstancesParams = new RuleInstancesParams(true, false, false, false, false, false, grammarDir);

		double[] betas = { 0.0001 };
		// just one beta value needed - no adaptive supertagging

		int beamSize = 32;
		double beta = Double.NEGATIVE_INFINITY;
		// this is the beta used by the parser, in a second beam
		// the closer to zero the more aggressive the beam

		if ( args.length < 6 ) {
			System.err.println("ParserBeam requires 6 arguments: <inputFile> <outputFile> <logFile> <weightsFile> <fromSentence> <toSentence>");
			return;
		}

		String inputFile = args[0];
		String outputFile = args[1];
		String logFile = args[2];
		String weightsFile = args[3];
		String fromSent = args[4];
		String toSent = args[5];

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
					newFeatures, compactWeights, cubePruning, beamSize, beta);
		} catch ( IOException e ) {
			System.err.println(e);
			return;
		}

		try ( BufferedReader in = new BufferedReader(new FileReader(inputFile));
				PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));
				PrintWriter log = new PrintWriter(new BufferedWriter(new FileWriter(logFile)));
				PrintWriter outChartDeps = printChartDeps ? new PrintWriter(new BufferedWriter(new FileWriter(outputFile + ".chartdeps"))) : null ) {

			Preface.readPreface(in);
			Preface.printPreface(out);

			if ( printChartDeps ) {
				Preface.printPreface(outChartDeps);
			}

			Sentences sentences = new Sentences(in, null, parser.categories, MAX_WORDS);
			sentences.skip(fromSentence - 1);

			for ( int numSentence = fromSentence; numSentence <= toSentence && sentences.hasNext(); numSentence++ ) {
				System.out.println("Parsing sentence " + numSentence);
				log.println("Parsing sentence " + numSentence);

				parser.parseSentence(sentences.next(), log, betas);

				if ( !parser.maxWordsExceeded && !parser.maxSuperCatsExceeded ) {
					boolean success = parser.root();

					if ( success ) {
						parser.printDeps(out, parser.categories.dependencyRelations, parser.sentence);
						parser.sentence.printC_line(out);
					} else {
						System.out.println("No root category.");
						log.println("No root category.");
					}
				}

				out.println();

				if ( printChartDeps ) {
					parser.printChartDeps(outChartDeps, parser.categories.dependencyRelations, parser.sentence);
					outChartDeps.println();
				}
			}
		} catch ( FileNotFoundException e ) {
			System.err.println(e);
		} catch ( IOException e ) {
			System.err.println(e);
		}
	}
}
