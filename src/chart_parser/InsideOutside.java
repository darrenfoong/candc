package chart_parser;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cat_combination.FilledDependency;
import cat_combination.SuperCategory;
import io.Sentence;
import lexicon.Relations;
import utils.NumericalFunctions;

/*
 * PUT SOME COMMENTS HERE
 * 
 */

public class InsideOutside {
	/*
	 * the equals method for FilledDependency ignores the ruleID and long-range
	 * fields - for the depScores HashMap the ruleID field at least should
	 * probably be taken into account
	 */
	HashMap<FilledDependency, Double> depScores;

	public static final Logger logger = LogManager.getLogger(InsideOutside.class);

	public InsideOutside() {
		depScores = new HashMap<FilledDependency, Double>();
	}

	public void calc(Chart chart) {
		double Z = calcInside(chart);
		calcOutside(chart, -Z);
	}

	private double calcInside(Chart chart) {
		Cell[] cells = chart.chart;
		// would be better to define an iterator for a Chart object
		for (int i = 0; i < chart.numCells; i++) {
			Cell cell = cells[i];
			for (SuperCategory superCat : cell.getSuperCategories()) {
				if (superCat.marker == 1) {
					disjCalcInside(superCat);
				}
			}
		}
		Cell root = chart.root();
		if (root.isEmpty()) {
			return 0.0;
		}

		double Z = 0.0;
		boolean start = true;
		for (SuperCategory superCat : root.getSuperCategories()) {
			if (start) {
				Z = superCat.disjInside;
				start = false;
			} else {
				Z = NumericalFunctions.addLogs(Z, superCat.disjInside);
			}
		}
		return Z;
	}

	private void disjCalcInside(SuperCategory disj) {
		double disjInside = conjCalcInside(disj);
		for (SuperCategory conj = disj.next; conj != null; conj = conj.next) {
			disjInside = NumericalFunctions.addLogs(disjInside,
					conjCalcInside(conj));
		}
		disj.disjInside = disjInside;
	}

	private double conjCalcInside(SuperCategory conj) {
		double inside = conj.score;
		SuperCategory leftChild = conj.leftChild;
		SuperCategory rightChild = conj.rightChild;

		if (leftChild != null) {
			if (rightChild != null) {
				inside += leftChild.disjInside + rightChild.disjInside;
			} else {
				inside += leftChild.disjInside;
			}
		}
		conj.inside = inside;
		return inside;
	}

	private void calcOutside(Chart chart, double invZ) {
		depScores.clear();

		// iterating backwards over the disj nodes should ensure all
		// parents are dealt with before any children

		Cell[] cells = chart.chart;
		// would be better to define an iterator for a Chart object
		for (int i = chart.numCells - 1; i >= 0; i--) {
			Cell cell = cells[i];

			// need to go backwards over cell too to ensure unary-rule
			// parents are dealt with before child

			ArrayList<SuperCategory> superCats = cell.getSuperCategories();
			for (int j = superCats.size() - 1; j >= 0; j--) {
				SuperCategory superCat = superCats.get(j);
				if (superCat.marker == 1) {
					disjCalcOutside(superCat, invZ);
				}
			}
		}
	}

	private void disjCalcOutside(SuperCategory disj, double invZ) {
		for (SuperCategory conj = disj; conj != null; conj = conj.next) {
			conjCalcOutside(conj, disj.outside, invZ);
		}
	}

	private void conjCalcOutside(SuperCategory conj, double outside, double invZ) {
		double sum = conj.score + outside;
		SuperCategory left = conj.leftChild;
		SuperCategory right = conj.rightChild;

		if (left != null) {
			if (right != null) {
				if (left.outside != 0.0) {
					left.outside = NumericalFunctions.addLogs(left.outside,
							right.disjInside + sum);
				} else {
					left.outside = right.disjInside + sum;
				}

				if (right.outside != 0.0) {
					right.outside = NumericalFunctions.addLogs(right.outside,
							left.disjInside + sum);
				} else {
					right.outside = left.disjInside + sum;
				}
			} else {
				if (left.outside != 0.0) {
					left.outside = NumericalFunctions
							.addLogs(left.outside, sum);
				} else {
					left.outside = sum;
				}
			}
		}
		double prob = Math.exp(outside + conj.inside + invZ);

		for ( FilledDependency filled : conj.filledDeps ) {
			double score = prob;

			Double currentScore = depScores.get(filled);
			if (currentScore != null) {
				score += currentScore;
			}

			depScores.put(filled, score);
		}
	}

	public void printDepScores(PrintWriter out, Relations relations,
			Sentence sentence) {
		for ( Map.Entry<FilledDependency, Double> entry : depScores.entrySet() ) {
			PrintWriter writer = new PrintWriter(System.out);
			FilledDependency filled = entry.getKey();
			filled.printFull(writer, relations, sentence);
			writer.flush();
			logger.info("Dependency score for dep: " + entry.getValue());
		}
	}

}
