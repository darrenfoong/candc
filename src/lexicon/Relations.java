package lexicon;

import java.util.ArrayList;
import java.util.HashMap;

/*
 * stores all the relation objects in an array;
 * HashMap gives index into the array for a string category
 *
 * first element of the array has an empty relation set in the constructor
 * i.e. the first (real) relation has a relID of 1
 * this is important because a relID of 0 on a category implies no relation
 * 
 * multiple relations for a single category are stored next to each other;
 * index of the first relation is stored in the HashMap
 *
 * assumes that the relations in a markedup string are ordered from 1,
 * from left to right, and increase by 1 each time, and that addRelation
 * is called according to this order
 */

public class Relations {
	private ArrayList<Relation> relations;
	private HashMap<String, Short> relIDs;
	private HashMap<CategoryJSlotPair, Short> relIDsII;
	// from pairs of Category strings and Julia slots to RelIDs used in the oracle deps decoder
	public static short conj1 = 0;
	// the special conj relation for dealing with coordination

	public Relations() {
		relations = new ArrayList<Relation>();
		relIDs = new HashMap<String, Short>();
		relIDsII = new HashMap<CategoryJSlotPair, Short>();
		addRelation("", (short) (0), (short) (0));
		// first index of a relation needs to be 1
		initConj();
	}

	public Relation getRelation(short relID) {
		return relations.get(relID);
	}

	public short getRelID(String category, short slot) {
		return (short) (relIDs.get(category) + slot - 1);
	}

	/*
	 * note that we may get cases where the category, slot pair is not in the
	 * hashMap, eg if the slot is a Julia slot and it doesn't correspond to any
	 * of the relations in the markedup file
	 */
	public short getRelID_II(String category, short slot) {
		CategoryJSlotPair pair = new CategoryJSlotPair(category, slot);
		if ( relIDsII.containsKey(pair) ) {
		// if (relIDsII.get(pair) != null) {
			return relIDsII.get(pair);
		} else {
			return (short) (0); // not a relation in markedup
		}
	}

	public short addRelation(String categoryString, short slot, short juliaSlot) {
		Relation relation = new Relation(categoryString, slot, juliaSlot);
		short relID = (short) (relations.size());
		relations.add(relation);

		if ( slot == 1 ) {
			relIDs.put(categoryString, relID);
			// relID gets converted to an object thro' autoboxing
		}

		CategoryJSlotPair pair = new CategoryJSlotPair(categoryString, juliaSlot);
		relIDsII.put(pair, relID);

		return relID;
	}

	private void initConj() {
		conj1 = addRelation("conj", (short) (1), (short) (1));
	}
}
