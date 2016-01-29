package io;

import java.io.PrintWriter;
import java.util.ArrayList;

import lexicon.Category;
import model.Lexicon;

public class Sentence {
	public ArrayList<String> words;
	public ArrayList<String> postags;
	public ArrayList<ArrayList<Supertag>> multiSupertags;
	public ArrayList<Integer> wordIDs;
	public ArrayList<Integer> postagIDs;
	public ArrayList<Category> outputSupertags;
	// stored before printing dependency structures

	public Sentence(int MAX_WORDS) {
		words = new ArrayList<String>(MAX_WORDS);
		postags = new ArrayList<String>(MAX_WORDS);
		multiSupertags = new ArrayList<ArrayList<Supertag>>(MAX_WORDS);
		wordIDs = new ArrayList<Integer>(MAX_WORDS);
		postagIDs = new ArrayList<Integer>(MAX_WORDS);
		outputSupertags = new ArrayList<Category>(MAX_WORDS);
	}

	public void addWord(String word) {
		words.add(word);
	}

	public void addPostag(String postag) {
		postags.add(postag);
	}

	public void addSupertags(ArrayList<Supertag> supertags) {
		multiSupertags.add(supertags);
	}

	public void addOutputSupertag(Category supertag) {
		outputSupertags.add(supertag);
	}

	/**
	 * Fills wordIDs and postagIDs with integer IDs obtained from lexicon.
	 * 
	 * @param lexicon lexicon
	 */
	public void addIDs(Lexicon lexicon) {
		for (int i = 0; i < words.size(); i++) {
			wordIDs.add(lexicon.getID(words.get(i)));
			postagIDs.add(lexicon.getID(postags.get(i)));
		}
	}

	public void clear() {
		words.clear();
		postags.clear();
		multiSupertags.clear();
		wordIDs.clear();
		postagIDs.clear();
		outputSupertags.clear();
	}

	public void printC_line(PrintWriter out) {
		out.print("<c>");

		for (int i = 0; i < words.size(); i++) {
			out.print(" " + words.get(i) + "|" + postags.get(i) + "|");
			out.print(outputSupertags.get(i).toStringNoOuterBrackets(true));
		}

		out.println();
	}

	public void printSupertags(PrintWriter out) {
		for (int i = 0; i < words.size(); i++) {
			if (i > 0) {
				out.print(" ");
			}

			out.print(outputSupertags.get(i).toStringNoOuterBrackets(true));
		}

		out.println();
	}
}
