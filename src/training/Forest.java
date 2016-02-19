package training;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import utils.NumericalFunctions;

public class Forest {
	DisjNode[] disjNodes;
	ArrayList<DisjNode> rootNodes; // roots are on both disjNodes and rootNodes

	double logZ;
	double logZgold;

	public static final Logger logger = LogManager.getLogger(Forest.class);

	public Forest(BufferedReader in, Feature[] features, int numNodes) {
		rootNodes = new ArrayList<DisjNode>();
		String line;

		try {
			disjNodes = new DisjNode[numNodes];

			for (int i = 0; i < numNodes; i++) {
				line = in.readLine();
				int nodeID = Integer.parseInt(line);
				if (nodeID != i) {
					throw new Error(
							"disj node id does not match expected value! "
									+ line);
				}
				readDisjNode(in, i, features);
			}
			line = in.readLine();
			if (!line.isEmpty()) {
				throw new Error("expecting new line at end of forest!");
			}
		} catch (IOException e) {
			logger.error(e);
		}
	}

	// if gold is true then only go over gold derivations
	public DisjNode viterbi(boolean gold) {
		DisjNode maxRoot = null;
		double maxScore = Double.NEGATIVE_INFINITY;

		for (DisjNode root : rootNodes) {
			// if gold only bother with the root if it is gold
			if (!gold || root.goldMarker()) {
				double score = root.viterbi(gold);

				// System.out.println("root score: " + score + " gold: " +
				// root.goldMarker());

				if (score > maxScore) {
					maxScore = score;
					maxRoot = root;
				}
			}
		}

		// System.out.println("viterbi max value: " + maxScore);

		return maxRoot;
	}

	public double calcInside(boolean gold) {
		for (int i = 0; i < disjNodes.length; i++) {
			disjNodes[i].calcInside(gold);
		}

		// sum up the values for the roots:
		Iterator<DisjNode> it = rootNodes.iterator();
		if (!gold) {
			logZ = 0.0;
			while (it.hasNext()) {
				DisjNode root = it.next();
				if (logZ == 0.0) {
					logZ = root.score;
				} else {
					logZ = NumericalFunctions.addLogs(logZ, root.score);
				}
			}
			return logZ;
		} else {
			logZgold = 0.0;
			while (it.hasNext()) {
				DisjNode root = it.next();
				if (root.goldMarker()) {
					if (logZgold == 0.0) {
						logZgold = root.score;
					} else {
						logZgold = NumericalFunctions.addLogs(logZgold,
								root.score);
					}
				}
			}
			return logZgold;
		}
	}

	// check initialisation - c&c code suggests roots shld be 1, but
	// then has them intialised to zero!

	public void calcOutside(double invZ, boolean gold) {
		// nodes already initisalised by calling forest.resetNodeValues()

		// this also calculates the feature expectations (in
		// ConjNode.calcOutside)

		// iterating backwards over the disj nodes should ensure all parents
		// are dealt with before any children
		for (int i = disjNodes.length - 1; i >= 0; i--) {
			disjNodes[i].calcOutside(invZ, gold);
		}
	}

	public double logLikelihood() {
		return logZgold - logZ;
	}

	public void perceptronUpdate(DisjNode maxRoot, boolean positiveUpdate) {
		maxRoot.perceptronUpdate(positiveUpdate);
	}

	public void resetNodeValues() {
		for (int i = 0; i < disjNodes.length; i++) {
			disjNodes[i].resetValues();
		}
	}

	private void readDisjNode(BufferedReader in, int nodeID, Feature[] features) {
		try {
			String line = in.readLine();
			int numConjs = Integer.parseInt(line);

			DisjNode disjNode = new DisjNode(numConjs);
			disjNodes[nodeID] = disjNode;

			boolean atRoot = false;
			for (int i = 0; i < numConjs; i++) {
				atRoot = readConjNode(in, i, disjNode, features);
			}
			if (atRoot) {
				rootNodes.add(disjNode);
			}

		} catch (IOException e) {
			logger.error(e);
		}
	}

	private boolean readConjNode(BufferedReader in, int nodeNum,
			DisjNode disjNode, Feature[] features) {
		try {
			String line = in.readLine();
			String[] tokens = line.split("\\s");
			int nodeType = Integer.parseInt(tokens[0]);
			int nextTokenIndex;
			int leftChildID = -1;
			int rightChildID = -1;
			boolean goldMarker;
			double supertagScore = 0.0;

			switch (nodeType) { // reading the children
			case 0: // leaf node
				supertagScore = Double.parseDouble(tokens[1]);
				nextTokenIndex = 2;
				break;
			case 1: // unary node
				leftChildID = Integer.parseInt(tokens[1]);
				nextTokenIndex = 2;
				break;
			case 2: // binary (non-root) node - fall through
			case 3: // root node
				leftChildID = Integer.parseInt(tokens[1]);
				rightChildID = Integer.parseInt(tokens[2]);
				nextTokenIndex = 3;
				break;
			default:
				throw new Error(
						"expecting 0, 1, 2 or 3 for conj node type in forests!");
			}
			if (tokens[nextTokenIndex].equals("0")) {
				goldMarker = false;
			} else if (tokens[nextTokenIndex].equals("1")) {
				goldMarker = true;
			} else {
				throw new Error("gold marker has to be 0 or 1!");
			}
			nextTokenIndex++;

			int numFeatures = Integer.parseInt(tokens[nextTokenIndex]);
			nextTokenIndex++;

			// assumes forests are printed bottom up, ie we already have
			// children nodes on disjNodes
			DisjNode leftChild = leftChildID == -1 ? null
					: disjNodes[leftChildID];
			DisjNode rightChild = rightChildID == -1 ? null
					: disjNodes[rightChildID];

			ConjNode conj = new ConjNode(leftChild, rightChild, numFeatures,
					goldMarker, supertagScore);
			disjNode.add(conj, nodeNum);

			for (int i = 0; i < numFeatures; i++) {
				int featureID = Integer.parseInt(tokens[nextTokenIndex + i]);
				conj.features[i] = features[featureID];
			}
			if (nodeType == 3) {
				return true;
			} else {
				return false;
			}

		} catch (IOException e) {
			logger.error(e);
		}
		return false; // should never get here
	}

}
