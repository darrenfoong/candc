package io;

import lexicon.Category;

public class Supertag {
	public double probability;
	public String supertagString;
	public Category lexicalCategory;

	public Supertag(String supertagString, Category lexicalCategory, double probability) {
		this.probability = probability;
		this.supertagString = supertagString;
		this.lexicalCategory = lexicalCategory;
	}
}
