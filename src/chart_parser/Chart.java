package chart_parser;

import java.util.ArrayList;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cat_combination.SuperCategory;
import io.Sentence;
import io.Supertag;
import lexicon.Category;
import lexicon.Relations;
import model.Weights;

/*
 * number of cells is n*(n+1)/2 where n is the number of words
 *
 * indexing into the array using position and span, where position
 * starts at 0, is given by:
 * index = (span - 1) * n - (span - 1) * (span - 2)/2 + position
 *
 * NUM_CATS_IN_CELL is used to guess at how big the hashMap inside
 * equiv should be initially
 */

public class Chart {
	public final int MAX_WORDS;
	public final int MAX_CELLS;
	public final int NUM_CATS_IN_CELL = 10;

	public int numWords;
	public int numCells;

	public Cell[] chart;
	public CategoryEquivalence equiv;
	public CategoryEquivalenceFscore equivFscore;
	public Relations relations;
	// used to print out the string when printing relations, and for reading gold deps in the oracle decoder

	public Weights weights;

	private static int numSuperCategories;

	public static final Logger logger = LogManager.getLogger(Chart.class);

	public static void setNumSuperCategories(int n) {
		numSuperCategories = n;
	}

	public static int getNumSuperCategories() {
		return numSuperCategories;
	}

	public Chart(int MAX_WORDS, Relations relations, boolean oracleFscore, boolean trainingBeamParser) {
		this.MAX_WORDS = MAX_WORDS;
		this.MAX_CELLS = (MAX_WORDS + 1) * MAX_WORDS / 2;

		if (!trainingBeamParser) {
			this.chart = new Cell[MAX_CELLS];
			for (int i = 0; i < MAX_CELLS; i++) {
				this.chart[i] = new Cell();
			}

			int initCapacity = MAX_CELLS * NUM_CATS_IN_CELL;
			// maybe this is too large?

			if (oracleFscore) {
				this.equivFscore = new CategoryEquivalenceFscore(initCapacity);
			} else {
				this.equiv = new CategoryEquivalence(initCapacity);
			}
		} else {
			this.chart = new CellTrainBeam[MAX_CELLS];
			for (int i = 0; i < MAX_CELLS; i++) {
				this.chart[i] = new CellTrainBeam();
			}
		}

		this.relations = relations;
	}

	private int index(int position, int span) {
		return ((span - 1) * numWords) - ((span - 1) * (span - 2) / 2) + position;
	}

	public Cell cell(int position, int span) {
		return chart[index(position, span)];
	}

	public Cell root() {
		return cell(0, numWords);
	}

	public void add(int position, int span, ArrayList<SuperCategory> superCategories, boolean oracleFscore) {
		for (SuperCategory superCat : superCategories) {
			if (!oracleFscore) {
				add(position, span, superCat);
			} else {
				addFscore(position, span, superCat);
			}
		}
	}

	public void add(int position, int span, SuperCategory superCat) {
		logger.trace("adding to chart: " + superCat.cat);

		if ( logger.getLevel().isLessSpecificThan(Level.TRACE) ) {
			superCat.printFilledDeps(relations);
		}

		if (equiv.add(position, span, superCat)) {
			logger.trace("no equivalent");
			cell(position, span).add(superCat);
		} else {
			logger.trace("found equivalent category!");
			Chart.setNumSuperCategories(Chart.getNumSuperCategories()+1);
		}
	}

	public void addFscore(int position, int span, SuperCategory superCat) {
		if (equivFscore.add(position, span, superCat)) {
			cell(position, span).add(superCat);
		} else {
			Chart.setNumSuperCategories(Chart.getNumSuperCategories()+1);
		}
	}

	public void addNoDP(int position, int span, SuperCategory superCat) {
		cell(position, span).add(superCat);
	}

	public void addNoDP(int position, int span, ArrayList<SuperCategory> superCategories) {
		cell(position, span).add(superCategories);
	}

	public void setWeights(Weights weights) {
		if ( weights == null ) {
			double[] weightsArray = {0.0};
			this.weights = new Weights();
			this.weights.setWeights(weightsArray);
		} else {
			this.weights = weights;
		}
	}

	/**
	 * Loads sentence into chart.
	 * 
	 * The method assumes that the sentence size is less than or equal
	 * to MAX_WORDS.
	 * 
	 * @param sentence sentence to be parsed
	 * @param beta beta which determines probability cutoff for loaded supertags
	 * @param oracleFscore TODO
	 */
	public void load(Sentence sentence, double beta, boolean oracleFscore, boolean beamParser) {
		// assumes the scores in the supertags vectors are ordered by size
		numWords = sentence.words.size();

		// TODO investigate purpose of +1
		numCells = (numWords + 1) * numWords / 2 + 1;
		// numCells = (numWords + 1) * numWords / 2;

		for (int i = 0; i < numWords; i++) {
			ArrayList<Supertag> supertags = (sentence.multiSupertags).get(i);
			double probCutoff = (supertags.get(0)).probability * beta;

			for (Supertag supertag : supertags) {
				if (supertag.probability < probCutoff) {
					continue;
				}

				Category cat = supertag.lexicalCategory;
				SuperCategory superCat = SuperCategory.Lexical((short) (i + 1), cat, (short) (0));

				superCat.logPScore = Math.log(supertag.probability);
				superCat.score = weights.getLogP() * superCat.logPScore;
				superCat.inside = superCat.score;
				// used by PrintForest (since the depsSumDecoder already resets score)

				if (!beamParser) {
					if (!oracleFscore) {
						add(i, 1, superCat);
					} else {
						addFscore(i, 1, superCat);
					}
				} else {
					addNoDP(i, 1, superCat);
				}
			}
		}
	}

	/**
	 * Clears chart.
	 */
	public void clear() {
		for (Cell cell : chart) {
			cell.clear();
		}

		if ( equiv != null ) {
			equiv.clear();
		}

		if ( equivFscore != null ) {
			equivFscore.clear();
		}

		Chart.setNumSuperCategories(0);
	}
}
