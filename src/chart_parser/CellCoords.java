package chart_parser;

public class CellCoords {
	public int pos;
	public int span;
	public int end;
	public double violation;

	public CellCoords(int pos, int span) {
		this.pos = pos;
		this.span = span;
		this.end = pos + span - 1;
	}

	public CellCoords(int pos, int span, double violation) {
		this.pos = pos;
		this.span = span;
		this.end = pos + span - 1;
		this.violation = violation;
	}
}
