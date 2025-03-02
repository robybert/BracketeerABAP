/*******************************************************************************
 * Copyright (c) Gil Barash - chookapp@yahoo.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Gil Barash - initial API and implementation
 *
 * Thanks to:
 *    emil.crumhorn@gmail.com - Some of the code was copied from the
 *    "eclipsemissingfeatrues" plugin.
 *******************************************************************************/
package me.robybert.plugin.bracketeerabap.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IPaintPositionManager;
import org.eclipse.jface.text.IPainter;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.JFaceTextUtil;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.services.IDisposable;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.SimpleMarkerAnnotation;

import me.robybert.plugin.bracketeerabap.Activator;
import me.robybert.plugin.bracketeerabap.common.BracketsPair;
import me.robybert.plugin.bracketeerabap.common.Hint;
import me.robybert.plugin.bracketeerabap.common.MatchObject;
import me.robybert.plugin.bracketeerabap.common.MatchingStatements;
import me.robybert.plugin.bracketeerabap.common.SingleBracket;
import me.robybert.plugin.bracketeerabap.common.SingleObject;
import me.robybert.plugin.bracketeerabap.common.SingleStatement;
import me.robybert.plugin.bracketeerabap.core.ProcessorConfiguration.HintConfiguration;
import me.robybert.plugin.bracketeerabap.extensionpoint.BracketeerProcessor;

