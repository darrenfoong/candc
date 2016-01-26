package cat_combination;

import io.Sentence;

import java.util.ArrayList;

import lexicon.Categories;
import lexicon.Category;
import lexicon.VarID;

public class Combinators {
	boolean eisnerNormalForm;
	Unify unification;
	RuleInstancesParams ruleInstancesParams; // settings determine which seen_rule instances are used
	RuleInstances coordinationRuleInstances;
	RuleInstances backwardCompRuleInstances;

	public Combinators(boolean eisnerNormalForm, Categories categories, RuleInstancesParams ruleInstancesParams) {
		this.ruleInstancesParams = ruleInstancesParams;
		this.eisnerNormalForm = eisnerNormalForm;
		unification = new Unify();
		if (ruleInstancesParams.getConj()) {
			coordinationRuleInstances = new RuleInstances(ruleInstancesParams.getDirectory() + "/conj_rule_instances", categories);
		}
		if (ruleInstancesParams.getBackwardComp()) {
			backwardCompRuleInstances = new RuleInstances(ruleInstancesParams.getDirectory() + "/backward_comp_rule_instances", categories);
		}
	}

	private boolean application(SuperCategory leftSuperCat, SuperCategory rightSuperCat, boolean FWD_APP, ArrayList<SuperCategory> results, Sentence sentence) {
		/*
		 * note that unification doesn't know anything about Variable objects,
		 * only the VarIDs. Variable objects for the result category get created
		 * in SuperCategory constructors
		 */

		// unification.unify builds translation tables of variables:
		if (FWD_APP) {
			if (!unification.unify(leftSuperCat.cat.argument, rightSuperCat.cat)) {
				return false;
			}
		} else {
			if (!unification.unify(leftSuperCat.cat, rightSuperCat.cat.argument)) {
				return false;
			}
		}

		// check to make sure we haven't unified two constant variables:
		if (!unification.unify(leftSuperCat, rightSuperCat)) {
			return false;
		}

		/*
		 * unification.addVars[12] adds to the translation tables any variables
		 * in the result category of the functor
		 */
		if (FWD_APP) {
			unification.addVars1(leftSuperCat.cat.result);
		} else {
			unification.addVars2(rightSuperCat.cat.result);
		}

		/*
		 * reorderVariables orders the (translations of the) variables on the
		 * result of the functor category:
		 */
		if (FWD_APP) {
			leftSuperCat.cat.result.reorderVariables(unification.trans1, unification.seen, unification.order);
		} else {
			rightSuperCat.cat.result.reorderVariables(unification.trans2, unification.seen, unification.order);
		}

		unification.reorderVariables(leftSuperCat, rightSuperCat);

		Category resultCat;
		if (FWD_APP) {
			resultCat = Category.TransVariable(leftSuperCat.cat.result, unification.trans1, unification.feature);
		} else {
			resultCat = Category.TransVariable(rightSuperCat.cat.result, unification.trans2, unification.feature);
		}

		SuperCategory resultSuperCat;
		if (FWD_APP) {
			resultSuperCat = SuperCategory.BinaryCombinator(resultCat, SuperCategory.FWD_APP, leftSuperCat, rightSuperCat, unification, sentence);
		} else {
			resultSuperCat = SuperCategory.BinaryCombinator(resultCat, SuperCategory.BWD_APP, leftSuperCat, rightSuperCat, unification, sentence);
		}

		if (resultSuperCat != null) {
			results.add(resultSuperCat);
			if (FWD_APP) {
				Rules.forwardAppCount++;
			} else {
				Rules.backwardAppCount++;
			}
			return true;
		} else {
			return false;
		}
	}

