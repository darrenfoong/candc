package chart_parser;

import java.io.IOException;
import java.util.ArrayList;

import org.nd4j.linalg.api.ndarray.INDArray;

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
		super(grammarDir, altMarkedup, eisnerNormalForm, MAX_WORDS, MAX_SUPERCATS,
				ruleInstancesParams, lexicon, null, null, false, false, cubePruning,
				betas, beamSize, beta);

		nn = new NeuralNetwork<Feature>(modelDir, new Feature());
		chart.weights.setLogP(1.0);
	}

	@Override
	public void calcScore(SuperCategory superCat, boolean atRoot) {
		SuperCategory leftChild = superCat.leftChild;
		SuperCategory rightChild = superCat.rightChild;

		if (leftChild != null) {
			if (rightChild != null) {
				superCat.score += rightChild.score;
			} else {
				superCat.score += leftChild.score;
			}
		}
	}

	@Override
	protected void calcSpanNNScore(int span) {
		int numWords = sentence.words.size();

		ArrayList<Integer> indices = new ArrayList<Integer>();
		ArrayList<Feature> features = new ArrayList<Feature>();

		indices.add(0);

		for ( int pos = 0; pos <= numWords - pos; pos++ ) {
			Cell cell = chart.cell(pos, span);

			for ( SuperCategory superCat : cell.getSuperCategories() ) {
				ArrayList<Feature> catFeatures = getFeature(sentence, superCat);
				features.addAll(catFeatures);
				indices.add(features.size());
			}
		}

		if ( features.isEmpty() ) {
			return;
		}

		double[] predictions = nn.predictSoft(features);

		for ( int pos = 0; pos <= numWords - pos; pos++ ) {
			Cell cell = chart.cell(pos, span);

			for ( int i = 0; i < cell.getSuperCategories().size(); i++ ) {
				SuperCategory superCat = cell.getSuperCategories().get(i);
				double nnScore = 0.0;

				for ( int j = indices.get(i); j < indices.get(i+1); j++ ) {
					nnScore += predictions[j];
					nnScore += Math.random();
				}

				superCat.score += weights.getDepNN() * nnScore;
			}
		}
	}

	@Override
	protected void loadEmbeddings() {
		wordEmbeddingsList = new ArrayList<INDArray>();
		posEmbeddingsList = new ArrayList<INDArray>();

		for ( int i = 0; i < sentence.words.size(); i++ ) {
			wordEmbeddingsList.add(nn.getWordVector(sentence.words.get(i)));
			posEmbeddingsList.add(nn.posEmbeddings.getINDArray(sentence.words.get(i)));
		}
	}
}
