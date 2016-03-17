package chart_parser;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cat_combination.FilledDependency;
import cat_combination.RuleInstancesParams;
import cat_combination.Rules;
import cat_combination.SuperCategory;
import cat_combination.Variable;
import io.Sentence;
import lexicon.Categories;
import lexicon.Relations;
import model.Features;
import model.Lexicon;
import model.Weights;
import uk.ac.cam.cl.depnn.io.Feature;

public class ChartParser {
	public final int MAX_SUPERCATS;

	public boolean oracleFscore;
	public boolean adaptiveSupertagging;
	public boolean maxSuperCatsExceeded = false;
	public boolean maxWordsExceeded = false;

	public Categories categories;
	public Rules rules;
	public Chart chart;
	public Lexicon lexicon;
	public Features features;
	public Weights weights;
	public Sentence sentence;
	public ArrayList<SuperCategory> results;
	public ArrayList<Integer> featureIDs;

	public static final Logger logger = LogManager.getLogger(ChartParser.class);

	public ChartParser(
					String grammarDir,
					boolean altMarkedup,
					boolean eisnerNormalForm,
					int MAX_WORDS,
					int MAX_SUPERCATS,
					boolean oracleFscore,
					boolean adaptiveSupertagging,
					RuleInstancesParams ruleInstancesParams,
					Lexicon lexicon,
					String featuresFile,
					String weightsFile,
					boolean newFeatures,
					boolean compactWeights) throws IOException {
		this.MAX_SUPERCATS = MAX_SUPERCATS;
		this.oracleFscore = oracleFscore;
		this.adaptiveSupertagging = adaptiveSupertagging;
		this.categories = new Categories(grammarDir, altMarkedup);
		this.rules = new Rules(eisnerNormalForm, categories, ruleInstancesParams);
		this.lexicon = lexicon;

		if ( compactWeights ) {
			this.weights = new Weights();
			this.features = new Features(featuresFile, weightsFile, weights, categories, newFeatures);
		} else {
			this.features = (featuresFile != null ? new Features(featuresFile, categories, newFeatures) : null);
			this.weights = (weightsFile != null ? new Weights(weightsFile, features.numFeatures) : null);
		}

		this.sentence = new Sentence(MAX_WORDS);
		this.results = new ArrayList<SuperCategory>();
		this.featureIDs = new ArrayList<Integer>();

		this.chart = new Chart(MAX_WORDS, categories.dependencyRelations, oracleFscore, false);
		this.chart.setWeights(this.weights);
	}

	public boolean parseSentence(Sentence sentence, double[] betas) throws IOException {
		int betaLevel;

		if (adaptiveSupertagging) {
			betaLevel = 0;
		} else {
			if (betas.length < 3) {
				logger.error("Need at least 3 beta values for reverse adaptive supertagging.");
				return true;
			}
			betaLevel = 2;
		}

		this.sentence = sentence;

		maxWordsExceeded = false;
		int numWords = sentence.words.size();
		if ( numWords > chart.MAX_WORDS ) {
			logger.info(" Sentence has " + numWords + " words; MAX_WORDS exceeded.");
			maxWordsExceeded = true;
			return true;
		} else {
			logger.info(" Sentence has " + numWords + " words.");
		}

		if (lexicon != null) {
			sentence.addIDs(lexicon);
		}

		while (betaLevel < betas.length) {
			logger.info("Trying beta level " + betaLevel + " with beta value " + betas[betaLevel]);

			maxSuperCatsExceeded = false;
			chart.clear();
			chart.load(sentence, betas[betaLevel], oracleFscore, false);

			/*
			 * apply unary rules to lexical categories; typeChange needs to come
			 * before typeRaise since some results of typeChange can be
			 * type-raised (but not vice versa)
			 */
			for (int i = 0; i < numWords; i++) {
				typeChange(chart.cell(i, 1), i, 1);
				typeRaise(chart.cell(i, 1), i, 1);
			}

			jloop:
			for (int j = 2; j <= numWords; j++) {
				for (int i = 0; i <= numWords - j; i++) {
					for (int k = 1; k < j; k++) {
						if (Chart.getNumSuperCategories() > MAX_SUPERCATS) {
							maxSuperCatsExceeded = true;
							logger.info("MAX_SUPERCATS exceeded. (" + Chart.getNumSuperCategories() + " > " + MAX_SUPERCATS + ")");
							break jloop;
						}

						logger.trace("Combining cells: (" + i + "," + k + ") (" + (i+k) + "," + (j-k) + ")");

						combine(chart.cell(i, k), chart.cell(i+k, j-k), i, j);
					}

					if (j < numWords) {
						typeChange(chart.cell(i, j), i, j);
						typeRaise(chart.cell(i, j), i, j);
					}
				}
			}

			Cell root = chart.root();

			if (adaptiveSupertagging) {
				if (maxSuperCatsExceeded) {
					return true;
				}

				if (root.isEmpty()) {
					betaLevel++;
				} else {
					return true;
				}
			} else {
				if (maxSuperCatsExceeded && betaLevel > 1) {
					betaLevel++;
				} else if (root.isEmpty() && (betaLevel == 1 || betaLevel == 2)) {
					betaLevel--;
				} else {
					return true;
				}
			}
		}

		return true;
	}

