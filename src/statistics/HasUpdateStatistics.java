package statistics;

import java.util.ArrayList;

public class HasUpdateStatistics {
	private ArrayList<Boolean> hasUpdates;

	public HasUpdateStatistics() {
		hasUpdates = new ArrayList<Boolean>();
	}

	public int getSize() {
		return hasUpdates.size();
	}

	public void addData(boolean hasUpdate) {
		hasUpdates.add(hasUpdate);
	}

	public int calcHasUpdates() {
		int numSentences = hasUpdates.size();
		int sum = 0;

		for ( int i = 0; i < numSentences; i++ ) {
			if ( hasUpdates.get(i) ) {
				sum++;
			}
		}

		return sum;
	}

	public void clear() {
		hasUpdates.clear();
	}
}
