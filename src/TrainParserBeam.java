import io.Preface;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import model.Lexicon;
import utils.Benchmark;
import cat_combination.RuleInstancesParams;
import cat_combination.SuperCategory;
import chart_parser.ChartTrainParserBeam;
import chart_parser.OracleDecoder;
import chart_parser.OracleDepsSumDecoder;

public class TrainParserBeam {
	public static void main(String[] args) {
		long TS_PROGRAM = Benchmark.getTime();

		int MAX_WORDS = 150;
		int MAX_SUPERCATS = 500000;

		boolean altMarkedup = false;
		boolean eisnerNormalForm = true;
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
			long TS_LEXICON = Benchmark.getTime();
			lexicon = new Lexicon(lexiconFile);
			long TE_LEXICON = Benchmark.getTime();
			Benchmark.printTime("load lexicon", TS_LEXICON, TE_LEXICON);
		} catch (IOException e) {
			System.err.println(e);
			return;
		}

		ChartTrainParserBeam parser = null;

		try {
			long TS_PARSER_INIT = Benchmark.getTime();
			parser = new ChartTrainParserBeam(grammarDir, altMarkedup,
					eisnerNormalForm, MAX_WORDS, MAX_SUPERCATS, detailedOutput,
					ruleInstancesParams, lexicon, featuresFile, weightsFile,
					newFeatures, cubePruning, beamSize, beta, parallelUpdate,
					updateLogP);
			long TE_PARSER_INIT = Benchmark.getTime();
			Benchmark.printTime("init parser", TS_PARSER_INIT, TE_PARSER_INIT);
		} catch (IOException e) {
			System.err.println(e);
			return;
		}

		OracleDecoder oracleDecoder = new OracleDepsSumDecoder(parser.categories, false);

		BufferedReader in = null;
		BufferedReader goldDeps = null;
		BufferedReader goldDepsPerCell = null;
		BufferedReader roots = null;
		PrintWriter out = null;
		PrintWriter log = null;
		PrintWriter writer = null;
		PrintWriter outIter = null;
		PrintWriter outIterPart = null;

		int numTrainInstances = 1;

		try {
			out = new PrintWriter(new BufferedWriter(new FileWriter(outputWeightsFile)));
			log = new PrintWriter(new BufferedWriter(new FileWriter(logFile)));
			writer = new PrintWriter(System.out);

			out.println("# mandatory preface");
			out.println("# mandatory preface");
			out.println();

			long TS_PARSING_TOTAL = Benchmark.getTime();
			for (int iteration = 1; iteration <= numIterations; iteration++) {
				in = new BufferedReader(new FileReader(inputFile));
				goldDeps = new BufferedReader(new FileReader(goldDepsFile));
				goldDepsPerCell = new BufferedReader(new FileReader(goldDepsFile + ".per_cell"));
				roots = new BufferedReader(new FileReader(rootCatsFile));

				Preface.readPreface(in);
				Preface.readPreface(goldDeps);
				Preface.readPreface(goldDepsPerCell);
				Preface.readPreface(roots);

				parser.hasUpdateStatistics.clear();
				parser.hypothesisSizeStatistics.clear();

				long TS_PARSING = Benchmark.getTime();
				for (int numSentence = fromSentence; numSentence <= toSentence; numSentence++) {
					System.out.println("Parsing sentence " + iteration + "/"+ numSentence);
					log.println("Parsing sentence " + iteration + "/"+ numSentence);

					if ( !parser.parseSentence(in, goldDepsPerCell, null, log, betas, oracleDecoder) ) {
						System.out.println("No such sentence; no more sentences.");
						log.println("No such sentence; no more sentences.");
						break;
					}

					oracleDecoder.readDeps(goldDeps, parser.categories);
					// oracleDecoder.readRootCat(roots, parser.categories);

					if ( oracleDecoder.numGoldDeps() != 0 ) {
						numTrainInstances++;
						SuperCategory best = parser.updateWeights(log, numTrainInstances);

						if (best != null) {
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
						System.out.println("No gold dependencies for sentence " + iteration + "/"+ numSentence);
						System.out.println();
						log.println("No gold dependencies for sentence " + iteration + "/"+ numSentence);
						log.println();
					}

					if (numSentence % 5000 == 0) {
						outIterPart = new PrintWriter(new BufferedWriter(new FileWriter(outputWeightsFile + "." + iteration + "." + numSentence)));
						parser.printWeights(outIterPart, numTrainInstances);
						outIterPart.close();
					}
				}
				long TE_PARSING = Benchmark.getTime();
				Benchmark.printTime("training iteration " + iteration, TS_PARSING, TE_PARSING);

				System.out.println("# Statistics for iteration " + iteration + ": hasUpdate: " + parser.hasUpdateStatistics.calcHasUpdates() + "/" + parser.hasUpdateStatistics.getSize() + " (" + ((double) parser.hasUpdateStatistics.calcHasUpdates()/(double) parser.hasUpdateStatistics.getSize()) + ")");
				System.out.println("# Statistics for iteration " + iteration + ": hypothesisSize: " + parser.hypothesisSizeStatistics.calcAverageProportion() + " (" + parser.hypothesisSizeStatistics.getSize() + ")");

				in.close();
				goldDeps.close();
				goldDepsPerCell.close();
				roots.close();

				outIter = new PrintWriter(new BufferedWriter(new FileWriter(outputWeightsFile + "." + iteration)));
				parser.printWeights(outIter, numTrainInstances);
				outIter.close();
			}
			long TE_PARSING_TOTAL = Benchmark.getTime();
			Benchmark.printTime("training total", TS_PARSING_TOTAL, TE_PARSING_TOTAL);

			parser.printWeights(out, numTrainInstances);
		} catch (IOException e) {
			System.err.println(e);
		} finally {
			try {
				if ( outIterPart != null ) { outIterPart.close(); }
				if ( outIter != null ) { outIter.close(); }
				if ( writer != null ) { writer.close(); }
				if ( log != null ) { log.close(); }
				if ( out != null ) { out.close(); }
				if ( roots != null ) { roots.close(); }
				if ( goldDepsPerCell != null ) { goldDepsPerCell.close(); }
				if ( goldDeps != null ) { goldDeps.close(); }
				if ( in != null ) { in.close(); }
			} catch (IOException e) {
				System.err.println(e);
			}
		}

		long TE_PROGRAM = Benchmark.getTime();
		Benchmark.printTime("program", TS_PROGRAM, TE_PROGRAM);
	}
}