	private boolean composition(SuperCategory leftSuperCat, SuperCategory rightSuperCat, short compositionType, ArrayList<SuperCategory> results, Sentence sentence) {
		/*
		 * the generalised composition rules get called here if the unify check
		 * fails
		 */
		// unify builds translation tables of variables:
		if (compositionType == SuperCategory.FWD_COMP) {
			if (!unification.unify(leftSuperCat.cat.argument, rightSuperCat.cat.result)) {
				return generalisedForwardComposition(leftSuperCat, rightSuperCat, results, sentence);
			}
		} else {
			// applies to both BWD_COMP and BWD_CROSS
			if (!unification.unify(leftSuperCat.cat.result, rightSuperCat.cat.argument)) {
				if (compositionType == SuperCategory.BWD_CROSS) {
					return generalisedBackwardCrossComposition(leftSuperCat, rightSuperCat, results, sentence);
				} else {
					return generalisedBackwardComposition(leftSuperCat, rightSuperCat, results, sentence);
				}
			}
		}

		// checks to make sure we haven't unified two constant variables:
		if (!unification.unify(leftSuperCat, rightSuperCat)) {
			return false;
		}

		/*
		 * unification.addVars[12] adds to the translation tables any variables
		 * in those parts of the categories not taking part in the unification
		 */
		if (compositionType == SuperCategory.FWD_COMP) {
			unification.addVars1(leftSuperCat.cat.result);
			unification.addVars2(rightSuperCat.cat.argument);
		} else {
			// applies to both BWD_COMP and BWD_CROSS
			unification.addVars2(rightSuperCat.cat.result);
			unification.addVars1(leftSuperCat.cat.argument);
		}

		/*
		 * this adds the "outer" variable on the left cat to the translation
		 * tables (if not there already), and this will get used as the variable
		 * on the whole category resulting from the composition (it's an
		 * arbitrary decision to take the "outer" variable from the left or the
		 * right)
		 */
		byte var1 = leftSuperCat.cat.var;
		unification.addVar1(var1);

		/*
		 * reorder the variables on the categories which will make up the
		 * resulting category, filling in the seenVariables table
		 */
		if (compositionType == SuperCategory.FWD_COMP) {
			leftSuperCat.cat.result.reorderVariables(unification.trans1, unification.seen, unification.order);
			rightSuperCat.cat.argument.reorderVariables(unification.trans2, unification.seen, unification.order);
		} else {
			// applies to both BWD_COMP and BWD_CROSS
			rightSuperCat.cat.result.reorderVariables(unification.trans2, unification.seen, unification.order);
			leftSuperCat.cat.argument.reorderVariables(unification.trans1, unification.seen, unification.order);
		}
		/*
		 * check to see if we've already seen the (translation of the) "outer"
		 * variable in the reordering process; if not give (its translation) the
		 * next available variable
		 */
		if (var1 != VarID.NONE && unification.seen[unification.trans1[var1]] != VarID.NONE) {
			unification.seen[unification.trans1[var1]] = ++unification.order.value;
		}

		if (unification.order.value >= VarID.NUM_VARS) {
			throw new Error("run out variables in forward composition");
		}

		unification.reorderVariables(leftSuperCat, rightSuperCat);

		// leftCat and rightCat not the best names here
		Category leftCat, rightCat;
		if (compositionType == SuperCategory.FWD_COMP) {
			leftCat = Category.TransVariable(leftSuperCat.cat.result, unification.trans1, unification.feature);
			rightCat = Category.TransVariable(rightSuperCat.cat.argument, unification.trans2, unification.feature);
		} else {
			// applies to both BWD_COMP and BWD_CROSS
			rightCat = Category.TransVariable(leftSuperCat.cat.argument, unification.trans1, unification.feature);
			leftCat = Category.TransVariable(rightSuperCat.cat.result, unification.trans2, unification.feature);
		}

		var1 = unification.trans1[var1]; // new variable for the "outer"
		// variable on the new category

		Category resultCat;
		SuperCategory resultSuperCat;
		byte slash;
		if (compositionType == SuperCategory.BWD_COMP) {
			slash = Category.BWD_SLASH;
		}
		else {
			slash = Category.FWD_SLASH; // applies to both forward and
			// backward-cross composition
		}

		resultCat = new Category(leftCat, slash, rightCat, var1, (short) (0), (short) (0));
		resultSuperCat = SuperCategory.BinaryCombinator(resultCat, compositionType, leftSuperCat, rightSuperCat, unification, sentence);
		if (resultSuperCat != null) {
			results.add(resultSuperCat);
			if (compositionType == SuperCategory.FWD_COMP) {
				Rules.forwardCompCount++;
			} else if (compositionType == SuperCategory.BWD_COMP) {
				Rules.backwardCompCount++;
			} else if (compositionType == SuperCategory.BWD_CROSS) {
				Rules.backwardCrossCount++;
			}
			return true;
		} else {
			return false;
		}
	}

