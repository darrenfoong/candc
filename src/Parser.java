import io.Preface;
import io.Sentences;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import model.Lexicon;
import utils.Benchmark;
import cat_combination.RuleInstancesParams;
import chart_parser.ChartParser;
import chart_parser.ViterbiDecoder;

public class Parser {
	public static void main(String[] args) {
		int iterations = 1;
		for ( int i = 1; i <= iterations; i++ ) {
			System.out.println("## Start program iteration " + i);
			run(args);
			System.out.println("## End program iteration " + i);
		}
	}

	public static void run(String[] args) {
		long TS_PROGRAM = Benchmark.getTime();

		int MAX_WORDS = 250;
		int MAX_SUPERCATS = 300000;

		boolean altMarkedup = false;
		boolean eisnerNormalForm = true;
		boolean detailedOutput = false;
		boolean newFeatures = false;
		boolean compactWeights = true;
		boolean oracleFscore = false;

		String grammarDir = "data/baseline_expts/grammar";
		String lexiconFile = "data/baseline_expts/working/lexicon/wsj02-21.wordsPos";
		String featuresFile = "data/baseline_expts/working/lexicon/wsj02-21.feats.1-22";

		RuleInstancesParams ruleInstancesParams = new RuleInstancesParams(true, false, false, false, false, false, grammarDir);

		boolean adaptiveSupertagging = false;
		double[] betas = { 0.0001, 0.001, 0.01, 0.03, 0.075 };
		// boolean adaptiveSupertagging = true;
		// double[] betas = { 0.075, 0.03, 0.01, 0.001, 0.0001 };
		// true indicates beta values get larger, and first value is betas[0];
		// false is the opposite, and first value is betas[2]

		if ( args.length < 6 ) {
			System.err.println("Parser requires 6 arguments: <inputFile> <outputFile> <logFile> <weightsFile> <fromSentence> <toSentence>");
			return;
		}

		String inputFile = args[0];
		String outputFile = args[1];
		String logFile = args[2];
		String weightsFile = args[3];
		String fromSent = args[4];
		String toSent = args[5];

		int fromSentence = Integer.parseInt(fromSent);
		int toSentence = Integer.parseInt(toSent);

		Lexicon lexicon = null;

		try {
			long TS_LEXICON = Benchmark.getTime();
			lexicon = new Lexicon(lexiconFile);
			long TE_LEXICON = Benchmark.getTime();
			Benchmark.printTime("load lexicon", TS_LEXICON, TE_LEXICON);
		} catch (IOException e) {
			System.err.println(e);
			return;
		}

		ChartParser parser = null;

		try {
			long TS_PARSER_INIT = Benchmark.getTime();
			parser = new ChartParser(grammarDir, altMarkedup,
					eisnerNormalForm, MAX_WORDS, MAX_SUPERCATS, detailedOutput,
					oracleFscore, adaptiveSupertagging, ruleInstancesParams,
					lexicon, featuresFile, weightsFile, newFeatures,
					compactWeights);
			long TE_PARSER_INIT = Benchmark.getTime();
			Benchmark.printTime("init parser", TS_PARSER_INIT, TE_PARSER_INIT);
		} catch (IOException e) {
			System.err.println(e);
			return;
		}

		ViterbiDecoder viterbiDecoder = new ViterbiDecoder();

		try ( BufferedReader in = new BufferedReader(new FileReader(inputFile));
			  PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));
			  PrintWriter log = new PrintWriter(new BufferedWriter(new FileWriter(logFile))) ) {

			Preface.readPreface(in);
			Preface.printPreface(out);

			Sentences sentences = new Sentences(in, null, parser.categories, MAX_WORDS);
			sentences.skip(fromSentence - 1);

			long TS_PARSING = Benchmark.getTime();
			for (int numSentence = fromSentence; numSentence <= toSentence && sentences.hasNext(); numSentence++) {
				System.out.println("Parsing sentence "+ numSentence);
				log.println("Parsing sentence "+ numSentence);

				parser.parseSentence(sentences.next(), log, betas);

				if (!parser.maxWordsExceeded && !parser.maxSuperCatsExceeded) {
					boolean success = parser.calcScores();

					if (success) {
						viterbiDecoder.decode(parser.chart, parser.sentence);
						viterbiDecoder.print(out, parser.categories.dependencyRelations, parser.sentence);

						parser.sentence.printC_line(out);
					} else {
						System.out.println("No root category.");
						log.println("No root category.");
					}
				}

				out.println();
			}
			long TE_PARSING = Benchmark.getTime();
			Benchmark.printTime("parsing", TS_PARSING, TE_PARSING);
		} catch (FileNotFoundException e) {
			System.err.println(e);
		} catch (IOException e) {
			System.err.println(e);
		}

		long TE_PROGRAM = Benchmark.getTime();
		Benchmark.printTime("program", TS_PROGRAM, TE_PROGRAM);
	}
}
