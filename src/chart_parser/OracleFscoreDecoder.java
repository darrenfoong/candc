package chart_parser;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cat_combination.FilledDependency;
import cat_combination.SuperCategory;
import io.Sentence;
import lexicon.Categories;
import lexicon.Relations;

/*
 * This decoder assumes that the chart has an equivalence class method
 * where items are required to have the same number of dependencies to
 * be in the same class (i.e. any derivation rooted in a particular
 * class will produce the same number of dependencies (not necessarily
 * correct)).
 */
public class OracleFscoreDecoder extends OracleDecoder {
	public static final Logger logger = LogManager.getLogger(OracleFscoreDecoder.class);

	public OracleFscoreDecoder(Categories categories,
			boolean extractRuleInstances,
			boolean ignoreDepsFlag,
			boolean checkRoot) throws IOException {
		super(categories, extractRuleInstances, ignoreDepsFlag, checkRoot);
		/*
		 * this is ugly, but this seems like the obvious place to set this
		 * static variable, since it's only used in conjunction with this
		 * decoder:
		 */
		SuperCategory.setIgnoreDepsEval(ignoreDeps);
	}

	@Override
	public double bestScore(SuperCategory superCat, Sentence sentence) {
		double score = 0.0;

		if ( superCat.leftChild != null ) {
			bestEquiv(superCat.leftChild, sentence);
			score += superCat.leftChild.maxEquivScore;

			if ( superCat.rightChild != null ) {
				bestEquiv(superCat.rightChild, sentence);
				score += superCat.rightChild.maxEquivScore;
			}
		}

		/*
		 * determine how many dependencies are gold (note we're ignoring any
		 * that match the first 4 elements of the tuple but would get ignored by
		 * the evaluate script - although the gold doesn't have these anyway?)
		 */
		for ( FilledDependency filled : superCat.filledDeps ) {
			if ( goldDeps.contains(filled) && !ignoreDeps.ignoreDependency(filled, sentence) ) {
				score++;
			}
		}

		superCat.score = score;
		return score;
	}

	/*
	 * finds the dependencies on a best-scoring parse; assumes we've already run
	 * decode
	 */
	@Override
	public boolean getParserDeps(Chart chart, Sentence sentence) {
		parserDeps.clear();

		Cell root = chart.root();
		SuperCategory bestEquivSuperCat = null;
		double maxScore = Double.NEGATIVE_INFINITY;

		if ( checkRoot ) {
			for ( SuperCategory superCat : root.getSuperCategories() ) {
				double currentScore = (2 * superCat.maxEquivScore) / (superCat.outside + numGoldDeps());
				// p.90 Auli's thesis

				if ( currentScore > maxScore && superCat.cat.equals(rootCat) ) {
					maxScore = currentScore;
					bestEquivSuperCat = superCat.maxEquivSuperCat;
				}
			}
		}

		if ( bestEquivSuperCat == null || !checkRoot ) {
			maxScore = Double.NEGATIVE_INFINITY;
			for ( SuperCategory superCat : root.getSuperCategories() ) {
				double currentScore = (2 * superCat.maxEquivScore) / (superCat.outside + numGoldDeps());
				// p.90 Auli's thesis

				if ( currentScore > maxScore ) {
					maxScore = currentScore;
					bestEquivSuperCat = superCat.maxEquivSuperCat;
				}
			}
		}

		this.maxRoot = bestEquivSuperCat;

		if ( bestEquivSuperCat == null ) {
			logger.info("No best!");
			return false;
		}

		getDeps(bestEquivSuperCat, sentence);

		return true;
	}

	@Override
	public boolean markOracleDeps(Chart chart, boolean extractRuleInstances) {
		Cell root = chart.root();
		double maxScore = Double.NEGATIVE_INFINITY;
		boolean foundRoot = false;
		boolean foundMax = false;

		// ignoring maxDeps - calculate maxScore again

		// can this fire more than once, ie can we not just find the *single*
		// highest scoring root?

		if ( checkRoot ) {
			for ( SuperCategory superCat : root.getSuperCategories() ) {
				double currentScore = (2 * superCat.maxEquivScore) / (superCat.outside + numGoldDeps());
				// p.90 Auli's thesis
				if ( currentScore > maxScore && superCat.cat.equals(rootCat) ) {
					foundRoot = true;
					maxScore = currentScore;
				}
			}

			if ( foundRoot ) {
				for ( SuperCategory superCat : root.getSuperCategories() ) {
					double currentScore = (2 * superCat.maxEquivScore) / (superCat.outside + numGoldDeps());
					// p.90 Auli's thesis
					if ( currentScore == maxScore && superCat.cat.equals(rootCat) ) {
						foundMax = true;
						markEquivOracleDeps(superCat, extractRuleInstances);
					}
				}
			}
		}

		if ( !foundRoot || !checkRoot ) {
			maxScore = Double.NEGATIVE_INFINITY;
			for ( SuperCategory superCat : root.getSuperCategories() ) {
				double currentScore = (2 * superCat.maxEquivScore) / (superCat.outside + numGoldDeps());
				// p.90 Auli's thesis
				if ( currentScore > maxScore ) {
					maxScore = currentScore;
				}
			}
			for ( SuperCategory superCat : root.getSuperCategories() ) {
				double currentScore = (2 * superCat.maxEquivScore) / (superCat.outside + numGoldDeps());
				// p.90 Auli's thesis
				if ( currentScore == maxScore ) {
					foundMax = true;
					markEquivOracleDeps(superCat, extractRuleInstances);
				}
			}
		}

		if ( !foundMax ) {
			logger.info("Not found a max in markOracleDeps!");
		}

		return foundMax;
	}

	public void printDeps(PrintWriter out, Relations relations, Sentence sentence) {
		Iterator<FilledDependency> iterator = parserDeps.iterator();
		while ( iterator.hasNext() ) {
			FilledDependency parserDep = iterator.next();
			parserDep.printFullJslot(out, relations, sentence);
		}
	}

	@Override
	public void printMissingDeps(PrintWriter out, Relations relations, Sentence sentence) {
		Iterator<FilledDependency> iterator = goldDeps.iterator();
		while ( iterator.hasNext() ) {
			FilledDependency goldDep = iterator.next();
			if ( !parserDeps.contains(goldDep) ) {
				out.println("Missing dep: ");
				goldDep.printFull(out, relations, sentence);
				out.println();
			}
		}
	}
}
