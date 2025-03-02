package me.robybert.plugin.bracketeerabap.core;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import me.robybert.plugin.bracketeerabap.preferences.PreferencesConstants;

public class PaintableStatement extends PaintableObject {
	private final String _highlightType;
	private RGB _outlineColor;

	public PaintableStatement(final Position position, final RGB foreground, final RGB background,
			final String highlightType) {
		super(position, foreground,
				highlightType.equals(PreferencesConstants.Statements.Highlights.HighlightTypeValSolid) ? background
						: null);
		_highlightType = highlightType;
		_outlineColor = background;
	}

	@Override
	protected void innerPaint(GC gc, StyledText st, IDocument doc, IRegion widgetRange, Rectangle widgetRect) {

		final int offset = widgetRange.getOffset();
		final int length = widgetRange.getLength();
//		TODO: maybe handle this better.
//		if (length != 1) {
//			throw new IllegalArgumentException(String.format("length %1$d != 1", length)); //$NON-NLS-1$
//		}

		final Point p = st.getLocationAtOffset(offset);
		String txt = null;

		try {
			txt = doc.get(_position.getOffset(), length);
		} catch (final BadLocationException e) {
			return;
		}

		gc.drawText(txt, p.x, p.y, _background == null);

		if (_highlightType.equals(PreferencesConstants.Statements.Highlights.HighlightTypeValOutline)) {
			final Color oldFg = gc.getForeground();
			Color fg = null;
			if (_outlineColor == null) {
				gc.setForeground(gc.getBackground());
			} else {
				fg = new Color(Display.getDefault(), _outlineColor);
				gc.setForeground(fg);
			}

			final Point metrics = gc.textExtent(txt);
			final Rectangle rect = new Rectangle(p.x, p.y, metrics.x - 1, metrics.y - 1);

			gc.drawRectangle(rect);

			gc.setForeground(oldFg);
			if (fg != null) {
				fg.dispose();
			}
		}

	}

	public PaintableObject clone(final Position newPos) {
		final PaintableStatement ret = new PaintableStatement(newPos, _foreground, _background, _highlightType);
		ret._outlineColor = _outlineColor;
		return ret;
	}

}
