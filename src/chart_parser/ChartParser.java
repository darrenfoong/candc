package chart_parser;

import io.Sentence;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;

import lexicon.Categories;
import model.Features;
import model.Lexicon;
import model.Weights;
import cat_combination.FilledDependency;
import cat_combination.RuleInstancesParams;
import cat_combination.Rules;
import cat_combination.SuperCategory;

public class ChartParser {
	public final int MAX_SUPERCATS;

	public boolean printDetailedOutput;
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

	public ChartParser(
					String grammarDir,
					boolean altMarkedup,
					boolean eisnerNormalForm,
					int MAX_WORDS,
					int MAX_SUPERCATS,
					boolean output,
					boolean oracleFscore,
					boolean adaptiveSupertagging,
					RuleInstancesParams ruleInstancesParams,
					Lexicon lexicon,
					String featuresFile,
					String weightsFile,
					boolean newFeatures,
					boolean compactWeights) throws IOException {
		this.MAX_SUPERCATS = MAX_SUPERCATS;
		this.printDetailedOutput = output;
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

		this.chart = new Chart(MAX_WORDS, output, categories.dependencyRelations, oracleFscore, false);
		this.chart.setWeights(this.weights);
	}

	public boolean parseSentence(Sentence sentence, PrintWriter log, double[] betas) {
		int betaLevel;

		if (adaptiveSupertagging) {
			betaLevel = 0;
		} else {
			if (betas.length < 3) {
				System.err.println("Need at least 3 beta values for reverse adaptive supertagging.");
				return true;
			}
			betaLevel = 2;
		}

		this.sentence = sentence;

		maxWordsExceeded = false;
		int numWords = sentence.words.size();
		if ( numWords > chart.MAX_WORDS ) {
			System.out.println(" Sentence has " + numWords + " words; MAX_WORDS exceeded.");
			log.println(" Sentence has " + numWords + " words; MAX_WORDS exceeded.");
			maxWordsExceeded = true;
			return true;
		} else {
			System.out.println(" Sentence has " + numWords + " words.");
			log.println(" Sentence has " + numWords + " words.");
		}

		if (lexicon != null) {
			sentence.addIDs(lexicon);
		}

		while (betaLevel < betas.length) {
			System.out.println("Trying beta level " + betaLevel + " with beta value " + betas[betaLevel]);
			log.println("Trying beta level " + betaLevel + " with beta value " + betas[betaLevel]);

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
				for (int i = j - 2; i >= 0; i--) {
					for (int k = i + 1; k < j; k++) {
						if (SuperCategory.getNumSuperCategories() > MAX_SUPERCATS) {
							maxSuperCatsExceeded = true;
							System.out.println("MAX_SUPERCATS exceeded!");
							log.println("MAX_SUPERCATS exceeded!");
							break jloop;
						}

						int right1 = k - i;
						int right2 = j - k;

						if (printDetailedOutput) {
							System.out.println("combining cells: (" + i + "," + right1 + ") (" + k + "," + right2 + ")");
						}

						combine(chart.cell(i, right1), chart.cell(k, right2), i, j - i);
					}

					/*
					 * apply unary rules to lexical categories; typeChange needs to come
					 * before typeRaise since some results of typeChange can be
					 * type-raised (but not vice versa)
					 */
					if (j - i < numWords) {
						typeChange(chart.cell(i, j - i), i, j - i);
						typeRaise(chart.cell(i, j - i), i, j - i);
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
				boolean success = rules.combine(leftSuperCat, rightSuperCat, results, printDetailedOutput, sentence);
				if (printDetailedOutput) {
					if (success) {
						System.out.println("success!: " + results.get(results.size() - 1).cat);
					} else {
						System.out.println("failed to combine");
					}
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
			System.out.println("Didn't find any oracle deps.");
		}
	}

	public void printChart() {
		int numWords = sentence.words.size();

		for (int span = numWords; span > 0; span--) {
			for (int pos = 0; pos <= numWords-span; pos++) {
				System.out.println("Cell (" + pos + "," + span + ") contains:");
				for (SuperCategory superCat : chart.cell(pos, span).getSuperCategories()) {
					System.out.println(" " + superCat.cat.toStringNoOuterBrackets() + " " + superCat.score);
				}
			}
		}
	}
}