	/*
	 * B2 indicates the depth to which we recurse in the verb e.g. (S\NP)/(S\NP)
	 * ((S\NP)/NP)/NP or ((S\NP)/NP)/NP (S\NP)\(S\NP)
	 * 
	 * applies to forward and backward cross, but not backward, comp
	 */
	private boolean generalisedCompositionB2(SuperCategory leftSuperCat, SuperCategory rightSuperCat, short compositionType, ArrayList<SuperCategory> results, Sentence sentence) {
		if (compositionType == SuperCategory.FWD_COMP) {
			if (!unification.unify(leftSuperCat.cat.argument, rightSuperCat.cat.result.result)) {
				return false;
			}
		} else {
			// BWD_CROSS
			if (!unification.unify(leftSuperCat.cat.result.result, rightSuperCat.cat.argument)) {
				return false;
			}
		}
		if (!unification.unify(leftSuperCat, rightSuperCat)) {
			return false;
		}

		if (compositionType == SuperCategory.FWD_COMP) {
			unification.addVars1(leftSuperCat.cat.result);
		} else {
			// BWD_CROSS case
			unification.addVars2(rightSuperCat.cat.result);
		}

		/*
		 * next two lines differ to the (non-recursive) composition case, by
		 * recursing one level into the result of the verb category; addVars2 is
		 * called on the argument of the result (which could be complex) and
		 * addVar2 on the result itself
		 */
		if (compositionType == SuperCategory.FWD_COMP) {
			unification.addVars2(rightSuperCat.cat.result.argument);
			unification.addVar2(rightSuperCat.cat.result.var);
		} else {
			// BWD_CROSS case
			unification.addVars1(leftSuperCat.cat.result.argument);
			unification.addVar1(leftSuperCat.cat.result.var);
		}
		if (compositionType == SuperCategory.FWD_COMP) {
			unification.addVars2(rightSuperCat.cat.argument);
		} else {
			// BWD_CROSS case
			unification.addVars1(leftSuperCat.cat.argument);
		}

		byte var1 = leftSuperCat.cat.var;
		unification.addVar1(var1);

		if (compositionType == SuperCategory.FWD_COMP) {
			leftSuperCat.cat.result.reorderVariables(unification.trans1, unification.seen, unification.order);
		} else {
			// BWD_CROSS case
			rightSuperCat.cat.result.reorderVariables(unification.trans2, unification.seen, unification.order);
		}

		/*
		 * new for the generalised case, recursing one level into the result of
		 * the verb category:
		 */
		Category innerCat;
		if (compositionType == SuperCategory.FWD_COMP) {
			innerCat = rightSuperCat.cat.result;
		} else {
			// BWD_CROSS case
			innerCat = leftSuperCat.cat.result;
		}

		byte var = innerCat.var;
		if (compositionType == SuperCategory.FWD_COMP) {
			innerCat.argument.reorderVariables(unification.trans2, unification.seen, unification.order);
		} else {
			// BWD_CROSS case
			innerCat.argument.reorderVariables(unification.trans1, unification.seen, unification.order);
		}

		if (compositionType == SuperCategory.FWD_COMP) {
			if (var != VarID.NONE && unification.seen[unification.trans2[var]] != VarID.NONE) {
				unification.seen[unification.trans2[var]] = ++unification.order.value;
			}
		} else {
			// BWD_CROSS case
			if (var != VarID.NONE && unification.seen[unification.trans1[var]] != VarID.NONE) {
				unification.seen[unification.trans1[var]] = ++unification.order.value;
			}
		}

		if (compositionType == SuperCategory.FWD_COMP) {
			rightSuperCat.cat.argument.reorderVariables(unification.trans2, unification.seen, unification.order);
		} else {
			// BWD_CROSS case
			leftSuperCat.cat.argument.reorderVariables(unification.trans1, unification.seen, unification.order);
		}

		if (var1 != VarID.NONE && unification.seen[unification.trans1[var1]] != VarID.NONE) {
			unification.seen[unification.trans1[var1]] = ++unification.order.value;
		}

		if (unification.order.value >= VarID.NUM_VARS) {
			throw new Error("run out variables in forward composition");
		}

		unification.reorderVariables(leftSuperCat, rightSuperCat);

		Category innerResult, innerArgument, newResult, newArgument, newCategory;
		SuperCategory resultSuperCat;
		byte innerVar;
		if (compositionType == SuperCategory.FWD_COMP) {
			innerResult = Category.TransVariable(leftSuperCat.cat.result, unification.trans1, unification.feature);
			innerArgument = Category.TransVariable(rightSuperCat.cat.result.argument, unification.trans2, unification.feature);
			innerVar = unification.trans2[rightSuperCat.cat.result.var];
			newResult = new Category(innerResult, rightSuperCat.cat.result.slash, innerArgument, innerVar, rightSuperCat.cat.result.relID, rightSuperCat.cat.result.lrange);
			newArgument = Category.TransVariable(rightSuperCat.cat.argument, unification.trans2, unification.feature);
		} else {
			// BWD_CROSS case
			innerResult = Category.TransVariable(rightSuperCat.cat.result, unification.trans2, unification.feature);
			innerArgument = Category.TransVariable(leftSuperCat.cat.result.argument, unification.trans1, unification.feature);
			innerVar = unification.trans1[leftSuperCat.cat.result.var];
			newResult = new Category(innerResult, leftSuperCat.cat.result.slash, innerArgument, innerVar, leftSuperCat.cat.result.relID, leftSuperCat.cat.result.lrange);
			newArgument = Category.TransVariable(leftSuperCat.cat.argument, unification.trans1, unification.feature);
		}
		var1 = unification.trans1[var1]; // new variable for the "outer"
		// variable on the new category

		if (compositionType == SuperCategory.FWD_COMP) {
			newCategory = new Category(newResult, rightSuperCat.cat.slash, newArgument, var1, rightSuperCat.cat.relID, rightSuperCat.cat.lrange);
		} else {
			// BWD_CROSS case
			newCategory = new Category(newResult, leftSuperCat.cat.slash, newArgument, var1, leftSuperCat.cat.relID, rightSuperCat.cat.lrange);
		}

		if (compositionType == SuperCategory.FWD_COMP) {
			resultSuperCat = SuperCategory.BinaryCombinator(newCategory, (short) (SuperCategory.FWD_COMP | SuperCategory.RECURSIVE), leftSuperCat, rightSuperCat, unification, sentence);
		} else {
			// BWD_CROSS case
			resultSuperCat = SuperCategory.BinaryCombinator(newCategory, (short) (SuperCategory.BWD_CROSS | SuperCategory.RECURSIVE), leftSuperCat, rightSuperCat, unification, sentence);
		}

		if (resultSuperCat != null) {
			results.add(resultSuperCat);
			if (compositionType == SuperCategory.FWD_COMP) {
				Rules.genForwardCompCount++;
			} else if (compositionType == SuperCategory.BWD_CROSS) {
				Rules.genBackwardCrossCount++;
			}
			return true;
		} else {
			return false;
		}
	}

