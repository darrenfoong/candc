import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.io.IoBuilder;

import cat_combination.RuleInstancesParams;
import cat_combination.Rules;
import chart_parser.ChartParser;
import chart_parser.OracleDecoder;
import chart_parser.OracleDepsSumDecoder;
import chart_parser.OracleFscoreDecoder;
import io.Params;
import io.Preface;
import io.Sentences;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

/*
 * this gets used for a number of tasks:
 *
 * printing out the best oracle deps for testing the coverage of the
 * grammar against the evaluate script, when cat strings are needed
 *
 * printing out the best oracle deps which will be used for training,
 * when relIDs are needed
 * 
 * saving the rule instances used in the correct oracle derivations
 * (currently not used)
 */

public class OracleParser {
	public static void main(String[] args) {
		double[] betas = { 0.01, 0.01, 0.01, 0.03, 0.075 };

		OptionParser optionParser = Params.getOracleParserOptionParser();
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

		RuleInstancesParams ruleInstancesParams = new RuleInstancesParams(true, false, false, false, false, false, grammarDir);

		boolean altMarkedup = (Boolean) options.valueOf("altMarkedup");
		boolean eisnerNormalForm = (Boolean) options.valueOf("eisnerNormalForm");
		boolean adaptiveSupertagging = (Boolean) options.valueOf("adaptiveSupertagging");
		boolean depsSumDecoder = (Boolean) options.valueOf("depsSumDecoder");
		boolean oracleFscore = !depsSumDecoder;
		boolean extractRuleInstances = (Boolean) options.valueOf("extractRuleInstances");
		String oracleRuleInstancesFile = (String) options.valueOf("oracleRuleInstancesFile");
		boolean training = (Boolean) options.valueOf("training");

		String inputFile = (String) options.valueOf("input");
		String goldSupertagsFile = (String) options.valueOf("goldSupertags");
		String outputFile = (String) options.valueOf("output");
		String logFile = (String) options.valueOf("log");
		String goldDepsFile = (String) options.valueOf("goldDeps");
		String rootCatsFile = (String) options.valueOf("rootCats");
		int fromSentence = (Integer) options.valueOf("from");
		int toSentence = (Integer) options.valueOf("to");

		System.setProperty("logLevel", options.has("verbose") ? "trace" : "info");
		System.setProperty("logFile", logFile);
		final Logger logger = LogManager.getLogger(OracleParser.class);

		ChartParser parser = null;
		OracleDecoder oracleDecoder = null;

		try {
			parser = new ChartParser(grammarDir, altMarkedup,
					eisnerNormalForm, MAX_WORDS, MAX_SUPERCATS,
					oracleFscore, adaptiveSupertagging, ruleInstancesParams, null,
					null, null, false, false);

			if ( depsSumDecoder ) {
				oracleDecoder = new OracleDepsSumDecoder(parser.categories, extractRuleInstances);
			} else {
				oracleDecoder = new OracleFscoreDecoder(parser.categories, extractRuleInstances);
			}
		} catch ( IOException e ) {
			logger.error(e);
			return;
		}

		try ( BufferedReader in = new BufferedReader(new FileReader(inputFile));
				BufferedReader gold = new BufferedReader(new FileReader(goldDepsFile));
				BufferedReader stagsIn = !goldSupertagsFile.equals("null") ? new BufferedReader(new FileReader(goldSupertagsFile)) : null;
				BufferedReader roots = !rootCatsFile.equals("null") ? new BufferedReader(new FileReader(rootCatsFile)) : null;
				PrintWriter out = new PrintWriter(new FileWriter(outputFile));
				PrintWriter out2 = new PrintWriter(new FileWriter(outputFile + ".per_cell"));
				PrintWriter log = IoBuilder.forLogger(logger).setLevel(Level.INFO).buildPrintWriter();
				PrintWriter rules = extractRuleInstances ? new PrintWriter(new FileWriter(oracleRuleInstancesFile)) : null ) {

			Preface.readPreface(in);
			Preface.readPreface(gold);
			if ( stagsIn != null ) {
				Preface.readPreface(stagsIn);
			}
			if ( roots != null ) {
				Preface.readPreface(roots);
			}
			Preface.printPreface(out);

			boolean ignoreDepsFlag = true;

			Sentences sentences = new Sentences(in, stagsIn, parser.categories, MAX_WORDS);
			sentences.skip(fromSentence - 1);

			for ( int numSentence = fromSentence; numSentence <= toSentence && sentences.hasNext(); numSentence++ ) {
				logger.info("Parsing sentence " + numSentence);

				parser.parseSentence(sentences.next(), betas);

				oracleDecoder.readDeps(gold, parser.categories);

				if ( roots != null ) {
					oracleDecoder.readRootCat(roots, parser.categories);
				}

				if ( !parser.maxWordsExceeded && !parser.maxSuperCatsExceeded ) {
					double maxScore = oracleDecoder.decode(parser.chart, parser.sentence);

					logger.info("Max score: " + maxScore);
					logger.info("Num gold deps: " + oracleDecoder.numGoldDeps());
					if ( maxScore == Double.NEGATIVE_INFINITY ) {
						logger.info("NO SPAN!");
					}

					boolean checkRoot = true;
					if ( oracleDecoder.getParserDeps(parser.chart, maxScore, parser.sentence, ignoreDepsFlag, checkRoot) ) {
						logger.info("Num parser deps: " + oracleDecoder.numParserDeps());
						oracleDecoder.printMissingDeps(log, parser.categories.dependencyRelations, parser.sentence);

						if ( !training ) {
							oracleDecoder.printDeps(out, parser.categories.dependencyRelations, parser.sentence, false);
							parser.sentence.printC_line(out);
						} else {
							boolean foundMax = oracleDecoder.markOracleDeps(parser.chart, maxScore, extractRuleInstances, checkRoot);
							if ( foundMax ) {
								oracleDecoder.printDepsForTraining(out, parser.categories, parser.sentence);
								parser.sentence.printSupertags(out2);
								parser.printCellDepsForTraining(out2, parser.categories, parser.sentence, oracleDecoder);
								// decoder is used to provide the IgnoreDepsEval object
							}
						}
					}

					if ( extractRuleInstances ) {
						oracleDecoder.markOracleDeps(parser.chart, maxScore, extractRuleInstances, checkRoot);
					}
				}

				out.println();
				out2.println();
				log.println();
			}

			if ( extractRuleInstances ) {
				oracleDecoder.printRuleInstances(rules);
			}

			logger.info("Rule counts:");
			logger.info("Forward application: " + Rules.forwardAppCount);
			logger.info("Backward application: " + Rules.backwardAppCount);
			logger.info("Forward Composition: " + Rules.forwardCompCount);
			logger.info("Backward Composition: " + Rules.backwardCompCount);
			logger.info("Backward Cross Composition: " + Rules.backwardCrossCount);
			logger.info("Generalised Forward Composition: " + Rules.genForwardCompCount);
			logger.info("Generalised Backward Composition: " + Rules.genBackwardCompCount);
			logger.info("Generalised Backward Cross Composition: " + Rules.genBackwardCrossCount);
			logger.info("Apposition: " + Rules.appositionCount);
			logger.info("Right punct: " + Rules.rightPunctCount);
			logger.info("Left punct: " + Rules.leftPunctCount);
			logger.info("Left punct conj: " + Rules.leftPunctConjCount);
			logger.info("Left comma type-change: " + Rules.leftCommaTCCount);
			logger.info("Right comma type-change: " + Rules.rightCommaTCCount);
			logger.info("Conj count: " + Rules.conjCount);
			logger.info("Funny conj count: " + Rules.funnyConjCount);
		} catch ( FileNotFoundException e ) {
			logger.error(e);
		} catch ( IOException e ) {
			logger.error(e);
		}
	}
}