	public void combine(Cell leftCell, Cell rightCell, int position, int span) {
		results.clear();

		for (SuperCategory leftSuperCat : leftCell.getSuperCategories()) {
			for (SuperCategory rightSuperCat : rightCell.getSuperCategories()) {
				boolean success = rules.combine(leftSuperCat, rightSuperCat, results, sentence);
					if (success) {
						logger.trace("success!: " + results.get(results.size() - 1).cat);
					} else {
						logger.trace("failed to combine");
					}
			}
		}

		chart.add(position, span, results, oracleFscore);
	}

	public void typeChange(Cell cell, int position, int span) {
		results.clear();
		rules.typeChange(cell.getSuperCategories(), results);
		chart.add(position, span, results, oracleFscore);
	}

	public void typeRaise(Cell cell, int position, int span) {
		results.clear();
		rules.typeRaise(cell.getSuperCategories(), results);
		chart.add(position, span, results, oracleFscore);
	}

	public boolean calcScores() {
		Cell root = chart.root();

		if (root.isEmpty()) {
			return false;
		}

		for (SuperCategory superCat : root.getSuperCategories()) {
			calcRootCanonical(superCat);
		}

		return true;
	}

	private void calcRootCanonical(SuperCategory superCat) {
		// TODO reimplement as linked list
		for (SuperCategory equiv = superCat; equiv != null; equiv = equiv.next) {
			calcScore(equiv);

			// deal with the root features here separately;
			// collectBinaryFeatures called separately from calcScore
			featureIDs.clear();

			features.collectRootFeatures(equiv, sentence, featureIDs);

			for ( int featureID : featureIDs ) {
				equiv.score += weights.getWeight(featureID);
			}
		}
	}

	private void calcScoreCanonical(SuperCategory superCat) {
		if (superCat.marker == 1) {
			return;
		}

		for (SuperCategory equiv = superCat; equiv != null; equiv = equiv.next) {
			calcScore(equiv);
		}
	}

	private void calcScore(SuperCategory superCat) {
		superCat.marker = 1;

		SuperCategory leftChild = superCat.leftChild;
		SuperCategory rightChild = superCat.rightChild;

		if (leftChild != null) {
			if (rightChild != null) {
				calcScoreCanonical(leftChild);
				calcScoreCanonical(rightChild);

				calcScoreBinary(superCat);
			} else {
				calcScoreCanonical(leftChild);

				calcScoreUnary(superCat);
			}
		} else {
			calcScoreLeaf(superCat);
		}
	}

	private void calcScoreUnary(SuperCategory superCat) {
		double score = 0.0;
		featureIDs.clear();

		features.collectUnaryFeatures(superCat, sentence, featureIDs);

		for ( int featureID : featureIDs ) {
			score += weights.getWeight(featureID);
		}

		superCat.score = score;
	}

