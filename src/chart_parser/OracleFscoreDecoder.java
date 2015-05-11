package chart_parser;

import io.Sentence;

import java.io.PrintWriter;
import java.util.Iterator;

import lexicon.Categories;
import lexicon.Relations;
import cat_combination.FilledDependency;
import cat_combination.SuperCategory;

/*
 * This decoder assumes that the chart has an equivalence class method
 * where items are required to have the same number of dependencies to
 * be in the same class (i.e. any derivation rooted in a particular
 * class will produce the same number of dependencies (not necessarily
 * correct)).
 */
public class OracleFscoreDecoder extends OracleDecoder {
	public OracleFscoreDecoder(Categories categories,
			boolean extractRuleInstances) {
		super(categories, extractRuleInstances);
		/*
		 * this is ugly, but this seems like the obvious place to set this
		 * static variable, since it's only used in conjunction with this
		 * decoder:
		 */
		SuperCategory.ignoreDeps = ignoreDeps;
	}

	@Override
	public double bestScore(SuperCategory superCat, Sentence sentence) {
		double score = 0.0;

		if (superCat.leftChild != null) {
			score += superCat.leftChild.maxEquivScore;

			if (superCat.rightChild != null) {
				score += superCat.rightChild.maxEquivScore;
			}
		}
		/*
		 * determine how many dependencies are gold (note we're ignoring any
		 * that match the first 4 elements of the tuple but would get ignored by
		 * the evaluate script - although the gold doesn't have these anyway?)
		 */
		for (FilledDependency filled = superCat.filledDeps; filled != null; filled = filled.next) {
			if (goldDeps.contains(filled)
					&& !ignoreDeps.ignoreDependency(filled, sentence)) {
				score++;
			}
		}

		/*
		 * System.out.print("bestScore: "); PrintWriter writer = new
		 * PrintWriter(System.out); superCat.cat.print(writer); writer.flush();
		 * System.out.println(" " + score + " children "); if
		 * (superCat.leftChild != null) { System.out.print(" left: ");
		 * superCat.leftChild.cat.print(writer); writer.flush(); if
		 * (superCat.rightChild != null) { System.out.print(" right: ");
		 * superCat.rightChild.cat.print(writer); writer.flush(); } }
		 * System.out.println();
		 */

		superCat.score = score;
		return score;
	}

	/*
	 * finds the dependencies on a best-scoring parse; assumes we've already run
	 * decode
	 */
	@Override
	public boolean getParserDeps(Chart chart, double inputScore,
			Sentence sentence, boolean ignoreDeps, boolean checkRoot) {
		parserDeps.clear();

		Cell root = chart.root();
		SuperCategory bestEquivSuperCat = null;
		double maxScore = Double.NEGATIVE_INFINITY;

		if (checkRoot) {
			for (SuperCategory superCat : root.getSuperCategories()) {
				double currentScore = (2 * superCat.maxEquivScore)
						/ (superCat.outside + numGoldDeps()); // p.90 Auli's
				// thesis

				/*
				 * double p = superCat.maxEquivScore / superCat.outside; double
				 * r = superCat.maxEquivScore / numGoldDeps();
				 * System.out.println("P: " + p + " R: " + r + " F: " +
				 * currentScore);
				 */

				if (currentScore > maxScore && superCat.cat.equals(rootCat)) {
					maxScore = currentScore;
					bestEquivSuperCat = superCat.maxEquivSuperCat;
				}
			}
		}
		if (bestEquivSuperCat == null || !checkRoot) {
			maxScore = Double.NEGATIVE_INFINITY;
			for (SuperCategory superCat : root.getSuperCategories()) {
				double currentScore = (2 * superCat.maxEquivScore)
						/ (superCat.outside + numGoldDeps()); // p.90 Auli's
				// thesis

				if (currentScore > maxScore) {
					maxScore = currentScore;
					bestEquivSuperCat = superCat.maxEquivSuperCat;
				}
			}
		}
		if (bestEquivSuperCat == null) {
			System.out.println("No best!\n");
			// System.out.println("No best!\n");
			return false;
		}
		getDeps(bestEquivSuperCat, sentence, ignoreDeps);

		return true;
	}

	@Override
	public boolean markOracleDeps(Chart chart, double maxDeps,
			boolean extractRuleInstances, boolean checkRoot) {
		Cell root = chart.root();
		double maxScore = Double.NEGATIVE_INFINITY;
		boolean foundRoot = false;
		boolean foundMax = false;

		// ignoring maxDeps - calculate maxScore again

		// can this fire more than once, ie can we not just find the *single*
		// highest scoring root?

		if (checkRoot) {
			for (SuperCategory superCat : root.getSuperCategories()) {
				double currentScore = (2 * superCat.maxEquivScore)
						/ (superCat.outside + numGoldDeps()); // p.90 Auli's
				// thesis
				if (currentScore > maxScore && superCat.cat.equals(rootCat)) {
					foundRoot = true;
					maxScore = currentScore;
				}
			}
			if (foundRoot) {
				for (SuperCategory superCat : root.getSuperCategories()) {
					double currentScore = (2 * superCat.maxEquivScore)
							/ (superCat.outside + numGoldDeps()); // p.90 Auli's
					// thesis
					if (currentScore == maxScore
							&& superCat.cat.equals(rootCat)) {
						foundMax = true;
						markEquivOracleDeps(superCat, extractRuleInstances);
					}
				}
			}
		}
		if (!foundRoot || !checkRoot) {
			maxScore = Double.NEGATIVE_INFINITY;
			for (SuperCategory superCat : root.getSuperCategories()) {
				double currentScore = (2 * superCat.maxEquivScore)
						/ (superCat.outside + numGoldDeps()); // p.90 Auli's
				// thesis
				if (currentScore > maxScore) {
					maxScore = currentScore;
				}
			}
			for (SuperCategory superCat : root.getSuperCategories()) {
				double currentScore = (2 * superCat.maxEquivScore)
						/ (superCat.outside + numGoldDeps()); // p.90 Auli's
				// thesis
				if (currentScore == maxScore) {
					foundMax = true;
					markEquivOracleDeps(superCat, extractRuleInstances);
				}
			}
		}
		if (!foundMax) {
			System.out.println("Not found a max in markOracleDeps!");
		}
		return foundMax;
	}

	public void printDeps(PrintWriter out, Relations relations,
			Sentence sentence) {
		Iterator<FilledDependency> iterator = parserDeps.iterator();
		while (iterator.hasNext()) {
			FilledDependency parserDep = iterator.next();
			parserDep.printFullJslot(out, relations, sentence);
		}
	}

	@Override
	public void printMissingDeps(PrintWriter out, Relations relations,
			Sentence sentence) {
		Iterator<FilledDependency> iterator = goldDeps.iterator();
		while (iterator.hasNext()) {
			FilledDependency goldDep = iterator.next();
			if (!parserDeps.contains(goldDep)) {
				out.println("Missing dep: ");
				goldDep.printFull(out, relations, sentence);
				out.println();
			} // else {
			// out.println("Correct dep: ");
			// goldDep.printFull(relations, sentence);
			// out.println();
			// }
		}
		iterator = parserDeps.iterator();
		// out.println("All parser deps: ");
		while (iterator.hasNext()) {
			FilledDependency parserDep = iterator.next();
			// if (!goldDeps.contains(parserDep))
			// out.println("Not in gold: ");
			// parserDep.printFull(relations, sentence);
		}
	}
}
