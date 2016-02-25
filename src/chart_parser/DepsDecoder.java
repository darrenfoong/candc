package chart_parser;

import java.io.PrintWriter;
import java.util.HashSet;

import cat_combination.FilledDependency;
import cat_combination.SuperCategory;
import io.Sentence;
import lexicon.Relations;

public class DepsDecoder extends Decoder {
	InsideOutside insideOutside;

	public DepsDecoder() {
		parserDeps = new HashSet<FilledDependency>();
		insideOutside = new InsideOutside();
	}

	/*
	 * decode goes through the chart marking the highest scoring derivations
	 * under each root; getParserDeps then traces the max derivation and stores
	 * the dependencies
	 */
	@Override
	public boolean decode(Chart chart, Sentence sentence) {
		insideOutside.calc(chart);

		Cell root = chart.root();

		for (SuperCategory superCat : root.getSuperCategories()) {
			bestEquiv(superCat);
		}

		return getParserDeps(chart, sentence);
	}

	@Override
	protected void bestEquiv(SuperCategory superCat) {
		if (superCat.maxEquivSuperCat != null)
		{
			return;
			// already been to this equivalence class
		}

		double maxScore = Double.NEGATIVE_INFINITY;
		SuperCategory maxEquivSuperCat = null;

		for (SuperCategory equivSuperCat = superCat; equivSuperCat != null; equivSuperCat = equivSuperCat.next) {
			double currentScore = bestScore(equivSuperCat);
			if (currentScore > maxScore) {
				maxScore = currentScore;
				maxEquivSuperCat = equivSuperCat;
			}
		}

		if (maxEquivSuperCat == null) {
			throw new Error("should always have a maxSuperCat!");
		}

		superCat.maxEquivScore = maxScore;
		superCat.maxEquivSuperCat = maxEquivSuperCat;
	}

	@Override
	protected double bestScore(SuperCategory superCat) {
		double score = 0.0;

		if (superCat.leftChild != null) {
			bestEquiv(superCat.leftChild);
			score += superCat.leftChild.maxEquivScore;

			if (superCat.rightChild != null) {
				bestEquiv(superCat.rightChild);
				score += superCat.rightChild.maxEquivScore;
			}
		}

		for ( FilledDependency filled : superCat.filledDeps ) {
			score += insideOutside.depScores.get(filled) / filled.conjFactor;
		}

		return score;
	}

	public void printDepScores(PrintWriter out, Relations relations, Sentence sentence) {
		insideOutside.printDepScores(out, relations, sentence);
	}
}