	private void calcScoreBinary(SuperCategory superCat) {
		double score = 0.0;
		featureIDs.clear();

		features.collectBinaryFeatures(superCat, sentence, featureIDs);

		for ( int featureID : featureIDs ) {
			score += weights.getWeight(featureID);
		}

		superCat.score = score;
	}

	private void calcScoreLeaf(SuperCategory superCat) {
		double score = superCat.score;
		// assume we already have the supertagger score
		featureIDs.clear();

		features.collectLeafFeatures(superCat, sentence, featureIDs);

		for ( int featureID : featureIDs ) {
			score += weights.getWeight(featureID);
		}

		superCat.score = score;
	}

	// copied from OracleDecoder (v. similar):
	private void getDeps(SuperCategory superCat, boolean ignoreDepsFlag, OracleDecoder oracle, HashSet<FilledDependency> deps) {
		if (superCat.leftChild != null) {
			getEquivDeps(superCat.leftChild, ignoreDepsFlag, oracle, deps);

			if (superCat.rightChild != null) {
				getEquivDeps(superCat.rightChild, ignoreDepsFlag, oracle, deps);
			}
		}

		for ( FilledDependency filled : superCat.filledDeps ) {
			if (!ignoreDepsFlag || !oracle.ignoreDeps.ignoreDependency(filled, sentence)) {
				deps.add(filled);
			}
		}
	}

	private void getEquivDeps(SuperCategory superCat, boolean ignoreDeps, OracleDecoder oracle, HashSet<FilledDependency> deps) {
		SuperCategory bestEquivSuperCat = superCat.maxEquivSuperCat;

		if (bestEquivSuperCat == null) {
			throw new Error("Should always have a maxEquivSuperCat.");
		}

		getDeps(bestEquivSuperCat, ignoreDeps, oracle, deps);
	}

	public void printCellDepsForTraining(PrintWriter out, Categories categories, Sentence sentence, OracleDecoder oracle) {
		HashSet<FilledDependency> deps = new HashSet<FilledDependency>();
		int numWords = sentence.words.size();
		boolean foundGold = false;

		for (int j = 2; j <= numWords; j++) {
			for (int i = j - 2; i >= 0; i--) {
				deps.clear();

				/*
				 * int spanner = j - i; System.out.println("IN CELL: " + i + " "
				 * + spanner);
				 */

				Cell cell = chart.cell(i, j - i);
				for (SuperCategory superCat : cell.getSuperCategories()) {
					for (SuperCategory equiv = superCat; equiv != null; equiv = equiv.next) {
						if (equiv.goldMarker == 1) {

							/*
							 * System.out.println("GOLD MARKER! "); PrintWriter
							 * systemOut = new PrintWriter(System.out, true);
							 * equiv.cat.print(systemOut); systemOut.flush();
							 * System.out.println();
							 */

							/*
							 * for (FilledDependency filled = equiv.filledDeps;
							 * filled != null; filled = filled.next) { if
							 * (!oracle.ignoreDeps.ignoreDependency(filled,
							 * sentence)) deps.add(filled); }
							 */
							getDeps(equiv, true, oracle, deps);

							break;
							// assume we only need to look at one gold in each cell
						} else {
							/*
							 * System.out.println("no gold with score: ");
							 * PrintWriter writer = new PrintWriter(System.out,
							 * true); equiv.cat.print(writer); writer.flush();
							 * System.out.println(" " + equiv.score);
							 * System.out.println();
							 * System.out.println("Children: "); if
							 * (equiv.leftChild != null) {
							 * System.out.print(" left: ");
							 * equiv.leftChild.cat.print(writer);
							 * writer.flush(); if (equiv.rightChild != null) {
							 * System.out.print(" right: ");
							 * equiv.rightChild.cat.print(writer);
							 * writer.flush(); } System.out.println(); }
							 */
						}
					}
				}

				if (!deps.isEmpty()) {
					int span = j - i;
					out.println(i + " " + span + " " + deps.size());

					for (FilledDependency dep : deps) {
						dep.printForTraining(out, categories, sentence);
					}

					foundGold = true;
				}
			}
		}

		if (!foundGold) {
			logger.info("Didn't find any oracle deps.");
		}
	}

