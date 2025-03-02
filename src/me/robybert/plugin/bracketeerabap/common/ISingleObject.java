package me.robybert.plugin.bracketeerabap.common;

import org.eclipse.jface.text.Position;

public interface ISingleObject {

	public Position getPosition();

	public Position getPositionRaw();

	public boolean isOpening();

	public char[] getChar();

	public String toString();

	public boolean equals(final Object obj);

	public int hashCode();
}
