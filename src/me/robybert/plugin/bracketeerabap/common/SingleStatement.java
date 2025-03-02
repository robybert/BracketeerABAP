package me.robybert.plugin.bracketeerabap.common;

import org.eclipse.jface.text.Position;

public class SingleStatement {
	Position _position;
	boolean _isStarting;
	char[] _char;
	
	
	
	public SingleStatement(final int offset, final boolean isStarting, final char[] ch) {
		_isStarting = isStarting;
		_position 	= new Position(offset, ch.length);
		_char 		= ch;
	}
	
	public Position getPosition() {
        if (!_position.isDeleted && _position.length > 0) {
            return _position;
        } else {
            return null;
        }
    }

    public Position getPositionRaw() {
        return _position;
    }

    public boolean isOpening() {
        return _isStarting;
    }

    public char[] getChar() {
        return _char;
    }

    @Override
    public String toString() {
        return String.format("[offset=%1$d, isOpening=%2$b]", _position.offset, _isStarting); //$NON-NLS-1$
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof final SingleBracket other)) {
            return false;
        }

        return other._isOpening == _isStarting && other._position.equals(_position);
    }

    @Override
    public int hashCode() {
        return _position.hashCode() ^ (_isStarting ? 2 : 4);
    }

}
