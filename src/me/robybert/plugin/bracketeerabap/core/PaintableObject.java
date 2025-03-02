/*******************************************************************************
 * Copyright (c) Gil Barash - chookapp@yahoo.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Gil Barash - initial API and implementation
 *******************************************************************************/
package me.robybert.plugin.bracketeerabap.core;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

public abstract class PaintableObject {
	protected Position _position;
	protected RGB _foreground;
	protected RGB _background;

	public PaintableObject(final Position position, final RGB foreground, final RGB background) {
		_position = position;
		_foreground = foreground;
		_background = background;
	}

	public Position getPosition() {
		return _position;
	}

	public RGB getForeground() {
		return _foreground;
	}

	public RGB getBackground() {
		return _background;
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof final PaintableObject other)) {
			return false;
		}

		boolean eq = true;
		eq &= _position.equals(other._position);
		eq &= equalNulls(_foreground, other._foreground);
		eq &= equalNulls(_background, other._background);

		return eq;
	}

	private boolean equalNulls(final Object obj1, final Object obj2) {
		if (obj1 == null ^ obj2 == null) {
			return false;
		}
		if (obj1 != null) {
			return obj1.equals(obj2);
		}
		return true;
	}

	public void paint(final GC gc, final StyledText st, final IDocument doc, final IRegion widgetRange,
			final Rectangle rect) {
		if (_position.isDeleted || _position.length == 0 || (widgetRange == null)) {
			return;
		}

		Color bg = null, fg = null;
		Color oldBackground = null, oldForeground = null;

		if (_background != null) {
			bg = new Color(Display.getDefault(), _background);
			oldBackground = gc.getBackground();
			gc.setBackground(bg);
		}

		if (_foreground != null) {
			fg = new Color(Display.getDefault(), _foreground);
			oldForeground = gc.getForeground();
			gc.setForeground(fg);
		}

		innerPaint(gc, st, doc, widgetRange, rect);

		if (_background != null) {
			gc.setBackground(oldBackground);
			bg.dispose();
		}
		if (_foreground != null) {
			gc.setForeground(oldForeground);
			fg.dispose();
		}

	}

	protected abstract void innerPaint(GC gc, StyledText st, IDocument doc, IRegion widgetRange, Rectangle rect);

	public abstract PaintableObject clone(final Position newPos);

}