	/*
	 * B3 indicates the depth to which we recurse in the verb e.g. (S\NP)/(S\NP)
	 * (((S\NP)/PP)/PP)/NP or (((S\NP)/PP)/PP)/NP (S\NP)\(S\NP)
	 * 
	 * applies to forward and backward cross, but not backward, comp
	 */
	private boolean generalisedCompositionB3(SuperCategory leftSuperCat, SuperCategory rightSuperCat, short compositionType, ArrayList<SuperCategory> results, Sentence sentence) {
		if (compositionType == SuperCategory.FWD_COMP) {
			if (!unification.unify(leftSuperCat.cat.argument, rightSuperCat.cat.result.result.result)) {
				return false;
			}
		} else {
			// BWD_CROSS
			if (!unification.unify(leftSuperCat.cat.result.result.result, rightSuperCat.cat.argument)) {
				return false;
			}
		}
		if (!unification.unify(leftSuperCat, rightSuperCat)) {
			return false;
		}

		if (compositionType == SuperCategory.FWD_COMP) {
			unification.addVars1(leftSuperCat.cat.result);
		} else {
			// BWD_CROSS case
			unification.addVars2(rightSuperCat.cat.result);
		}

		if (compositionType == SuperCategory.FWD_COMP) {
			unification.addVars2(rightSuperCat.cat.result.argument);
			unification.addVar2(rightSuperCat.cat.result.var);
			unification.addVars2(rightSuperCat.cat.result.result.argument);
			unification.addVar2(rightSuperCat.cat.result.result.var);
		} else {
			// BWD_CROSS case
			unification.addVars1(leftSuperCat.cat.result.argument);
			unification.addVar1(leftSuperCat.cat.result.var);
			unification.addVars1(leftSuperCat.cat.result.result.argument);
			unification.addVar1(leftSuperCat.cat.result.result.var);
		}

		if (compositionType == SuperCategory.FWD_COMP) {
			unification.addVars2(rightSuperCat.cat.argument);
		} else {
			// BWD_CROSS case
			unification.addVars1(leftSuperCat.cat.argument);
		}

		byte var1 = leftSuperCat.cat.var;
		unification.addVar1(var1);

		if (compositionType == SuperCategory.FWD_COMP) {
			leftSuperCat.cat.result.reorderVariables(unification.trans1, unification.seen, unification.order);
		} else {
			// BWD_CROSS case
			rightSuperCat.cat.result.reorderVariables(unification.trans2, unification.seen, unification.order);
		}

		Category innerCat;
		if (compositionType == SuperCategory.FWD_COMP) {
			innerCat = rightSuperCat.cat.result.result;
		} else {
			// BWD_CROSS case
			innerCat = leftSuperCat.cat.result.result;
		}

		byte var = innerCat.var;
		if (compositionType == SuperCategory.FWD_COMP) {
			innerCat.argument.reorderVariables(unification.trans2, unification.seen, unification.order);
		} else {
			// BWD_CROSS case
			innerCat.argument.reorderVariables(unification.trans1, unification.seen, unification.order);
		}

		if (compositionType == SuperCategory.FWD_COMP) {
			if (var != VarID.NONE && unification.seen[unification.trans2[var]] != VarID.NONE) {
				unification.seen[unification.trans2[var]] = ++unification.order.value;
			}
		} else { // BWD_CROSS case
			if (var != VarID.NONE && unification.seen[unification.trans1[var]] != VarID.NONE) {
				unification.seen[unification.trans1[var]] = ++unification.order.value;
			}
		}
		/*
		 * repeat the above but one argument "higher" (this is done nicely with
		 * a recursive call in C&C)
		 */
		if (compositionType == SuperCategory.FWD_COMP) {
			innerCat = rightSuperCat.cat.result;
		} else {
			// BWD_CROSS case
			innerCat = leftSuperCat.cat.result;
		}

		var = innerCat.var;
		if (compositionType == SuperCategory.FWD_COMP) {
			innerCat.argument.reorderVariables(unification.trans2, unification.seen, unification.order);
		} else {
			// BWD_CROSS case
			innerCat.argument.reorderVariables(unification.trans1, unification.seen, unification.order);
		}

		if (compositionType == SuperCategory.FWD_COMP) {
			if (var != VarID.NONE && unification.seen[unification.trans2[var]] != VarID.NONE) {
				unification.seen[unification.trans2[var]] = ++unification.order.value;
			}
		} else {
			// BWD_CROSS case
			if (var != VarID.NONE && unification.seen[unification.trans1[var]] != VarID.NONE) {
				unification.seen[unification.trans1[var]] = ++unification.order.value;
			}
		}

		if (compositionType == SuperCategory.FWD_COMP) {
			rightSuperCat.cat.argument.reorderVariables(unification.trans2, unification.seen, unification.order);
		} else {
			// BWD_CROSS case
			leftSuperCat.cat.argument.reorderVariables(unification.trans1, unification.seen, unification.order);
		}

		if (var1 != VarID.NONE && unification.seen[unification.trans1[var1]] != VarID.NONE) {
			unification.seen[unification.trans1[var1]] = ++unification.order.value;
		}

		if (unification.order.value >= VarID.NUM_VARS) {
			throw new Error("run out variables in forward composition");
		}

		unification.reorderVariables(leftSuperCat, rightSuperCat);

		Category innerInnerResult, innerResult, innerInnerArgument, innerArgument, newResult, newArgument, newCategory;
		SuperCategory resultSuperCat;
		byte innerVar, innerInnerVar;
		if (compositionType == SuperCategory.FWD_COMP) {
			innerInnerResult = Category.TransVariable(leftSuperCat.cat.result, unification.trans1, unification.feature);
			innerInnerArgument = Category.TransVariable(rightSuperCat.cat.result.result.argument, unification.trans2, unification.feature);
			innerInnerVar = unification.trans2[rightSuperCat.cat.result.result.var];
			innerResult = new Category(innerInnerResult, rightSuperCat.cat.result.result.slash, innerInnerArgument, innerInnerVar, rightSuperCat.cat.result.result.relID, rightSuperCat.cat.result.result.lrange);
			innerArgument = Category.TransVariable(rightSuperCat.cat.result.argument, unification.trans2, unification.feature);
			innerVar = unification.trans2[rightSuperCat.cat.result.var];
			newResult = new Category(innerResult, rightSuperCat.cat.result.slash, innerArgument, innerVar, rightSuperCat.cat.result.relID, rightSuperCat.cat.result.lrange);
			newArgument = Category.TransVariable(rightSuperCat.cat.argument, unification.trans2, unification.feature);
		} else {
			// BWD_CROSS case
			innerInnerResult = Category.TransVariable(rightSuperCat.cat.result, unification.trans2, unification.feature);
			innerInnerArgument = Category.TransVariable(leftSuperCat.cat.result.result.argument, unification.trans1, unification.feature);
			innerInnerVar = unification.trans1[leftSuperCat.cat.result.result.var];
			innerResult = new Category(innerInnerResult, leftSuperCat.cat.result.result.slash, innerInnerArgument, innerInnerVar, leftSuperCat.cat.result.result.relID, leftSuperCat.cat.result.result.lrange);
			innerArgument = Category.TransVariable( leftSuperCat.cat.result.argument, unification.trans1, unification.feature);
			innerVar = unification.trans1[leftSuperCat.cat.result.var];
			newResult = new Category(innerResult, leftSuperCat.cat.result.slash, innerArgument, innerVar, leftSuperCat.cat.result.relID, leftSuperCat.cat.result.lrange);
			newArgument = Category.TransVariable(leftSuperCat.cat.argument, unification.trans1, unification.feature);
		}
		var1 = unification.trans1[var1]; // new variable for the "outer"
		// variable on the new category

		if (compositionType == SuperCategory.FWD_COMP) {
			newCategory = new Category(newResult, rightSuperCat.cat.slash, newArgument, var1, rightSuperCat.cat.relID, rightSuperCat.cat.lrange);
		} else {
			// BWD_CROSS case
			newCategory = new Category(newResult, leftSuperCat.cat.slash, newArgument, var1, leftSuperCat.cat.relID, rightSuperCat.cat.lrange);
		}

		if (compositionType == SuperCategory.FWD_COMP) {
			resultSuperCat = SuperCategory.BinaryCombinator(newCategory, (short) (SuperCategory.FWD_COMP | SuperCategory.RECURSIVE), leftSuperCat, rightSuperCat, unification, sentence);
		} else {
			// BWD_CROSS case
			resultSuperCat = SuperCategory.BinaryCombinator(newCategory, (short) (SuperCategory.BWD_CROSS | SuperCategory.RECURSIVE), leftSuperCat, rightSuperCat, unification, sentence);
		}
		if (resultSuperCat != null) {
			results.add(resultSuperCat);
			if (compositionType == SuperCategory.FWD_COMP) {
				Rules.genForwardCompCount++;
			} else if (compositionType == SuperCategory.BWD_CROSS) {
				Rules.genBackwardCrossCount++;
			}
			return true;
		} else {
			return false;
		}
	}

