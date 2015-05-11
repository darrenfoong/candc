package training;

import utils.NumericalFunctions;

public class ConjNode {
	DisjNode leftChild;
	DisjNode rightChild;

	boolean goldMarker;
	boolean viterbiMarker;

	double inside;
	double supertagScore;

	Feature[] features;

	public ConjNode(DisjNode leftChild, DisjNode rightChild, int numFeatures,
			boolean goldMarker, double supertagScore) {
		this.leftChild = leftChild;
		this.rightChild = rightChild;
		this.goldMarker = goldMarker;
		this.supertagScore = supertagScore;
		viterbiMarker = false;
		features = new Feature[numFeatures];
	}

	public void resetValues() {
		viterbiMarker = false;
		inside = 0.0;
	}

	public double viterbi(boolean gold) {
		double score = 0.0;
		for (int i = 0; i < features.length; i++) {
			score += features[i].getLambda();
		}

		if (leftChild != null) {
			if (rightChild != null) {
				return leftChild.viterbi(gold) + rightChild.viterbi(gold)
						+ score;
			} else {
				return leftChild.viterbi(gold) + score;
			}
		} else {
			score += supertagScore;
		}
		return score;
	}

	public double calcInside() {
		if (leftChild != null) {
			if (rightChild != null) {
				inside = leftChild.score + rightChild.score; // score is used
				// for inside
				// and viterbi
				// scores on
				// disj nodes
			} else {
				inside = leftChild.score;
			}
		} else {
			inside = 0.0;
		}

		for (int i = 0; i < features.length; i++) {
			inside += features[i].getLambda();
		}

		return inside;
	}

	// also calculates the feature expectations
	public void calcOutside(double outside, double invZ, boolean gold) {
		double sum = 0.0;
		for (int i = 0; i < features.length; i++) {
			sum += features[i].getLambda();
		}
		sum += outside;

		if (leftChild != null) {
			if (rightChild != null) {
				if (leftChild.outside != 0.0) {
					// suggest this may not be
					// completely correct
					leftChild.outside = NumericalFunctions.addLogs(
							leftChild.outside, rightChild.score + sum);
				} else {
					leftChild.outside = rightChild.score + sum;
				}

				if (rightChild.outside != 0.0) {
					rightChild.outside = NumericalFunctions.addLogs(
							rightChild.outside, leftChild.score + sum);
				} else {
					rightChild.outside = leftChild.score + sum;
				}
			} else {
				if (leftChild.outside != 0.0) {
					leftChild.outside = NumericalFunctions.addLogs(
							leftChild.outside, sum);
				} else {
					leftChild.outside = sum;
				}
			}
		}
		// calculate feature expectations:
		double prob = Math.exp(outside + inside + invZ);
		for (int i = 0; i < features.length; i++) {
			if (!gold) {
				features[i].setExpectedValue(features[i].getExpectedValue()+prob);
			} else {
				features[i].setEmpiricalValue(features[i].getEmpiricalValue()+prob);
			}
		}
	}

	public void perceptronUpdate(boolean positiveUpdate) {
		for (int i = 0; i < features.length; i++) {
			if (positiveUpdate) {
				features[i].setLambdaUpdate(features[i].getLambdaUpdate()+1);
			} else {
				features[i].setLambdaUpdate(features[i].getLambdaUpdate()-1);
			}
		}
		if (leftChild != null) {
			if (rightChild != null) {
				leftChild.perceptronUpdate(positiveUpdate);
				rightChild.perceptronUpdate(positiveUpdate);
			} else {
				leftChild.perceptronUpdate(positiveUpdate);
			}
		}
	}

}
