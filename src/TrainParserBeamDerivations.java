import io.Preface;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import model.Lexicon;
import cat_combination.RuleInstancesParams;
import cat_combination.SuperCategory;
import chart_parser.CellTrainBeam;
import chart_parser.ChartParser;
import chart_parser.ChartTrainParserBeamDerivations;
import chart_parser.OracleDecoder;
import chart_parser.OracleFscoreDecoder;

public class TrainParserBeamDerivations {
	public static void main(String[] args) {
		int MAX_WORDS = 150;
		int MAX_SUPERCATS = 500000;
		int GOLD_MAX_WORDS = 250;
		int GOLD_MAX_SUPERCATS = 1000000;

		boolean altMarkedup = false;
		boolean eisnerNormalForm = true;
		boolean detailedOutput = false;
		boolean newFeatures = false;
		boolean cubePruning = false;
		boolean parallelUpdate = false;
		boolean updateLogP = true;

		boolean oracleFscore = true;

		String grammarDir = "data/baseline_expts/grammar";
		String lexiconFile = "data/baseline_expts/working/lexicon/wsj02-21.wordsPos";
		String featuresFile = "data/baseline_expts/working/lexicon/wsj02-21.feats.1-22";

		RuleInstancesParams ruleInstancesParams = new RuleInstancesParams(true, false, false, false, false, false, grammarDir);

		double[] betas = { 0.0001 };
		// just one beta value needed - no adaptive supertagging

		boolean adaptiveSupertagging = false;
		double[] goldBetas = { 0.0001, 0.001, 0.01, 0.03, 0.075 };

		int beamSize = 32;
		double beta = Double.NEGATIVE_INFINITY;
		// this is the beta used by the parser, in a second beam
		// the closer to zero the more aggressive the beam

		if ( args.length < 10 ) {
			System.err.println("TrainParserBeamDerivations requires 10 arguments: <inputFile> <outputWeightsFile> <outputPipeFile> <logFile> <weightsFile> <goldDepsFile> <rootCatsFile> <numIters> <fromSentence> <toSentence>");
			return;
		}

		String inputFile = args[0];
		String outputWeightsFile = args[1];
		String outputPipeFile = args[2];
		String logFile = args[3];
		String weightsFile = args[4];
		String goldDepsFile = args[5];
		String rootCatsFile = args[6];
		String numItersStr = args[7];
		String fromSent = args[8];
		String toSent = args[9];

		int fromSentence = Integer.valueOf(fromSent);
		int toSentence = Integer.valueOf(toSent);
		int numIterations = Integer.valueOf(numItersStr);

		Lexicon lexicon = null;

		try {
			lexicon = new Lexicon(lexiconFile);
		} catch (IOException e) {
			System.err.println(e);
			return;
		}

		ChartTrainParserBeamDerivations parser = null;
		ChartParser goldParser = null;

		try {
			parser = new ChartTrainParserBeamDerivations(grammarDir, altMarkedup,
					eisnerNormalForm, MAX_WORDS, MAX_SUPERCATS, detailedOutput,
					ruleInstancesParams, lexicon, featuresFile, weightsFile,
					newFeatures, cubePruning, beamSize, beta, parallelUpdate,
					updateLogP);
			goldParser = new ChartParser(grammarDir, altMarkedup,
					eisnerNormalForm, GOLD_MAX_WORDS, GOLD_MAX_SUPERCATS, detailedOutput,
					oracleFscore, adaptiveSupertagging, ruleInstancesParams,
					lexicon, featuresFile, weightsFile, newFeatures);
		} catch (IOException e) {
			System.err.println(e);
			return;
		}

		// ViterbiDecoder oracleDecoder = new ViterbiDecoder();
		OracleDecoder oracleDecoder = new OracleFscoreDecoder(parser.categories, false);

		BufferedReader in = null;
		BufferedReader in2 = null;
		BufferedReader goldDeps = null;
		BufferedReader roots = null;
		PrintWriter out = null;
		PrintWriter log = null;
		PrintWriter writer = null;
		PrintWriter outIter = null;
		PrintWriter outIterPart = null;
		PrintWriter pipe = null;

		int numTrainInstances = 1;

		try {
			out = new PrintWriter(new BufferedWriter(new FileWriter(outputWeightsFile)));
			log = new PrintWriter(new BufferedWriter(new FileWriter(logFile)));
			writer = new PrintWriter(System.out);
			pipe = new PrintWriter(new BufferedWriter(new FileWriter(outputPipeFile)));

			out.println("# mandatory preface");
			out.println("# mandatory preface");
			out.println();

			for (int iteration = 1; iteration <= numIterations; iteration++) {
				in = new BufferedReader(new FileReader(inputFile));
				in2 = new BufferedReader(new FileReader(inputFile));
				goldDeps = new BufferedReader(new FileReader(goldDepsFile));
				roots = new BufferedReader(new FileReader(rootCatsFile));

				Preface.readPreface(in);
				Preface.readPreface(in2);
				Preface.readPreface(goldDeps);
				Preface.readPreface(roots);

				for (int numSentence = fromSentence; numSentence <= toSentence; numSentence++) {
					System.out.println("Parsing sentence " + iteration + "/"+ numSentence);
					log.println("Parsing sentence " + iteration + "/"+ numSentence);

					if ( goldParser.parseSentence(in, null, log, goldBetas) ) {
						if ( goldParser.calcScores() ) {
							oracleDecoder.readDeps(goldDeps, parser.categories);
							oracleDecoder.readRootCat(roots, parser.categories);
							oracleDecoder.decode(goldParser.chart, goldParser.sentence);
						}
					}

					if ( !parser.parseSentence(in2, goldParser.chart, null, log, betas) ) {
						System.out.println("No such sentence; no more sentences.");
						log.println("No such sentence; no more sentences.");
						break;
					}

					CellTrainBeam root = (CellTrainBeam) parser.chart.root();
					if ( root.goldSuperCat != null ) {
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
						System.out.println("No gold derivation for sentence " + iteration + "/"+ numSentence);
						System.out.println();
						log.println("No gold derivation for sentence " + iteration + "/"+ numSentence);
						log.println();
					}

					if ( iteration == 1 ) {
						goldParser.printPipeViterbi(pipe);
					}

					if (numSentence % 5000 == 0) {
						outIterPart = new PrintWriter(new BufferedWriter(new FileWriter(outputWeightsFile + "." + iteration + "." + numSentence)));
						parser.printWeights(outIterPart, numTrainInstances);
						outIterPart.close();
					}
				}

				in.close();
				in2.close();
				goldDeps.close();
				roots.close();

				outIter = new PrintWriter(new BufferedWriter(new FileWriter(outputWeightsFile + "." + iteration)));
				parser.printWeights(outIter, numTrainInstances);
				outIter.close();
				pipe.close();
			}

			parser.printWeights(out, numTrainInstances);
		} catch (IOException e) {
			System.err.println(e);
		} finally {
			try {
				if ( pipe != null ) { pipe.close(); }
				if ( outIterPart != null ) { outIterPart.close(); }
				if ( outIter != null ) { outIter.close(); }
				if ( writer != null ) { writer.close(); }
				if ( log != null ) { log.close(); }
				if ( out != null ) { out.close(); }
				if ( roots != null ) { roots.close(); }
				if ( goldDeps != null ) { goldDeps.close(); }
				if ( in2 != null ) { in.close(); }
				if ( in != null ) { in.close(); }
			} catch (IOException e) {
				System.err.println(e);
			}
		}
	}
}
