package cat_combination;

public class RuleInstancesParams {
	private boolean allRules;
	private boolean rightPunct;
	private boolean leftPunct;
	private boolean leftPunctConj;
	private boolean backwardComp;
	private boolean conj;
	private String directory;

	public RuleInstancesParams(boolean allRules, boolean rightPunct, boolean leftPunct, boolean leftPunctConj, boolean backwardComp, boolean conj, String directory) {
		this.allRules = allRules;
		this.rightPunct = rightPunct;
		this.leftPunct = leftPunct;
		this.leftPunctConj = leftPunctConj;
		this.backwardComp = backwardComp;
		this.conj = conj;
		this.directory = directory;
	}

	public boolean getAllRules() {
		return allRules;
	}

	public boolean getRightPunct() {
		return rightPunct;
	}

	public boolean getLeftPunct() {
		return leftPunct;
	}

	public boolean getLeftPunctConj() {
		return leftPunctConj;
	}

	public boolean getBackwardComp() {
		return backwardComp;
	}

	public boolean getConj() {
		return conj;
	}

	public String getDirectory() {
		return directory;
	}

	public void setAllRules(boolean allRules) {
		this.allRules = allRules;
	}

	public void setRightPunct(boolean rightPunct) {
		this.rightPunct = rightPunct;
	}

	public void setLeftPunct(boolean leftPunct) {
		this.leftPunct = leftPunct;
	}

	public void setLeftPunctConj(boolean leftPunctConj) {
		this.leftPunctConj = leftPunctConj;
	}

	public void setBackwardComp(boolean backwardComp) {
		this.backwardComp = backwardComp;
	}

	public void setConj(boolean conj) {
		this.conj = conj;
	}

	public void setDirectory(String directory) {
		this.directory = directory;
	}
}
