package chart_parser;

import utils.Hash;
import cat_combination.SuperCategory;

public class EquivKeyFscore {
	/*
	 * keys for the hashMap in CategoryEquivalenceFscore; 4-tuples of:
	 * (position, span, SuperCategory, d) where d is the maximum number of
	 * dependencies (p.90 of Auli's thesis)
	 */
	private int position;
	// starts at zero
	private int span;
	private SuperCategory superCategory;
	private double maxDeps;

	public EquivKeyFscore(int position, int span, SuperCategory superCategory, double maxDeps) {
		this.position = position;
		this.span = span;
		this.superCategory = superCategory;
		this.maxDeps = maxDeps;
	}

	@Override
	public boolean equals(Object other) {
		return superCategory.getEhash() == ((EquivKeyFscore) (other)).superCategory.getEhash()
				&& position == ((EquivKeyFscore) (other)).position
				&& span == ((EquivKeyFscore) (other)).span
				&& SuperCategory.equal(superCategory, ((EquivKeyFscore) (other)).superCategory)
				&& maxDeps == ((EquivKeyFscore) (other)).maxDeps;
	}

	@Override
	public int hashCode() {
		Hash h = new Hash(superCategory.getEhash());
		h.plusEqual(position);
		h.plusEqual(span);
		h.plusEqual((int) (maxDeps));
		return (int) (h.value());
		// int's go up to around 2 billion
	}
}
