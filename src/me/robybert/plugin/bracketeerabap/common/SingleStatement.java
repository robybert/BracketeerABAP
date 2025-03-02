package me.robybert.plugin.bracketeerabap.common;

import org.eclipse.jface.text.Position;

public class SingleStatement extends SingleObject {
	char[] _char;

	public SingleStatement(final int offset, final boolean isOpening, final char[] ch) {
		_isOpening = isOpening;
		_position = new Position(offset, ch.length);
		_char = ch;
	}

	public char[] getChar() {
		return _char;
	}

}
