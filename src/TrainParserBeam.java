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
import cat_combination.SuperCategory;
import chart_parser.ChartTrainParserBeam;
import chart_parser.OracleDecoder;
import chart_parser.OracleDepsSumDecoder;

public class TrainParserBeam {
	public static void main(String[] args) {
		int MAX_WORDS = 250;
		int MAX_SUPERCATS = 50000;

		boolean altMarkedup = false;
		boolean eisnerNormalForm = false;
		boolean detailedOutput = false;
		boolean newFeatures = false;
		boolean cubePruning = false;
		boolean parallelUpdate = false;
		boolean updateLogP = true;

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

		if ( args.length < 9 ) {
			System.err.println("TrainParserBeam requires 9 arguments: <inputFile> <outputWeightsFile> <logFile> <weightsFile> <goldDepsFile> <rootCatsFile> <numIters> <fromSentence> <toSentence>");
			return;
		}

		String inputFile = args[0];
		String outputWeightsFile = args[1];
		String logFile = args[2];
		String weightsFile = args[3];
		String goldDepsFile = args[4];
		String rootCatsFile = args[5];
		String numItersStr = args[6];
		String fromSent = args[7];
		String toSent = args[8];

		int fromSentence = Integer.parseInt(fromSent);
		int toSentence = Integer.parseInt(toSent);
		int numIterations = Integer.parseInt(numItersStr);

		Lexicon lexicon = null;

		try {
			lexicon = new Lexicon(lexiconFile);
		} catch ( IOException e ) {
			System.err.println(e);
			return;
		}

		ChartTrainParserBeam parser = null;

		try {
			parser = new ChartTrainParserBeam(grammarDir, altMarkedup,
					eisnerNormalForm, MAX_WORDS, MAX_SUPERCATS, detailedOutput,
					ruleInstancesParams, lexicon, featuresFile, weightsFile,
					newFeatures, cubePruning, beamSize, beta, parallelUpdate, updateLogP);
		} catch ( IOException e ) {
			System.err.println(e);
			return;
		}

		OracleDecoder oracleDecoder = new OracleDepsSumDecoder(parser.categories, false);

		int numTrainInstances = 1;

		try ( PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputWeightsFile)));
				PrintWriter log = new PrintWriter(new BufferedWriter(new FileWriter(logFile)));
				PrintWriter writer = new PrintWriter(System.out) ) {

			Preface.printPreface(out);

			for ( int iteration = 1; iteration <= numIterations; iteration++ ) {
				try ( BufferedReader in = new BufferedReader(new FileReader(inputFile));
						BufferedReader goldDeps = new BufferedReader(new FileReader(goldDepsFile));
						BufferedReader goldDepsPerCell = new BufferedReader(new FileReader(goldDepsFile + ".per_cell"));
						BufferedReader roots = new BufferedReader(new FileReader(rootCatsFile));
						PrintWriter outIter = new PrintWriter(new BufferedWriter(new FileWriter(outputWeightsFile + "." + iteration))) ) {

					Preface.readPreface(in);
					Preface.readPreface(goldDeps);
					Preface.readPreface(goldDepsPerCell);
					Preface.readPreface(roots);

					parser.hasUpdateStatistics.clear();
					parser.hypothesisSizeStatistics.clear();

					Sentences sentences = new Sentences(in, null, parser.categories, MAX_WORDS);
					sentences.skip(fromSentence - 1);

					for ( int numSentence = fromSentence; numSentence <= toSentence && sentences.hasNext(); numSentence++ ) {
						System.out.println("Parsing sentence " + iteration + "/" + numSentence);
						log.println("Parsing sentence " + iteration + "/" + numSentence);

						parser.parseSentence(sentences.next(), log, betas);

						oracleDecoder.readDeps(goldDeps, parser.categories);

						if ( oracleDecoder.numGoldDeps() != 0 ) {
							numTrainInstances++;
							SuperCategory best = parser.updateWeights(log, numTrainInstances);

							if ( best != null ) {
								System.out.println("best category deps: ");
								parser.printDeps(writer, parser.categories.dependencyRelations, parser.sentence, best);
								writer.flush();
								System.out.println();
							} else {
								System.out.println("No update took place!");
								System.out.println();
								log.println("No update took place!");
								log.println();
							}
						} else {
							System.out.println("No gold dependencies for sentence " + iteration + "/" + numSentence);
							System.out.println();
							log.println("No gold dependencies for sentence " + iteration + "/" + numSentence);
							log.println();
						}

						if ( numSentence % 5000 == 0 ) {
							try ( PrintWriter outIterPart = new PrintWriter(new BufferedWriter(new FileWriter(outputWeightsFile + "." + iteration + "." + numSentence))) ) {
								Preface.printPreface(outIterPart);
								parser.printWeights(outIterPart, numTrainInstances);
							}
						}
					}

					System.out.println("# Statistics for iteration " + iteration + ": hasUpdate: " + parser.hasUpdateStatistics.calcHasUpdates() + "/" + parser.hasUpdateStatistics.getSize() + " (" + ((double) parser.hasUpdateStatistics.calcHasUpdates() / (double) parser.hasUpdateStatistics.getSize()) + ")");
					System.out.println("# Statistics for iteration " + iteration + ": hypothesisSize: " + parser.hypothesisSizeStatistics.calcAverageProportion() + " (" + parser.hypothesisSizeStatistics.getSize() + ")");

					Preface.printPreface(outIter);
					parser.printWeights(outIter, numTrainInstances);
				}
			}

			parser.printWeights(out, numTrainInstances);
		} catch ( FileNotFoundException e ) {
			System.err.println(e);
		} catch ( IOException e ) {
			System.err.println(e);
		}
	}
}
