package chart_parser;

import java.io.IOException;
import java.util.ArrayList;

import cat_combination.RuleInstancesParams;
import cat_combination.SuperCategory;
import model.Lexicon;
import uk.ac.cam.cl.depnn.DependencyNeuralNetwork;

public class ChartParserBeamNN extends ChartParserBeam {
	private DependencyNeuralNetwork nn;

	public ChartParserBeamNN(String grammarDir,
			boolean altMarkedup,
			boolean eisnerNormalForm,
			int MAX_WORDS,
			int MAX_SUPERCATS,
			RuleInstancesParams ruleInstancesParams,
			Lexicon lexicon,
			String modelDir,
			boolean cubePruning,
			int beamSize,
			double beta) throws IOException {
		super(grammarDir, altMarkedup, eisnerNormalForm, MAX_WORDS, MAX_SUPERCATS, ruleInstancesParams, lexicon, null, null, false, false, cubePruning, beamSize, beta);

		nn = new DependencyNeuralNetwork(modelDir);
	}

	@Override
	public void calcScore(SuperCategory superCat, boolean atRoot) {
		SuperCategory leftChild = superCat.leftChild;
		SuperCategory rightChild = superCat.rightChild;

		ArrayList<ArrayList<String>> features = getFeature(sentence, superCat);

		for ( ArrayList<String> feature : features ) {
			// double score = nn.predict(feature)
			// superCat.score += score;
		}

		if (leftChild != null) {
			if (rightChild != null) {
				superCat.score += rightChild.score;
			} else {
				superCat.score += leftChild.score;
			}
		}
	}
}
