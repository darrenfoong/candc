import java.io.IOException;

import lexicon.Categories;
import model.FeaturesHashCode;
import model.Weights;

public class HashCodeDist {
	public static void main(String[] args) {
		String grammarDir = "data/baseline_expts/grammar";
		String featuresFile = "data/baseline_expts/working/lexicon/wsj02-21.feats.1-22";

		String weightsFile = args[0];

		Categories categories = new Categories(grammarDir, false);
		Weights weights = new Weights();

		try {
			FeaturesHashCode features = new FeaturesHashCode(featuresFile, weightsFile, weights, categories);
			features.countAllHashes();
			features.printAllHashCounts();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
