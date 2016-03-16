package chart_parser;

import java.util.HashSet;

import cat_combination.FilledDependency;
import cat_combination.SuperCategory;
import io.Sentence;

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
			bestEquiv(superCat, sentence);
		}

		return getParserDeps(chart, sentence);
	}

	@Override
	protected double bestScore(SuperCategory superCat, Sentence sentence) {
		double score = superCat.score;

		if (superCat.leftChild != null) {
			bestEquiv(superCat.leftChild, sentence);
			score += superCat.leftChild.maxEquivScore;

			if (superCat.rightChild != null) {
				bestEquiv(superCat.rightChild, sentence);
				score += superCat.rightChild.maxEquivScore;
			}
		}

		return score;
	}

	/*
	 * finds the dependencies on a best-scoring parse; assumes we've already run
	 * decode
	 */
	private boolean getParserDeps(Chart chart, Sentence sentence) {
		parserDeps.clear();
		Cell root = chart.root();

		double maxScore = Double.NEGATIVE_INFINITY;
		SuperCategory maxRoot = null;

		for (SuperCategory superCat : root.getSuperCategories()) {
			double currentScore = superCat.maxEquivScore;
			if (currentScore > maxScore) {
				maxScore = currentScore;
				maxRoot = superCat.maxEquivSuperCat;
			}
		}

		this.maxRoot = maxRoot;

		if (maxRoot == null) {
			logger.info("No best!");
			return false;
		}

		getDeps(maxRoot, sentence);

		return true;
	}

	private void getDeps(SuperCategory superCat, Sentence sentence) {
		if (superCat.leftChild != null) {
			getEquivDeps(superCat.leftChild, sentence);

			if (superCat.rightChild != null) {
				getEquivDeps(superCat.rightChild, sentence);
			}
		} else {
			sentence.addOutputSupertag(superCat.cat);
		}

		for ( FilledDependency filled : superCat.filledDeps ) {
			parserDeps.add(filled);
		}
	}

	private void getEquivDeps(SuperCategory superCat, Sentence sentence) {
		SuperCategory bestEquivSuperCat = superCat.maxEquivSuperCat;

		if (bestEquivSuperCat == null) {
			throw new Error("should always have a maxEquivSuperCat!");
		}

		getDeps(bestEquivSuperCat, sentence);
	}
}
