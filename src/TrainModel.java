import io.Preface;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import training.DisjNode;
import training.Feature;
import training.Forest;

class TrainModel {
	public static void main(String[] args) {
		int iteration = 1;
		int numForest = 0;
		int fromSentence = 1;
		int toSentence = 40000;

		if ( args.length < 3 ) {
			System.err.println("TrainModel requires 3 arguments: <forestFile> <weightsFile> <numIters>");
			return;
		}

		String forestFile = args[0];
		String weightsFile = args[1];
		String numItersStr = args[2];
		int numIterations = Integer.parseInt(numItersStr);

		Feature[] features = null;
		ArrayList<Forest> forests = new ArrayList<Forest>();

		BufferedReader in = null;
		PrintWriter out = null;
		PrintWriter outIter = null;

		try {
			in = new BufferedReader(new FileReader(forestFile));

			Preface.readPreface(in);

			out = new PrintWriter(new FileWriter(weightsFile));
			outIter = new PrintWriter(new FileWriter(weightsFile + "." + iteration));

			String line = in.readLine();
			int numFeatures = Integer.parseInt(line);
			features = new Feature[numFeatures];

			for (int i = 0; i < numFeatures; i++) {
				features[i] = new Feature(i);
			}

			numForest = 0;

			while (true) {
				numForest++;

				if (numForest > toSentence) {
					break;
				}

				System.out.println("reading forest " + numForest);

				line = in.readLine();
				if (line == null) {
					break;
				}

				int numNodes = Integer.parseInt(line);
				Forest forest = new Forest(in, features, numNodes);

				if (numForest < fromSentence) {
					continue;
				}

				forests.add(forest);
			}

			while (iteration <= numIterations) {
				numForest = 0;
				for (Forest forest : forests) {
					numForest++;

					if (numForest % 100 == 0) {
						System.out.println("doing update for forest " + numForest + " on iteration " + iteration);
					}

					forest.resetNodeValues();
					DisjNode maxRoot = forest.viterbi(false);
					// boolean indicates we go over all derivations
					forest.perceptronUpdate(maxRoot, false);
					// boolean indicates a negative update

					forest.resetNodeValues();
					maxRoot = forest.viterbi(true);
					// boolean indicates we only go over gold derivations
					forest.perceptronUpdate(maxRoot, true);
					// boolean indicates a positive update

					for (int i = 0; i < numFeatures; i++) {
						features[i].perceptronUpdate();
					}
				}

				for (int i = 0; i < features.length; i++) {
					outIter.print(i + " " + features[i].getLambda() + " ");
					outIter.println(features[i].getCumulativeLambda() / (numForest * iteration));
				}
				iteration++;
			}

			for (int i = 0; i < features.length; i++) {
				out.print(i + " " + features[i].getLambda() + " ");
				out.println(features[i].getCumulativeLambda() / (numForest * numIterations));
			}
		} catch (IOException e) {
			System.err.println(e);
		} finally {
			try {
				if ( outIter != null ) { outIter.close(); }
				if ( out != null ) { out.close(); }
				if ( in != null ) { in.close(); }
			} catch (IOException e) {
				System.err.println(e);
			}
		}
	}
}
