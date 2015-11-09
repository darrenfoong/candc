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
import chart_parser.ChartTrainParserBeamPipe;

public class TrainParserBeamPipe {
	public static void main(String[] args) {
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

		if ( args.length < 8 ) {
			System.err.println("TrainParserBeamPipe requires 8 arguments: <inputFile> <outputWeightsFile> <logFile> <weightsFile> <goldDerivationsFile> <numIters> <fromSentence> <toSentence>");
			return;
		}

		String inputFile = args[0];
		String outputWeightsFile = args[1];
		String logFile = args[2];
		String weightsFile = args[3];
		String goldDerivationsFile = args[4];
		String numItersStr = args[5];
		String fromSent = args[6];
		String toSent = args[7];

		int fromSentence = Integer.parseInt(fromSent);
		int toSentence = Integer.parseInt(toSent);
		int numIterations = Integer.parseInt(numItersStr);

		Lexicon lexicon = null;

		try {
			lexicon = new Lexicon(lexiconFile);
		} catch (IOException e) {
			System.err.println(e);
			return;
		}

		ChartTrainParserBeamPipe parser = null;

		try {
			parser = new ChartTrainParserBeamPipe(grammarDir, altMarkedup,
					eisnerNormalForm, MAX_WORDS, MAX_SUPERCATS, detailedOutput,
					ruleInstancesParams, lexicon, featuresFile, weightsFile,
					newFeatures, cubePruning, beamSize, beta, parallelUpdate,
					updateLogP);
		} catch (IOException e) {
			System.err.println(e);
			return;
		}

		BufferedReader in = null;
		BufferedReader goldDerivations = null;
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

			for (int iteration = 1; iteration <= numIterations; iteration++) {
				in = new BufferedReader(new FileReader(inputFile));
				goldDerivations = new BufferedReader(new FileReader(goldDerivationsFile));

				Preface.readPreface(in);

				for (int numSentence = fromSentence; numSentence <= toSentence; numSentence++) {
					System.out.println("Parsing sentence " + iteration + "/"+ numSentence);
					log.println("Parsing sentence " + iteration + "/"+ numSentence);

					if ( !parser.parseSentence(in, goldDerivations, null, log, betas) ) {
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

					if (numSentence % 5000 == 0) {
						outIterPart = new PrintWriter(new BufferedWriter(new FileWriter(outputWeightsFile + "." + iteration + "." + numSentence)));
						outIterPart.println("# mandatory preface");
						outIterPart.println("# mandatory preface");
						outIterPart.println();
						parser.printWeights(outIterPart, numTrainInstances);
						outIterPart.close();
					}
				}

				in.close();
				goldDerivations.close();

				outIter = new PrintWriter(new BufferedWriter(new FileWriter(outputWeightsFile + "." + iteration)));
				parser.printWeights(outIter, numTrainInstances);
				outIter.close();
			}

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
				if ( in != null ) { in.close(); }
			} catch (IOException e) {
				System.err.println(e);
			}
		}
	}
}
