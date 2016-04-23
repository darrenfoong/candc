package chart_parser;

import java.io.IOException;
import java.util.ArrayList;

import cat_combination.RuleInstancesParams;
import cat_combination.SuperCategory;
import model.Lexicon;
import uk.ac.cam.cl.depnn.io.Feature;
import uk.ac.cam.cl.depnn.nn.NeuralNetwork;

public class ChartParserBeamNN extends ChartParserBeam {
	private NeuralNetwork<Feature> nn;

	public ChartParserBeamNN(String grammarDir,
			boolean altMarkedup,
			boolean eisnerNormalForm,
			int MAX_WORDS,
			int MAX_SUPERCATS,
			RuleInstancesParams ruleInstancesParams,
			Lexicon lexicon,
			String modelDir,
			boolean cubePruning,
			double[] betas,
			int beamSize,
			double beta) throws IOException {
		super(grammarDir, altMarkedup, eisnerNormalForm, MAX_WORDS, MAX_SUPERCATS, ruleInstancesParams, lexicon, null, null, false, false, cubePruning, betas, beamSize, beta);

		nn = new NeuralNetwork<Feature>(modelDir, new Feature());
	}

	@Override
	public void calcScore(SuperCategory superCat, boolean atRoot) {
		SuperCategory leftChild = superCat.leftChild;
		SuperCategory rightChild = superCat.rightChild;

		ArrayList<Feature> features = getFeature(sentence, superCat);

		for ( Feature feature : features ) {
			double score = nn.predictSoft(feature);
			superCat.score += score;
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
