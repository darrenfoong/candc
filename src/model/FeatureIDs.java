package model;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class FeatureIDs<T extends Feature<T>> {
	private HashMap<T,Integer> featureIDs;

	public FeatureIDs() {
		this.featureIDs = new HashMap<T,Integer>(1000000);
	}

	public HashMap<T,Integer> getFeatureIDs() {
		return featureIDs;
	}

	public void addFeature(T feature, int ID) {
		featureIDs.put(feature, ID);
	}

	public void print(PrintWriter out) {
		for (Map.Entry<T, Integer> entry : featureIDs.entrySet()) {
			entry.getKey().print(out);
			out.print(" ");
			out.print(entry.getValue());
			out.println();
		}
	}

	public void printWeights(Weights weights, PrintWriter out) {
		for (Map.Entry<T, Integer> entry : featureIDs.entrySet()) {
			int index = entry.getValue();
			if ( index == -2 ) {
				out.print("0.0");
			} else {
				out.print(weights.getWeight(index));
			}

			out.println();
		}
	}
}
