package chart_parser;

import io.Sentence;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;

import lexicon.Relations;
import cat_combination.FilledDependency;
import cat_combination.SuperCategory;

public abstract class Decoder {
	HashSet<FilledDependency> parserDeps;

	public int numParserDeps() {
		return parserDeps.size();
	}

	public abstract boolean decode(Chart chart, Sentence sentence);

	protected abstract void bestEquiv(SuperCategory superCat);

	protected abstract double bestScore(SuperCategory superCat);

	/*
	 * finds the dependencies on a best-scoring parse; assumes we've already run
	 * decode
	 */
	protected boolean getParserDeps(Chart chart, Sentence sentence) {
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

		if (maxRoot == null) {
			System.out.println("No best!\n");
			return false;
		}

		getDeps(maxRoot, sentence);

		return true;
	}

	protected void getDeps(SuperCategory superCat, Sentence sentence) {
		if (superCat.leftChild != null) {
			/*
			 * System.out.println("left: "); superCat.leftChild.cat.print();
			 * System.out.println();
			 */
			getEquivDeps(superCat.leftChild, sentence);

			if (superCat.rightChild != null) {
				/*
				 * System.out.println("right: ");
				 * superCat.rightChild.cat.print(); System.out.println();
				 */
				getEquivDeps(superCat.rightChild, sentence);
			}
		} else {
			sentence.addOutputSupertag(superCat.cat);
		}

		for ( FilledDependency filled : superCat.filledDeps ) {
			parserDeps.add(filled);
		}
	}

	protected void getEquivDeps(SuperCategory superCat, Sentence sentence) {
		SuperCategory bestEquivSuperCat = superCat.maxEquivSuperCat;

		if (bestEquivSuperCat == null) {
			throw new Error("should always have a maxEquivSuperCat!");
		}

		getDeps(bestEquivSuperCat, sentence);
	}

	public void print(PrintWriter out, Relations relations, Sentence sentence) {
		Iterator<FilledDependency> iterator = parserDeps.iterator();

		while (iterator.hasNext()) {
			FilledDependency parserDep = iterator.next();
			parserDep.printFullJslot(out, relations, sentence);
		}
	}
}