public class BracketsHighlighter implements CaretListener, Listener, PaintListener, IDisposable, IPainter,
		IProcessingContainerListener, IProcessorConfigurationListener, FocusListener {

	private ISourceViewer _sourceViewer;
	private StyledText _textWidget;
	private ProcessingThread _processingThread;
	private IDocument _doc;
	private ProcessorConfiguration _conf;
	private IResource _resource;
	private IAnnotationModel _annotationModel;
	private Map<Annotation, Position> _annotationMap;

	private boolean _isActive;

	private final List<PaintableBracket> _hoveredPairsToPaint;
	private final List<PaintableBracket> _surroundingPairsToPaint;
	private final List<PaintableBracket> _singleBracketsToPaint;
	private final List<PaintableStatement> _hoveredStatementsToPaint;
	private final List<PaintableStatement> _surroundingStatementsToPaint;
	private final List<PaintableStatement> _singleStatementsToPaint;
	private List<PaintableHint> _hintsToPaint;
	private PaintableHint _hoveredHintToPaint;
	private Point m_hoverEntryPoint;
	private Popup _popup;

	private PaintableHint _mousePointingAtHint;
	private SingleBracket _mousePointingAtBracket;
	private SingleStatement _mousePointingAtStatement;
	private boolean _mousePointerHand;

	private int _caretOffset;
	private int m_hyperlinkModifiers;

	public BracketsHighlighter() {
		_sourceViewer = null;
		_processingThread = null;
		_textWidget = null;
		_conf = null;
		_doc = null;
		_resource = null;
		_annotationModel = null;
		_annotationMap = new HashMap<>();

		_isActive = false;

		_hoveredPairsToPaint = new LinkedList<>();
		_surroundingPairsToPaint = new LinkedList<>();
		_singleBracketsToPaint = new LinkedList<>();
		_hoveredStatementsToPaint = new LinkedList<>();
		_surroundingStatementsToPaint = new LinkedList<>();
		_singleStatementsToPaint = new LinkedList<>();
		_hintsToPaint = new ArrayList<>();
		m_hoverEntryPoint = null;
		_hoveredHintToPaint = null;
		_popup = null;

		_mousePointingAtHint = null;
		_mousePointingAtBracket = null;
		_mousePointingAtStatement = null;
		_mousePointerHand = false;
	}

	@Override
	public void dispose() {
		clearPopup();

		if (_sourceViewer == null) {
			return;
		}

		_conf.removeListener(this);

		deactivate(false);

		final ITextViewerExtension2 extension = (ITextViewerExtension2) _sourceViewer;
		extension.removePainter(this);

		if (_processingThread != null) {
			_processingThread.getBracketContainer().removeListener(this);
			_processingThread.dispose();
			_processingThread = null;
		}

		_sourceViewer = null;
		_textWidget = null;
	}

	/************************************************************
	 * public methods
	 *
	 * @param part
	 * @param part
	 ************************************************************/

	public void Init(final BracketeerProcessor processor, final IEditorPart part, final IDocument doc,
			final ITextViewer textViewer, final ProcessorConfiguration conf) {

		_sourceViewer = (ISourceViewer) textViewer;
		_textWidget = _sourceViewer.getTextWidget();
		_conf = conf;
		processor.setHintConf(conf.getHintConfiguration());
		_doc = doc;

		final boolean editable = _textWidget.getEditable();

		_resource = part.getEditorInput().getAdapter(IResource.class);
		if (_resource == null && editable) {
			Activator.log(Messages.BracketsHighlighter_UnableToGetResource);
		}

		final ITextEditor editor = part.getAdapter(ITextEditor.class);
		if (editor == null) {
			Activator.log(Messages.BracketsHighlighter_UnableToGetEditor);
		} else {
			final IDocumentProvider provider = editor.getDocumentProvider();
			_annotationModel = provider.getAnnotationModel(editor.getEditorInput());
		}

		_processingThread = new ProcessingThread(doc, processor);
		_processingThread.getBracketContainer().addListener(this);
		_conf.addListener(this);

		final ITextViewerExtension2 extension = (ITextViewerExtension2) textViewer;
		extension.addPainter(this);

		m_hyperlinkModifiers = _conf.getGeneralConfiguration().getHyperlinkModifiers();
	}

	public ISourceViewer getSourceViewer() {
		return _sourceViewer;
	}

	public ProcessorConfiguration getConfiguration() {
		return _conf;
	}

	/************************************************************
	 * listeners
	 ************************************************************/

	@Override
	public void caretMoved(final CaretEvent event) {
		_caretOffset = getCurrentCaretOffset();
		caretMovedTo(_caretOffset);
	}

	/*
	 * Events: - MouseHover - MouseMove - MouseDown - KeyDown - KeyUp
	 *
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
	 */
	@Override
	public void handleEvent(final Event event) {
		switch (event.type) {
		case SWT.MouseHover:

			// hovering disabled when in "hyperlink mode"
			if ((event.stateMask & SWT.MODIFIER_MASK) == m_hyperlinkModifiers) {
				return;
			}

			try {
				final int caret = getDocCarretAdvanced(null, event.x, event.y);

				if (mouseHoverAt(_textWidget, caret)) {
					m_hoverEntryPoint = new Point(event.x, event.y);
				}
			} catch (final IllegalArgumentException e) {
			} catch (final Exception e) {
				Activator.log(e);
			}
			break;

		case SWT.MouseMove:
			if ((event.stateMask & SWT.MODIFIER_MASK) == m_hyperlinkModifiers) {
				if (_textWidget.isFocusControl()) {
					mousePointingAt(event.x, event.y);
					updateMousePointer();
				}
			}

			if (m_hoverEntryPoint == null) {
				break;
			}

			if (getDistanceBetween(new Point(event.x, event.y), m_hoverEntryPoint) > 20) {
				caretMovedTo(getCurrentCaretOffset());
				m_hoverEntryPoint = null;
			}
			break;

		case SWT.MouseDown:
			if (_mousePointingAtHint != null) {
				final Hint hint = _processingThread.getBracketContainer()
						.getHint(_mousePointingAtHint.getPosition().getOffset());
				if (hint == null) {
					Activator.log(Messages.BracketsHighlighter_ErrHintNotFound);
					break;
				}
				jumpToPosition(hint.getOriginPosition());
			}
			if (_mousePointingAtBracket != null) {
				final List<BracketsPair> pairs = _processingThread.getBracketContainer()
						.getMatchingPairs(_mousePointingAtBracket.getPosition().getOffset(), 1);
				if (pairs.size() == 0 || pairs.size() > 1) {
					Activator.log(Messages.BracketsHighlighter_ErrPairNotFound);
					break;
				}
				final BracketsPair pair = pairs.get(0);
				Position pos = null;
				if (pair.getOpeningBracket().equals(_mousePointingAtBracket)) {
					pos = pair.getClosingBracket().getPosition();
				}
				if (pair.getClosingBracket().equals(_mousePointingAtBracket)) {
					pos = pair.getOpeningBracket().getPosition();
				}
				jumpToPosition(pos);
			}
			if (_mousePointingAtStatement != null) {
				final List<MatchingStatements> statements = _processingThread.getBracketContainer()
						.getMatchingStatements(_mousePointingAtStatement.getPosition().getOffset(), 1);
				if (statements.size() == 0 || statements.size() > 1) {
					Activator.log(Messages.BracketsHighlighter_ErrMatchingStatementsNotFound);
					break;
				}
				final MatchingStatements statement = statements.get(0);
				Position pos = null;
				if (statement.getOpeningStatement().equals(_mousePointingAtStatement)) {
					pos = statement.getClosingStatement().getPosition();
				}
				if (statement.getClosingStatement().equals(_mousePointingAtStatement)) {
					pos = statement.getOpeningStatement().getPosition();
				}
				jumpToPosition(pos);
			}
			break;

		case SWT.KeyDown:
			if ((event.keyCode | event.stateMask) == m_hyperlinkModifiers) {
				/* clearing hovered pairs */
				if (m_hoverEntryPoint != null) {
					caretMovedTo(getCurrentCaretOffset());
					m_hoverEntryPoint = null;
				}

				final Display display = _textWidget.getDisplay();
				Point point = display.getCursorLocation();
				point = display.map(null, _textWidget, point);
				mousePointingAt(point.x, point.y);
				updateMousePointer();
			} else {
				clearHyperlink();
				updateMousePointer();
			}
			break;

		case SWT.KeyUp:
			if ((event.keyCode & m_hyperlinkModifiers) > 0) // is one of the modifiers being released?
			{
				clearHyperlink();
				updateMousePointer();
			}
			break;

		default:
			Assert.isTrue(false, Messages.BracketsHighlighter_ErrUnexpectedEvent + event.type);
		}
	}

	private int getDocCarretAdvanced(final GC outerGc, final int x, final int y) {
		int caret = -1;

		GC gc = null;
		if (outerGc == null) {
			gc = new GC(_textWidget);
		} else {
			gc = outerGc;
		}

		try {
			final int charWidth = gc.getFontMetrics().getAverageCharWidth();

			caret = _textWidget.getOffsetAtPoint(new Point(x + charWidth / 2, y));
			caret = ((ProjectionViewer) _sourceViewer).widgetOffset2ModelOffset(caret);
			if (caret > 0) {
				caret--;
			}
		} catch (final IllegalArgumentException e) {
			caret = -1;
		} finally {
			if (outerGc == null) {
				gc.dispose();
			}
		}

		if (caret == -1) {
			try {
				caret = _textWidget.getOffsetAtPoint(new Point(x, y));
				caret = ((ProjectionViewer) _sourceViewer).widgetOffset2ModelOffset(caret);
				if (caret > 0) {
					caret--;
				}
			} catch (final IllegalArgumentException e) {
				caret = -1;
			}
		}

		return caret;
	}

	@Override
	public void focusGained(final FocusEvent e) {
	}

	@Override
	public void focusLost(final FocusEvent e) {
		clearHyperlink();
		updateMousePointer();
	}

	@Override
	public void paintControl(final PaintEvent event) {
		try {
			final IRegion region = computeClippingRegion(event);
			if (region == null) {
				return;
			}

			final int startOfset = region.getOffset();
			final int length = region.getLength();

			for (final PaintableObject paintObj : _singleBracketsToPaint) {
				if (paintObj.getPosition().overlapsWith(startOfset, length)) {
					paintObj.paint(event.gc, _textWidget, _sourceViewer.getDocument(),
							getWidgetRange(paintObj.getPosition().getOffset(), paintObj.getPosition().getLength()),
							null);
				}
			}

			List<PaintableBracket> pairsToPaint;
			if (_hoveredPairsToPaint.isEmpty()) {
				pairsToPaint = _surroundingPairsToPaint;
			} else {
				pairsToPaint = _hoveredPairsToPaint;
			}

			for (final PaintableObject paintObj : pairsToPaint) {
				if (paintObj.getPosition().overlapsWith(startOfset, length)) {
					paintObj.paint(event.gc, _textWidget, _sourceViewer.getDocument(),
							getWidgetRange(paintObj.getPosition().getOffset(), paintObj.getPosition().getLength()),
							null);
				}
			}

			for (final PaintableObject paintObj : _singleStatementsToPaint) {
				if (paintObj.getPosition().overlapsWith(startOfset, length)) {
					paintObj.paint(event.gc, _textWidget, _sourceViewer.getDocument(),
							getWidgetRange(paintObj.getPosition().getOffset(), paintObj.getPosition().getLength()),
							null);
				}
			}

			List<PaintableStatement> statementsToPaint;
			if (_hoveredStatementsToPaint.isEmpty()) {
				statementsToPaint = _surroundingStatementsToPaint;
			} else {
				statementsToPaint = _hoveredStatementsToPaint;
			}

			for (final PaintableObject paintObj : statementsToPaint) {
				if (paintObj.getPosition().overlapsWith(startOfset, length)) {
					paintObj.paint(event.gc, _textWidget, _sourceViewer.getDocument(),
							getWidgetRange(paintObj.getPosition().getOffset(), paintObj.getPosition().getLength()),
							null);
				}
			}

			boolean hoveredHintPainted = false;
			for (PaintableHint paintObj : _hintsToPaint) {
				if (_hoveredHintToPaint != null && _hoveredHintToPaint.getPosition().equals(paintObj.getPosition())) {
					paintObj = _hoveredHintToPaint;
					hoveredHintPainted = true;
				}

				paintHint(paintObj, event);
			}

			if (!hoveredHintPainted && _hoveredHintToPaint != null) {
				paintHint(_hoveredHintToPaint, event);
			}
		} catch (final Exception e) {
			Activator.log(e);
		}
	}

	private void paintHint(final PaintableHint paintObj, final PaintEvent event) {
		final IRegion widgetRange = getWidgetRange(paintObj.getPosition().getOffset(),
				paintObj.getPosition().getLength());
		final Rectangle widgetRect = paintObj.getWidgetRect(event.gc, _textWidget, _sourceViewer.getDocument(),
				widgetRange);
		if (widgetRect != null && widgetRect.intersects(event.x, event.y, event.width, event.height)) {
			paintObj.paint(event.gc, _textWidget, _sourceViewer.getDocument(), widgetRange, widgetRect);
		}
	}

	public ITextViewer getTextViewer() {
		return _sourceViewer;
	}

	@Override
	public void configurationUpdated() {
		m_hyperlinkModifiers = _conf.getGeneralConfiguration().getHyperlinkModifiers();

		boolean updated = false;
		updated |= clearSurroundingPairsToPaint();
		updated |= clearSingleBracketsToPaint();
		updated |= clearSurroundingStatementsToPaint();
		updated |= clearSingleStatementsToPaint();

		rebuild(true, true, true, true, true, updated);
	}

	@Override
	public void containerUpdated(final boolean bracketsPairsTouched, final boolean singleBracketsTouched,
			final boolean hintsTouched, final boolean matchingStatementsTouched,
			final boolean singleStatementsTouched) {
		rebuild(bracketsPairsTouched, singleBracketsTouched, hintsTouched, matchingStatementsTouched,
				singleStatementsTouched, false);
	}

	/************************************************************
	 * IPainter interface
	 ************************************************************/

	@Override
	public void paint(final int reason) {
		if (!_isActive) {
			if (_sourceViewer == null) {
				Activator.log(Messages.BracketsHighlighter_UnableToPaint_SourceViewer);
				return;
			}

			_isActive = true;

			final StyledText st = _sourceViewer.getTextWidget();

			st.addCaretListener(this);
			st.addListener(SWT.MouseHover, this);
			st.addListener(SWT.MouseMove, this);

			st.addListener(SWT.MouseDown, this);

			st.addListener(SWT.KeyDown, this);
			st.addListener(SWT.KeyUp, this);
			st.addPaintListener(this);
			st.addFocusListener(this);

			_caretOffset = getCurrentCaretOffset();
		}
	}

	@Override
	public void deactivate(final boolean redraw) {
		if (!_isActive) {
			return;
		}

		_isActive = false;

		if (_sourceViewer == null) {
			return;
		}

		final StyledText st = _sourceViewer.getTextWidget();
		if (st == null) {
			return;
		}

		st.removeCaretListener(this);
		st.removeListener(SWT.MouseHover, this);
		st.removeListener(SWT.MouseMove, this);

		st.removeListener(SWT.MouseDown, this);

		st.removeListener(SWT.KeyDown, this);
		st.removeListener(SWT.KeyUp, this);
		st.removePaintListener(this);
		st.removeFocusListener(this);
	}

	@Override
	public void setPositionManager(final IPaintPositionManager manager) {
	}

	/************************************************************
	 * the work itself
	 ************************************************************/

	private void rebuild(final boolean bracketsPairsTouched, final boolean singleBracketsTouched,
			final boolean hintsTouched, final boolean matchingStatementsTouched, final boolean singleStatementsTouched,
			final boolean alwaysRedraw) {

		boolean update = alwaysRedraw;
		if (bracketsPairsTouched) {
			update |= updateSurroundingPairsToPaint(_caretOffset);
			update |= clearHoveredPairsToPaint();
		}
		if (singleBracketsTouched) {
			update |= updateSingleBrackets();
		}

		// I'm ignoring 'hintsTouched' because the "line distance" might have been
		// modified
		update |= updateHints();
		update |= clearHoveredHint();

		if (matchingStatementsTouched) {
			update |= updateSurroundingStatementsToPaint(_caretOffset);
			update |= clearHoveredStatementsToPaint();
		}

		if (singleStatementsTouched) {
			update |= updateSingleStatements();
		}

		if (update) {
			// TODO: optimize? (redraw only the needed sections)
			_textWidget.getDisplay().asyncExec(() -> {
				if (_textWidget != null) {
					_textWidget.redraw();
				}
			});
		}
	}

	private void updateMousePointer() {
		if (_mousePointingAtHint != null || _mousePointingAtBracket != null || _mousePointingAtStatement != null) {
			if (_mousePointerHand) {
				return;
			}
			_textWidget.setCursor(_textWidget.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
			_mousePointerHand = true;
		} else {
			if (!_mousePointerHand) {
				return;
			}
			_textWidget.setCursor(null);
			_mousePointerHand = false;
		}
	}

	private void clearHyperlink() {
		if (_mousePointingAtHint != null) {
			// GC gc = new GC(_textWidget);
			//
			// try
			// {
			// IRegion widgetRange =
			// getWidgetRange(_mousePointingAtHint.getPosition().getOffset(),
			// _mousePointingAtHint.getPosition().getLength());
			// Rectangle rect = _mousePointingAtHint.getWidgetRect(gc, _textWidget,
			// _sourceViewer.getDocument(), widgetRange);
			//
			// _mousePointingAtHint.setUnderline(false);
			// if( rect != null )
			// _textWidget.redraw(rect.x, rect.y, rect.width, rect.height, true);
			// else
			// _textWidget.redraw();
			// _mousePointingAtHint = null;
			// }
			// finally
			// {
			// gc.dispose();
			// }

			// unoptimize... (I'm not sure that the code above, which works, doesn't take
			// more time...)
			_mousePointingAtHint.setUnderline(false);
			_textWidget.redraw();
			_mousePointingAtHint = null;

		}

		if (_mousePointingAtBracket != null) {
			clearHoveredPairsToPaint();
			_textWidget.redraw();
			_mousePointingAtBracket = null;
		}

		if (_mousePointingAtStatement != null) {
			clearHoveredStatementsToPaint();
			_textWidget.redraw();
			_mousePointingAtStatement = null;
		}
	}

	private void mousePointingAt(final int x, final int y) {
		int caret = -1;

		final GC gc = new GC(_textWidget);

		try {
			caret = getDocCarretAdvanced(gc, x, y);

			if (_mousePointingAtBracket != null) {
				final Position pos = _mousePointingAtBracket.getPosition();
				if (pos != null && pos.getOffset() == caret) {
					return;
				}

				_mousePointingAtBracket = null;
				clearHoveredPairsToPaint();
				clearHoveredHint();
//				TODO: check this
//				clearPopup();

				// TODO: optimize? (redraw only the needed sections)
				_textWidget.redraw();
			}

			if (_mousePointingAtStatement != null) {
				final Position pos = _mousePointingAtStatement.getPosition();
				if (pos != null && pos.getOffset() == caret) {
					return;
				}

				_mousePointingAtStatement = null;
				clearHoveredStatementsToPaint();
				clearHoveredHint();
				clearPopup();

				// TODO: optimize? (redraw only the needed sections)
				_textWidget.redraw();
			}

			if (_mousePointingAtHint != null) {
				final IRegion widgetRange = getWidgetRange(_mousePointingAtHint.getPosition().getOffset(),
						_mousePointingAtHint.getPosition().getLength());
				final Rectangle rect = _mousePointingAtHint.getWidgetRect(gc, _textWidget, _sourceViewer.getDocument(),
						widgetRange);

				if (rect != null && rect.intersects(x, y, 1, 1)) {
					return;
				}

				_mousePointingAtHint.setUnderline(false);
				_textWidget.redraw();
				_mousePointingAtHint = null;
			}

			for (final PaintableHint paintObj : _hintsToPaint) {
				final IRegion widgetRange = getWidgetRange(paintObj.getPosition().getOffset(),
						paintObj.getPosition().getLength());
				final Rectangle rect = paintObj.getWidgetRect(gc, _textWidget, _sourceViewer.getDocument(),
						widgetRange);
				if (rect != null && rect.intersects(x, y, 1, 1)) {
					_mousePointingAtHint = paintObj;
					_mousePointingAtHint.setUnderline(true);
					_textWidget.redraw(rect.x, rect.y, rect.width, rect.height, true);
					return;
				}
			}
		} finally {
			gc.dispose();
		}

		final BracketeerProcessingContainer cont = _processingThread.getBracketContainer();
		final List<BracketsPair> pairs = cont.getMatchingPairs(caret, 1);
		final List<MatchingStatements> statements = cont.getMatchingStatements(caret, 1);
		Assert.isTrue(pairs.size() <= 1);
		if (pairs.size() == 0) {
			return;
		}

		final BracketsPair pair = pairs.get(0);
		Position brPos = pair.getOpeningBracket().getPosition();
		if (brPos != null && brPos.getOffset() == caret) {
			_mousePointingAtBracket = pair.getOpeningBracket();
		}
		brPos = pair.getClosingBracket().getPosition();
		if (brPos != null && brPos.getOffset() == caret) {
			_mousePointingAtBracket = pair.getClosingBracket();
		}

		final MatchingStatements statement = statements.get(0);
		Position stPos = statement.getOpeningStatement().getPosition();
		if (stPos != null && stPos.getOffset() == caret) {
			_mousePointingAtStatement = statement.getOpeningStatement();
		}
		stPos = statement.getClosingStatement().getPosition();
		if (stPos != null && stPos.getOffset() == caret) {
			_mousePointingAtStatement = statement.getClosingStatement();
		}

		if (_mousePointingAtBracket == null && _mousePointingAtStatement == null) {
			Activator.log(Messages.BracketsHighlighter_ErrBracketNotFound);
			return;
		}

		if (_mousePointingAtStatement == null) {
			synchronized (_hoveredPairsToPaint) {
				addPaintableObjectsPairs(pairs, 0, 1, _hoveredPairsToPaint);
			}
		} else {
			synchronized (_hoveredStatementsToPaint) {
				addPaintableObjectsPairs(statements, 0, 1, _hoveredStatementsToPaint);
			}
		}

		// TODO: optimize? (redraw only the needed sections)
		_textWidget.redraw();
	}

	private void jumpToPosition(final Position pos) {
		if (pos == null) {
			return;
		}

		_sourceViewer.setSelectedRange(pos.getOffset(), 0);
		_sourceViewer.revealRange(pos.getOffset(), 0);
	}

	private void caretMovedTo(final int caretOffset) {
		boolean update = updateSurroundingPairsToPaint(caretOffset);
		update |= updateSurroundingStatementsToPaint(caretOffset);
		update |= clearHoveredPairsToPaint();
		update |= clearHoveredHint();
		clearPopup();

		if (update) {
			// TODO: optimize? (redraw only the needed sections)
			_textWidget.redraw();
		}
	}

	private boolean updateSurroundingPairsToPaint(final int caretOffset) {
		if (!_conf.getPairConfiguration().isSurroundingPairsEnabled()) {
			return clearSurroundingPairsToPaint();
		}

		final BracketeerProcessingContainer cont = _processingThread.getBracketContainer();
		List<? extends MatchObject> listOfPairs = cont.getPairsSurrounding(caretOffset);

		/* excluding... */
		final String includedPairs = _conf.getPairConfiguration().getSurroundingPairsToInclude();
		final Iterator<? extends MatchObject> it = listOfPairs.iterator();
		while (it.hasNext()) {
			final BracketsPair pair = (BracketsPair) it.next();
			for (final SingleBracket br : pair.getBrackets()) {
				if (includedPairs.indexOf(br.getChar()) == -1) {
					it.remove();
					break;
				}
			}

			if (pair.getDistanceBetweenBrackets() - 1 < _conf.getPairConfiguration().getMinDistanceBetweenBrackets()) {
				it.remove();
			}
		}

		listOfPairs = sortPairs(listOfPairs);
		listOfPairs = listOfPairs.subList(0,
				Math.min(_conf.getPairConfiguration().getSurroundingPairsCount(), listOfPairs.size()));

		// do nothing if _surroundingPairsToPaint is equal to listOfPairs
		if (areEqualPairs(listOfPairs, _surroundingPairsToPaint)) {
			return false;
		}

		clearSurroundingPairsToPaint();
		synchronized (_surroundingPairsToPaint) {
			addPaintableObjectsPairs(listOfPairs, 0, 1, _surroundingPairsToPaint);
		}

		return true;
	}

	private boolean updateSurroundingStatementsToPaint(final int caretOffset) {
		if (!_conf.getMatchingStatementsConfiguration().isSurroundingStatementsEnabled()) {
			return clearSurroundingStatementsToPaint();
		}

		final BracketeerProcessingContainer cont = _processingThread.getBracketContainer();
		List<? extends MatchObject> listOfStatements = cont.getStatementsSurrounding(caretOffset);

		/* excluding... */
//		TODO:check this
//		final String includedStatements = _conf.getMatchingStatementsConfiguration()
//				.getSurroundingStatementsToInclude();
		final Iterator<? extends MatchObject> it = listOfStatements.iterator();
		while (it.hasNext()) {
			final MatchingStatements statements = (MatchingStatements) it.next();
			for (final SingleStatement br : statements.getStatements()) {
				if (includedStatements.indexOf(br.getChar()) == -1) {
					it.remove();
					break;
				}
			}

			if (statements.getDistanceBetweenStatements() - 1 < _conf.getMatchingStatementsConfiguration()
					.getMinDistanceBetweenStatements()) {
				it.remove();
			}
		}

		listOfStatements = sortPairs(listOfStatements);
		listOfStatements = listOfStatements.subList(0, Math.min(
				_conf.getMatchingStatementsConfiguration().getSurroundingStatementsCount(), listOfStatements.size()));

		// do nothing if _surroundingPairsToPaint is equal to listOfPairs
		if (areEqualPairs(listOfStatements, _surroundingPairsToPaint)) {
			return false;
		}

		clearSurroundingPairsToPaint();
		synchronized (_surroundingPairsToPaint) {
			addPaintableObjectsPairs(listOfStatements, 0, 1, _surroundingPairsToPaint);
		}

		return true;
	}

	private boolean updateSingleBrackets() {
		final BracketeerProcessingContainer cont = _processingThread.getBracketContainer();
		final List<SingleBracket> list = cont.getSingleBrackets();

		// do nothing if _surroundingPairsToPaint is equal to listOfPairs
		if (areEqualSingle(list, _singleBracketsToPaint)) {
			return false;
		}

		clearSingleBracketsToPaint();
		synchronized (_singleBracketsToPaint) {
			addPaintableObjectsSingles(list, _singleBracketsToPaint);
		}

		return true;
	}

	private boolean updateSingleStatements() {
		final BracketeerProcessingContainer cont = _processingThread.getBracketContainer();
		final List<SingleStatement> list = cont.getSingleStatements();

		// do nothing if _surroundingPairsToPaint is equal to listOfPairs
		if (areEqualSingle(list, _singleStatementsToPaint)) {
			return false;
		}

		clearSingleStatementsToPaint();
		synchronized (_singleStatementsToPaint) {
			addPaintableObjectsSingles(list, _singleStatementsToPaint);
		}

		return true;
	}

	private boolean updateHints() {
		final BracketeerProcessingContainer cont = _processingThread.getBracketContainer();

		final ArrayList<PaintableHint> hintsToPaint = new ArrayList<>();
		final HintConfiguration conf = _conf.getHintConfiguration();
		final IDocument doc = _doc;

		for (final Hint hint : cont.getHints()) {
			final String type = hint.getType();
			if (!_conf.getHintConfiguration().isShowInEditor(type)) {
				continue;
			}

			int originLine, drawLine;
			try {
				originLine = doc.getLineOfOffset(hint.getOriginPositionRaw().getOffset());
				drawLine = doc.getLineOfOffset(hint.getHintPositionRaw().getOffset());
			} catch (final BadLocationException e) {
				continue;
			}

			if (drawLine - originLine < conf.getMinLineDistance(type)) {
				continue;
			}

			final PaintableHint pHint = new PaintableHint(hint.getHintPositionRaw(), conf.getColor(type, true),
					conf.getColor(type, false), conf.isItalic(type), conf.formatText(type, hint.getTxt()));

			hintsToPaint.add(pHint);
		}

		if (_hintsToPaint.equals(hintsToPaint)) {
			return false;
		}

		synchronized (_hintsToPaint) {
			_hintsToPaint = hintsToPaint;
		}

		return true;
	}

	private List<MatchObject> sortPairs(final List<? extends MatchObject> listOfbjects) {
		final List<MatchObject> ret = new ArrayList<>(listOfbjects.size());

		for (final MatchObject object : listOfbjects) {
			int i = 0;
			while (i < ret.size()) {
				if (ret.get(i).getOpeningObject().getPositionRaw().offset < object.getOpeningObject()
						.getPositionRaw().offset) {
					break;
				}
				i++;
			}
			ret.add(i, object);
		}

		return ret;
	}

	/*
	 * Return true iff the hover is not empty...
	 */
	private boolean mouseHoverAt(final StyledText st, final int origCaret) {
		boolean ret = markHoveredBrackets(origCaret);
		ret |= markHoveredStatements(origCaret);
		ret |= showHoveredHint(origCaret);
		ret |= showPopup(origCaret);
		return ret;
	}

	private boolean showPopup(final int origCaret) {
		clearPopup();
		if (!_conf.getMatchingStatementsConfiguration().isPopupEnabled()) {
			return false;
		}

		try {
			if (_conf.getMatchingStatementsConfiguration().showPopupOnlyWithoutHint() && _hoveredHintToPaint != null
					&& _hoveredHintToPaint.isOkToShow(_doc)) {
				return false;
			}
		} catch (final BadLocationException e) {
			return false;
		}

		final BracketeerProcessingContainer cont = _processingThread.getBracketContainer();
		final List<MatchingStatements> listOfStatements = cont.getMatchingStatements(origCaret, 1);
		if (listOfStatements.isEmpty()) {
			return false;
		}

		final MatchingStatements statements = listOfStatements.get(0);
		Position pos = statements.getClosingStatement().getPosition();
//		TODO: check this
//		if (pos == null || !pos.overlapsWith(origCaret, 2) || statements.getClosingStatement().getChar() != '}') {
//			return false;
//		}

		pos = statements.getOpeningStatement().getPosition();
		// this this statement visible?
		if (pos == null || getInclusiveTopIndexStartOffset() < pos.getOffset()) {
			return false;
		}

		PaintableStatement paintStatement = null;
		synchronized (_hoveredStatementsToPaint) {
			for (final PaintableStatement paintableStatement : _hoveredStatementsToPaint) {
				if (paintableStatement.getPosition().equals(pos)) {
					paintStatement = paintableStatement;
					break;
				}
			}
		}

		if (paintStatement == null) {
			Activator.log(Messages.BracketsHighlighter_MatchNotHighlighetd);
			return false;
		}

		try {
			_popup = new Popup(_sourceViewer, _textWidget, _doc, paintStatement);
		} catch (final BadLocationException e) {
			_popup = null;
			return false;
		}

		return true;
	}

	private boolean markHoveredBrackets(final int origCaret) {

		// int startPoint = Math.max(0, origCaret - 2);
		// int endPoint = Math.min(_sourceViewer.getDocument().getLength(),
		// origCaret + 2);

		if (!_conf.getPairConfiguration().isHoveredPairsEnabled()) {
			return false;
		}

		final int length = 4;
		final int startPoint = origCaret - 2;

		final BracketeerProcessingContainer cont = _processingThread.getBracketContainer();
		List<? extends MatchObject> listOfPairs = cont.getMatchingPairs(startPoint, length);
		listOfPairs = sortPairs(listOfPairs);

		if (listOfPairs.isEmpty()) {
			return false;
		}

		// do nothing if _hoveredPairsToPaint is equal to listOfPairs
		if (areEqualPairs(listOfPairs, _hoveredPairsToPaint)) {
			return true;
		}

		clearHoveredPairsToPaint();
		synchronized (_hoveredPairsToPaint) {
			addPaintableObjectsPairs(listOfPairs, 0, 1, _hoveredPairsToPaint);
		}

		// TODO: optimize? (redraw only the needed sections)
		_textWidget.redraw();

		// drawHighlights();
		return true;
	}

	private boolean markHoveredStatements(final int origCaret) {

		// int startPoint = Math.max(0, origCaret - 2);
		// int endPoint = Math.min(_sourceViewer.getDocument().getLength(),
		// origCaret + 2);

		if (!_conf.getMatchingStatementsConfiguration().isHoveredPairsEnabled()) {
			return false;
		}

		final int length = 4;
		final int startPoint = origCaret - 2;

		final BracketeerProcessingContainer cont = _processingThread.getBracketContainer();
		List<? extends MatchObject> listOfStatements = cont.getMatchingStatements(startPoint, length);
		listOfStatements = sortPairs(listOfStatements);

		if (listOfStatements.isEmpty()) {
			return false;
		}

		// do nothing if _hoveredPairsToPaint is equal to listOfPairs
		if (areEqualPairs(listOfStatements, _hoveredStatementsToPaint)) {
			return true;
		}

		clearHoveredPairsToPaint();
		synchronized (_hoveredPairsToPaint) {
			addPaintableObjectsPairs(listOfStatements, 0, 1, _hoveredStatementsToPaint);
		}

		// TODO: optimize? (redraw only the needed sections)
		_textWidget.redraw();

		// drawHighlights();
		return true;
	}

	private boolean showHoveredHint(final int origCaret) {
		final BracketeerProcessingContainer cont = _processingThread.getBracketContainer();
		Hint hint = cont.getHint(origCaret);
		final HintConfiguration conf = _conf.getHintConfiguration();
		if (!conf.isShowOnHover()) {
			hint = null;
		}

		PaintableHint hintToPaint = null;
		if (hint != null) {
			final String type = hint.getType();
			hintToPaint = new PaintableHint(hint.getHintPositionRaw(), conf.getColor(type, true),
					conf.getColor(type, false), conf.isItalic(type), conf.formatTextHovered(type, hint.getTxt()));
		}

		boolean redraw = false;
		if (_hoveredHintToPaint == null && hintToPaint != null
				|| _hoveredHintToPaint != null && !_hoveredHintToPaint.equals(hintToPaint)) {
			redraw = true;
		}

		_hoveredHintToPaint = hintToPaint;

		if (redraw) {
			// TODO: optimize? (redraw only the needed sections)
			_textWidget.redraw();
		}

		return hint != null;
	}

	private boolean areEqualPairs(final List<? extends MatchObject> listOfobjects,
			final List<? extends PaintableObject> objectsToPaint) {
		if (listOfobjects.size() * 2 != objectsToPaint.size()) {
			return false;
		}

		for (final MatchObject objects : listOfobjects) {
			for (final SingleObject object : objects.getObjects()) {
				boolean found = false;
				for (final PaintableObject paintableObject : objectsToPaint) {
					if (paintableObject.getPosition().equals(object.getPositionRaw())) {
						found = true;
						break;
					}
				}
				if (!found) {
					return false;
				}
			}
		}

		return true;
	}

	private boolean areEqualSingle(final List<? extends SingleObject> list,
			final List<? extends PaintableObject> singlesToPaint) {
		if (list.size() != singlesToPaint.size()) {
			return false;
		}

		for (final SingleObject object : list) {
			boolean found = false;
			for (final PaintableObject paintableObject : singlesToPaint) {
				if (paintableObject.getPosition().equals(object.getPositionRaw())) {
					found = true;
					break;
				}
			}
			if (!found) {
				return false;
			}
		}

		return true;
	}

	private void addPaintableObjectsPairs(final List<? extends MatchObject> listOfObjects, int colorCode,
			final int colorCodeStep, final List<? extends PaintableObject> paintableObjectsList) {
		for (final MatchObject matchingObjects : listOfObjects) {
			for (final SingleObject singleObject : matchingObjects.getObjects()) {
				final Position pos = singleObject.getPositionRaw();
				final RGB fg = _conf.getPairConfiguration().getColor(true, colorCode);
				final RGB bg = _conf.getPairConfiguration().getColor(false, colorCode);
				final String highlightType = _conf.getPairConfiguration().getHighlightType(colorCode);
				if (singleObject instanceof SingleStatement) {
					paintableObjectsList.add(new PaintableStatement(pos, fg, bg, highlightType));
				} else {
					paintableObjectsList.add(new PaintableBracket(pos, fg, bg, highlightType));
				}

			}
			colorCode += colorCodeStep;
		}
	}

	private void addPaintableObjectsSingles(final List<? extends SingleObject> listOfSingles,
			final List<? extends PaintableObject> paintableObjectsList) {
		boolean annotateIsSet;
		final Map<Annotation, Position> newMap = new HashMap<>();
		for (final SingleObject object : listOfSingles) {
			final Position pos = object.getPositionRaw();
			final RGB fg = _conf.getSingleBracketConfiguration().getColor(true);
			final RGB bg = _conf.getSingleBracketConfiguration().getColor(false);
			final String highlightType = _conf.getSingleBracketConfiguration().getHighlightType();
			if (object instanceof SingleStatement) {
				paintableObjectsList.add(new PaintableStatement(pos, fg, bg, highlightType));
				annotateIsSet = _conf.getSingleStatementConfiguration().getAnnotate();
			} else {
				paintableObjectsList.add(new PaintableBracket(pos, fg, bg, highlightType));
				annotateIsSet = _conf.getSingleBracketConfiguration().getAnnotate();
			}

			if (annotateIsSet && _resource != null && _annotationMap != null) {
				try {
					final IMarker marker = _resource
							.createMarker("me.glindholm.plugin.bracketeer2.unmatchedBracket.marker"); //$NON-NLS-1$

					final SimpleMarkerAnnotation ma = new SimpleMarkerAnnotation(
							"me.glindholm.plugin.bracketeer2.unmatchedBracket.annotation", //$NON-NLS-1$
							marker);

					final Position newPos = new Position(pos.getOffset());
					newMap.put(ma, newPos);
				} catch (final CoreException e) {
					Activator.log(e);
				}
			}
		}

		final Set<Annotation> oldKeySet = _annotationMap.keySet();
		if (!oldKeySet.isEmpty() || !newMap.isEmpty()) {
			_annotationModel.connect(_doc);

			if (_annotationModel instanceof IAnnotationModelExtension) {
				((IAnnotationModelExtension) _annotationModel)
						.replaceAnnotations(oldKeySet.toArray(new Annotation[oldKeySet.size()]), newMap);
			} else {
				for (final Annotation annotation : oldKeySet) {
					_annotationModel.removeAnnotation(annotation);
				}
				for (final Entry<Annotation, Position> mapEntry : newMap.entrySet()) {
					_annotationModel.addAnnotation(mapEntry.getKey(), mapEntry.getValue());
				}
			}

			_annotationMap = newMap;
			_annotationModel.disconnect(_doc);
		}
	}

	private void clearPopup() {
		if (_popup == null) {
			return;
		}

		_popup.dispose();
		_popup = null;
	}

	private boolean clearHoveredPairsToPaint() {
		synchronized (_hoveredPairsToPaint) {
			if (!_hoveredPairsToPaint.isEmpty()) {
				_hoveredPairsToPaint.clear();
				return true;
			}
		}
		return false;
	}

	private boolean clearHoveredHint() {
		final boolean ret = _hoveredHintToPaint != null;
		_hoveredHintToPaint = null;
		return ret;
	}

	private boolean clearSurroundingPairsToPaint() {
		synchronized (_surroundingPairsToPaint) {
			if (!_surroundingPairsToPaint.isEmpty()) {
				_surroundingPairsToPaint.clear();
				return true;
			}
		}
		return false;
	}

	private boolean clearSingleBracketsToPaint() {
		synchronized (_singleBracketsToPaint) {
			if (!_singleBracketsToPaint.isEmpty()) {
				_singleBracketsToPaint.clear();
				return true;
			}
		}
		return false;
	}

	private boolean clearHoveredStatementsToPaint() {
		synchronized (_hoveredStatementsToPaint) {
			if (!_hoveredStatementsToPaint.isEmpty()) {
				_hoveredStatementsToPaint.clear();
				return true;
			}
		}
		return false;
	}

	private boolean clearSurroundingStatementsToPaint() {
		synchronized (_surroundingStatementsToPaint) {
			if (!_surroundingStatementsToPaint.isEmpty()) {
				_surroundingStatementsToPaint.clear();
				return true;
			}
		}
		return false;
	}

	private boolean clearSingleStatementsToPaint() {
		synchronized (_singleStatementsToPaint) {
			if (!_singleStatementsToPaint.isEmpty()) {
				_singleStatementsToPaint.clear();
				return true;
			}
		}
		return false;
	}

	/**
	 * (Copied from AnnotationPainter)
	 *
	 * Computes the model (document) region that is covered by the paint event's
	 * clipping region. If <code>event</code> is <code>null</code>, the model range
	 * covered by the visible editor area (viewport) is returned.
	 *
	 * @param event      the paint event or <code>null</code> to use the entire
	 *                   viewport
	 * @param isClearing tells whether the clipping is need for clearing an
	 *                   annotation
	 * @return the model region comprised by either the paint event's clipping
	 *         region or the viewport
	 * @since 3.2
	 */
	private IRegion computeClippingRegion(final PaintEvent event) {
		if (event == null) {

			// trigger a repaint of the entire viewport
			final int vOffset = getInclusiveTopIndexStartOffset();
			if (vOffset == -1) {
				return null;
			}

			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=17147
			final int vLength = getExclusiveBottomIndexEndOffset() - vOffset;

			return new Region(vOffset, vLength);
		}

		int widgetOffset;
		try {
			final int widgetClippingStartOffset = _textWidget.getOffsetAtPoint(new Point(0, event.y));
			final int firstWidgetLine = _textWidget.getLineAtOffset(widgetClippingStartOffset);
			widgetOffset = _textWidget.getOffsetAtLine(firstWidgetLine);
		} catch (final IllegalArgumentException ex1) {
			try {
				final int firstVisibleLine = JFaceTextUtil.getPartialTopIndex(_textWidget);
				widgetOffset = _textWidget.getOffsetAtLine(firstVisibleLine);
			} catch (final IllegalArgumentException ex2) { // above try code might fail too
				widgetOffset = 0;
			}
		}

		int widgetEndOffset;
		try {
			final int widgetClippingEndOffset = _textWidget.getOffsetAtPoint(new Point(0, event.y + event.height));
			final int lastWidgetLine = _textWidget.getLineAtOffset(widgetClippingEndOffset);
			widgetEndOffset = _textWidget.getOffsetAtLine(lastWidgetLine + 1);
		} catch (final IllegalArgumentException ex1) {
			// happens if the editor is not "full", e.g. the last line of the document is
			// visible in the editor
			try {
				final int lastVisibleLine = JFaceTextUtil.getPartialBottomIndex(_textWidget);
				if (lastVisibleLine == _textWidget.getLineCount() - 1) {
					// last line
					widgetEndOffset = _textWidget.getCharCount();
				} else {
					widgetEndOffset = _textWidget.getOffsetAtLine(lastVisibleLine + 1) - 1;
				}
			} catch (final IllegalArgumentException ex2) { // above try code might fail too
				widgetEndOffset = _textWidget.getCharCount();
			}
		}

		final IRegion clippingRegion = getModelRange(widgetOffset, widgetEndOffset - widgetOffset);

		return clippingRegion;
	}

	/**
	 * Returns the document offset of the upper left corner of the source viewer's
	 * view port, possibly including partially visible lines.
	 *
	 * @return the document offset if the upper left corner of the view port
	 */
	private int getInclusiveTopIndexStartOffset() {

		if (_textWidget != null && !_textWidget.isDisposed()) {
			final int top = JFaceTextUtil.getPartialTopIndex(_sourceViewer);
			try {
				final IDocument document = _sourceViewer.getDocument();
				return document.getLineOffset(top);
			} catch (final BadLocationException x) {
			}
		}

		return -1;
	}

	/**
	 * Returns the first invisible document offset of the lower right corner of the
	 * source viewer's view port, possibly including partially visible lines.
	 *
	 * @return the first invisible document offset of the lower right corner of the
	 *         view port
	 */
	private int getExclusiveBottomIndexEndOffset() {

		if (_textWidget != null && !_textWidget.isDisposed()) {
			int bottom = JFaceTextUtil.getPartialBottomIndex(_sourceViewer);
			try {
				final IDocument document = _sourceViewer.getDocument();

				if (bottom >= document.getNumberOfLines()) {
					bottom = document.getNumberOfLines() - 1;
				}

				return document.getLineOffset(bottom) + document.getLineLength(bottom);
			} catch (final BadLocationException x) {
			}
		}

		return -1;
	}

	/**
	 * Returns the model region that corresponds to the given region in the viewer's
	 * text widget.
	 *
	 * @param offset the offset in the viewer's widget
	 * @param length the length in the viewer's widget
	 * @return the corresponding document region
	 * @since 3.2
	 */
	private IRegion getModelRange(final int offset, final int length) {
		if (offset == Integer.MAX_VALUE) {
			return null;
		}

		if (_sourceViewer instanceof final ITextViewerExtension5 extension) {
			return extension.widgetRange2ModelRange(new Region(offset, length));
		}

		final IRegion region = _sourceViewer.getVisibleRegion();
		return new Region(region.getOffset() + offset, length);
	}

	private IRegion getWidgetRange(final int offset, final int length) {
		return TextUtils.getWidgetRange(_sourceViewer, offset, length);
	}

	private int getDistanceBetween(final Point p1, final Point p2) {
		return (int) Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
	}

	private int getCurrentCaretOffset() {
		int caret = _textWidget.getCaretOffset();
		caret = ((ProjectionViewer) _sourceViewer).widgetOffset2ModelOffset(caret);
		caret -= 1;
		return caret;
	}

}