	public boolean forwardApplication(SuperCategory leftSuperCat, SuperCategory rightSuperCat, ArrayList<SuperCategory> results, Sentence sentence) {
		if (forwardAppReject(leftSuperCat, rightSuperCat)) {
			return false;
		}
		else {
			return application(leftSuperCat, rightSuperCat, true, results, sentence); // boolean indicates FWD_APP
		}
	}

	public boolean backwardApplication(SuperCategory leftSuperCat, SuperCategory rightSuperCat, ArrayList<SuperCategory> results, Sentence sentence) {
		if (backwardAppReject(leftSuperCat, rightSuperCat)) {
			return false;
		}
		else {
			return application(leftSuperCat, rightSuperCat, false, results, sentence); // boolean indicates BWD_APP
		}
	}

	public boolean forwardComposition(SuperCategory leftSuperCat, SuperCategory rightSuperCat, ArrayList<SuperCategory> results, Sentence sentence) {
		if (forwardCompReject(leftSuperCat, rightSuperCat)) {
			return false;
		} else {
			return composition(leftSuperCat, rightSuperCat, SuperCategory.FWD_COMP, results, sentence);
		}
	}

	private boolean generalisedForwardComposition(SuperCategory leftSuperCat, SuperCategory rightSuperCat, ArrayList<SuperCategory> results, Sentence sentence) {
		if (generalisedForwardCompB2Reject(leftSuperCat, rightSuperCat)) {
			return false;
		}

		if (generalisedCompositionB2(leftSuperCat, rightSuperCat,
				SuperCategory.FWD_COMP, results, sentence)) {
			return true;
		}

		if (generalisedForwardCompB3Reject(leftSuperCat, rightSuperCat)) {
			return false;
		} else {
			return generalisedCompositionB3(leftSuperCat, rightSuperCat, SuperCategory.FWD_COMP, results, sentence);
		}
	}

