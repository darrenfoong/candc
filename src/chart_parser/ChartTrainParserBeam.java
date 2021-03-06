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

import cat_combination.FilledDependency;
import cat_combination.RuleInstancesParams;
import cat_combination.SuperCategory;
import lexicon.Category;
import model.Lexicon;
import training.Feature;

public class ChartTrainParserBeam extends ChartParserBeam {
	protected boolean parallelUpdate;
	protected boolean updateLogP;
	protected boolean updateDepNN;
	protected double maxViolation;

	protected Feature logPFeature;
	protected Feature depNNFeature;

	protected CellCoords maxViolationCell;
	protected LinkedList<CellCoords> violationCells = new LinkedList<CellCoords>();
	protected ArrayList<CellCoords> maxViolationCells = new ArrayList<CellCoords>();

	private Feature[] trainingFeatures;
	protected BufferedReader goldDepsPerCell;
	protected ArrayList<Category> oracleSupertags;
	protected OracleDecoder oracleDecoder;

	public ChartTrainParserBeam(
			String grammarDir,
			boolean altMarkedup,
			boolean eisnerNormalForm,
			int MAX_WORDS,
			RuleInstancesParams ruleInstancesParams,
			Lexicon lexicon,
			String featuresFile,
			String weightsFile,
			boolean newFeatures,
			boolean cubePruning,
			double[] betas,
			int beamSize,
			double beta,
			boolean parallelUpdate,
			boolean updateLogP,
			boolean updateDepNN) throws IOException {
		super(grammarDir, altMarkedup, eisnerNormalForm, MAX_WORDS, ruleInstancesParams,
				lexicon, featuresFile, weightsFile, newFeatures, false, cubePruning,
				betas, beamSize, beta);

		this.chart = new Chart(MAX_WORDS, categories.dependencyRelations, false, true);
		this.chart.setWeights(this.weights);

		int numFeatures = features.numFeatures;
		this.trainingFeatures = new Feature[numFeatures];

		for (int i = 0; i < numFeatures; i++) {
			trainingFeatures[i] = new Feature(i, weights.getWeight(i));
		}

		this.parallelUpdate = parallelUpdate;
		this.updateLogP = updateLogP;
		this.updateDepNN = updateDepNN;

		logPFeature = new Feature(-1, weights.getLogP());
		depNNFeature = new Feature(-2, weights.getDepNN());

		this.oracleSupertags = new ArrayList<Category>();
	}

	public void setOracleDecoder(OracleDecoder oracleDecoder) {
		this.oracleDecoder = oracleDecoder;
	}

	public void setGoldDepsPerCell(BufferedReader goldDepsPerCell) {
		this.goldDepsPerCell = goldDepsPerCell;
	}

