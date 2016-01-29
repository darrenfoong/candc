package training;

import io.Sentence;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;

import lexicon.Categories;
import lexicon.Category;
import model.Features;
import utils.IntWrapper;
import cat_combination.FilledDependency;
import cat_combination.SuperCategory;
import chart_parser.Cell;
import chart_parser.Chart;

public class PrintForest {
	ArrayList<Integer> featureIDs; // used to collect features before printing
	Features features; // the complete feature set

	HashSet<FilledDependency> goldDeps;
	Category rootCat;

	public PrintForest(Features features) {
		featureIDs = new ArrayList<Integer>();
		this.features = features;
		goldDeps = new HashSet<FilledDependency>();
	}

	public boolean print(PrintWriter out, Chart chart, Sentence sentence) {
		IntWrapper numDisjNodes = new IntWrapper(0);

		Cell root = chart.root();

		if (root.isEmpty()) {
			return false;
		}

		for (SuperCategory superCat : root.getSuperCategories()) {
			superCat.markActiveDisj(numDisjNodes);
		}

		// not sure what either of these are from C&C (id is the
		// sentence length for the perceptron trainer?!) so let's wait
		// to see how they're used
		//
		// out << id << ' ' << inside_outside.depscores.size() << '\n';

		out.println(numDisjNodes.value);

		int numWords = chart.numWords;
		int nodeID = 0;
		for (int position = 0; position < numWords; position++) {
			Cell cell = chart.cell(position, 1);
			for (SuperCategory canonical : cell.getSuperCategories()) {
				if (!canonical.isActive()) {
					continue;
				}

				canonical.marker = nodeID++;

				int numEquivNodes = canonical.numEquivNodes();
				out.println(canonical.marker);
				out.println(numEquivNodes);
				for (SuperCategory equiv = canonical; equiv != null; equiv = equiv.next) {
					if (equiv.unary()) {
						printUnaryFeatures(out, equiv, sentence, featureIDs);
					} else {
						printLeafFeatures(out, equiv, sentence, featureIDs);
					}
				}
			}
		}

		for (int j = 2; j <= numWords; j++) {
			for (int i = j - 2; i >= 0; i--) {
				Cell cell = chart.cell(i, j - i);
				if (i == 0 && j == numWords)
				{
					break; // root is treated separately below
				}

				for (SuperCategory canonical : cell.getSuperCategories()) {
					if (!canonical.isActive()) {
						continue;
					}

					canonical.marker = nodeID++;

					int numEquivNodes = canonical.numEquivNodes();
					out.println(canonical.marker);
					out.println(numEquivNodes);

					for (SuperCategory equiv = canonical; equiv != null; equiv = equiv.next) {
						if (equiv.unary()) {
							printUnaryFeatures(out, equiv, sentence, featureIDs);
						} else {
							printBinaryFeatures(out, equiv, sentence,
									featureIDs);
						}
					}
				}
			}
		}

		for (SuperCategory canonical : root.getSuperCategories()) {
			if (!canonical.isActive()) {
				continue;
			}

			canonical.marker = nodeID++;

			int numEquivNodes = canonical.numEquivNodes();
			out.println(canonical.marker);
			out.println(numEquivNodes);

			// assumes unary rules are never applied at the root
			for (SuperCategory equiv = canonical; equiv != null; equiv = equiv.next) {
				printRootFeatures(out, equiv, sentence, featureIDs);
			}
		}
		out.println();

		return true;
	}

	private void printLeafFeatures(PrintWriter out, SuperCategory superCat,
			Sentence sentence, ArrayList<Integer> featureIDs) {
		out.print("0 ");
		out.print(superCat.inside + " "); // log_prob of the lexical category
		// stored here
		// (as well as on score, but that's
		// already been used by the decoder)
		if (superCat.goldMarker == 1) {
			out.print("1 ");
		}
		else {
			out.print("0 "); // the goldMarker could be 0 or -1 here
		}

		featureIDs.clear();
		features.collectLeafFeatures(superCat, sentence, featureIDs);
		printFeatureIDs(out, featureIDs);
	}

