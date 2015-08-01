package chart_parser;

import utils.Hash;
import cat_combination.SuperCategory;

public class EquivKey {
	/*
	 * keys for the hashMap in CategoryEquivalence; triples of: (position, span,
	 * SuperCategory)
	 */
	private int position;
	// starts at zero
	private int span;
	private SuperCategory superCategory;

	public EquivKey(int position, int span, SuperCategory superCategory) {
		this.position = position;
		this.span = span;
		this.superCategory = superCategory;
	}

	@Override
	public boolean equals(Object other) {
		if ( other == null ) {
			return false;
		}

		return superCategory.getEhash() == ((EquivKey) (other)).superCategory.getEhash()
				&& position == ((EquivKey) (other)).position
				&& span == ((EquivKey) (other)).span
				&& SuperCategory.equal(superCategory, ((EquivKey) (other)).superCategory);
	}

	// same code as used in the equiv class in C&C
	@Override
	public int hashCode() {
		Hash h = new Hash(superCategory.getEhash());
		h.plusEqual(position);
		h.plusEqual(span);
		return (int) (h.value());
		// int's go up to around 2 billion
	}
}
