package lexicon;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/*
 * a triple consisting of: markedup category string, relation slot number,
 * and relation slot number according to Julia's ordering in CCGbank, which
 * is different and required for evaluating against CCGbank
 */

public class Relation {
	public final String category;
	public final short slot;
	public final short jslot;

	public static final Logger logger = LogManager.getLogger(Relation.class);

	public Relation(String category, short slot, short jslot) {
		this.category = category;
		this.slot = slot;
		this.jslot = jslot;
	}

	public Relation(Relation other) {
		category = other.category;
		slot = other.slot;
		jslot = other.jslot;
	}

	public void printSlot(boolean juliaSlots) {
		if ( juliaSlots ) {
			logger.info(category + " " + jslot);
		} else {
			logger.info(category + " " + slot);
		}
	}
}
