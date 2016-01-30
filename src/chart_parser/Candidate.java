package chart_parser;

import utils.Hash;

public class Candidate implements Comparable<Candidate> {
	int k; // choice point
	int leftIndex; // index into left k-best array
	int rightIndex; // index into right k-best array
	double score; // sum of the scores of the two children

	public Candidate(int k, int leftIndex, int rightIndex, double score) {
		this.k = k;
		this.leftIndex = leftIndex;
		this.rightIndex = rightIndex;
		this.score = score;
	}

	@Override
	public int compareTo(Candidate other) {
		return Double.compare(other.score, this.score);
	}

	@Override
	public int hashCode() {
		Hash h = new Hash(k);
		h.plusEqual(leftIndex);
		h.plusEqual(rightIndex);
		h.plusEqual(Double.doubleToLongBits(score));
		return (int) (h.value());
	}

	@Override
	public boolean equals(Object other) {
		if ( other == null || getClass() != other.getClass() ) {
			return false;
		}

		Candidate cother = (Candidate) other;

		return compareTo(cother) == 0;
	}
}
