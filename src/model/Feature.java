package model;

import lexicon.Categories;

public interface Feature<T> {
	public T canonize(Categories categories);

	@Override
	public String toString();
}
