package chart_parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cat_combination.FilledDependency;
import cat_combination.IgnoreDepsEval;
import cat_combination.RuleCategoryPair;
import cat_combination.RuleInstances;
import cat_combination.SuperCategory;
import io.Sentence;
import lexicon.Categories;
import lexicon.Category;
import lexicon.Relations;

public abstract class OracleDecoder {
	public HashSet<FilledDependency> goldDeps;
	// these are the dependencies we're trying to find
	public HashSet<FilledDependency> parserDeps;
	// storage for the dependencies we do find
	public Category rootCat;
	// some oracle decoders assume a root category
	public Category newRootCat;
	// may want to print out the root category found if the gold root not available
	public IgnoreDepsEval ignoreDeps;
	// the dependencies ignored by the evaluate script (not in CCGbank)
	public Categories categories;
	public RuleInstances allRules = null;
	// stores the extracted rules if oracle decoder used to extract rules

	private String ruleIDsFile = "data/grammar/ruleIDsNoEval.txt";
	private String relRuleIDsFile = "data/grammar/relsNoEval.txt";
	private String relHeadFile = "data/grammar/relsHeadsNoEval.txt";
	private String relHeadFillerFile = "data/grammar/relsHeadsFillersNoEval.txt";

	public static final Logger logger = LogManager.getLogger(OracleDecoder.class);

	public OracleDecoder(Categories categories, boolean extractRuleInstances) {
		this.goldDeps = new HashSet<FilledDependency>();
		this.parserDeps = new HashSet<FilledDependency>();
		this.rootCat = null;
		this.newRootCat = null;
		this.ignoreDeps = new IgnoreDepsEval(ruleIDsFile, relRuleIDsFile, relHeadFile, relHeadFillerFile, categories.dependencyRelations);
		this.categories = categories;

		if (extractRuleInstances) {
			allRules = new RuleInstances(null, categories);
			// null indicates we're not reading rules in from a file
		}
	}

	public int numGoldDeps() {
		return goldDeps.size();
	}

	public boolean isEmptyGoldDeps() {
		return goldDeps.isEmpty();
	}

	public boolean containsGoldDep(FilledDependency dep) {
		return goldDeps.contains(dep);
	}

	public int numGoldDepsIgnoreEval(Sentence sentence) {
		int numGold = 0;

		for (FilledDependency dep : goldDeps) {
			if (!ignoreDeps.ignoreDependency(dep, sentence)) {
				numGold++;
			}
		}

		return numGold;
	}

	public int numParserDeps() {
		return parserDeps.size();
	}

	public boolean isEmptyParserDeps() {
		return parserDeps.isEmpty();
	}

	/*
	 * decode goes through the chart counting maximum gold deps; getParserDeps
	 * then traces a max derivation and stores the dependencies
	 */
	public double decode(Chart chart, Sentence sentence) {
		Cell root = chart.root();
		double maxScore = Double.NEGATIVE_INFINITY;

		for (SuperCategory superCat : root.getSuperCategories()) {
			bestEquiv(superCat, sentence);
			double currentScore = superCat.maxEquivScore;
			if (currentScore > maxScore) {
				maxScore = currentScore;
			}
		}

		return maxScore;
	}

	public abstract double bestScore(SuperCategory superCat, Sentence sentence);

	public SuperCategory bestEquiv(SuperCategory superCat, Sentence sentence) {
		if (superCat.maxEquivSuperCat != null) {
			return superCat.maxEquivSuperCat;
			// already been to this equivalence class
		}

		double maxScore = Double.NEGATIVE_INFINITY;
		SuperCategory maxEquivSuperCat = null;

		for (SuperCategory equivSuperCat = superCat; equivSuperCat != null; equivSuperCat = equivSuperCat.next) {
			double currentScore = bestScore(equivSuperCat, sentence);
			if (currentScore > maxScore) {
				maxScore = currentScore;
				maxEquivSuperCat = equivSuperCat;
			}
		}

		superCat.maxEquivScore = maxScore;
		superCat.maxEquivSuperCat = maxEquivSuperCat;
		return maxEquivSuperCat;
	}

