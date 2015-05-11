package chart_parser;

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
		if (this.score == other.score) {
			return 0;
		} else if (this.score < other.score) {
			return 1;
		} else {
			return -1;
		}
	}
}
