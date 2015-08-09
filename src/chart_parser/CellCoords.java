package chart_parser;

public class CellCoords {
	int pos;
	int span;
	int end;
	double violation;

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