	public abstract boolean getParserDeps(Chart chart, double inputScore, Sentence sentence, boolean ignoreDepsFlag, boolean checkRoot);

	public void getDeps(SuperCategory superCat, Sentence sentence, boolean ignoreDepsFlag) {
		if (superCat.leftChild != null) {
			getEquivDeps(superCat.leftChild, sentence, ignoreDepsFlag);

			if (superCat.rightChild != null) {
				getEquivDeps(superCat.rightChild, sentence, ignoreDepsFlag);
			}
		} else {
			sentence.addOutputSupertag(superCat.cat);
		}

		for ( FilledDependency filled : superCat.filledDeps ) {
			if (!ignoreDepsFlag || !ignoreDeps.ignoreDependency(filled, sentence)) {
				parserDeps.add(filled);
			}
		}
	}

	private void getEquivDeps(SuperCategory superCat, Sentence sentence, boolean ignoreDeps) {
		SuperCategory bestEquivSuperCat = superCat.maxEquivSuperCat;

		if (bestEquivSuperCat == null) {
			throw new IllegalArgumentException("Should always have a maxEquivSuperCat.");
		}

		getDeps(bestEquivSuperCat, sentence, ignoreDeps);
	}

	public void readRootCat(BufferedReader roots, Categories categories) {
		newRootCat = null;

		try {
			String line = roots.readLine();

			if (line == null) {
				throw new IllegalArgumentException("Run out of roots to read.");
			}

			rootCat = categories.canonize(line);

			if (rootCat == null) {
				throw new Error("Couldn't parse rootCat string.");
			}
		} catch (IOException e) {
			logger.error(e);
		}
	}

	public boolean readDeps(BufferedReader in, Categories categories) {
		try {
			goldDeps.clear();

			String line = in.readLine();

			while (true) {
				if (line == null) {
					return false;
				} else if (line.isEmpty()) {
					return true;
				}

				String[] tokens = line.split("\\s");
				short headIndex = Short.parseShort(tokens[0]);
				short slot = Short.parseShort(tokens[2]);
				short fillerIndex = Short.parseShort(tokens[3]);
				short unaryRuleID = (short) (0);
				short lrange = (short) (0);
				short relID;

				String markedupString = categories.getString(tokens[1]);
				// we're relying on this case being picked up elsewhere:
				if (markedupString == null) {
					relID = 0;
				} else {
					// note the relID below could be zero if getRelID_II returns null from the hashMap
					relID = categories.dependencyRelations.getRelID_II(markedupString, slot);
				}

				// keep the relID = 0 cases; these are rels in the gold we can't get (?)
				FilledDependency dep = new FilledDependency(relID, headIndex, fillerIndex, unaryRuleID, lrange);
				goldDeps.add(dep);

				line = in.readLine();
			}
		} catch (IOException e) {
			logger.error(e);
			return false;
		}
	}

	public void printDepsForTraining(PrintWriter out, Categories categories, Sentence sentence) {
		Iterator<FilledDependency> iterator = parserDeps.iterator();

		while (iterator.hasNext()) {
			FilledDependency parserDep = iterator.next();
			parserDep.printForTraining(out, categories, sentence);
		}
	}

	public void printDeps(PrintWriter out, Relations relations, Sentence sentence, Boolean relIDonly) {
		if (relIDonly) {
			if (newRootCat != null) {
				out.print(newRootCat);
			} else {
				out.print(rootCat);
			}
			out.println(" " + rootCat);
		}

		Iterator<FilledDependency> iterator = parserDeps.iterator();

		while (iterator.hasNext()) {
			FilledDependency parserDep = iterator.next();
			if (relIDonly) {
				out.println(parserDep);
			} else {
				parserDep.printFullJslot(out, relations, sentence);
			}
		}
	}

	public void printMissingDeps(PrintWriter out, Relations relations, Sentence sentence) {
		Iterator<FilledDependency> iterator = goldDeps.iterator();

		while (iterator.hasNext()) {
			FilledDependency goldDep = iterator.next();

			if (!parserDeps.contains(goldDep)) {
				out.println("Missing dep: ");
				goldDep.printFull(out, relations, sentence);
				out.println();
			}
		}

		iterator = parserDeps.iterator();

		while (iterator.hasNext()) {
			FilledDependency parserDep = iterator.next();

			if (!goldDeps.contains(parserDep)) {
				out.println("Not in gold: ");
			}

			parserDep.printFull(out, relations, sentence);
		}
	}

