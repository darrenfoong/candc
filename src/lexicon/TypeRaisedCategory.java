package lexicon;


/*
 * some categories to be type-raised already have a dependency, eg the
 * AP case:
 * ((S[X]{Y}\NP{Z}){Y}\((S[X]{Y}\NP{Z}){Y}/(S[adj]{_}\NP{Z}){_}){Y}){Y} Z
 * Z here indicates the variable associated with that dependency;
 * for NP there is no such dependency, here indicated with the sentinel:
 * (S[X]{Y}/(S[X]{Y}\NP{_}){Y}){Y} +
 *
 * lexVar is the new variable associated with the lexical item; this
 * is needed by the relevant SuperCategory constructor since it needs
 * to get the Variable associated with the lexical item
 */
public class TypeRaisedCategory {
	public Category cat;
	public byte lexVar; // variable associated with lexical item
	public byte depVar; // variable for any dependency in the original category

	public TypeRaisedCategory(Category cat, byte lexVar, byte depVar) {
		this.cat = cat;
		this.lexVar = lexVar;
		this.depVar = depVar;
	}
}
