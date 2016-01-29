package lexicon;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class AtomTest {
	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void convertTest() {
		String[] stringAtoms= { ":", ",", "c", "LR", "LQ", "N", "NP", "P", "RR", "RQ", "S", ";" };
		assertEquals((new Atom(stringAtoms[0])).value(), Atom.COLON);
		assertEquals((new Atom(stringAtoms[1])).value(), Atom.COMMA);
		assertEquals((new Atom(stringAtoms[2])).value(), Atom.CONJ);
		assertEquals((new Atom(stringAtoms[3])).value(), Atom.LRB);
		assertEquals((new Atom(stringAtoms[4])).value(), Atom.LQU);
		assertEquals((new Atom(stringAtoms[5])).value(), Atom.N);
		assertEquals((new Atom(stringAtoms[6])).value(), Atom.NP);
		assertEquals((new Atom(stringAtoms[7])).value(), Atom.PP);
		assertEquals((new Atom(stringAtoms[8])).value(), Atom.RRB);
		assertEquals((new Atom(stringAtoms[9])).value(), Atom.RQU);
		assertEquals((new Atom(stringAtoms[10])).value(), Atom.S);
		assertEquals((new Atom(stringAtoms[11])).value(), Atom.SEMICOLON);
	}

	@Test
	public void convertTestEmptyString() {
		exception.expect(IllegalArgumentException.class);
		new Atom("");
	}

	@Test
	public void convertTestInvalidString() {
		exception.expect(IllegalArgumentException.class);
		new Atom("zz");
	}

	@Test
	public void toStringTest() {
		String[] stringAtoms= { ":", ",", "c", "LR", "LQ", "N", "NP", "P", "RR", "RQ", "S", ";" };
		assertEquals((new Atom(stringAtoms[0])).toString(), ":");
		assertEquals((new Atom(stringAtoms[1])).toString(), ",");
		assertEquals((new Atom(stringAtoms[2])).toString(), "conj");
		assertEquals((new Atom(stringAtoms[3])).toString(), "LRB");
		assertEquals((new Atom(stringAtoms[4])).toString(), "LQU");
		assertEquals((new Atom(stringAtoms[5])).toString(), "N");
		assertEquals((new Atom(stringAtoms[6])).toString(), "NP");
		assertEquals((new Atom(stringAtoms[7])).toString(), "PP");
		assertEquals((new Atom(stringAtoms[8])).toString(), "RRB");
		assertEquals((new Atom(stringAtoms[9])).toString(), "RQU");
		assertEquals((new Atom(stringAtoms[10])).toString(), "S");
		assertEquals((new Atom(stringAtoms[11])).toString(), ";");
	}
}
