package io;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import training.Feature;
import training.Forest;

public class Forests implements Iterator<Forest> {
	private BufferedReader in;
	private Feature[] features;
	private Forest next;

	private boolean forestRead = false;

	public Forests(BufferedReader in, Feature[] features) throws IOException {
		this.in = in;
		this.features = features;

		in.mark(0);
	}

	public void reset() throws IOException {
		in.reset();
	}

	public void skip(int n) {
		for ( int i = 0; i < n; i++ ) {
			readForest();
		}
	}

	private void readForest() {
			try {
			String line = in.readLine();
	
			if ( line == null ) {
				// no valid forest
				next = null;
				return;
			}
	
			int numNodes = Integer.parseInt(line);
			Forest forest = new Forest(in, features, numNodes);
			next = forest;
		} catch ( IOException e ) {
			System.err.println(e);
			next = null;
		}
	}

	@Override
	public boolean hasNext() {
		if ( !forestRead ) {
			readForest();
			forestRead = true;
		}

		return next != null;
	}

	@Override
	public Forest next() {
		if ( !forestRead ) {
			readForest();
			forestRead = true;
		}

		if ( next == null ) {
			throw new NoSuchElementException();
		} else {
			forestRead = false;
			return next;
		}
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
