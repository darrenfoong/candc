package training;

import utils.NumericalFunctions;

public class DisjNode {
	ConjNode[] conjNodes;

	double score; // used for Viterbi for the perceptron and inside for the
	// log-linear model
	double outside;
	boolean marker;

	public DisjNode(int numConjs) {
		conjNodes = new ConjNode[numConjs];
		score = 0.0;
		marker = false;
	}

	public void add(ConjNode conjNode, int nodeNum) {
		conjNodes[nodeNum] = conjNode;
	}

	public void resetValues() {
		score = 0.0;
		outside = 0.0;
		marker = false;

		for (int i = 0; i < conjNodes.length; i++) {
			conjNodes[i].resetValues();
		}
	}

	public boolean goldMarker() {
		for (int i = 0; i < conjNodes.length; i++) {
			ConjNode node = conjNodes[i];
			if (node.goldMarker) {
				return true;
			}
		}
		return false;
	}

	public double viterbi(boolean gold) {
		if (marker) {
			return score;
		}

		double maxScore = Double.NEGATIVE_INFINITY;
		double viterbiScore;
		ConjNode maxNode = null;

		for (int i = 0; i < conjNodes.length; i++) {
			ConjNode node = conjNodes[i];
			if (!gold || node.goldMarker) {
				viterbiScore = node.viterbi(gold);

				/*
				 * if (node.goldMarker) { if (gold)
				 * System.out.print("GOLD VITERBI "); else
				 * System.out.print("ALL VITERBI ");
				 * System.out.println("tracing out gold: " + viterbiScore); }
				 */

				if (viterbiScore > maxScore) {
					maxScore = viterbiScore;
					maxNode = node;
				}
			}
		}
		if (maxNode != null) {
			maxNode.viterbiMarker = true; // mark the conjNode as being in the
			// highest scoring derivation
		} else {
			throw new Error("shld always have a maxNode!");
		}

		score = maxScore; // keep this in case we see this node again
		marker = true; // record the fact we've been here

		return maxScore;
	}

	public void calcInside(boolean gold) {
		score = 0.0;

		for (int i = 0; i < conjNodes.length; i++) {
			ConjNode node = conjNodes[i];
			if (!gold || node.goldMarker) {
				if (score == 0.0) {
					score = node.calcInside(); // need to get a non-zero score
					// before passing to addLogs (?)
				} else {
					score = NumericalFunctions
							.addLogs(score, node.calcInside());
				}
			}
		}
	}

	public void calcOutside(double invZ, boolean gold) {
		for (int i = 0; i < conjNodes.length; i++) {
			ConjNode node = conjNodes[i];
			if (!gold || node.goldMarker) {
				node.calcOutside(outside, invZ, gold);
			}
		}
	}

	public void perceptronUpdate(boolean positiveUpdate) {
		for (int i = 0; i < conjNodes.length; i++) {
			if (conjNodes[i].viterbiMarker) {
				conjNodes[i].perceptronUpdate(positiveUpdate);
				return;
			}
		}
		throw new Error("didn't find a viterbi marker!");
	}

}