	public boolean backwardComposition(SuperCategory leftSuperCat, SuperCategory rightSuperCat, ArrayList<SuperCategory> results, Sentence sentence) {
		if (backwardCompReject(leftSuperCat, rightSuperCat)) {
			return false;
		} else {
			return composition(leftSuperCat, rightSuperCat, SuperCategory.BWD_COMP, results, sentence);
		}
	}

	/*
	 * this rule is hard coded for only one case: (S[dcl]\S[dcl])\NP S\S -->
	 * (S[dcl]\S[dcl])\NP
	 * 
	 * comment above is from C&C, but works with more cases than this?
	 */
	private boolean generalisedBackwardComposition(SuperCategory leftSuperCat, SuperCategory rightSuperCat, ArrayList<SuperCategory> results, Sentence sentence) {
		if (generalisedBackwardCompReject(leftSuperCat, rightSuperCat)) {
			return false;
		}

		if (!unification.unify(leftSuperCat.cat.result.result, rightSuperCat.cat.argument)) {
			return false;
		}

		if (!unification.unify(leftSuperCat, rightSuperCat)) {
			return false;
		}

		/*
		 * just use the left category as it is, ie not a copy; assume this is
		 * okay since Category objects are effectively immutable; they only get
		 * copied when they need to be changed, eg when using the TransVariable
		 * constructor
		 */
		unification.addVars1(leftSuperCat.cat);
		SuperCategory resultSuperCat = SuperCategory.BinaryCombinator(leftSuperCat.cat, (short) (SuperCategory.BWD_COMP | SuperCategory.RECURSIVE), leftSuperCat, rightSuperCat, unification, sentence);
		/*
		 * the BinaryCombinator can return null, to rule out some pathological
		 * cases in which numActiveVars > nuMVars
		 */
		if (resultSuperCat != null) {
			Rules.genBackwardCompCount++;
			results.add(resultSuperCat);
			return true;
		} else {
			return false;
		}
	}

