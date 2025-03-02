package me.robybert.plugin.bracketeerabap.common;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.Position;

public class MatchingStatements {
	private final List<SingleStatement> _statements;

    public MatchingStatements(final int openingOffset, final char[] openingStatement, final int closingOffset, final char[] closingStatement) {
        _statements = new ArrayList<>();
        _statements.add(new SingleStatement(openingOffset, true, openingStatement));
        _statements.add(new SingleStatement(closingOffset, false, closingStatement));
    }
    
    public MatchingStatements(final List<Integer> offsetList, final List<String> statementList  ) {
    	
    	boolean firstStatement = true;
    	_statements = new ArrayList<>();
    	
    	Iterator<Integer>  itOffset = offsetList.iterator();
    	for (Iterator<String> itStatement = statementList.iterator(); itStatement.hasNext() && itOffset.hasNext() ;) {
			char[] str =  itStatement.next().toCharArray();
			int offset = (int) itOffset.next();
			
			_statements.add(new SingleStatement(offset, firstStatement, str));
			
			firstStatement = false;
			
		}
    	
    }
    

    public List<SingleStatement> getStatements() {
        return _statements;
    }

    @Override
    public String toString() {
        return _statements.toString();
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof final MatchingStatements other)) {
            return false;
        }

        return getStatements().equals(other.getStatements());
        /*
         * for (SingleStatement bracket : _brackets) { if(! other.getBrackets().contains(bracket) ) return
         * false; }
         * 
         * return true;
         */
    }

    @Override
    public int hashCode() {
        return _statements.hashCode();
    }

    public SingleStatement getOpeningStatement() {
        return _statements.get(0);
    }

    public SingleStatement getClosingStatement() {
        return _statements.get( _statements.size() - 1 );
    }
    

    public SingleStatement getStatementAt(final int offset) {
        for (final SingleStatement br : _statements) {
            final Position pos = br.getPosition();
            if (pos != null && pos.offset == offset) {
                return br;
            }
        }
        return null;
    }

    public boolean hasDeletedPosition() {
        for (final SingleStatement br : _statements) {
            if (br.getPosition() == null) {
                return true;
            }
        }
        return false;
    }

    public int getDistanceBetweenBrackets() {
        return getClosingStatement().getPositionRaw().getOffset() - getOpeningStatement().getPositionRaw().getOffset();
    }
}
