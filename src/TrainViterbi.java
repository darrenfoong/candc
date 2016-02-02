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

public class TrainViterbi {
	public static void main(String[] args) {
		if ( args.length < 5 ) {
			System.err.println("TrainViterbi requires 5 arguments: <forestFile> <weightsFile> <numIters> <fromSentence> <toSentence>");
			return;
		}

		String forestFile = args[0];
		String weightsFile = args[1];
		String numItersStr = args[2];
		String fromSent = args[3];
		String toSent = args[4];

		int fromSentence = Integer.parseInt(fromSent);
		int toSentence = Integer.parseInt(toSent);
		int numIterations = Integer.parseInt(numItersStr);

		try ( BufferedReader in = new BufferedReader(new FileReader(forestFile));
			 PrintWriter out = new PrintWriter(new FileWriter(weightsFile)) ) {

			Preface.readPreface(in);

			String line = in.readLine();
			int numFeatures = Integer.parseInt(line);
			Feature[] features = new Feature[numFeatures];

			for ( int i = 0; i < numFeatures; i++ ) {
				features[i] = new Feature(i);
			}

			ArrayList<Forest> forests = new ArrayList<Forest>();

			for ( int numForest = 1; numForest <= (toSentence - fromSentence + 1) ; numForest++ ) {
				System.out.println("Reading forest " + numForest);

				if ( (line = in.readLine()) == null) {
					break;
				}

				int numNodes = Integer.parseInt(line);
				forests.add(new Forest(in, features, numNodes));
			}

			for ( int iteration = 1; iteration <= numIterations; iteration++ ) {
				for (Forest forest : forests) {
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

					for ( int i = 0; i < numFeatures; i++ ) {
						features[i].perceptronUpdate();
					}
				}

				try ( PrintWriter outIter = new PrintWriter(new FileWriter(weightsFile + "." + iteration)) ) {
					for ( int i = 0; i < features.length; i++ ) {
						outIter.print(i + " " + features[i].getLambda() + " ");
						outIter.println(features[i].getCumulativeLambda() / (forests.size() * iteration));
					}
				}
			}

			for ( int i = 0; i < features.length; i++ ) {
				out.print(i + " " + features[i].getLambda() + " ");
				out.println(features[i].getCumulativeLambda() / (forests.size() * numIterations));
			}
		} catch (IOException e) {
			System.err.println(e);
		}
	}
}