	public boolean backwardCrossComposition(SuperCategory leftSuperCat, SuperCategory rightSuperCat, ArrayList<SuperCategory> results, Sentence sentence) {
		if (backwardCrossReject(leftSuperCat, rightSuperCat)) {
			return false;
		} else {
			return composition(leftSuperCat, rightSuperCat, SuperCategory.BWD_CROSS, results, sentence);
		}
	}

	private boolean generalisedBackwardCrossComposition(SuperCategory leftSuperCat, SuperCategory rightSuperCat, ArrayList<SuperCategory> results, Sentence sentence) {
		if (generalisedBackwardCrossB2Reject(leftSuperCat, rightSuperCat)) {
			return false;
		}

		if (generalisedCompositionB2(leftSuperCat, rightSuperCat, SuperCategory.BWD_CROSS, results, sentence)) {
			return true;
		}

		if (generalisedBackwardCrossB3Reject(leftSuperCat, rightSuperCat)) {
			return false;
		} else {
			return generalisedCompositionB3(leftSuperCat, rightSuperCat, SuperCategory.BWD_CROSS, results, sentence);
		}
	}

	public boolean coordination(SuperCategory leftSuperCat, SuperCategory rightSuperCat, ArrayList<SuperCategory> results) {
		if (coordinationReject(leftSuperCat, rightSuperCat)) {
			return false;
		}

		/*
		 * just use the right category as it is (both for the argument and the
		 * result), ie not a copy; assume this is okay since Category objects
		 * are effectively immutable; they only get copied when they need to be
		 * changed, eg when using the TransVariable constructor
		 */
		Category cat = new Category(rightSuperCat.cat, Category.BWD_SLASH, rightSuperCat.cat, VarID.NONE, (short) (0), (short) (0));
		results.add(SuperCategory.Coordination(cat, SuperCategory.CONJ, leftSuperCat, rightSuperCat));
		Rules.conjCount++;
		return true;
	}

	/*
	 * retaining the same name from C&C; comment from C&C: // TODO: funny conj
	 * allows cases like this through: // John likes and cars - need // to block
	 * this but funny conj needed because of the way some // coordinations
	 * analysed in the Penn Treebank (and hence CCGBank)
	 */
	public boolean funnyConj(SuperCategory leftSuperCat, SuperCategory rightSuperCat, ArrayList<SuperCategory> results) {
		if (!leftSuperCat.cat.isConj() || !rightSuperCat.cat.isN()) {
			return false;
		}

		results.add(SuperCategory.Punct(rightSuperCat.cat, SuperCategory.FUNNY_CONJ, leftSuperCat, rightSuperCat, rightSuperCat));
		Rules.funnyConjCount++;
		return true;
	}

	// X X => X where X is NP or S[dcl]
	// not sure these are ever used in CCGbank?
	public boolean apposition(SuperCategory leftSuperCat, SuperCategory rightSuperCat, ArrayList<SuperCategory> results) {
		if ((leftSuperCat.cat.isNP() && rightSuperCat.cat.isNP()) || (leftSuperCat.cat.isSdcl() && rightSuperCat.cat.isSdcl())) {
			results.add(SuperCategory.Apposition(SuperCategory.APPO, leftSuperCat, rightSuperCat));
			Rules.appositionCount++;
			return true;
		} else {
			return false;
		}
	}

	private boolean forwardAppReject(SuperCategory left, SuperCategory right) {
		return left.cat.notFwd() || right.coordinatedOrTypeRaised() || (eisnerNormalForm && left.forwardComp());
	}

	private boolean backwardAppReject(SuperCategory left, SuperCategory right) {
		return right.cat.notBwd() || left.coordinatedOrTypeRaised() || (eisnerNormalForm && right.backwardComp());
	}

	// note coordinated categories (X\X) get caught by the notFwd() check
	private boolean forwardCompReject(SuperCategory left, SuperCategory right) {
		return left.cat.notFwd() || right.cat.notFwd() || (eisnerNormalForm && left.forwardComp());
	}

