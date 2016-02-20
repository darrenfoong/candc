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
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import training.Feature;
import training.Forest;

public class TrainLogLinear {
	public static void main(String[] args) {
		double LEARNING_RATE = 0.5;

		OptionParser optionParser = new OptionParser();
		optionParser.accepts("help").forHelp();
		optionParser.accepts("verbose");
		optionParser.accepts("forest").withRequiredArg().ofType(String.class).required();
		optionParser.accepts("weights").withRequiredArg().ofType(String.class).required();
		optionParser.accepts("log").withRequiredArg().ofType(String.class).required();
		optionParser.accepts("numIterations").withRequiredArg().ofType(Integer.class).required();
		optionParser.accepts("from").withRequiredArg().ofType(Integer.class).defaultsTo(1);
		optionParser.accepts("to").withRequiredArg().ofType(Integer.class).defaultsTo(Integer.MAX_VALUE);

		OptionSet options = null;

		try {
			options = optionParser.parse(args);
		} catch ( OptionException e ) {
			System.err.println(e.getMessage());
			return;
		}

		try {
			if ( options.has("help") ) {
				optionParser.printHelpOn(System.out);
				return;
			}
		} catch ( IOException e ) {
			System.err.println(e);
			return;
		}

		if ( options.has("verbose") ) {
			System.setProperty("logLevel", "trace");
		} else {
			System.setProperty("logLevel", "info");
		}

		String forestFile = (String) options.valueOf("forest");
		String weightsFile = (String) options.valueOf("weights");
		String logFile = (String) options.valueOf("log");
		int numIterations = (Integer) options.valueOf("numIterations");
		int fromSentence = (Integer) options.valueOf("from");
		int toSentence = (Integer) options.valueOf("to");

		System.setProperty("logFile", logFile);
		final Logger logger = LogManager.getLogger(TrainLogLinear.class);

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
					logger.info("Reading forest " + numForest);

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

				logger.info("Log-likelihood after iteration " + iteration + ": " + logLikelihood);
			}

			for ( int i = 0; i < features.length; i++ ) {
				out.println(i + " " + features[i].getLambda());
			}
		} catch (FileNotFoundException e) {
			logger.error(e);
		} catch (IOException e) {
			logger.error(e);
		}
	}
}
