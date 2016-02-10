import io.Preface;
import io.Sentences;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import cat_combination.RuleInstancesParams;
import cat_combination.Rules;
import chart_parser.ChartParser;
import chart_parser.OracleDecoder;
import chart_parser.OracleDepsSumDecoder;
import chart_parser.OracleFscoreDecoder;

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
		int MAX_WORDS = 250;
		int MAX_SUPERCATS = 2000000;

		boolean altMarkedup = false;
		boolean eisnerNormalForm = true;
		boolean detailedOutput = false;

		boolean depsSumDecoder = false;
		// if false then uses the F-score decoder
		boolean oracleFscore = !depsSumDecoder;

		String grammarDir = "grammar";

		RuleInstancesParams ruleInstancesParams = new RuleInstancesParams(true, false, false, false, false, false, grammarDir);

		boolean extractRuleInstances = false;
		String oracleRuleInstancesFile = "data/baseline_expts/working/oracleRuleInstances";
		// not currently used (but can be used to get rules files above)

		boolean adaptiveSupertagging = false;
		double[] betas = { 0.01, 0.01, 0.01, 0.03, 0.075 };

		boolean training = true;
		// set to false if the string category and slot are needed for evaluation; true gives deps used for training

		if ( args.length < 8 ) {
			System.err.println("OracleParser requires 8 arguments: <inputFile> <goldSupertagsFile> <outputFile> <logFile> <goldDepsFile> <rootCatsFile> <fromSentence> <toSentence>");
			return;
		}

		String inputFile = args[0];
		String goldSupertagsFile = args[1]; // could be "null"
		String outputFile = args[2];
		String logFile = args[3];
		String goldDepsFile = args[4];
		String rootCatsFile = args[5]; // could be "null"
		String fromSent = args[6];
		String toSent = args[7];

		int fromSentence = Integer.parseInt(fromSent);
		int toSentence = Integer.parseInt(toSent);

		ChartParser parser = null;

		try {
			parser = new ChartParser(grammarDir, altMarkedup,
					eisnerNormalForm, MAX_WORDS, MAX_SUPERCATS, detailedOutput,
					oracleFscore, adaptiveSupertagging, ruleInstancesParams, null,
					null, null, false, false);
		} catch ( IOException e ) {
			System.err.println(e);
			return;
		}

		OracleDecoder oracleDecoder = null;

		if ( depsSumDecoder ) {
			oracleDecoder = new OracleDepsSumDecoder(parser.categories, extractRuleInstances);
		} else {
			oracleDecoder = new OracleFscoreDecoder(parser.categories, extractRuleInstances);
		}

		try ( BufferedReader in = new BufferedReader(new FileReader(inputFile));
				BufferedReader gold = new BufferedReader(new FileReader(goldDepsFile));
				BufferedReader stagsIn = !goldSupertagsFile.equals("null") ? new BufferedReader(new FileReader(goldSupertagsFile)) : null;
				BufferedReader roots = new BufferedReader(new FileReader(rootCatsFile));
				PrintWriter out = new PrintWriter(new FileWriter(outputFile));
				PrintWriter out2 = new PrintWriter(new FileWriter(outputFile + ".per_cell"));
				PrintWriter log = new PrintWriter(new FileWriter(logFile));
				PrintWriter rules = extractRuleInstances ? new PrintWriter(new FileWriter(oracleRuleInstancesFile)) : null ) {

			Preface.readPreface(in);
			Preface.readPreface(gold);
			if ( stagsIn != null ) {
				Preface.readPreface(stagsIn);
			}
			Preface.readPreface(roots);
			Preface.printPreface(out);

			boolean ignoreDepsFlag = true;

			Sentences sentences = new Sentences(in, null, parser.categories, MAX_WORDS);
			sentences.skip(fromSentence - 1);

			for ( int numSentence = fromSentence; numSentence <= toSentence && sentences.hasNext(); numSentence++ ) {
				System.out.println("Parsing sentence " + numSentence);
				log.println("Parsing sentence " + numSentence);

				parser.parseSentence(sentences.next(), log, betas);

				oracleDecoder.readDeps(gold, parser.categories);

				if ( roots != null ) {
					oracleDecoder.readRootCat(roots, parser.categories);
				}

				if ( !parser.maxWordsExceeded && !parser.maxSuperCatsExceeded ) {
					double maxScore = oracleDecoder.decode(parser.chart, parser.sentence);

					log.println("Max score: " + maxScore);
					log.println("Num gold deps: " + oracleDecoder.numGoldDeps());
					if ( maxScore == Double.NEGATIVE_INFINITY ) {
						log.println("NO SPAN!");
					}

					boolean checkRoot = true;
					if ( oracleDecoder.getParserDeps(parser.chart, maxScore, parser.sentence, ignoreDepsFlag, checkRoot) ) {
						log.println("Num parser deps: " + oracleDecoder.numParserDeps());
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

			log.println("Rule counts:");
			log.println("Forward application: " + Rules.forwardAppCount);
			log.println("Backward application: " + Rules.backwardAppCount);
			log.println("Forward Composition: " + Rules.forwardCompCount);
			log.println("Backward Composition: " + Rules.backwardCompCount);
			log.println("Backward Cross Composition: " + Rules.backwardCrossCount);
			log.println("Generalised Forward Composition: " + Rules.genForwardCompCount);
			log.println("Generalised Backward Composition: " + Rules.genBackwardCompCount);
			log.println("Generalised Backward Cross Composition: " + Rules.genBackwardCrossCount);
			log.println("Apposition: " + Rules.appositionCount);
			log.println("Right punct: " + Rules.rightPunctCount);
			log.println("Left punct: " + Rules.leftPunctCount);
			log.println("Left punct conj: " + Rules.leftPunctConjCount);
			log.println("Left comma type-change: " + Rules.leftCommaTCCount);
			log.println("Right comma type-change: " + Rules.rightCommaTCCount);
			log.println("Conj count: " + Rules.conjCount);
			log.println("Funny conj count: " + Rules.funnyConjCount);
		} catch ( FileNotFoundException e ) {
			System.err.println(e);
		} catch ( IOException e ) {
			System.err.println(e);
		}
	}
}