	public void printDeps(PrintWriter out, Relations relations, Sentence sentence, SuperCategory superCat) {
		for ( FilledDependency filled : superCat.filledDeps ) {
			filled.printFullJslot(out, relations, sentence);
		}

		if (superCat.leftChild != null) {
			printDeps(out, relations, sentence, superCat.leftChild);

			if (superCat.rightChild != null) {
				printDeps(out, relations, sentence, superCat.rightChild);
			}
		} else {
			sentence.addOutputSupertag(superCat.cat);
		}
	}

	public void printFeature(PrintWriter outFeatures, Sentence sentence, SuperCategory superCat) {
		for ( ArrayList<String> feature : getFeature(sentence, superCat) ) {
			StringBuilder featureBuilder = new StringBuilder();

			for ( int i = 0; i < feature.size()-1; i++ ) {
				featureBuilder.append(feature.get(i));
				featureBuilder.append(" ");
			}

			featureBuilder.append(feature.get(feature.size()-1));
			outFeatures.println(featureBuilder.toString());
		}
	}

	public void printFeatures(PrintWriter outFeatures, Sentence sentence, SuperCategory superCat) {
		printFeature(outFeatures, sentence, superCat);

		if (superCat.leftChild != null) {
			printFeatures(outFeatures, sentence, superCat.leftChild);

			if (superCat.rightChild != null) {
				printFeatures(outFeatures, sentence, superCat.rightChild);
			}
		}
	}

	public ArrayList<Feature> getFeature(Sentence sentence, SuperCategory superCat) {
		ArrayList<Feature> features = new ArrayList<Feature>();

		String topCat = superCat.cat.toString();
		String leftCat = "";
		String rightCat = "";
		String leftLeftCat = "";
		String leftRightCat = "";
		String rightLeftCat = "";
		String rightRightCat = "";

		ArrayList<Integer> topCatWords = new ArrayList<Integer>();
		ArrayList<Integer> leftCatWords = new ArrayList<Integer>();
		ArrayList<Integer> rightCatWords = new ArrayList<Integer>();
		ArrayList<Integer> leftLeftCatWords = new ArrayList<Integer>();
		ArrayList<Integer> leftRightCatWords = new ArrayList<Integer>();
		ArrayList<Integer> rightLeftCatWords = new ArrayList<Integer>();
		ArrayList<Integer> rightRightCatWords = new ArrayList<Integer>();

		ArrayList<Integer> topCatPoss = new ArrayList<Integer>();
		ArrayList<Integer> leftCatPoss = new ArrayList<Integer>();
		ArrayList<Integer> rightCatPoss = new ArrayList<Integer>();
		ArrayList<Integer> leftLeftCatPoss = new ArrayList<Integer>();
		ArrayList<Integer> leftRightCatPoss = new ArrayList<Integer>();
		ArrayList<Integer> rightLeftCatPoss = new ArrayList<Integer>();
		ArrayList<Integer> rightRightCatPoss = new ArrayList<Integer>();

		getWordPos(sentence, superCat, topCatWords, topCatPoss);

		if ( superCat.leftChild != null ) {
			leftCat = superCat.leftChild.cat.toString();
			getWordPos(sentence, superCat.leftChild, leftCatWords, leftCatPoss);

			if ( superCat.leftChild.leftChild != null ) {
				leftLeftCat = superCat.leftChild.leftChild.cat.toString();
				getWordPos(sentence, superCat.leftChild.leftChild, leftLeftCatWords, leftLeftCatPoss);
			}

			if ( superCat.leftChild.rightChild != null ) {
				leftRightCat = superCat.leftChild.rightChild.cat.toString();
				getWordPos(sentence, superCat.leftChild.rightChild, leftRightCatWords, leftRightCatPoss);
			}

			if ( superCat.rightChild != null ) {
				rightCat = superCat.rightChild.cat.toString();
				getWordPos(sentence, superCat.rightChild, rightCatWords, rightCatPoss);

				if ( superCat.rightChild.leftChild != null ) {
					rightLeftCat = superCat.rightChild.leftChild.cat.toString();
					getWordPos(sentence, superCat.rightChild.leftChild, rightLeftCatWords, rightLeftCatPoss);
				}

				if ( superCat.rightChild.rightChild != null ) {
					rightRightCat = superCat.rightChild.rightChild.cat.toString();
					getWordPos(sentence, superCat.rightChild.rightChild, rightRightCatWords, rightRightCatPoss);
				}
			}
		}

		for ( Integer topCatWord : topCatWords ) {
		for ( Integer leftCatWord : leftCatWords ) {
		for ( Integer rightCatWord : rightCatWords ) {
		for ( Integer leftLeftCatWord : leftLeftCatWords ) {
		for ( Integer leftRightCatWord : leftRightCatWords ) {
		for ( Integer rightLeftCatWord : rightLeftCatWords ) {
		for ( Integer rightRightCatWord : rightRightCatWords ) {
		for ( Integer topCatPos : topCatPoss ) {
		for ( Integer leftCatPos : leftCatPoss ) {
		for ( Integer rightCatPos : rightCatPoss ) {
		for ( Integer leftLeftCatPos : leftLeftCatPoss ) {
		for ( Integer leftRightCatPos : leftRightCatPoss ) {
		for ( Integer rightLeftCatPos : rightLeftCatPoss ) {
		for ( Integer rightRightCatPos : rightRightCatPoss ) {
			Feature feature = new Feature();
			feature.add(topCat);
			feature.add(leftCat);
			feature.add(rightCat);
			feature.add(leftLeftCat);
			feature.add(leftRightCat);
			feature.add(rightLeftCat);
			feature.add(rightRightCat);
			feature.add(topCatWord.toString());
			feature.add(leftCatWord.toString());
			feature.add(rightCatWord.toString());
			feature.add(leftLeftCatWord.toString());
			feature.add(leftRightCatWord.toString());
			feature.add(rightLeftCatWord.toString());
			feature.add(rightRightCatWord.toString());
			feature.add(topCatPos.toString());
			feature.add(leftCatPos.toString());
			feature.add(rightCatPos.toString());
			feature.add(leftLeftCatPos.toString());
			feature.add(leftRightCatPos.toString());
			feature.add(rightLeftCatPos.toString());
			feature.add(rightRightCatPos.toString());
			features.add(feature);
		}}}}}}}}}}}}}}

		return features;
	}

