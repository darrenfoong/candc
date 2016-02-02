package cat_combination;

import java.util.Arrays;

public class Variable {
	/*
	 * A Variable object is essentially an array which can store words an array
	 * is needed to deal with coordination cases, eg "apples and pears"
	 * 
	 * There are 2 cases: - unfilled, which means the variable is yet to be
	 * unified with a constant (word) - filled, which means it has unified with
	 * a word (represented as a position index)
	 * 
	 * There are also 2 subcases: - no chain, which means we haven't collected
	 * any constants - chain, which means we have
	 * 
	 * chaining occurs when we have eg "and pears", so that "pears" is stored on
	 * the array but we're still waiting to unify with the other conjunct long
	 * chains can occur with eg "and pears and bananas and coconuts"
	 */

	final static int NUM_FILLERS = 8; // maximum number of words allowed in
	// chains
	final static short UNFILLED = 0; // assumes sentence positions start at 1
	final static public short SENTINEL = 32767; // assumes sentence positions
	// never get this big

	public short[] fillers;

	// can we create a default constructor called by all these which
	// creates memory for the fillers array?

	/*
	 * unfilled variable the first position in the array indicates whether the
	 * variable is filled
	 */
	public Variable() {
		fillers = new short[NUM_FILLERS];
		Arrays.fill(fillers, 1, NUM_FILLERS - 1, SENTINEL);
		fillers[0] = UNFILLED;
	}

	/*
	 * filled variable the first position in the array stores the word's
	 * position in the sentence
	 */
	public Variable(short position) {
		fillers = new short[NUM_FILLERS];
		Arrays.fill(fillers, 1, NUM_FILLERS - 1, SENTINEL);
		fillers[0] = position;
	}

	/*
	 * "if" condition allows the other Variable to be filled, which creates the
	 * unfilled chained case
	 */
	public Variable(Variable other) {
		fillers = new short[NUM_FILLERS];
		if (other.isFilled()) {
			fillers[0] = UNFILLED;
			System.arraycopy(other.fillers, 0, fillers, 1, NUM_FILLERS - 1);
		} else {
			Arrays.fill(fillers, 1, NUM_FILLERS - 1, SENTINEL);
			fillers[0] = UNFILLED;
		}
	}

	/*
	 * unifying two variables to construct a new one
	 */
	public Variable(Variable v1, Variable v2) {
		fillers = new short[NUM_FILLERS];

		int v1Index = 0;
		int v2Index = 0;
		if (v1.fillers[0] == UNFILLED) {
			v1Index++;
		} else if (v2.fillers[0] == UNFILLED)
		{
			v2Index++;
			// } else
			// throw new
			// Error("trying to unify two constant variables and create a new one!");
			// also been checked earlier using unify(SuperCategory,
			// SuperCategory) method so Error should never get thrown (but
			// note this also gets called directly by the Apposition
			// SuperCategory constructor - in which case this Error bites)
		}

		/*
		 * shifts along both arrays until this array is full; "zero" elements
		 * are SENTINELS which have the *maximum* value
		 */
		for (int i = 0; i < fillers.length; i++) {
			if (v1.fillers[v1Index] < v2.fillers[v2Index]) {
				fillers[i] = v1.fillers[v1Index];
				v1Index++;
			} else {
				fillers[i] = v2.fillers[v2Index];
				v2Index++;
			}
		}
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(fillers);
	}

	@Override
	public boolean equals(Object other) {
		if ( other == null || getClass() != other.getClass() ) {
			return false;
		}

		Variable cother = (Variable) other;

		return Arrays.equals(this.fillers, cother.fillers);
	}

	public boolean isUnfilled() {
		return fillers[0] == UNFILLED;
	}

	public boolean isFilled() {
		return fillers[0] > UNFILLED;
	}

	public boolean isSingleConst() {
		return fillers[1] == SENTINEL && isFilled();
	}

	public Boolean isSet() {
		return fillers[1] != SENTINEL;
	}

	public Boolean isUnfilledNoChain() {
		return isUnfilled() & !isSet();
	}

	short getFiller() {
		return fillers[0];
	}

	short countFillers() {
		short count = 0;
		for (int i = 0; i < fillers.length; i++) {
			if (fillers[i] == SENTINEL) {
				break;
			} else {
				count++;
			}
		}
		return count;
	}
}