	/**
	 * Clears max violation fields and reads in gold dependencies per cell.
	 */
	@Override
	protected boolean preParse() throws IOException {
		maxViolation = 0;
		maxViolationCell = null;
		maxViolationCells.clear();
		violationCells.clear();

		if (!readDepsPerCell(goldDepsPerCell)) {
			return false;
		}

		logger.info("Gold dependencies per cell merged.");

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
				logger.info("New maxViolation found at (" + pos + "," + span + "); maxViolation: " + violation);
			}
		} else {
			if ( violation > 0 ) {
				violationCells.add(new CellCoords(pos, span, violation));
				logger.info("Adding (" + pos + "," + span + "); to violationCells; violation: " + violation);
			}
		}

		logger.info("cell (" + pos + "," + span + "); violation: " + violation + "; current maxViolation: " + maxViolation);
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
			logger.info("Adding (" + top.pos + "," + top.span + "); violation: " + top.violation);
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
				logger.info("Adding (" + top.pos + "," + top.span + "); violation: " + top.violation);
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

		if ( superCat.marked ) {
			return;
		}

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

	private void markCommonSuperCats(SuperCategory goldRootSuperCat, SuperCategory foundRootSuperCat) {
		ArrayList<SuperCategory> goldSuperCats = new ArrayList<SuperCategory>();
		ArrayList<SuperCategory> foundSuperCats = new ArrayList<SuperCategory>();

		preOrder(goldRootSuperCat, goldSuperCats);
		preOrder(foundRootSuperCat, foundSuperCats);

		for ( SuperCategory goldCat : goldSuperCats ) {
			for ( SuperCategory foundCat : foundSuperCats ) {
				if ( goldCat == foundCat ) {
					foundCat.marked = true;
					break;
				}
			}
		}
	}

	private void preOrder(SuperCategory superCat, ArrayList<SuperCategory> superCats) {
		SuperCategory leftChild = superCat.leftChild;
		SuperCategory rightChild = superCat.rightChild;

		superCats.add(superCat);

		if (leftChild != null) {
			preOrder(leftChild, superCats);
			if (rightChild != null) {
				preOrder(rightChild, superCats);
			}
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
	public SuperCategory updateWeights(int numTrainInstances) {
		if ( !parallelUpdate ) {
			if (maxViolationCell == null) {
				logger.info("No non-zero violation in the chart.");
				return null;
			} else {
				CellTrainBeam cell = (CellTrainBeam) chart.cell(maxViolationCell.pos, maxViolationCell.span);

				logger.info("maxViolation exists in chart (score: " + cell.maxSuper.score + ", gold score: " + cell.goldSuperCat.score + ", violation: " + maxViolation + "); updating features.");

				boolean atRoot;

				HashSet<Integer> featuresToUpdate = new HashSet<Integer>();

				logger.info("Updating features for (" + maxViolationCell.pos + "," + maxViolationCell.span + ")");

				if ( maxViolationCell.pos == 0 && maxViolationCell.span == sentence.words.size() ) {
					atRoot = true;
					logger.info("maxViolation found at root");
				} else {
					atRoot = false;
				}

				markCommonSuperCats(cell.goldSuperCat, cell.maxSuper);

				logger.info("Incrementing gold tree features");
				updateFeatureParams(cell.goldSuperCat, true, atRoot, featuresToUpdate);
				logger.info("Decrementing found tree features");
				updateFeatureParams(cell.maxSuper, false, atRoot, featuresToUpdate);

				updateAllWeights(featuresToUpdate, numTrainInstances);

				return cell.maxSuper;
			}
		} else {
			findLocalMaxViolations();

			if (maxViolationCells.isEmpty()) {
				logger.info("No non-zero violation in the chart.");
				return null;
			} else {
				CellTrainBeam cell = null;

				logger.info("maxViolations exist in chart.");

				boolean atRoot;

				HashSet<Integer> featuresToUpdate = new HashSet<Integer>();

				for ( CellCoords pair : maxViolationCells ) {
					logger.info("Updating features for (" + pair.pos + "," + pair.span + ")");
					cell = (CellTrainBeam) chart.cell(pair.pos, pair.span);
					if ( pair.pos == 0 && pair.span == sentence.words.size() ) {
						atRoot = true;
						logger.info("maxViolation found at root");
					} else {
						atRoot = false;
					}

					markCommonSuperCats(cell.goldSuperCat, cell.maxSuper);

					updateFeatureParams(cell.goldSuperCat, true, atRoot, featuresToUpdate);
					updateFeatureParams(cell.maxSuper, false, atRoot, featuresToUpdate);
				}

				updateAllWeights(featuresToUpdate, numTrainInstances);

				int totalSpan = 0;

				for ( CellCoords pair : maxViolationCells ) {
					totalSpan += pair.span;
				}

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

		for ( int ID : featureIDs ) {
			logger.info(" Updating feature " + ID);

			if (positiveUpdate) {
				trainingFeatures[ID].incrementLambdaUpdate();
			} else {
				trainingFeatures[ID].decrementLambdaUpdate();
			}

			featuresToUpdate.add(ID);
		}

		if (updateLogP) {
			logger.info(" Finally updating feature logp");

			if (positiveUpdate) {
				logPFeature.setLambdaUpdate(logPFeature.getLambdaUpdate() + calcSumLeafInitialScore(superCat));
			} else {
				logPFeature.setLambdaUpdate(logPFeature.getLambdaUpdate() - calcSumLeafInitialScore(superCat));
			}
		}

		if (updateDepNN) {
			logger.info(" Finally updating feature depnn");

			if (positiveUpdate) {
				depNNFeature.setLambdaUpdate(depNNFeature.getLambdaUpdate() + calcSumDepNNScore(superCat));
			} else {
				depNNFeature.setLambdaUpdate(depNNFeature.getLambdaUpdate() - calcSumDepNNScore(superCat));
			}
		}
	}

	/**
	 * Updates weights for all features.
	 * 
	 * @param featuresToUpdate set containing IDs for features to be updated
	 * @param numTrainInstances for the fast averaged perceptron
	 */
	private void updateAllWeights(HashSet<Integer> featuresToUpdate, int numTrainInstances) {
		for ( Integer ID : featuresToUpdate ) {
			if ( ID != null ) {
				if ( trainingFeatures[ID].getLambdaUpdate() != 0.0 ) {
					trainingFeatures[ID].perceptronUpdateFast(numTrainInstances);
					weights.setWeight(ID, trainingFeatures[ID].getLambda());
				}
			}
		}

		if ( updateLogP ) {
			logPFeature.perceptronUpdateFast(numTrainInstances);
			weights.setLogP(logPFeature.getLambda());
			logger.info("Feature logp is now " + weights.getLogP());
		}

		if ( updateDepNN ) {
			depNNFeature.perceptronUpdateFast(numTrainInstances);
			weights.setDepNN(depNNFeature.getLambda());
			logger.info("Feature depnn is now " + weights.getDepNN());
		}
	}

	/**
	 * Prints weights to file.
	 * 
	 * @param out file containing weights
	 * @param numTrainInstances averaging factor for fast averaged perceptron
	 */
	public void printWeights(PrintWriter out, int numTrainInstances) {
		logPFeature.perceptronUpdateFast(numTrainInstances);
		out.println("logp:" + logPFeature.getCumulativeLambda()/numTrainInstances);

		depNNFeature.perceptronUpdateFast(numTrainInstances);
		out.println("depnn:" + depNNFeature.getCumulativeLambda()/numTrainInstances);

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
	protected boolean readDepsPerCell(BufferedReader goldDepsPerCell) throws IOException {
		// read oracle supertags
		String line = goldDepsPerCell.readLine();

		if ( line == null ) {
			return false;
		}

		if ( line.isEmpty() ) {
			return true;
		}

		String[] tokens;

		oracleSupertags.clear();
		tokens = line.split("\\s");
		for ( String token : tokens ) {
			Category lexicalCategory = categories.getCategory(token);
			if ( lexicalCategory == null ) {
				throw new Error("can't find oracle supertag! " + token);
			}
			oracleSupertags.add(lexicalCategory);
		}

		if ( oracleSupertags.size() != sentence.words.size() ) {
			throw new Error("Incorrect number of oracle supertags: " + oracleSupertags.size() + "; expected: " + sentence.words.size());
		}

		// read each cell
		while (true) {
			line = goldDepsPerCell.readLine();

			if ( line.isEmpty() ) {
				return true;
			}

			tokens = line.split("\\s");
			if ( tokens.length != 3 ) {
				throw new Error("expecting 3 fields");
			}

			int pos = Integer.parseInt(tokens[0]);
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
				CellTrainBeam cell = (CellTrainBeam) (chart.cell(pos, span));
				cell.addDep(dep);
			}
		}
	}
}
