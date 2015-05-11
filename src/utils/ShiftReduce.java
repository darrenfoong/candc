package utils;

import io.Sentence;

import java.util.ArrayList;
import java.util.LinkedList;

import lexicon.Categories;
import cat_combination.Rules;
import cat_combination.SuperCategory;
import chart_parser.CellTrainBeam;
import chart_parser.Chart;

public class ShiftReduce {
	private LinkedList<ShiftReduceNode> nodeStack;
	private Chart chart;
	private Rules rules;
	private Sentence sentence;
	private Categories categories;
	private int numWords = 0;

	public ShiftReduce(Chart chart, Rules rules, Sentence sentence, Categories categories) {
		this.nodeStack = new LinkedList<ShiftReduceNode>();
		this.chart = chart;
		this.rules = rules;
		this.sentence = sentence;
		this.categories = categories;
	}

	/**
	 * Shifts in a new node.
	 * 
	 * @param catString category string of node
	 * @param arity arity of node
	 */
	public void shift(String catString, int arity) {
		ShiftReduceNode node = new ShiftReduceNode(catString, arity);
		nodeStack.push(node);
	}

	/**
	 * Reduces nodes on stack while possible.
	 * 
	 * @return true if reduce succeeds, false if reduce fails.
	 */
	public boolean reduce() {
		// unary case
		if ( nodeStack.size() >= 2
				&& nodeStack.get(0).arity == 0 && nodeStack.get(1).arity == 1 ) {
			ShiftReduceNode leftNode = nodeStack.pop();
			ShiftReduceNode parentNode = nodeStack.getFirst();
			parentNode.arity = 0;
			parentNode.position = leftNode.position;
			parentNode.span = leftNode.span;

			ArrayList<SuperCategory> reduceResults = new ArrayList<SuperCategory>();
			ArrayList<SuperCategory> reduceInput = new ArrayList<SuperCategory>();
			reduceInput.add(leftNode.superCat);

			rules.typeChange(reduceInput, reduceResults);
			rules.typeRaise(reduceInput, reduceResults);

			for ( SuperCategory resultSuperCat : reduceResults ) {
				String resultString = resultSuperCat.cat.toStringNoOuterBrackets();
				// resultString = resultString.replaceAll("S\\[X\\]", "S");
				// resultString = resultString.replaceAll("\\[nb\\]", "");
				if ( resultString.equals(parentNode.catString)) {
					parentNode.superCat = resultSuperCat;
				}
			}

			if ( parentNode.superCat == null ) {
				System.out.println("Unary: failed to find match; parent catString is " + parentNode.catString);
				return false;
			}

			CellTrainBeam cell = (CellTrainBeam) chart.cell(parentNode.position, parentNode.span);
			cell.goldSuperCat = parentNode.superCat;

			// recurse
			return reduce();
		}

		// binary case
		if ( nodeStack.size() >= 3
				&& nodeStack.get(0).arity == 0 && nodeStack.get(1).arity == 0 && nodeStack.get(2).arity == 2 ) {
			ShiftReduceNode rightNode = nodeStack.pop();
			ShiftReduceNode leftNode = nodeStack.pop();
			ShiftReduceNode parentNode = nodeStack.getFirst();
			parentNode.arity = 0;
			parentNode.position = leftNode.position;
			parentNode.span = leftNode.span + rightNode.span;

			ArrayList<SuperCategory> reduceResults = new ArrayList<SuperCategory>();
			rules.combine(leftNode.superCat, rightNode.superCat, reduceResults, false, sentence);

			for ( SuperCategory resultSuperCat : reduceResults ) {
				String resultString = resultSuperCat.cat.toStringNoOuterBrackets();
				// resultString = resultString.replaceAll("S\\[X\\]", "S");
				// resultString = resultString.replaceAll("\\[nb\\]", "");
				if ( resultString.equals(parentNode.catString)) {
					parentNode.superCat = resultSuperCat;
				}
			}

			if ( parentNode.superCat == null && !reduceResults.isEmpty() ) {
				parentNode.superCat = reduceResults.get(0);
			}

			if ( parentNode.superCat == null ) {
				System.out.println("Binary: failed to find match; parent catString is " + parentNode.catString);
				return false;
			}

			CellTrainBeam cell = (CellTrainBeam) chart.cell(parentNode.position, parentNode.span);
			cell.goldSuperCat = parentNode.superCat;

			// recurse
			return reduce();
		}

		return true;
	}

	private class ShiftReduceNode {
		private String catString;
		private SuperCategory superCat;
		private int arity;
		private int position;
		private int span;

		public ShiftReduceNode(String catString, int arity) {
			this.catString = catString;
			this.arity = arity;

			// node is a leaf
			if ( arity == 0 ) {
				// create new SuperCategory for actual SuperCategory combining during reduce()
				catString = catString.replaceAll("S\\[X\\]", "S");
				this.superCat = new SuperCategory((short) (numWords+1), categories.getCategory(catString), (short) (0));

				this.position = numWords;
				numWords++;
				this.span = 1;

				// place SuperCategory in correct cell in chart
				CellTrainBeam cell = (CellTrainBeam) chart.cell(position, span);
				cell.goldSuperCat = superCat;
			} else {
				this.superCat = null;
			}
		}
	}
}
