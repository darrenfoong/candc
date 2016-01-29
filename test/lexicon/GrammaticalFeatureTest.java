package lexicon;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class GrammaticalFeatureTest {
	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void convertTest() {
		String[] stringFeatures= { "X", "adj", "as", "asup", "b", "bem", "dcl", "em", "expl", "for", "frg", "intj", "inv", "nb", "ng", "num", "poss", "pss", "pt", "q", "qem", "thr", "to", "wq" };
		assertEquals((new GrammaticalFeature(stringFeatures[0])).value(), GrammaticalFeature.X);
		assertEquals((new GrammaticalFeature(stringFeatures[1])).value(), GrammaticalFeature.ADJ);
		assertEquals((new GrammaticalFeature(stringFeatures[2])).value(), GrammaticalFeature.AS);
		assertEquals((new GrammaticalFeature(stringFeatures[3])).value(), GrammaticalFeature.ASUP);
		assertEquals((new GrammaticalFeature(stringFeatures[4])).value(), GrammaticalFeature.B);
		assertEquals((new GrammaticalFeature(stringFeatures[5])).value(), GrammaticalFeature.BEM);
		assertEquals((new GrammaticalFeature(stringFeatures[6])).value(), GrammaticalFeature.DCL);
		assertEquals((new GrammaticalFeature(stringFeatures[7])).value(), GrammaticalFeature.EM);
		assertEquals((new GrammaticalFeature(stringFeatures[8])).value(), GrammaticalFeature.EXPL);
		assertEquals((new GrammaticalFeature(stringFeatures[9])).value(), GrammaticalFeature.FOR);
		assertEquals((new GrammaticalFeature(stringFeatures[10])).value(), GrammaticalFeature.FRG);
		assertEquals((new GrammaticalFeature(stringFeatures[11])).value(), GrammaticalFeature.INTJ);
		assertEquals((new GrammaticalFeature(stringFeatures[12])).value(), GrammaticalFeature.INV);
		assertEquals((new GrammaticalFeature(stringFeatures[13])).value(), GrammaticalFeature.NB);
		assertEquals((new GrammaticalFeature(stringFeatures[14])).value(), GrammaticalFeature.NG);
		assertEquals((new GrammaticalFeature(stringFeatures[15])).value(), GrammaticalFeature.NUM);
		assertEquals((new GrammaticalFeature(stringFeatures[16])).value(), GrammaticalFeature.POSS);
		assertEquals((new GrammaticalFeature(stringFeatures[17])).value(), GrammaticalFeature.PSS);
		assertEquals((new GrammaticalFeature(stringFeatures[18])).value(), GrammaticalFeature.PT);
		assertEquals((new GrammaticalFeature(stringFeatures[19])).value(), GrammaticalFeature.Q);
		assertEquals((new GrammaticalFeature(stringFeatures[20])).value(), GrammaticalFeature.QEM);
		assertEquals((new GrammaticalFeature(stringFeatures[21])).value(), GrammaticalFeature.THR);
		assertEquals((new GrammaticalFeature(stringFeatures[22])).value(), GrammaticalFeature.TO);
		assertEquals((new GrammaticalFeature(stringFeatures[23])).value(), GrammaticalFeature.WQ);
	}

	@Test
	public void convertTestInvalidNString() {
		exception.expect(IllegalArgumentException.class);
		new GrammaticalFeature("nz");
	}

	@Test
	public void convertTestInvalidPString() {
		exception.expect(IllegalArgumentException.class);
		new GrammaticalFeature("pz");
	}

	@Test
	public void convertTestInvalidString() {
		exception.expect(IllegalArgumentException.class);
		new GrammaticalFeature("zz");
	}

	@Test
	public void toStringTest() {
		String[] stringFeatures= { "X", "adj", "as", "asup", "b", "bem", "dcl", "em", "expl", "for", "frg", "intj", "inv", "nb", "ng", "num", "poss", "pss", "pt", "q", "qem", "thr", "to", "wq" };
		assertEquals((new GrammaticalFeature(stringFeatures[0])).toString(), "X");
		assertEquals((new GrammaticalFeature(stringFeatures[1])).toString(), "adj");
		assertEquals((new GrammaticalFeature(stringFeatures[2])).toString(), "as");
		assertEquals((new GrammaticalFeature(stringFeatures[3])).toString(), "asup");
		assertEquals((new GrammaticalFeature(stringFeatures[4])).toString(), "b");
		assertEquals((new GrammaticalFeature(stringFeatures[5])).toString(), "bem");
		assertEquals((new GrammaticalFeature(stringFeatures[6])).toString(), "dcl");
		assertEquals((new GrammaticalFeature(stringFeatures[7])).toString(), "em");
		assertEquals((new GrammaticalFeature(stringFeatures[8])).toString(), "expl");
		assertEquals((new GrammaticalFeature(stringFeatures[9])).toString(), "for");
		assertEquals((new GrammaticalFeature(stringFeatures[10])).toString(), "frg");
		assertEquals((new GrammaticalFeature(stringFeatures[11])).toString(), "intj");
		assertEquals((new GrammaticalFeature(stringFeatures[12])).toString(), "inv");
		assertEquals((new GrammaticalFeature(stringFeatures[13])).toString(), "nb");
		assertEquals((new GrammaticalFeature(stringFeatures[14])).toString(), "ng");
		assertEquals((new GrammaticalFeature(stringFeatures[15])).toString(), "num");
		assertEquals((new GrammaticalFeature(stringFeatures[16])).toString(), "poss");
		assertEquals((new GrammaticalFeature(stringFeatures[17])).toString(), "pss");
		assertEquals((new GrammaticalFeature(stringFeatures[18])).toString(), "pt");
		assertEquals((new GrammaticalFeature(stringFeatures[19])).toString(), "q");
		assertEquals((new GrammaticalFeature(stringFeatures[20])).toString(), "qem");
		assertEquals((new GrammaticalFeature(stringFeatures[21])).toString(), "thr");
		assertEquals((new GrammaticalFeature(stringFeatures[22])).toString(), "to");
		assertEquals((new GrammaticalFeature(stringFeatures[23])).toString(), "wq");
	}
}
