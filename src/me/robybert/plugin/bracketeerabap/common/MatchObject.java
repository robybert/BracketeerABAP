package me.robybert.plugin.bracketeerabap.common;

import java.util.List;

public abstract class MatchObject {

	public List<? extends SingleObject> getObjects() {
		if (this instanceof MatchingStatements) {
			MatchingStatements statements = (MatchingStatements) this;
			return statements.getStatements();
		} else {
			BracketsPair pair = (BracketsPair) this;
			return pair.getBrackets();
		}
	}

	public SingleObject getOpeningObject() {
		if (this instanceof MatchingStatements) {
			MatchingStatements statements = (MatchingStatements) this;
			return statements.getOpeningStatement();
		} else {
			BracketsPair pair = (BracketsPair) this;
			return pair.getOpeningBracket();
		}
	}

	public SingleObject getClosingObject() {
		if (this instanceof MatchingStatements) {
			MatchingStatements statements = (MatchingStatements) this;
			return statements.getClosingStatement();
		} else {
			BracketsPair pair = (BracketsPair) this;
			return pair.getClosingBracket();
		}
	}

	public SingleObject getObjectAt(final int offset) {
		if (this instanceof MatchingStatements) {
			MatchingStatements statements = (MatchingStatements) this;
			return statements.getStatementAt(offset);
		} else {
			BracketsPair pair = (BracketsPair) this;
			return pair.getBracketAt(offset);
		}
	}

	public int getDistanceBetweenObjects() {
		if (this instanceof MatchingStatements) {
			MatchingStatements statements = (MatchingStatements) this;
			return statements.getDistanceBetweenStatements();
		} else {
			BracketsPair pair = (BracketsPair) this;
			return pair.getDistanceBetweenBrackets();
		}
	}

	public abstract String toString();

	public abstract boolean equals(final Object obj);

	public abstract int hashCode();

	public abstract boolean hasDeletedPosition();

}
