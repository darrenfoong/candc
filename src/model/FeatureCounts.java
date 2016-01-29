package model;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import lexicon.Categories;

public class FeatureCounts<T extends Feature<T>> {
	private HashMap<T, Integer> featureCounts;
	private Categories categories;

	public FeatureCounts(Categories categories) {
		this.featureCounts = new HashMap<T, Integer>();
		this.categories = categories;
	}

	public HashMap<T, Integer> getFeatureCounts() {
		return featureCounts;
	}

	public void addCount(T feature) {
		Integer count = featureCounts.get(feature);

		if (count != null) {
			featureCounts.put(feature, count+1);
		} else {
			feature = feature.canonize(categories);
			featureCounts.put(feature, 1);
		}
	}

	public void print(PrintWriter out) {
		for (Map.Entry<T, Integer> entry : featureCounts.entrySet()) {
			out.println(entry.getKey() + " " + entry.getValue());
		}
	}
}
