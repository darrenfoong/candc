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
import chart_parser.ChartParserBeamOracle;
import chart_parser.OracleDecoder;
import chart_parser.OracleDepsSumDecoder;
import io.Params;
import io.Preface;
import io.Sentences;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import model.Lexicon;

public class ParserBeamOracle {
	public static void main(String[] args) {
		OptionParser optionParser = Params.getParserBeamOracleOptionParser();
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
		int MAX_SUPERCATS = (Integer) options.valueOf("maxSupercats");

		String grammarDir = (String) options.valueOf("grammarDir");
		String lexiconFile = (String) options.valueOf("lexiconFile");
		String featuresFile = (String) options.valueOf("featuresFile");

		RuleInstancesParams ruleInstancesParams = new RuleInstancesParams(true, false, false, false, false, false, grammarDir);

		boolean altMarkedup = (Boolean) options.valueOf("altMarkedup");
		boolean eisnerNormalForm = (Boolean) options.valueOf("eisnerNormalForm");
		boolean newFeatures = (Boolean) options.valueOf("newFeatures");
		boolean cubePruning = (Boolean) options.valueOf("cubePruning");

		double[] betas = Params.betasArray((String) options.valueOf("betas"));
		int beamSize = (Integer) options.valueOf("beamSize");
		double beta = (Double) options.valueOf("beta");

		String inputFile = (String) options.valueOf("input");
		String outputFile = (String) options.valueOf("output");
		String logFile = (String) options.valueOf("log");
		String weightsFile = (String) options.valueOf("weights");
		String goldDepsFile = (String) options.valueOf("goldDeps");
		int fromSentence = (Integer) options.valueOf("from");
		int toSentence = (Integer) options.valueOf("to");

		System.setProperty("logLevel", options.has("verbose") ? "trace" : "info");
		System.setProperty("logFile", logFile);
		final Logger logger = LogManager.getLogger(ParserBeamOracle.class);

		logger.info(Params.printOptions(options));

		Lexicon lexicon = null;
		ChartParserBeamOracle parser = null;
		OracleDecoder oracleDecoder = null;

		try {
			lexicon = new Lexicon(lexiconFile);
			parser = new ChartParserBeamOracle(grammarDir, altMarkedup,
					eisnerNormalForm, MAX_WORDS, MAX_SUPERCATS,
					ruleInstancesParams, lexicon, featuresFile, weightsFile,
					newFeatures, cubePruning, betas, beamSize, beta, oracleDecoder);
			oracleDecoder = new OracleDepsSumDecoder(parser.categories, false, true ,true);
		} catch ( IOException e ) {
			logger.error(e);
			return;
		}

		try ( BufferedReader in = new BufferedReader(new FileReader(inputFile));
				BufferedReader goldDepsPerCell = new BufferedReader(new FileReader(goldDepsFile + ".per_cell"));
				PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile))) ) {

			Preface.readPreface(in);
			Preface.printPreface(out);

			parser.setGoldDepsPerCell(goldDepsPerCell);

			Sentences sentences = new Sentences(in, null, parser.categories, MAX_WORDS);
			sentences.skip(fromSentence - 1);

			for ( int numSentence = fromSentence; numSentence <= toSentence && sentences.hasNext(); numSentence++ ) {
				logger.info("Parsing sentence " + numSentence);

				parser.parseSentence(sentences.next());

				if ( !parser.maxWordsExceeded && !parser.maxSuperCatsExceeded ) {
					boolean success = parser.root();

					if ( success ) {
						parser.printDeps(out, parser.categories.dependencyRelations, parser.sentence);
						parser.sentence.printC_line(out);
					} else {
						logger.info("No root category.");
					}
				}

				out.println();
			}
		} catch ( FileNotFoundException e ) {
			logger.error(e);
		} catch ( IOException e ) {
			logger.error(e);
		}
	}
}
