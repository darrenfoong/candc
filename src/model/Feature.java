package model;

import java.io.PrintWriter;

import lexicon.Categories;

public interface Feature<T> {
	public T canonize(Categories categories);
	public void print(PrintWriter out);
}
