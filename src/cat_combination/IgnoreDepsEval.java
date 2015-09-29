package cat_combination;

import io.Sentence;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import lexicon.Relations;

/*
 * an object of this class is used by the max F-score oracle decoder;
 * it takes a dependency as input and outputs 1 if the dependency is
 * ignored by the evaluate script; the ignored dependencies are read
 * in as separate text files depending on their type
 */

public class IgnoreDepsEval {
	HashSet<Short> ruleIDs;
	// deps ignored based on the ruleID alone
	HashMap<Short, HashSet<Short>> relRuleIDs;
	// deps ignored based on the relation and ruleID
	HashSet<DependencyStringWords> unfilledDeps;
	// deps ignored based on the relation and head
	HashSet<DependencyStringWords> filledDeps;
	// deps ignored based on the relation, head and filler

	Relations relations;
	// used to get ruleIDs from categories and slots

	String EMPTY_FILLER = "-EMPTY-";
	// some string unlikely to occur in a filled dependency

	public IgnoreDepsEval(String ruleIDsFile, String relRuleIDsFile,
			String relHeadFile, String relHeadFillerFile, Relations relations) {
		ruleIDs = new HashSet<Short>();
		relRuleIDs = new HashMap<Short, HashSet<Short>>();
		unfilledDeps = new HashSet<DependencyStringWords>();
		filledDeps = new HashSet<DependencyStringWords>();

		BufferedReader ruleIDsBuffer = null;
		BufferedReader relRuleIDsBuffer = null;
		BufferedReader relHeadBuffer = null;
		BufferedReader relHeadFillerBuffer = null;

		try {
			ruleIDsBuffer = new BufferedReader(new FileReader(ruleIDsFile));
			relRuleIDsBuffer = new BufferedReader(new FileReader(relRuleIDsFile));
			relHeadBuffer = new BufferedReader(new FileReader(relHeadFile));
			relHeadFillerBuffer = new BufferedReader(new FileReader(relHeadFillerFile));

			String line;
			String[] tokens;
			String head;
			String filler;
			String markedupCatString;
			short ruleID;
			short relID;
			short jslot;
			HashSet<Short> ruleIDsSet = null;

			// read ruleIDs
			while ((line = ruleIDsBuffer.readLine()) != null) {
				if (line.length() == 0) {
					throw new Error(
							"empty line in ruleIDs file for ignoring dependencies");
				}
				ruleID = Short.parseShort(line);
				ruleIDs.add(ruleID);
			}

			// read pairs of relations and ruleIDs
			while ((line = relRuleIDsBuffer.readLine()) != null) {
				tokens = line.split("\\s");
				if (tokens.length != 3) {
					throw new Error(
							"expecting 3 items per line in the rel+rules file for ignoring deps!");
				}

				markedupCatString = tokens[0];
				jslot = Short.parseShort(tokens[1]);
				ruleID = Short.parseShort(tokens[2]);

				relID = relations.getRelID_II(markedupCatString, jslot);
				if (relID == 0) {
					throw new Error("can't find relID in the relations map! "
							+ markedupCatString);
				}
				ruleIDsSet = relRuleIDs.get(relID);
				if (ruleIDsSet == null) {
					ruleIDsSet = new HashSet<Short>();
					relRuleIDs.put(relID, ruleIDsSet);
				}
				ruleIDsSet.add(ruleID);
			}

			// read unfilled dependencies
			while ((line = relHeadBuffer.readLine()) != null) {
				tokens = line.split("\\s");
				if (tokens.length != 4) {
					throw new Error(
							"expecting 4 items per line in the rel+head file for ignoring deps!");
				}

				head = tokens[0];
				markedupCatString = tokens[1];
				jslot = Short.parseShort(tokens[2]);
				relID = relations.getRelID_II(markedupCatString, jslot);
				if (relID == 0) {
					throw new Error("should always have a relID!"
							+ markedupCatString + " " + jslot);
				}

				ruleID = Short.parseShort(tokens[3]);

				DependencyStringWords unfilledDep = new DependencyStringWords(
						relID, head, EMPTY_FILLER, ruleID);
				unfilledDeps.add(unfilledDep);
			}

			// read filled deps
			while ((line = relHeadFillerBuffer.readLine()) != null) {
				tokens = line.split("\\s");
				if (tokens.length != 5) {
					throw new Error(
							"expecting 5 items per line in the rel+head+filler file for ignoring deps!");
				}

				head = tokens[0];
				filler = tokens[3];
				markedupCatString = tokens[1];
				jslot = Short.parseShort(tokens[2]);
				relID = relations.getRelID_II(markedupCatString, jslot);
				ruleID = Short.parseShort(tokens[4]);

				DependencyStringWords filledDep = new DependencyStringWords(
						relID, head, filler, ruleID);
				filledDeps.add(filledDep);
			}

		} catch (IOException e) {
			System.err.println(e);
		} finally {
			try {
				if ( ruleIDsBuffer != null ) { ruleIDsBuffer.close(); }
				if ( relRuleIDsBuffer != null ) { relRuleIDsBuffer.close(); }
				if ( relHeadBuffer != null ) { relHeadBuffer.close(); }
				if ( relHeadFillerBuffer != null ) { relHeadFillerBuffer.close(); }
			} catch (IOException e) {
				System.err.println(e);
			}
		}
	}

	// returns true if the dependency is in the class of ignored deps
	public boolean ignoreDependency(FilledDependency dep, Sentence sentence) {
		if (ruleIDs.contains(dep.unaryRuleID)) {
			return true;
		}

		HashSet<Short> ruleIDsSet = relRuleIDs.get(dep.relID);
		if (ruleIDsSet != null && ruleIDsSet.contains(dep.unaryRuleID)) {
			return true;
		}

		// note word indices start at 1 not 0
		DependencyStringWords depStringWords = new DependencyStringWords(
				dep.relID, sentence.words.get(dep.headIndex - 1), EMPTY_FILLER,
				dep.unaryRuleID);
		if (unfilledDeps.contains(depStringWords)) {
			return true;
		}

		depStringWords = new DependencyStringWords(dep.relID,
				sentence.words.get(dep.headIndex - 1),
				sentence.words.get(dep.fillerIndex - 1), dep.unaryRuleID);
		if (filledDeps.contains(depStringWords)) {
			return true;
		}

		return false;
	}
}
