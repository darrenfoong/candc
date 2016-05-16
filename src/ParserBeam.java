import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cat_combination.RuleInstancesParams;
import chart_parser.ChartParserBeam;
import io.Params;
import io.Preface;
import io.Sentences;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import model.Lexicon;

public class ParserBeam {
	public static void main(String[] args) {
		OptionParser optionParser = Params.getParserBeamOptionParser();
		OptionSet options = null;

		try {
			options = optionParser.parse(args);
			if ( options.has("help") ) {
				optionParser.printHelpOn(System.out);
				return;
			}
		} catch ( OptionException e ) {
			System.err.println(e.getMessage());
			return;
		} catch ( IOException e ) {
			System.err.println(e);
			return;
		}

		int MAX_WORDS = (Integer) options.valueOf("maxWords");

		String grammarDir = (String) options.valueOf("grammarDir");
		String lexiconFile = (String) options.valueOf("lexiconFile");
		String featuresFile = (String) options.valueOf("featuresFile");

		RuleInstancesParams ruleInstancesParams = new RuleInstancesParams(true, false, false, false, false, false, grammarDir);

		boolean altMarkedup = (Boolean) options.valueOf("altMarkedup");
		boolean eisnerNormalForm = (Boolean) options.valueOf("eisnerNormalForm");
		boolean newFeatures = (Boolean) options.valueOf("newFeatures");
		boolean compactWeights = (Boolean) options.valueOf("compactWeights");
		boolean cubePruning = (Boolean) options.valueOf("cubePruning");
		boolean depnn = options.has("depnn");

		String modelDir = null;
		boolean nnHardLabels = true;
		double nnPosThres = 0;
		double nnNegThres = 0;

		if ( depnn ) {
			modelDir = (String) options.valueOf("modelDir");
			nnHardLabels = (Boolean) options.valueOf("nnHardLabels");
			nnPosThres = (Double) options.valueOf("nnPosThres");
			nnNegThres = (Double) options.valueOf("nnNegThres");
		}

		boolean skimmer = (Boolean) options.valueOf("skimmer");
		boolean printChartDeps = (Boolean) options.valueOf("printChartDeps");
		boolean printChartFeatures = (Boolean) options.valueOf("printChartFeatures");
		double[] betas = Params.betasArray((String) options.valueOf("betas"));
		int beamSize = (Integer) options.valueOf("beamSize");
		double beta = (Double) options.valueOf("beta");

		String inputFile = (String) options.valueOf("input");
		String outputFile = (String) options.valueOf("output");
		String logFile = (String) options.valueOf("log");
		String weightsFile = (String) options.valueOf("weights");
		int fromSentence = (Integer) options.valueOf("from");
		int toSentence = (Integer) options.valueOf("to");

		System.setProperty("logLevel", options.has("verbose") ? "trace" : "info");
		System.setProperty("logFile", logFile);
		final Logger logger = LogManager.getLogger(ParserBeam.class);

		logger.info(Params.printOptions(options));

		Lexicon lexicon = null;
		ChartParserBeam parser = null;

		try {
			lexicon = new Lexicon(lexiconFile);
			parser = new ChartParserBeam(grammarDir, altMarkedup,
					eisnerNormalForm, MAX_WORDS,
					ruleInstancesParams, lexicon, featuresFile, weightsFile,
					newFeatures, compactWeights, cubePruning, betas, beamSize, beta);
			if ( depnn ) {
				parser.initDepNN(modelDir, nnHardLabels, nnPosThres, nnNegThres);
			}
		} catch ( IOException e ) {
			logger.error(e);
			return;
		}

		try ( BufferedReader in = new BufferedReader(new FileReader(inputFile));
				PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));
				PrintWriter outChartDeps = printChartDeps ? new PrintWriter(new BufferedWriter(new FileWriter(outputFile + ".chartdeps"))) : null;
				PrintWriter outFeatures = printChartFeatures ? new PrintWriter(new BufferedWriter(new FileWriter(outputFile + ".feats"))) : null;
				PrintWriter outChartFeatures = printChartFeatures ? new PrintWriter(new BufferedWriter(new FileWriter(outputFile + ".chartfeats"))) : null ) {

			Preface.readPreface(in);
			Preface.printPreface(out);

			if ( printChartDeps ) {
				Preface.printPreface(outChartDeps);
			}

			if ( printChartFeatures ) {
				Preface.printPreface(outFeatures);
				Preface.printPreface(outChartFeatures);
			}

			Sentences sentences = new Sentences(in, null, parser.categories, MAX_WORDS);
			sentences.skip(fromSentence - 1);

			for ( int numSentence = fromSentence; numSentence <= toSentence && sentences.hasNext(); numSentence++ ) {
				logger.info("Parsing sentence " + numSentence);

				parser.parseSentence(sentences.next());

				if ( !parser.maxWordsExceeded ) {
					boolean success = parser.root();

					if ( success ) {
						parser.printDeps(out, parser.categories.dependencyRelations, parser.sentence);
						parser.sentence.printC_line(out);
					} else {
						logger.info("No root category.");

						if ( skimmer ) {
							logger.info("Calling skimmer");
							parser.skimmer(out, parser.categories.dependencyRelations, parser.sentence);
							parser.sentence.printC_line(out);
						}
					}

					if ( printChartFeatures ) {
						parser.printFeatures(outFeatures, parser.sentence);
					}
				}

				out.println();

				if ( printChartDeps ) {
					parser.printChartDeps(outChartDeps, parser.categories.dependencyRelations, parser.sentence);
				}

				if ( printChartFeatures ) {
					outFeatures.println();
					parser.printChartFeatures(outChartFeatures, parser.sentence);
				}
			}
		} catch ( FileNotFoundException e ) {
			logger.error(e);
		} catch ( IOException e ) {
			logger.error(e);
		}
	}
}
