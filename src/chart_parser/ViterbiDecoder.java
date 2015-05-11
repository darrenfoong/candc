package chart_parser;

import io.Sentence;

import java.util.HashSet;

import cat_combination.FilledDependency;
import cat_combination.SuperCategory;

public class ViterbiDecoder extends Decoder {
	public ViterbiDecoder() {
		parserDeps = new HashSet<FilledDependency>();
	}

	/*
	 * decode goes through the chart marking the highest scoring derivations
	 * under each root; getParserDeps then traces the max derivation and stores
	 * the dependencies
	 */
	@Override
	public boolean decode(Chart chart, Sentence sentence) {
		Cell root = chart.root();

		for (SuperCategory superCat : root.getSuperCategories()) {
			bestEquiv(superCat);
		}

		return getParserDeps(chart, sentence);
	}

	@Override
	protected void bestEquiv(SuperCategory superCat) {
		if (superCat.maxEquivSuperCat != null) {
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
		double score = superCat.score;

		if (superCat.leftChild != null) {
			bestEquiv(superCat.leftChild);
			score += superCat.leftChild.maxEquivScore;

			if (superCat.rightChild != null) {
				bestEquiv(superCat.rightChild);
				score += superCat.rightChild.maxEquivScore;
			}
		}

		return score;
	}
}
