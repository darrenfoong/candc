package chart_parser;

import io.Supertag;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import model.Lexicon;
import cat_combination.RuleInstancesParams;
import cat_combination.SuperCategory;

public class ChartTrainParserBeamDerivations extends ChartTrainParserBeam {
	protected Chart goldChart;

	private int buildTreeLeafIndex;

	public ChartTrainParserBeamDerivations(
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
	 * while training using a gold derivation from a gold parser.
	 * 
	 * @param in file containing supertagged sentences to parse
	 * @param goldChart gold chart from gold parser
	 * @param stagsIn file containing additional supertags
	 * @param log log file
	 * @param betas array of values of beta
	 * @return true if sentence is parsed or skipped, false if there are no
	 * sentences left
	 */
	public boolean parseSentence(BufferedReader in, Chart goldChart, BufferedReader stagsIn, PrintWriter log, double[] betas) {
		this.goldChart = goldChart;
		return parseSentence(in, stagsIn, log, betas);
	}

	/**
	 * Initialises maxviolation search variables, merges gold chart for
	 * current sentence, and recomputes scores for gold derivation based on
	 * current weights.
	 */
	@Override
	protected boolean preParse() {
		maxViolation = 0;
		maxViolationCell = null;
		maxViolationCells.clear();
		violationCells.clear();

		if (!mergeGoldChart(goldChart)) {
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

		System.out.println("Gold chart merged.");

		return true;
	}

	/**
	 * Calculates violation of current cell and searches for maxviolation,
	 * updating it if a larger violation is found.
	 */
	@Override
	protected void postParse(int pos, int span, int numWords) {
		CellTrainBeam cell = (CellTrainBeam) (chart.cell(pos, span));
		double violation = cell.calcViolation();

		if ( !parallelUpdate ) {
			if (violation > maxViolation) {
				maxViolation = violation;
				maxViolationCell = new CellCoords(pos, span);
				System.out.println("New maxViolation found at (" + pos + "," + span + "); maxViolation: " + violation);
			}
		} else {
			if ( violation > 0 ) {
				violationCells.add(new CellCoords(pos, span, violation));
				System.out.println("Adding (" + pos + "," + span + "); to violationCells; violation: " + violation);
			}
		}

		System.out.println("cell (" + pos + "," + span + "); violation: " + violation + "; current maxViolation: " + maxViolation);
	}

	/**
	 * Initialises scores of leaf supercategories of gold derivation tree by
	 * looking up supertag probabilities in loaded sentence.
	 */
	protected void initGoldLeafScores() {
		int numWords = sentence.words.size();

		System.out.println("Initialising gold leaf scores");
		for (int position = 0; position < numWords; position++) {
			CellTrainBeam cell = (CellTrainBeam) chart.cell(position, 1);
			SuperCategory goldCat = cell.goldSuperCat;

			while ( goldCat.leftChild != null ) {
				goldCat = goldCat.leftChild;
			}

			ArrayList<Supertag> supertags = sentence.multiSupertags.get(position);
			for ( Supertag supertag : supertags ) {
				if ( supertag.lexicalCategory.toStringNoOuterBrackets().equals(goldCat.cat.toStringNoOuterBrackets()) ) {
					double log_prob = chart.weights.getWeight(0) * Math.log(supertag.probability);
					goldCat.score = log_prob;
					goldCat.inside = log_prob;
					break;
				}
			}
		}

		System.out.println("Initialised");
	}

	/**
	 * Merges a gold chart from a gold parser into current chart i.e. updates
	 * the goldCat of each cell to the supercategory of the gold derivation in
	 * the corresponding cell.
	 * 
	 * @param goldChart gold chart from gold parser containing gold derivation
	 * @return true if merge succeeds, false if merge fails
	 */
	private boolean mergeGoldChart(Chart goldChart) {
		Cell root = goldChart.root();

		double maxScore = -Double.MAX_VALUE;
		SuperCategory maxRoot = null;

		for (SuperCategory superCat : root.getSuperCategories()) {
			double currentScore = superCat.maxEquivScore;
			if (currentScore > maxScore) {
				maxScore = currentScore;
				maxRoot = superCat.maxEquivSuperCat;
			}
		}

		buildTreeLeafIndex = 0;

		if ( maxRoot != null ) {
			int[] result = buildTree(maxRoot);
			System.out.println("Root of gold chart is at (" + result[0] + "," + result[1] + "); buildTreeLeafIndex is " + buildTreeLeafIndex);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Given the root supercategory of a tree, places each supercategory of the
	 * tree in the correct cell.
	 * 
	 * The method assumes that the first invocation is on the root supercategory
	 * and the class variable buildTreeLeafIndex is the position of the first
	 * leaf supercategory reached through depth-first search.
	 * 
	 * @param parentCat root supercategory of tree
	 * @return an array of two integers, containing the position and span of the
	 * cell where the root supercategory resides in the chart
	 */
	private int[] buildTree(SuperCategory parentCat) {
		int[] leftResult;
		int[] rightResult;
		int position;
		int span;

		if ( parentCat.leftChild != null ) {
				leftResult = buildTree(parentCat.leftChild);
				position = leftResult[0];

				if ( parentCat.rightChild != null ) {
					rightResult = buildTree(parentCat.rightChild);
					span = leftResult[1] + rightResult[1];
				} else {
					span = leftResult[1];
				}
		} else {
			position = buildTreeLeafIndex;
			span = 1;
			buildTreeLeafIndex++;
		}

		CellTrainBeam foundCell = (CellTrainBeam) chart.cell(position, span);
		foundCell.goldSuperCat = parentCat;

		int[] result = {position, span};
		return result;
	}
}