	private boolean generalisedForwardCompB2Reject(SuperCategory left, SuperCategory right) {
		/*
		 * only do generalised composition when the larger category has a VP
		 * innermost result (which needs to be the same as the argument of the
		 * smaller category)
		 * 
		 * note assuming forwardCompReject() has already been called
		 * 
		 * the hasFeatureVar() check makes sure we don't have S[X]\NP on the
		 * innermost category on the right (after checking that the right
		 * category has enough "depth")
		 */
		return !left.cat.argument.isSbNP() || right.cat.result.notFwd() || right.cat.result.result.isAtomic() || right.cat.result.result.result.hasFeatureVar();
	}

	private boolean generalisedForwardCompB3Reject(SuperCategory left, SuperCategory right) {
		/*
		 * note assuming forwardCompReject() and
		 * generalisedForwardCompB2Reject() have already been called
		 * 
		 * the hasFeatureVar() check makes sure we don't have S[X]\NP on the
		 * innermost category on the right (after checking that the right
		 * category has enough "depth")
		 */
		return right.cat.result.result.notFwd() || right.cat.result.result.result.isAtomic() || right.cat.result.result.result.result.hasFeatureVar();
	}

	private boolean backwardCompReject(SuperCategory left, SuperCategory right) {
		/*
		 * if (left.cat.isBwd() && right.cat.isBwd() && !left.coordinated() &&
		 * !right.coordinated()) { if
		 * (backwardCompRuleInstances.contains(left.cat, right.cat)) {
		 * System.out.print("BCOMP SUCCESS: "); left.cat.print();
		 * System.out.print(" "); right.cat.print(); System.out.println(); }
		 * else { System.out.print("BCOMP FAILURE: "); left.cat.print();
		 * System.out.print(" "); right.cat.print(); System.out.println(); } }
		 */
		if (ruleInstancesParams.getBackwardComp()) {
			return left.cat.notBwd() || right.cat.notBwd()
					|| left.coordinated() || right.coordinated()
					|| (eisnerNormalForm && right.backwardComp())
					|| !backwardCompRuleInstances.contains(left.cat, right.cat);
		} else {
			return left.cat.notBwd() || right.cat.notBwd()
					|| left.coordinated() || right.coordinated()
					|| (eisnerNormalForm && right.backwardComp());
		}
	}

	/*
	 * this rule is hard coded for only one case: (S[dcl]\S[dcl])\NP S\S -->
	 * (S[dcl]\S[dcl])\NP
	 * 
	 * comment above is from C&C, but works with more cases than this?
	 * 
	 * note we're assuming backwardCompReject has already been called
	 */
	private boolean generalisedBackwardCompReject(SuperCategory left, SuperCategory right) {
		if (!left.cat.result.isSdclbSdcl()) {
			return true;
		}

		// only want to apply this rule when the S\S dependency can be
		// filled, i.e. when the inner result S[dcl] has a head
		if (left.vars[left.cat.result.result.var].isUnfilled()) {
			return true;
		}

		return false;
	}

	/*
	 * took this out: (eisnerNormalForm && (left.forwardComp() since we need it
	 * for cases like this: What the investors who oppose the proposed changes
	 * object to most is ...
	 * 
	 * and this: (eisnerNormalForm && left.backwardCrossComp()) needed for
	 * sentences like 242 in 2-21 (players who I remember vividly from ...)
	 */
	// left coordination gets caught by notFwd() check
	private boolean backwardCrossReject(SuperCategory left, SuperCategory right) {
		return left.cat.notFwd() || right.cat.notBwd() || right.coordinated() || right.cat.isArgNorNP();
	}

	private boolean generalisedBackwardCrossB2Reject(SuperCategory left, SuperCategory right) {
		return !right.cat.argument.isSbNP() || left.cat.result.notFwd() || left.cat.result.result.isAtomic() || left.cat.result.result.result.hasFeatureVar();
	}

	private boolean generalisedBackwardCrossB3Reject(SuperCategory left, SuperCategory right) {
		return left.cat.result.result.notFwd() || left.cat.result.result.result.isAtomic() || left.cat.result.result.result.result.hasFeatureVar();
	}

	private boolean coordinationReject(SuperCategory left, SuperCategory right) {
		/*
		 * if (left.cat.isConj()) { if
		 * (coordinationRuleInstances.contains(left.cat, right.cat)) {
		 * System.out.print("COORD SUCCESS: "); left.cat.print();
		 * System.out.print(" "); right.cat.print(); System.out.println(); }
		 * else { System.out.print("COORD FAILURE: "); left.cat.print();
		 * System.out.print(" "); right.cat.print(); System.out.println(); } }
		 */
		if (ruleInstancesParams.getConj()) {
			return !left.cat.isConj() || !coordinationRuleInstances.contains(left.cat, right.cat);
		} else {
			return !left.cat.isConj() || right.coordinatedOrTypeRaised() || right.cat.isConjbConj();
		}
	}
}
