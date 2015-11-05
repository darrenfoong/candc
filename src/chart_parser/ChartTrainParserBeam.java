package chart_parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import lexicon.Category;
import model.Lexicon;
import statistics.HasUpdateStatistics;
import statistics.HypothesisSizeStatistics;
import training.Feature;
import cat_combination.FilledDependency;
import cat_combination.RuleInstancesParams;
import cat_combination.SuperCategory;

public class ChartTrainParserBeam extends ChartParserBeam {
	public HasUpdateStatistics hasUpdateStatistics = new HasUpdateStatistics();
	public HypothesisSizeStatistics hypothesisSizeStatistics = new HypothesisSizeStatistics();

	protected boolean parallelUpdate;
	protected boolean updateLogP;
	protected double maxViolation;

	protected CellCoords maxViolationCell;
	protected LinkedList<CellCoords> violationCells = new LinkedList<CellCoords>();
	protected ArrayList<CellCoords> maxViolationCells = new ArrayList<CellCoords>();

	private Feature[] trainingFeatures;
	private BufferedReader goldDepsPerCell;
	private ArrayList<Category> oracleSupertags;
	private OracleDecoder oracleDecoder;

	public ChartTrainParserBeam(
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
				lexicon, featuresFile, weightsFile, newFeatures, false, cubePruning,
				beamSize, beta);

		this.chart = new Chart(MAX_WORDS, output, categories.dependencyRelations, false, true);
		this.chart.setWeights(this.weights);

		int numFeatures = features.numFeatures;
		this.trainingFeatures = new Feature[numFeatures];

		for (int i = 0; i < numFeatures; i++) {
			trainingFeatures[i] = new Feature(i, weights.getWeight(i));
		}

