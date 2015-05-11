package chart_parser;

import java.util.HashMap;

import cat_combination.SuperCategory;

public class CategoryEquivalenceFscore {
	/*
	 * keys are tuples of (position, span, SuperCategory, d); value becomes the
	 * *last* SuperCategory in the linked list of equivalents
	 */
	// should refactor into Hashmap<EquivKeyFscore, LinkedList<SuperCategory>>!
	private HashMap<EquivKeyFscore, SuperCategory> equiv;

	public CategoryEquivalenceFscore(int initCapacity) {
		equiv = new HashMap<EquivKeyFscore, SuperCategory>(initCapacity);
	}

	/*
	 * returns true if equivalent category not already there, false otherwise
	 */
	public boolean add(int position, int span, SuperCategory superCat) {
		double d = superCat.outside;
		// not ideal naming, but better to use existing field rather than use a new one
		EquivKeyFscore equivKey = new EquivKeyFscore(position, span, superCat, d);
		SuperCategory value = equiv.get(equivKey);
		equiv.put(equivKey, superCat);
		// overrides the previous value (if there was one)

		if (value != null) {
			value.next = superCat;
			return false;
		} else {
			return true;
		}
	}

	public void clear() {
		equiv.clear();
	}
}
