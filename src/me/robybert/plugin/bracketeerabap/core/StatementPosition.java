package me.robybert.plugin.bracketeerabap.core;

import org.eclipse.jface.text.Position;

public class StatementPosition implements IPosition {
	private final Position _pos;
	private final int _colorCode;
	
	public StatementPosition(final int offset, final int colorCode, final int length) {
		_pos = new Position(offset, length);
		_colorCode = colorCode;
	}
	

	@Override
	public Position getPosition() {
		return _pos;
	}

	@Override
	public int getColorCode() {
		return _colorCode;
	}

}
