package statistics;

import java.util.ArrayList;

public class HypothesisSizeStatistics {
	private ArrayList<Integer> hypothesisSizes;
	private ArrayList<Integer> sentenceSizes;

	public HypothesisSizeStatistics() {
		hypothesisSizes = new ArrayList<Integer>();
		sentenceSizes = new ArrayList<Integer>();
	}

	public int getSize() {
		return sentenceSizes.size();
	}

	public void addData(int hypothesisSize, int sentenceSize) {
		hypothesisSizes.add(hypothesisSize);
		sentenceSizes.add(sentenceSize);
	}

	public double calcAverageProportion() {
		int numSentences = sentenceSizes.size();
		int sum = 0;

		for ( int i = 0; i < numSentences; i++ ) {
			sum += hypothesisSizes.get(i)/sentenceSizes.get(i);
		}

		return ((double) sum/(double) numSentences);
	}

	public void clear() {
		hypothesisSizes.clear();
		sentenceSizes.clear();
	}
}
