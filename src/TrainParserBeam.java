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
import io.Params;
import io.Preface;
import io.Sentences;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import model.Lexicon;

public class TrainParserBeam {
	public static void main(String[] args) {
		OptionParser optionParser = Params.getTrainParserBeamOptionParser();
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
		boolean newFeatures = (Boolean) options.valueOf("newFeatures");
		boolean cubePruning = (Boolean) options.valueOf("cubePruning");
		boolean depnn = options.has("depnn");

		String modelDir = null;

		if ( depnn ) {
			modelDir = (String) options.valueOf("modelDir");
		}

		boolean parallelUpdate = (Boolean) options.valueOf("parallelUpdate");
		boolean updateLogP = (Boolean) options.valueOf("updateLogP");
		boolean updateDepNN = (Boolean) options.valueOf("updateDepNN");
		int beamSize = (Integer) options.valueOf("beamSize");
		double beta = (Double) options.valueOf("beta");
		double[] betas = Params.betasArray((String) options.valueOf("betas"));

		String inputFile = (String) options.valueOf("input");
		String outputWeightsFile = (String) options.valueOf("outputWeights");
		String logFile = (String) options.valueOf("log");
		String weightsFile = (String) options.valueOf("weights");
		String goldDepsFile = (String) options.valueOf("goldDeps");
		String rootCatsFile = (String) options.valueOf("rootCats");
		int numIterations = (Integer) options.valueOf("numIterations");
		int fromSentence = (Integer) options.valueOf("from");
		int toSentence = (Integer) options.valueOf("to");

		System.setProperty("logLevel", options.has("verbose") ? "trace" : "info");
		System.setProperty("logFile", logFile);
		final Logger logger = LogManager.getLogger(TrainParserBeam.class);

		logger.info(Params.printOptions(options));

		Lexicon lexicon = null;
		ChartTrainParserBeam parser = null;
		OracleDecoder oracleDecoder = null;

		try {
			lexicon = new Lexicon(lexiconFile);
			parser = new ChartTrainParserBeam(grammarDir, altMarkedup,
					eisnerNormalForm, MAX_WORDS, MAX_SUPERCATS,
					ruleInstancesParams, lexicon, featuresFile, weightsFile,
					newFeatures, cubePruning, beamSize, beta, parallelUpdate,
					updateLogP, updateDepNN);
			oracleDecoder = new OracleDepsSumDecoder(parser.categories, false);
			if ( depnn ) {
				parser.initDepNN(modelDir);
			}
		} catch ( IOException e ) {
			logger.error(e);
			return;
		}

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
