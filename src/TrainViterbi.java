import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.Forests;
import io.Preface;
import training.DisjNode;
import training.Feature;
import training.Forest;

public class TrainViterbi {
	public static void main(String[] args) {
		if ( args.length < 6 ) {
			System.err.println("TrainViterbi requires 6 arguments: <forestFile> <weightsFile> <logFile> <numIters> <fromSentence> <toSentence>");
			return;
		}

		String forestFile = args[0];
		String weightsFile = args[1];
		String logFile = args[2];
		String numItersStr = args[3];
		String fromSent = args[4];
		String toSent = args[5];

		System.setProperty("logFile", logFile);
		final Logger logger = LogManager.getLogger(TrainViterbi.class);

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

			int numTrainInstances = 1;

			for ( int iteration = 1; iteration <= numIterations; iteration++ ) {

				forests.skip(fromSentence - 1);

				for ( int numForest = fromSentence; numForest <= toSentence && forests.hasNext(); numForest++ ) {
					logger.info("Reading forest " + numForest);

					Forest forest = forests.next();

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

					for ( Feature feature : features ) {
						feature.perceptronUpdate();
					}

					numTrainInstances++;
				}

				try ( PrintWriter outIter = new PrintWriter(new FileWriter(weightsFile + "." + iteration)) ) {
					for ( int i = 0; i < features.length; i++ ) {
						outIter.print(i + " " + features[i].getLambda() + " ");
						outIter.println(features[i].getCumulativeLambda() / numTrainInstances);
					}
				}

				forests.reset();
			}

			for ( int i = 0; i < features.length; i++ ) {
				out.print(i + " " + features[i].getLambda() + " ");
				out.println(features[i].getCumulativeLambda() / numTrainInstances);
			}
		} catch ( FileNotFoundException e ) {
			logger.error(e);
		} catch ( IOException e ) {
			logger.error(e);
		}
	}
}
