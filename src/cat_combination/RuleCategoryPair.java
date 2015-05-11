package cat_combination;

import java.io.PrintWriter;

import lexicon.Category;
import utils.Hash;

/*
 * keys for the rule instances hash maps
 */
public class RuleCategoryPair {
	public Category cat1;
	public Category cat2;

	public RuleCategoryPair(Category cat1, Category cat2) {
		this.cat1 = cat1;
		this.cat2 = cat2;
	}

	public void print(PrintWriter out) {
		cat1.print(out);
		out.print(" ");
		cat2.print(out);
		out.println();
	}

	/*
	 * equivalenceHash checks the feature on S (but ignores the variable X
	 * feature)
	 */
	@Override
	public int hashCode() {
		Hash h = new Hash(cat1.getEhash());
		h.plusEqual(cat2.getEhash());
		return (int) (h.value()); // int's go up to around 2 billion
	}

	/*
	 * the category.equals method has S[X] = S, and now does not ignore features
	 * on categories other than S, so NP[nb] != NP and N != N[num] (update as of
	 * 10/6/14)
	 */
	@Override
	public boolean equals(Object other) {
		return cat1.equals(((RuleCategoryPair) (other)).cat1)
				&& cat2.equals(((RuleCategoryPair) (other)).cat2);
	}
}