		this.parallelUpdate = parallelUpdate;
		this.updateLogP = updateLogP;
		this.oracleSupertags = new ArrayList<Category>();
	}

	/**
	 * Parses one supertagged sentence using chart parser with beam search,
	 * while training using gold dependencies.
	 * 
	 * @param in file containing supertagged sentences to parse
	 * @param goldDepsPerCell file containing gold dependencies per cell
	 * @param stagsIn file containing additional supertags
	 * @param log log file
	 * @param betas array of values of beta
	 * @param oracleDecoder oracle decoder
	 * @return true if sentence is parsed or skipped, false if there are no
	 * sentences left
	 */
	public boolean parseSentence(BufferedReader in, BufferedReader goldDepsPerCell, BufferedReader stagsIn, PrintWriter log, double[] betas, OracleDecoder oracleDecoder) {
		this.goldDepsPerCell = goldDepsPerCell;
		this.oracleDecoder = oracleDecoder;
		return parseSentence(in, stagsIn, log, betas);
	}

	/**
	 * Clears max violation fields and reads in gold dependencies per cell.
	 */
	@Override
	protected boolean preParse() {
		maxViolation = 0;
		maxViolationCell = null;
		maxViolationCells.clear();
		violationCells.clear();

		if (!readDepsPerCell(goldDepsPerCell)) {
			return false;
		}

		System.out.println("Gold dependencies per cell merged.");

		return true;
	}

	/**
	 * Calculates violation of current cell. 
	 * 
	 * Single node max violation update: searches for max violation,
	 * updating it if a larger violation is found.
	 * 
	 * Parallel max violation update: nothing.
	 */
	@Override
	protected void postParse(int pos, int span, int numWords) {
		CellTrainBeam cell = (CellTrainBeam) (chart.cell(pos, span));
		double violation = cell.calcViolation(sentence, oracleDecoder, oracleSupertags, pos);

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
	 * Computes sets of frontiers for parallel max violation update for a
	 * subsentence of a sentence and adds it to maxViolationCells.
	 */
	private void findLocalMaxViolations() {
		Collections.sort(violationCells, new Comparator<CellCoords>(){
			@Override
			public int compare(CellCoords p1, CellCoords p2){
				return Double.compare(p2.violation, p1.violation);
			}});

		while ( !violationCells.isEmpty() ) {
			CellCoords top = violationCells.removeFirst();
			System.out.println("Adding (" + top.pos + "," + top.span + "); violation: " + top.violation);
			maxViolationCells.add(top);

			int topLeft = top.pos;
			int topRight = top.end;

			Iterator<CellCoords> it = violationCells.iterator();
			while (it.hasNext()) {
				CellCoords p = it.next();
				int pLeft = p.pos;
				int pRight = p.end;

				if ( !((pRight < topLeft) || (pLeft > topRight)) ) {
					it.remove();
				}
			}
		}
	}

	private void findLocalMaxViolationsDP() {
		/*
		 * dynamic programming solution to the weighted interval scheduling
		 * problem
		 */

		if ( violationCells.isEmpty() ) {
			return;
		}

		// sort by end ("finishing time")
		Collections.sort(violationCells, new Comparator<CellCoords>(){
			@Override
			public int compare(CellCoords p1, CellCoords p2){
				return p1.end - p2.end;
			}});

		int numViolationCells = violationCells.size();

		int[] rightMostArray = new int[numViolationCells];
		double[] optArray = new double[numViolationCells];

		boolean[] inclArray = new boolean[numViolationCells];
		int[] backTrackArray = new int[numViolationCells];

		// calculate rightmost non-overlapping interval for each interval
		for ( int i = 0; i < numViolationCells; i++ ) {
			// default: no rightmost non-overlapping interval
			int rightMost = -1;

			// algorithm checks from right to left
			// apparently can be optimised with binary search
			for ( int j = i - 1; j >= 0; j-- ) {
				if ( violationCells.get(i).pos > violationCells.get(j).end ) {
					rightMost = j;

					// break upon finding non-overlapping interval
					// guaranteed to be rightmost due to search direction
					break;
				}
			}

			rightMostArray[i] = rightMost;
		}

		// base case for one interval
		optArray[0] = violationCells.get(0).violation;
		inclArray[0] = true;
		backTrackArray[0] = -1;

		// iterative bottom-up solution starting from second interval
		for ( int i = 1; i < numViolationCells; i++ ) {
			double v1, v2;

			if ( rightMostArray[i] == -1 ) {
				// if interval does not have rightmost non-overlapping interval
				// optArray[-1] throws ArrayOutOfBounds exception
				v1 = violationCells.get(i).violation;
			} else {
				v1 = violationCells.get(i).violation + optArray[rightMostArray[i]];
			}

			v2 = optArray[i-1];

			if ( v1 > v2 ) {
				optArray[i] = v1;
				inclArray[i] = true;
				backTrackArray[i] = rightMostArray[i];
			} else {
				optArray[i] = v2;
				inclArray[i] = false;
				backTrackArray[i] = i - 1;
			}
		}

		int backTrack = numViolationCells - 1;

		while ( backTrack >= 0 ) {
			if ( inclArray[backTrack] ) {
				CellCoords top = violationCells.get(backTrack);
				System.out.println("Adding (" + top.pos + "," + top.span + "); violation: " + top.violation);
				maxViolationCells.add(top);
			}

			backTrack = backTrackArray[backTrack];
		}
	}

	/**
	 * Collects all features in tree rooted by superCat in featuresID.
	 * 
	 * @param superCat root supercategory of tree
	 */
	private void collectFeatures(SuperCategory superCat) {
		SuperCategory leftChild = superCat.leftChild;
		SuperCategory rightChild = superCat.rightChild;

		if (leftChild != null) {
			collectFeatures(leftChild);
			if (rightChild != null) {
				collectFeatures(rightChild);
				features.collectBinaryFeatures(superCat, sentence, featureIDs);
			} else {
				features.collectUnaryFeatures(superCat, sentence, featureIDs);
			}
		} else {
			features.collectLeafFeatures(superCat, sentence, featureIDs);
		}
	}

	/**
	 * Single node max violation update: updates weights.
	 * 
	 * Parallel max violation update: computes set of frontiers for entire
	 * sentence, updates weights.
	 * 
	 * @param log log file
	 * @param numTrainInstances for the fast averaged perceptron
	 * @return highest scoring supercategory in the cell that has the largest
	 * violation (last frontier in the case of parallel max violation update),
	 * null if there is no non-zero violation in the chart.
	 */
	public SuperCategory updateWeights(PrintWriter log, int numTrainInstances) {
		if ( !parallelUpdate ) {
			if (maxViolationCell == null) {
				System.out.println("No non-zero violation in the chart.");
				log.println("No non-zero violation in the chart.");

				hasUpdateStatistics.addData(false);
				// hypothesisSizeStatistics.addData(0, sentence.words.size());

				return null;
			} else {
				CellTrainBeam cell = (CellTrainBeam) chart.cell(maxViolationCell.pos, maxViolationCell.span);

				System.out.println("maxViolation exists in chart (score: " + cell.maxSuper.score + ", gold score: " + cell.goldSuperCat.score + ", violation: " + maxViolation + "); updating features.");
				log.println("maxViolation exists in chart (score: " + cell.maxSuper.score + ", gold score: " + cell.goldSuperCat.score + ", violation: " + maxViolation + "); updating features.");

				boolean atRoot;

				HashSet<Integer> featuresToUpdate = new HashSet<Integer>();

				System.out.println("Updating features for (" + maxViolationCell.pos + "," + maxViolationCell.span + ")");

				if ( maxViolationCell.pos == 0 && maxViolationCell.span == sentence.words.size() ) {
					atRoot = true;
					System.out.println("maxViolation found at root");
				} else {
					atRoot = false;
				}

				System.out.println("Incrementing gold tree features");
				updateFeatureParams(cell.goldSuperCat, true, atRoot, featuresToUpdate);
				System.out.println("Decrementing found tree features");
				updateFeatureParams(cell.maxSuper, false, atRoot, featuresToUpdate);

				updateAllWeights(featuresToUpdate, numTrainInstances);

				hasUpdateStatistics.addData(true);
				hypothesisSizeStatistics.addData(maxViolationCell.span, sentence.words.size());

				return cell.maxSuper;
			}
		} else {
			findLocalMaxViolations();

			if (maxViolationCells.isEmpty()) {
				System.out.println("No non-zero violation in the chart.");
				log.println("No non-zero violation in the chart.");

				hasUpdateStatistics.addData(false);
				// hypothesisSizeStatistics.addData(0, sentence.words.size());

				return null;
			} else {
				CellTrainBeam cell = null;

				System.out.println("maxViolations exist in chart.");
				log.println("maxViolations exist in chart.");

				boolean atRoot;

				HashSet<Integer> featuresToUpdate = new HashSet<Integer>();

				for ( CellCoords pair : maxViolationCells ) {
					System.out.println("Updating features for (" + pair.pos + "," + pair.span + ")");
					cell = (CellTrainBeam) chart.cell(pair.pos, pair.span);
					if ( pair.pos == 0 && pair.span == sentence.words.size() ) {
						atRoot = true;
						System.out.println("maxViolation found at root");
					} else {
						atRoot = false;
					}
					updateFeatureParams(cell.goldSuperCat, true, atRoot, featuresToUpdate);
					updateFeatureParams(cell.maxSuper, false, atRoot, featuresToUpdate);
				}

				updateAllWeights(featuresToUpdate, numTrainInstances);

				hasUpdateStatistics.addData(true);

				int totalSpan = 0;

				for ( CellCoords pair : maxViolationCells ) {
					totalSpan += pair.span;
				}

				hypothesisSizeStatistics.addData(totalSpan, sentence.words.size());

				return cell.maxSuper;
			}
		}
	}

	/**
	 * Updates parameters for features of supercategory.
	 * 
	 * @param superCat supercategory
	 * @param positiveUpdate true if positive update, false if negative update
	 * @param featuresToUpdate set containing IDs of features to be updated
	 */
	private void updateFeatureParams(SuperCategory superCat, boolean positiveUpdate, boolean atRoot, HashSet<Integer> featuresToUpdate) {
		featureIDs.clear();

		if (atRoot) {
			features.collectRootFeatures(superCat, sentence, featureIDs);
		}

		collectFeatures(superCat);

		Iterator<Integer> it = featureIDs.iterator();
		while ( it.hasNext() ) {
			int ID = it.next();

			System.out.println(" Updating feature " + ID);

			if (positiveUpdate) {
				trainingFeatures[ID].incrementLambdaUpdate();
			} else {
				trainingFeatures[ID].decrementLambdaUpdate();
			}

			featuresToUpdate.add(ID);
		}

		if (updateLogP) {
			System.out.println(" Finally updating feature 0");
	
			if (positiveUpdate) {
				trainingFeatures[0].setLambdaUpdate(trainingFeatures[0].getLambdaUpdate() + calcSumLeafInitialScore(superCat));
			} else {
				trainingFeatures[0].setLambdaUpdate(trainingFeatures[0].getLambdaUpdate() - calcSumLeafInitialScore(superCat));
			}

			featuresToUpdate.add(0);
		}
	}

	/**
	 * Updates weights for all features.
	 * 
	 * @param featuresToUpdate set containing IDs for features to be updated
	 * @param numTrainInstances for the fast averaged perceptron
	 */
	private void updateAllWeights(HashSet<Integer> featuresToUpdate, int numTrainInstances) {
		Iterator<Integer> it = featuresToUpdate.iterator();
		while ( it.hasNext() ) {
			Integer ID = it.next();

			if ( ID != null ) {
				if ( trainingFeatures[ID].getLambdaUpdate() != 0.0 ) {
					trainingFeatures[ID].perceptronUpdateFast(numTrainInstances);
					weights.setWeight(ID, trainingFeatures[ID].getLambda());
				}
			}
		}

		System.out.println("Feature 0 is now " + weights.getWeight(0));
	}

	/**
	 * Prints weights to file.
	 * 
	 * @param out file containing weights
	 * @param numTrainInstances averaging factor for fast averaged perceptron
	 */
	public void printWeights(PrintWriter out, int numTrainInstances) {
		for (int i = 0; i < trainingFeatures.length; i++) {
			trainingFeatures[i].perceptronUpdateFast(numTrainInstances);
			out.println(trainingFeatures[i].getCumulativeLambda()/numTrainInstances);
		}
	}

	/**
	 * Reads gold dependencies per cell for current sentence and adds them
	 * to corresponding cells
	 * 
	 * @param goldDepsPerCell file containing gold dependencies per cell
	 * @return true if success, false if failure or there are no gold 
	 * dependencies left.
	 */
	private boolean readDepsPerCell(BufferedReader goldDepsPerCell) {
		try {
			String line = goldDepsPerCell.readLine();
			boolean readStags = false;

			if (line == null) {
				return false;
			}

			if (line.isEmpty()) {
				return false;
			}

			while (true) {
				if (line == null) {
					return false;
				}

				if (line.isEmpty()) {
					return true;
				}

				String[] tokens;

				if (!readStags) {
					oracleSupertags.clear();
					tokens = line.split("\\s");
					for ( String token : tokens ) {
						Category lexicalCategory = categories.getCategory(token);
						if ( lexicalCategory == null ) {
							throw new Error("can't find oracle supertag! " + token);
						}
						oracleSupertags.add(lexicalCategory);
					}

					readStags = true;
					line = goldDepsPerCell.readLine();

					if ( line == null ) {
						throw new IllegalArgumentException("Unexpected end of stream");
					}

					if (line.isEmpty()) {
						return false;
					}
				}

				tokens = line.split("\\s");
				if ( tokens.length != 3 ) {
					throw new Error("expecting 3 fields");
				}

				int start = Integer.parseInt(tokens[0]);
				int span = Integer.parseInt(tokens[1]);
				int numDeps = Integer.parseInt(tokens[2]);

				for (int i = 0; i < numDeps; i++) {
					line = goldDepsPerCell.readLine();

					if ( line != null ) {
						tokens = line.split("\\s");
					} else {
						throw new IllegalArgumentException("Unexpected end of stream");
					}

					short headIndex = Short.parseShort(tokens[0]);
					short slot = Short.parseShort(tokens[2]);
					short fillerIndex = Short.parseShort(tokens[3]);
					short unaryRuleID = (short) (0);
					short lrange = (short) (0);
					short relID;

					String markedupString = categories.getString(tokens[1]);

					if (markedupString == null) {
						relID = 0;
					} else {
						relID = categories.dependencyRelations.getRelID_II(markedupString, slot);
					}

					FilledDependency dep = new FilledDependency(relID, headIndex, fillerIndex, unaryRuleID, lrange);
					CellTrainBeam cell = (CellTrainBeam) (chart.cell(start, span));
					cell.addDep(dep);
				}

				line = goldDepsPerCell.readLine();
			}
		} catch (IOException e) {
			System.err.println(e);
			return false;
		}
	}
}
