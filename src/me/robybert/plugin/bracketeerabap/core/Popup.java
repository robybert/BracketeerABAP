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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.services.IDisposable;

public class Popup implements IDisposable, PaintListener {
	private final Shell _shell;
	private final SourceViewer _sourceViewer;
	private PaintableObject _objectToPaint;

	public Popup(final ISourceViewer parentSv, final StyledText parentSt, final IDocument origDoc,
			final PaintableObject paintObject) throws BadLocationException {
		_objectToPaint = null;

		_shell = new Shell(parentSt.getShell(), SWT.NO_FOCUS | SWT.ON_TOP | SWT.TOOL);
		_shell.setLayout(new FillLayout());

		final Composite inner = new Composite(_shell, SWT.NONE);
		inner.setLayout(new FillLayout());

		_sourceViewer = new SourceViewer(inner, null, null, false, SWT.MULTI);

		_sourceViewer.setEditable(false);
		final StyledText textWidget = _sourceViewer.getTextWidget();
		textWidget.setFont(parentSt.getFont());
		textWidget.setTabs(parentSt.getTabs());
		textWidget.setForeground(parentSt.getForeground());
		textWidget.setBackground(parentSt.getBackground());
		textWidget.addPaintListener(this);

		final GC gc = new GC(textWidget);
		int popupHight = 70;
		try {
			popupHight = gc.getFontMetrics().getHeight() * 3;
		} finally {
			gc.dispose();
		}

		try {
			updatePopupContent(_sourceViewer, parentSv, parentSt, origDoc, paintObject);
		} catch (final BadLocationException e) {
			dispose();
			throw e;
		}

		final Point txtSize = new Point(parentSt.getSize().x, popupHight);

		_shell.setSize(txtSize);
		final Point parentLocation = parentSt.getDisplay().map(parentSt, null, 0, 0);
		_shell.setLocation(parentLocation.x, parentLocation.y - txtSize.y);

		_shell.setVisible(true);
	}

	private void updatePopupContent(final SourceViewer sv, final ISourceViewer parentSv, final StyledText parentSt,
			final IDocument origDoc, final PaintableObject paintObject) throws BadLocationException {
		final Document newDoc = new Document();
		sv.setInput(newDoc);

		String txt = ""; //$NON-NLS-1$
		final List<StyleRange> styleRanges = new ArrayList<>();

		final Position origPos = paintObject.getPosition();

		final Integer[] lines = new Integer[3];
		lines[1] = origDoc.getLineOfOffset(origPos.getOffset());
		lines[0] = getLineBefore(origDoc, lines[1]);
		lines[2] = getLineAfter(origDoc, lines[1]);

		for (int i = 0; i < 3; i++) {
			if (lines[i] == null) {
				continue;
			}
			final int line = lines[i];

			if (!txt.isEmpty()) {
				txt += "\r\n"; //$NON-NLS-1$
			}

			int newDocOffset = txt.length();
			IRegion region = origDoc.getLineInformation(line);
			txt += origDoc.get(region.getOffset(), region.getLength());
			if (i == 1) {
				final Position newPos = new Position(origPos.getOffset() - region.getOffset() + newDocOffset, 1);
				_objectToPaint = paintObject.clone(newPos);
			}

			region = TextUtils.getWidgetRange(parentSv, region.getOffset(), region.getLength());

			final StyleRange[] ranges = parentSt.getStyleRanges(region.getOffset(), region.getLength());
			for (final StyleRange styleRange : ranges) {
				styleRange.start = newDocOffset;
				newDocOffset += styleRange.length;
				styleRanges.add(styleRange);
			}
		}

		sv.getDocument().set(txt);
		sv.setDocument(newDoc);
		final StyleRange[] stArray = new StyleRange[styleRanges.size()];
		styleRanges.toArray(stArray);
		sv.getTextWidget().setStyleRanges(stArray);

	}

	private Integer getLineBefore(final IDocument origDoc, final int startFromLine) throws BadLocationException {
		for (int line = startFromLine - 1; line >= 0; line--) {
			final IRegion region = origDoc.getLineInformation(line);
			final String txt = origDoc.get(region.getOffset(), region.getLength());
			if (!txt.trim().isEmpty()) {
				return line;
			}
		}
		return null;
	}

	private Integer getLineAfter(final IDocument origDoc, final int startFromLine) throws BadLocationException {
		final int linesCount = origDoc.getNumberOfLines();
		for (int line = startFromLine + 1; line < linesCount; line++) {
			final IRegion region = origDoc.getLineInformation(line);
			final String txt = origDoc.get(region.getOffset(), region.getLength());
			if (!txt.trim().isEmpty()) {
				return line;
			}
		}
		return null;
	}

	@Override
	public void dispose() {
		_shell.close();
		_shell.dispose();
	}

	@Override
	public void paintControl(final PaintEvent event) {
		if (_objectToPaint == null) {
			return;
		}

		_objectToPaint.paint(event.gc, _sourceViewer.getTextWidget(), _sourceViewer.getDocument(),
				new Region(_objectToPaint.getPosition().getOffset(), _objectToPaint.getPosition().getLength()), null);
	}
}