	private void getWordPos(Sentence sentence, SuperCategory superCat, ArrayList<Integer> words, ArrayList<Integer> poss) {
		Variable var = superCat.vars[superCat.cat.var];

		for ( int i = 0; i < var.fillers.length && var.fillers[i] != Variable.SENTINEL; i++ ) {
			if ( var.fillers[i] == 0 ) {
				continue;
			}

			words.add(sentence.wordIDs.get(var.fillers[i] - 1));
			poss.add(sentence.postagIDs.get(var.fillers[i] - 1));
		}
	}

	public void printChartDeps(PrintWriter outChartDeps, Relations relations, Sentence sentence) {
		for ( Cell cell : chart.chart ) {
			for ( SuperCategory superCat : cell.getSuperCategories() ) {
				for (SuperCategory equivSuperCat = superCat; equivSuperCat != null; equivSuperCat = equivSuperCat.next) {
					for ( FilledDependency filled : equivSuperCat.filledDeps ) {
						filled.printFullJslot(outChartDeps, relations, sentence);
					}
				}
			}
		}

		outChartDeps.println();
	}

	public void printChartFeatures(PrintWriter outChartFeatures, Sentence sentence) {
		for ( Cell cell : chart.chart ) {
			for ( SuperCategory superCat : cell.getSuperCategories() ) {
				for (SuperCategory equivSuperCat = superCat; equivSuperCat != null; equivSuperCat = equivSuperCat.next) {
					printFeature(outChartFeatures, sentence, equivSuperCat);
				}
			}
		}

		outChartFeatures.println();
	}
}
