import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.io.IoBuilder;

import cat_combination.RuleInstancesParams;
import cat_combination.SuperCategory;
import chart_parser.ChartTrainParserBeam;
import chart_parser.OracleDecoder;
import chart_parser.OracleDepsSumDecoder;
import io.Preface;
import io.Sentences;
import model.Lexicon;

public class TrainParserBeam {
	public static final Logger logger = LogManager.getLogger(TrainParserBeam.class);

	public static void main(String[] args) {
		int MAX_WORDS = 150;
		int MAX_SUPERCATS = 500000;

		boolean altMarkedup = false;
		boolean eisnerNormalForm = true;
		boolean newFeatures = false;
		boolean cubePruning = false;
		boolean parallelUpdate = false;
		boolean updateLogP = true;

		String grammarDir = "grammar";
		String lexiconFile = "words_feats/wsj02-21.wordsPos";
		String featuresFile = "words_feats/wsj02-21.feats.1-22";

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

		System.setProperty("logFile", logFile);

		int fromSentence = Integer.parseInt(fromSent);
		int toSentence = Integer.parseInt(toSent);
		int numIterations = Integer.parseInt(numItersStr);

		Lexicon lexicon = null;

		try {
			lexicon = new Lexicon(lexiconFile);
		} catch ( IOException e ) {
			logger.error(e);
			return;
		}

		ChartTrainParserBeam parser = null;

		try {
			parser = new ChartTrainParserBeam(grammarDir, altMarkedup,
					eisnerNormalForm, MAX_WORDS, MAX_SUPERCATS,
					ruleInstancesParams, lexicon, featuresFile, weightsFile,
					newFeatures, cubePruning, beamSize, beta, parallelUpdate, updateLogP);
		} catch ( IOException e ) {
			logger.error(e);
			return;
		}

		OracleDecoder oracleDecoder = new OracleDepsSumDecoder(parser.categories, false);

		int numTrainInstances = 1;

		try ( PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputWeightsFile)));
				PrintWriter writer = IoBuilder.forLogger(logger).setLevel(Level.INFO).buildPrintWriter() ) {

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
						logger.info("Parsing sentence " + iteration + "/" + numSentence);

						parser.parseSentence(sentences.next(), betas, goldDepsPerCell, oracleDecoder);

						oracleDecoder.readDeps(goldDeps, parser.categories);

						if ( oracleDecoder.numGoldDeps() != 0 ) {
							numTrainInstances++;
							SuperCategory best = parser.updateWeights(numTrainInstances);

							if ( best != null ) {
								logger.info("best category deps: ");
								parser.printDeps(writer, parser.categories.dependencyRelations, parser.sentence, best);
								writer.flush();
								logger.info("");
							} else {
								logger.info("No update took place!");
								logger.info("");
							}
						} else {
							logger.info("No gold dependencies for sentence " + iteration + "/" + numSentence);
							logger.info("");
						}

						if ( numSentence % 5000 == 0 ) {
							try ( PrintWriter outIterPart = new PrintWriter(new BufferedWriter(new FileWriter(outputWeightsFile + "." + iteration + "." + numSentence))) ) {
								Preface.printPreface(outIterPart);
								parser.printWeights(outIterPart, numTrainInstances);
							}
						}
					}

					logger.info("# Statistics for iteration " + iteration + ": hasUpdate: " + parser.hasUpdateStatistics.calcHasUpdates() + "/" + parser.hasUpdateStatistics.getSize() + " (" + ((double) parser.hasUpdateStatistics.calcHasUpdates() / (double) parser.hasUpdateStatistics.getSize()) + ")");
					logger.info("# Statistics for iteration " + iteration + ": hypothesisSize: " + parser.hypothesisSizeStatistics.calcAverageProportion() + " (" + parser.hypothesisSizeStatistics.getSize() + ")");

					Preface.printPreface(outIter);
					parser.printWeights(outIter, numTrainInstances);
				}
			}

			parser.printWeights(out, numTrainInstances);
		} catch ( FileNotFoundException e ) {
			logger.error(e);
		} catch ( IOException e ) {
			logger.error(e);
		}
	}
}
