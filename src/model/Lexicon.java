package model;

import io.Preface;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class Lexicon {
	private HashMap<String,Integer> lexicon;
	// map from words and pos as strings to IDs

	public Lexicon(String file) throws IOException {
		this.lexicon = new HashMap<String,Integer>();
		readWordPosFile(file);
	}

	public int getID(String word) {
		// returns -1 if word not in lexicon
		Integer ID = lexicon.get(word);
		if (ID == null) {
			return -1;
		} else {
			return ID;
		}
	}

	/*
	 * not assuming an empty intersection between words and pos (ie word and pos
	 * can get the same id eg punctuation); this is fine since the feature types
	 * distinguish word and pos
	 */
	private void readWordPosFile(String file) throws IOException {
		try ( BufferedReader in = new BufferedReader(new FileReader(file)) ) {
			Preface.readPreface(in);

			String wordPos = null;
			int ID = 0;

			while ((wordPos = in.readLine()) != null) {
				lexicon.put(wordPos, ID);
				ID++;
			}
		}
	}
}
