package io;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lexicon.Categories;
import lexicon.Category;

public class Sentences implements Iterator<Sentence> {
	private int MAX_WORDS;
	private BufferedReader in;
	private BufferedReader stagsIn;
	private Categories categories;
	private Sentence next;

	private boolean sentenceRead = false;

	public static final Logger logger = LogManager.getLogger(Sentences.class);

	public Sentences(BufferedReader in,
			BufferedReader stagsIn,
			Categories categories,
			int MAX_WORDS) {
		this.MAX_WORDS = MAX_WORDS;
		this.in = in;
		this.stagsIn = stagsIn;
		this.categories = categories;
	}

	public void skip(int n) {
		for ( int i = 0; i < n; i++ ) {
			readSentence();
		}
	}

	private void readSentence() {
		Sentence sentence = new Sentence(MAX_WORDS);

		try {
			while ( true ) {
				String line = in.readLine();
				String stagsLine = null;
				if ( stagsIn != null ) {
					stagsLine = stagsIn.readLine();
				}

				if ( line == null ) {
					// no valid sentence
					next = null;
					return;
				} else if ( line.isEmpty() ) {
					// end of sentence
					next = sentence;
					return;
				}

				String[] tokens = line.split("\\s");
				sentence.addWord(tokens[0]);
				sentence.addPostag(tokens[1]);

				int numSupertags = Integer.parseInt(tokens[2]);
				ArrayList<Supertag> supertags = new ArrayList<Supertag>(numSupertags);

				String supertagString = null;
				String goldSupertagString = null;

				if ( stagsIn != null ) {
					String[] stagTokens = stagsLine.split("\\s");

					if ( stagTokens.length != 3 || !stagTokens[0].equals(tokens[0]) || !stagTokens[1].equals(tokens[1]) ) {
						throw new IllegalArgumentException("Mismatch between input and gold supertags: " + tokens[0] + " " + tokens[1] + " " + stagTokens[0] + " " + stagTokens[1]);
					}

					goldSupertagString = stagTokens[2];
				}

				for ( int i = 0; i < numSupertags; i++ ) {
					supertagString = tokens[2 * i + 3];
					double probability = Double.parseDouble(tokens[2 * i + 4]);

					if ( stagsIn != null && supertagString.equals(goldSupertagString) ) {
						continue;
					}

					Category lexicalCategory = categories.getCategory(supertagString);

					if ( lexicalCategory == null ) {
						throw new IllegalArgumentException("No such supertag: " + supertagString);
					}

					supertags.add(new Supertag(supertagString, lexicalCategory, probability));
				}

				if ( stagsIn != null ) {
					Category goldLexicalCategory = categories.getCategory(goldSupertagString);

					if ( goldLexicalCategory == null ) {
						logger.error("No such gold supertag: " + goldSupertagString);
					} else {
						supertags.add(0, new Supertag(goldSupertagString, goldLexicalCategory, 1.0));
					}
				}

				sentence.addSupertags(supertags);
			}
		} catch ( IOException e ) {
			logger.error(e);
			next = null;
		}
	}

	@Override
	public boolean hasNext() {
		if ( !sentenceRead ) {
			readSentence();
			sentenceRead = true;
		}

		return next != null;
	}

	@Override
	public Sentence next() {
		if ( !sentenceRead ) {
			readSentence();
			sentenceRead = true;
		}

		if ( next == null ) {
			throw new NoSuchElementException();
		} else {
			sentenceRead = false;
			return next;
		}
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