	/*
	 * this is for extracting rule instances (not being used) and also by
	 * PrintForests; presumably we could print the forests in one step, as we do
	 * for printing deps above, but easiest to mark the superCategories in the
	 * chart if it's being passed to a PrintForest object for printing
	 */
	public boolean markOracleDeps(Chart chart, double maxDeps, boolean extractRuleInstances, boolean checkRoot) {
		Cell root = chart.root();

		boolean foundMax = false;

		if (checkRoot) {
			for (SuperCategory superCat : root.getSuperCategories()) {
				if (superCat.maxEquivScore == maxDeps && superCat.cat.equals(rootCat)) {
					foundMax = true;
					markEquivOracleDeps(superCat, extractRuleInstances);
					break;
					// assuming we only match one root
				}
			}
		}

		if (!foundMax || !checkRoot) {
			for (SuperCategory superCat : root.getSuperCategories()) {
				if (superCat.maxEquivScore == maxDeps) {
					foundMax = true;
					markEquivOracleDeps(superCat, extractRuleInstances);
					break;
					// assuming we only match one root
				}
			}
		}

		if (!foundMax) {
			System.out.println("No best!\n");
			return false;
		}

		return true;
	}

	public void markEquivOracleDeps(SuperCategory superCat, boolean extractRuleInstances) {
		// does this work? we effectively only mark the *conj* nodes:
		if (superCat.goldMarker == -1 || superCat.goldMarker == 1) {

			/*
			 * System.out.println("been here!"); PrintWriter writer = new
			 * PrintWriter(System.out); superCat.cat.print(writer);
			 * writer.flush(); System.out.println();
			 */

			return;
			// already been to this equivalence class
		}

		double maxScore = superCat.maxEquivScore;

		for (SuperCategory equivSuperCat = superCat; equivSuperCat != null; equivSuperCat = equivSuperCat.next) {
			if (equivSuperCat.score == maxScore) {

				/*
				 * System.out.println("max score match: " + maxScore);
				 * PrintWriter writer = new PrintWriter(System.out);
				 * equivSuperCat.cat.print(writer); writer.flush();
				 * System.out.println();
				 */

				equivSuperCat.goldMarker = 1;
				markConjOracleDeps(equivSuperCat, extractRuleInstances);
			} else {
				equivSuperCat.goldMarker = -1;
				// don't bother recursing into incorrect derivations

				/*
				 * System.out.println("NO MATCH: " + equivSuperCat.score);
				 * PrintWriter writer = new PrintWriter(System.out);
				 * equivSuperCat.cat.print(writer); writer.flush();
				 * System.out.println(); System.out.println("Children: "); if
				 * (equivSuperCat.leftChild != null) {
				 * System.out.println(" left: ");
				 * equivSuperCat.leftChild.cat.print(writer); writer.flush(); if
				 * (equivSuperCat.rightChild != null) {
				 * System.out.println(" right: ");
				 * equivSuperCat.rightChild.cat.print(writer); writer.flush(); }
				 * System.out.println(); }
				 */
			}
		}
	}

	public void markConjOracleDeps(SuperCategory superCat, boolean extractRuleInstances) {
		if (superCat.leftChild != null) {
			markEquivOracleDeps(superCat.leftChild, extractRuleInstances);

			if (superCat.rightChild != null) {
				if (extractRuleInstances) {
					extractRules(superCat);
				}

				markEquivOracleDeps(superCat.rightChild, extractRuleInstances);
			}
		}
	}

	// assuming we have two children
	public void extractRules(SuperCategory superCat) {
		Category cat1 = categories.canonize(superCat.leftChild.cat);
		Category cat2 = categories.canonize(superCat.rightChild.cat);
		RuleCategoryPair catPair = new RuleCategoryPair(cat1, cat2);
		allRules.add(catPair);
	}

	public void printRuleInstances(PrintWriter out) {
		allRules.print(out);
	}
}
