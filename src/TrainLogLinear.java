import io.Preface;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import training.Feature;
import training.Forest;

class TrainLogLinear {
	public static void main(String[] args) {
		int iteration = 1;
		int numForest = 0;
		int numSentences = 40000;
		double learningRate = 0.5;

		if ( args.length < 3 ) {
			System.err.println("TrainLogLinear requires 3 arguments: <forestFile> <weightsFile> <numIters>");
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

		try {
			in = new BufferedReader(new FileReader(forestFile));

			Preface.readPreface(in);

			out = new PrintWriter(new FileWriter(weightsFile));

			String line = in.readLine();
			int numFeatures = Integer.parseInt(line);
			features = new Feature[numFeatures];

			for (int i = 0; i < numFeatures; i++) {
				features[i] = new Feature(i);
			}

			numForest = 0;

			while (true) {
				numForest++;

				if (numForest > numSentences) {
					break;
				}

				System.out.println("reading forest " + numForest);

				line = in.readLine();
				if (line == null) {
					break;
				}

				int numNodes = Integer.parseInt(line);
				Forest forest = new Forest(in, features, numNodes);
				forests.add(forest);
			}

			while (iteration <= numIterations) {
				double logLikelihood = 0.0;
				// double sumZ = 0.0;
				numForest = 0;

				for (Forest forest : forests) {
					forest.resetNodeValues();
					numForest++;

					for (int i = 0; i < numFeatures; i++) {
						features[i].resetExpValues();
					}

					if (numForest % 50 == 0) {
						System.out.println("calculating inside and outside for forest " + numForest + " on iteration " + iteration);
					}

					double Z = forest.calcInside(false);
					// sumZ += Z;
					forest.calcOutside(-Z, false);
					// outside also calculates the feature expectations

					if (numForest % 50 == 0) {
						System.out.println("calculating inside and outside for gold forest " + numForest + " on iteration " + iteration);
					}

					forest.resetNodeValues();
					// presumably we need to reset here?
					double goldZ = forest.calcInside(true);
					forest.calcOutside(-goldZ, true);

					logLikelihood += forest.logLikelihood();

					if (numForest % 50 == 0) {
						System.out.println("updating features");
					}

					for (int i = 0; i < numFeatures; i++) {
						features[i].adaGradUpdate(learningRate);
					}
				}

				System.out.println("log-likelihood after iteration " + iteration + " " + logLikelihood);

				iteration++;
			}

			for (int i = 0; i < features.length; i++) {
				out.println(i + " " + features[i].getLambda());
			}
		} catch (FileNotFoundException e) {
			System.err.println(e);
		} catch (IOException e) {
			System.err.println(e);
		} finally {
			try {
				if ( out != null ) { out.close(); }
				if ( in != null ) { in.close(); }
			} catch (IOException e) {
				System.err.println(e);
			}
		}
	}
}
