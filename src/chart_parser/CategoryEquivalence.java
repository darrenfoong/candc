package chart_parser;

import java.util.HashMap;

import cat_combination.SuperCategory;

public class CategoryEquivalence {
	/*
	 * keys are triples of (position, span, SuperCategory); value becomes the *last*
	 * SuperCategory in the linked list of equivalents
	 */
	// should refactor into Hashmap<EquivKey, LinkedList<SuperCategory>>!
	private HashMap<EquivKey, SuperCategory> equiv;

	public CategoryEquivalence(int initCapacity) {
		equiv = new HashMap<EquivKey, SuperCategory>(initCapacity);
	}

	/*
	 * returns true if equivalent category not already there, false otherwise
	 */
	public boolean add(int position, int span, SuperCategory superCat) {
		EquivKey equivKey = new EquivKey(position, span, superCat);
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
