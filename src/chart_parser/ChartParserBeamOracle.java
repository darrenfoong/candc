package chart_parser;

import java.io.IOException;

import cat_combination.FilledDependency;
import cat_combination.RuleInstancesParams;
import cat_combination.SuperCategory;
import model.Lexicon;
import uk.ac.cam.cl.depnn.io.Dependency;
import uk.ac.cam.cl.depnn.nn.NeuralNetwork;

public class ChartParserBeamOracle extends ChartTrainParserBeam {
	public ChartParserBeamOracle(
			String grammarDir,
			boolean altMarkedup,
			boolean eisnerNormalForm,
			int MAX_WORDS,
			int MAX_SUPERCATS,
			RuleInstancesParams ruleInstancesParams,
			Lexicon lexicon,
			String featuresFile,
			String weightsFile,
			boolean newFeatures,
			boolean cubePruning,
			int beamSize,
			double beta) throws IOException {
		super(grammarDir, altMarkedup, eisnerNormalForm, MAX_WORDS,
				MAX_SUPERCATS, ruleInstancesParams,
				lexicon, featuresFile, weightsFile, newFeatures, cubePruning,
				beamSize, beta, false, false, false);

		this.depnn = new NeuralNetwork<Dependency>();
	}

	@Override
	protected boolean preParse() throws IOException {
		if (!readDepsPerCell(goldDepsPerCell)) {
			return false;
		}

		logger.info("Gold dependencies per cell merged.");

		return true;
	}

	@Override
	protected void postParse(int pos, int span, int numWords) {
		return;
	}

	@Override
	protected void calcSpanNNScore(int span) {
		int numWords = sentence.words.size();

		for ( int pos = 0; pos <= numWords - pos; pos++ ) {
			CellTrainBeam cell = (CellTrainBeam) chart.cell(pos, span);

			for ( SuperCategory superCat : cell.getSuperCategories() ) {
				if ( superCat.filledDeps == null || superCat.filledDeps.isEmpty() ) {
					continue;
				}

				for ( FilledDependency dep : superCat.filledDeps ) {
					if ( !oracleDecoder.ignoreDeps.ignoreDependency(dep, sentence) ) {
						if ( cell.goldDeps.contains(dep) ) {
							superCat.depnnScore += 1;
						} else {
							superCat.depnnScore -= 1;
						}
					}
				}

				superCat.score += weights.getDepNN() * superCat.depnnScore;
			}
		}
	}

	@Override
	protected void loadEmbeddings() {
		return;
	}
}
