import io.Forests;
import io.Preface;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import training.Feature;
import training.Forest;

public class TrainLogLinear {
	public static void main(String[] args) {
		double LEARNING_RATE = 0.5;

		if ( args.length < 5 ) {
			System.err.println("TrainLogLinear requires 5 arguments: <forestFile> <weightsFile> <numIters> <fromSentence> <toSentence>");
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

			Forests forests = new Forests(in, features);

			for ( int iteration = 1; iteration <= numIterations; iteration++ ) {
				double logLikelihood = 0.0;

				forests.skip(fromSentence - 1);

				for ( int numForest = fromSentence; numForest <= toSentence && forests.hasNext() ; numForest++ ) {
					System.out.println("Reading forest " + numForest);

					Forest forest = forests.next();

					forest.resetNodeValues();

					for ( int i = 0; i < numFeatures; i++ ) {
						features[i].resetExpValues();
					}

					double Z = forest.calcInside(false);
					forest.calcOutside(-Z, false);
					// outside also calculates the feature expectations

					forest.resetNodeValues();
					// presumably we need to reset here?
					double goldZ = forest.calcInside(true);
					forest.calcOutside(-goldZ, true);

					logLikelihood += forest.logLikelihood();

					for ( Feature feature : features ) {
						feature.adaGradUpdate(LEARNING_RATE);
					}
				}

				System.out.println("Log-likelihood after iteration " + iteration + ": " + logLikelihood);
			}

			for ( int i = 0; i < features.length; i++ ) {
				out.println(i + " " + features[i].getLambda());
			}
		} catch (IOException e) {
			System.err.println(e);
		}
	}
}
