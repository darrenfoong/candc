package chart_parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import model.Lexicon;
import utils.ShiftReduce;
import cat_combination.RuleInstancesParams;

public class ChartTrainParserBeamPipe extends ChartTrainParserBeamDerivations {
	protected BufferedReader goldDerivations;

	public ChartTrainParserBeamPipe(
			String grammarDir,
			boolean altMarkedup,
			boolean eisnerNormalForm,
			int MAX_WORDS,
			int MAX_SUPERCATS,
			boolean output,
			RuleInstancesParams ruleInstancesParams,
			Lexicon lexicon,
			String featuresFile,
			String weightsFile,
			boolean newFeatures,
			boolean cubePruning,
			int beamSize,
			double beta,
			boolean parallelUpdate,
			boolean updateLogP) throws IOException {
		super(grammarDir, altMarkedup, eisnerNormalForm, MAX_WORDS,
				MAX_SUPERCATS, output, ruleInstancesParams,
				lexicon, featuresFile, weightsFile, newFeatures, cubePruning, beamSize, beta,
				parallelUpdate, updateLogP);
	}

	/**
	 * Parses one supertagged sentence using chart parser with beam search,
	 * while training using a gold derivation built from an input file.
	 * 
	 * @param in file containing supertagged sentences to parse
	 * @param goldDerivations file containing gold derivations
	 * @param stagsIn file containing additional supertags
	 * @param log log file
	 * @param betas array of values of beta
	 * @return true if sentence is parsed or skipped, false if there are no
	 * sentences left
	 */
	public boolean parseSentence(BufferedReader in, BufferedReader goldDerivations, BufferedReader stagsIn, PrintWriter log, double[] betas) {
		this.goldDerivations = goldDerivations;
		return parseSentence(in, stagsIn, log, betas);
	}

	/**
	 * Initialises maxviolation search variables, reads gold derivation for
	 * current sentence, and recomputes scores for gold derivation based on
	 * current weights.
	 */
	@Override
	protected boolean preParse() {
		maxViolation = 0;
		maxViolationCell = null;

		if (!readDerivations(goldDerivations)) {
			return false;
		}

		CellTrainBeam root = (CellTrainBeam) chart.root();
		if ( root.goldSuperCat != null ) {
			initGoldLeafScores();
			calcScoreRecursive(root.goldSuperCat, true);
		} else {
			System.out.println("No gold derivation available.");
			return false;
		}

		System.out.println("Gold derivation merged.");

		return true;
	}

	/**
	 * Reads gold derivation for current sentence, constructs gold supercategory
	 * tree, and merges it into current chart.
	 * 
	 * @param goldDerivations file containing gold derivations
	 * @return true if success, false if failure or there are no derivations
	 * left.
	 */
	private boolean readDerivations(BufferedReader goldDerivations) {
		try {
			String line = goldDerivations.readLine();

			if (line == null) {
				return false;
			}

			if (line.isEmpty()) {
				return false;
			}

			ShiftReduce shiftReduce = new ShiftReduce(chart, rules, sentence, categories);
			boolean error = false;

			while (true) {
				if (line == null) {
					return false;
				}

				if (line.isEmpty()) {
					return !error;
				}

				if (!error && !line.equals("###") && line.length() > 1) {
					line = line.substring(2, line.length()-1);
					String[] tokens = line.split(" ");

					String token_original = tokens[2];
					tokens[2] = tokens[2].replaceAll("(.*[\\/\\\\]+.*)\\Q[conj]\\E", "($1)\\\\($1)");
					tokens[2] = tokens[2].replaceAll("(.*)\\Q[conj]\\E", "$1\\\\$1");

					if ( tokens[0].equals("L") ) {
						shiftReduce.shift(tokens[2], 0);
					} else if ( tokens[0].equals("T") ) {
						shiftReduce.shift(tokens[2], Integer.parseInt(tokens[5]));
					}

					boolean reduceSuccess = shiftReduce.reduce();
					if ( !reduceSuccess ) {
						System.out.println("Failed to reduce; token_original: " + token_original + "; regexed: " + tokens[2]);
						error = true;
					}
				}

				line = goldDerivations.readLine();
			}
		} catch (IOException e) {
			System.err.println(e);
			return false;
		}
	}
}
