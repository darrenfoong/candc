package chart_parser;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cat_combination.FilledDependency;
import cat_combination.SuperCategory;
import io.Sentence;
import lexicon.Relations;

public abstract class Decoder {
	HashSet<FilledDependency> parserDeps;

	public static final Logger logger = LogManager.getLogger(Decoder.class);

	public int numParserDeps() {
		return parserDeps.size();
	}

	public boolean isEmptyParserDeps() {
		return parserDeps.isEmpty();
	}

	public abstract boolean decode(Chart chart, Sentence sentence);

	protected void bestEquiv(SuperCategory superCat, Sentence sentence) {
		if (superCat.maxEquivSuperCat != null) {
			return;
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

		if (maxEquivSuperCat == null) {
			throw new Error("should always have a maxSuperCat!");
		}

		superCat.maxEquivScore = maxScore;
		superCat.maxEquivSuperCat = maxEquivSuperCat;
	}

	protected abstract double bestScore(SuperCategory equivSuperCat, Sentence sentence);

	public void print(PrintWriter out, Relations relations, Sentence sentence) {
		Iterator<FilledDependency> iterator = parserDeps.iterator();

		while ( iterator.hasNext() ) {
			FilledDependency parserDep = iterator.next();
			parserDep.printFullJslot(out, relations, sentence);
		}
	}
}