	private void printUnaryFeatures(PrintWriter out, SuperCategory superCat,
			Sentence sentence, ArrayList<Integer> featureIDs) {
		out.print("1 " + superCat.leftChild.marker + " ");
		if (superCat.goldMarker == 1) {
			out.print("1 ");
		}
		else {
			out.print("0 "); // the goldMarker could be 0 or -1 here
		}

		featureIDs.clear();
		features.collectUnaryFeatures(superCat, sentence, featureIDs);
		printFeatureIDs(out, featureIDs);
	}

	private void printBinaryFeatures(PrintWriter out, SuperCategory superCat,
			Sentence sentence, ArrayList<Integer> featureIDs) {
		out.print("2 " + superCat.leftChild.marker + " "
				+ superCat.rightChild.marker + " ");
		// countGoldDeps(superCat) + " ");
		if (superCat.goldMarker == 1) {
			out.print("1 ");
		}
		else {
			out.print("0 "); // the goldMarker could be 0 or -1 here
		}

		featureIDs.clear();
		features.collectBinaryFeatures(superCat, sentence, featureIDs);
		printFeatureIDs(out, featureIDs);
	}

	private void printRootFeatures(PrintWriter out, SuperCategory superCat,
			Sentence sentence, ArrayList<Integer> featureIDs) {
		out.print("3 " + superCat.leftChild.marker + " "
				+ superCat.rightChild.marker + " ");
		// countGoldDeps(superCat) + " ");
		if (superCat.goldMarker == 1) {
			out.print("1 ");
		}
		else {
			out.print("0 "); // the goldMarker could be 0 or -1 here
		}

		featureIDs.clear();
		features.collectRootFeatures(superCat, sentence, featureIDs);
		printFeatureIDs(out, featureIDs);
	}

	private void printFeatureIDs(PrintWriter out, ArrayList<Integer> featureIDs) {
		if (featureIDs.isEmpty()) {
			out.println("0");
		} else {
			out.print(featureIDs.size());
			for (int i = 0; i < featureIDs.size(); i++) {
				out.print(" " + featureIDs.get(i));
			}
			out.println();
		}
	}

	private int countGoldDeps(SuperCategory superCat) {
		int score = 0;
		for ( FilledDependency filled : superCat.filledDeps ) {
			if (goldDeps.contains(filled)) {
				score++;
			}
			else {
				return -1; // at least one incorrect dep triggers -1
			}
		}
		return score;
	}

	public boolean readGoldDeps(BufferedReader in, Categories categories) {
		try {
			goldDeps.clear();

			String line = in.readLine();
			if (line == null) {
				return false;
			} else if (line.isEmpty()) {
				return true;
			}

			// read the two rootCats, the first of which is the automatically
			// generated "gold"
			String[] rootCats = line.split("\\s");
			if (rootCats.length != 2) {
				throw new Error(
						"should be 2 root cats before each set of gold deps!");
			}
			rootCat = categories.canonize(rootCats[0]);

			line = in.readLine();
			while (true) {
				if (line.isEmpty()) {
					return true;
				}

				String[] tokens = line.split("\\s");
				if (tokens.length != 4) {
					throw new Error("gold deps should always have 4 fields!");
				}

				short headIndex = Short.parseShort(tokens[0]);
				short relID = Short.parseShort(tokens[1]);
				short fillerIndex = Short.parseShort(tokens[2]);
				short unaryRuleID = Short.parseShort(tokens[3]);
				short lrange = (short) (0); // assuming lrange is not part of
				// the data

				FilledDependency dep = new FilledDependency(relID, headIndex,
						fillerIndex, unaryRuleID, lrange);
				goldDeps.add(dep);

				line = in.readLine();
			}
		} catch (IOException e) {
			System.err.println(e);
			return false;
		}
	}

}
