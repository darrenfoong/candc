package chart_parser;

import cat_combination.SuperCategory;
import utils.Hash;

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
		if ( other == null || getClass() != other.getClass() ) {
			return false;
		}

		EquivKey cother = (EquivKey) other;

		return position == cother.position
				&& span == cother.span
				&& superCategory.equals(cother.superCategory);
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
