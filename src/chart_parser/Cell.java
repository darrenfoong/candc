package chart_parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.PriorityQueue;

import utils.Pair;
import cat_combination.SuperCategory;

public class Cell {
	private ArrayList<SuperCategory> superCategories;
	private ArrayList<LinkedList<SuperCategory>> preSuperCategories;

	private int clearCounter;

	public Cell() {
		this.superCategories = new ArrayList<SuperCategory>();
		this.preSuperCategories = new ArrayList<LinkedList<SuperCategory>>();
	}

	public ArrayList<SuperCategory> getSuperCategories() {
		return superCategories;
	}

	public ArrayList<LinkedList<SuperCategory>> getPreSuperCategories() {
		return preSuperCategories;
	}

	public void setClearCounter(int clearCounter) {
		this.clearCounter = clearCounter;
	}

	/**
	 * Decrements clearCounter for combine operations. When clearCounter reaches
	 * zero, superCategories will no longer be needed and can be cleared i.e.
	 * garbage collected.
	 */
	public void decrementClearCounter() {
		this.clearCounter--;
		if ( clearCounter == 0 ) {
			Chart.numSuperCategories -= superCategories.size();
			superCategories.clear();
		}
	}

	/**
	 * Filters ArrayList of superCategories to contain less or equal than beamSize
	 * superCategories, with scores more or equal than maxScore + beta.
	 * 
	 * The method sorts the list, finds the cutoff element (index), and removes 
	 * all elements from the cutoff element (inclusive) onwards.
	 * 
	 * @param beamSize beam size
	 * @param beta beta (negative)
	 */
	public void applyBeam(int beamSize, double beta) {
		if (!superCategories.isEmpty()) {
			Collections.sort(superCategories);
			// sorts in descending order of scores

			if ( beamSize > 0 ) {
				double maxScore = superCategories.get(0).score;
				double cutoff = maxScore + beta;

				int index = getIndexFast(superCategories, cutoff);
				int finalSize = Math.min(beamSize, index);

				int listSize = superCategories.size();
				if (finalSize < listSize) {
					superCategories.subList(finalSize, listSize).clear();
					superCategories.trimToSize();
					Chart.numSuperCategories -= (listSize - finalSize);
				}
			}
		}
	}

	/**
	 * Returns the index of the element of the list with the largest score strictly
	 * lesser than cutoff.
	 * 
	 * The method assumes that the list is not empty and is sorted in descending
	 * order.
	 * 
	 * The method returns list.size() + 1 if all elements are greater than the
	 * cutoff.
	 * 
	 * @param list list of supercategories
	 * @param cutoff cutoff
	 * @return index of element with largest score strictly lesser than cutoff
	 */
	private int getIndex(ArrayList<SuperCategory> list, double cutoff) {
		Iterator<SuperCategory> itr = superCategories.iterator();

		int index = 0;
		while (itr.hasNext()) {
			SuperCategory superCat = itr.next();
			if (superCat.score < cutoff) {
				break;
			}
			index++;
		}

		return index;
	}

	/**
	 * Returns the index of the element of the list with the largest score strictly
	 * lesser than cutoff.
	 * 
	 * The method assumes that the list is not empty and is sorted in descending
	 * order.
	 * 
	 * The method returns list.size() + 1 if all elements are greater than the
	 * cutoff.
	 * 
	 * @param list list of supercategories
	 * @param cutoff cutoff
	 * @return index of element with largest score strictly lesser than cutoff
	 */
	private int getIndexFast(ArrayList<SuperCategory> list, double cutoff) {
		int leftPtr = 0;
		int rightPtr = list.size() - 1;

		while (true) {
			if ( list.get(rightPtr).score >= cutoff ) {
				return rightPtr + 1;
			}

			if ( list.get(leftPtr).score < cutoff ) {
				return leftPtr;
			}

			int midPtr = (int) Math.floor((leftPtr + rightPtr)/2.0);

			if ( list.get(midPtr).score >= cutoff ) {
				leftPtr = midPtr + 1;
			} else {
				rightPtr = midPtr;
			}
		}
	}

	/**
	 * Merges the sorted lists of supercategories in preSuperCategories into a
	 * list of size beamSize. The method uses a priority queue to merge the
	 * lists, using the fact that they are sorted.
	 * 
	 * The method assumes superCategories is empty.
	 * 
	 * @param beamSize beamsize
	 */
	public void combinePreSuperCategories(int beamSize) {
		PriorityQueue<Pair<SuperCategory, Integer>> queue =
				new PriorityQueue<Pair<SuperCategory, Integer>>(beamSize,
						new Comparator<Pair<SuperCategory, Integer>>(){
					@Override
					public int compare(Pair<SuperCategory, Integer> p1, Pair<SuperCategory, Integer> p2){
						return p1.x.compareTo(p2.x);
					}});

		for ( int i = 0; i < preSuperCategories.size(); i++ ) {
			LinkedList<SuperCategory> preSuperCategory = preSuperCategories.get(i);
			if ( !preSuperCategory.isEmpty() ) {
				queue.add(new Pair<SuperCategory, Integer>(preSuperCategory.poll(), i));
			}
		}

		while ( superCategories.size() < beamSize ) {
			if ( !queue.isEmpty() ) {
				Pair<SuperCategory, Integer> top = queue.poll();
				superCategories.add(top.x);

				if ( !preSuperCategories.get(top.y).isEmpty() ) {
					queue.add(new Pair<SuperCategory, Integer>(preSuperCategories.get(top.y).poll(), top.y));
				}
			} else {
				break;
			}
		}

		preSuperCategories.clear();
	}

	public void add(SuperCategory superCat) {
		superCategories.add(superCat);
		Chart.numSuperCategories++;
	}

	public int size() {
		return superCategories.size();
	}

	public boolean isEmpty() {
		return superCategories.isEmpty();
	}

	public void clear() {
		superCategories.clear();
		preSuperCategories.clear();
		clearCounter = 0;
	}
}
