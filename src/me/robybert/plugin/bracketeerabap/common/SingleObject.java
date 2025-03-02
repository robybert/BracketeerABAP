package me.robybert.plugin.bracketeerabap.common;

import org.eclipse.jface.text.Position;

public abstract class SingleObject {
	Position _position;
	boolean _isOpening; // is this an opening bracket, such as "("

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
		return _isOpening;
	}

	@Override
	public String toString() {
		return String.format("[offset=%1$d, isOpening=%2$b]", _position.offset, _isOpening); //$NON-NLS-1$
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof final SingleBracket other)) {
			return false;
		}

		return other._isOpening == _isOpening && other._position.equals(_position);
	}

	@Override
	public int hashCode() {
		return _position.hashCode() ^ (_isOpening ? 2 : 4);
	}

	public abstract char[] getChar();
}
