package cat_combination;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;

import io.Preface;
import lexicon.Categories;
import lexicon.Category;

public class RuleInstances {
	HashSet<RuleCategoryPair> ruleInstances;

	public RuleInstances(String ruleInstancesFile, Categories categories) throws IOException {
		ruleInstances = new HashSet<RuleCategoryPair>();
		if (ruleInstancesFile != null) {
			readRuleInstances(ruleInstancesFile, categories);
		}
	}

	public boolean contains(Category cat1, Category cat2) {
		RuleCategoryPair catPair = new RuleCategoryPair(cat1, cat2);
		return ruleInstances.contains(catPair);
	}

	public void add(RuleCategoryPair catPair) {
		ruleInstances.add(catPair);
	}

	public void print(PrintWriter out) {
		for ( RuleCategoryPair ruleInstance : ruleInstances ) {
			out.println(ruleInstance);
		}
	}

	private void readRuleInstances(String ruleInstancesFile, Categories categories) throws IOException {
		try ( BufferedReader in = new BufferedReader(new FileReader(ruleInstancesFile)) ) {
			Preface.readPreface(in);

			String line;
			while ((line = in.readLine()) != null) {
				String[] tokens = line.split("\\s");
				String catString1 = tokens[0];
				String catString2 = tokens[1];
				Category cat1 = categories.canonize(catString1);
				Category cat2 = categories.canonize(catString2);
				RuleCategoryPair catPair = new RuleCategoryPair(cat1, cat2);

				ruleInstances.add(catPair);
			}
		} catch (IOException e) {
			throw e;
		}
	}
}
