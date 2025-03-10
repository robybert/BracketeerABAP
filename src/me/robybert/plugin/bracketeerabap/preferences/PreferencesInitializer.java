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
package me.robybert.plugin.bracketeerabap.preferences;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;

import me.robybert.plugin.bracketeerabap.Activator;
import me.robybert.plugin.bracketeerabap.core.ProcessorsRegistry;

public class PreferencesInitializer extends AbstractPreferenceInitializer {
	@Override
	public void initializeDefaultPreferences() {
		final IPreferenceStore store = Activator.getDefault().getPreferenceStore();

		final IConfigurationElement[] config = Platform.getExtensionRegistry()
				.getConfigurationElementsFor(ProcessorsRegistry.PROC_FACTORY_ID);

		for (final IConfigurationElement element : config) {
			final String pluginName = element.getAttribute("name"); //$NON-NLS-1$

			defualtHighlights(store, element, pluginName);
			defualtHints(store, element, pluginName);
		}

		final IPreferenceStore editorsStore = EditorsUI.getPreferenceStore();
		store.setDefault(PreferencesConstants.General.HYPERLINK_MODIFIERS,
				editorsStore.getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_HYPERLINK_KEY_MODIFIER_MASK));
	}

	private void defualtHints(final IPreferenceStore store, final IConfigurationElement element,
			final String pluginName) {
		String prefBase = PreferencesConstants.preferencePath(pluginName);
		store.setDefault(prefBase + PreferencesConstants.Hints.Globals.SHOW_IN_EDITOR, true);

		store.setDefault(prefBase + PreferencesConstants.Hints.Hover.ENABLE, true);
		store.setDefault(prefBase + PreferencesConstants.Hints.Hover.MAX_LEN, 80);

		String hintType = PreferencesConstants.Hints.DEFAULT_TYPE;
		prefBase = PreferencesConstants.preferencePath(pluginName)
				+ PreferencesConstants.Hints.preferencePath(hintType);

		store.setDefault(prefBase + PreferencesConstants.Hints.WhenToShow.SHOW_IN_EDITOR, true);
		store.setDefault(prefBase + PreferencesConstants.Hints.WhenToShow.MIN_LINES_DISTANCE, 15);

		store.setDefault(prefBase + PreferencesConstants.Hints.Font.FG_DEFAULT, false);
		PreferenceConverter.setDefault(store, prefBase + PreferencesConstants.Hints.Font.FG_COLOR,
				new RGB(160, 160, 160));
		store.setDefault(prefBase + PreferencesConstants.Hints.Font.BG_DEFAULT, true);

		store.setDefault(prefBase + PreferencesConstants.Hints.Display.Ellipsis.ATTR,
				PreferencesConstants.Hints.Display.Ellipsis.VAL_MID);
		store.setDefault(prefBase + PreferencesConstants.Hints.Display.MAX_LENGTH, 30);
		store.setDefault(prefBase + PreferencesConstants.Hints.Display.STRIP_WHITESPACE, false);

		final String[] prefsToCopyFromDef = { PreferencesConstants.Hints.WhenToShow.SHOW_IN_EDITOR,
				PreferencesConstants.Hints.WhenToShow.MIN_LINES_DISTANCE, PreferencesConstants.Hints.Font.FG_DEFAULT,
				PreferencesConstants.Hints.Font.FG_COLOR, PreferencesConstants.Hints.Font.BG_DEFAULT,
				PreferencesConstants.Hints.Display.Ellipsis.ATTR, PreferencesConstants.Hints.Display.MAX_LENGTH,
				PreferencesConstants.Hints.Display.STRIP_WHITESPACE };

		final IConfigurationElement[] hints = element.getChildren("Hint"); //$NON-NLS-1$
		for (final IConfigurationElement hint : hints) {
			hintType = hint.getAttribute("type"); //$NON-NLS-1$
			prefBase = PreferencesConstants.preferencePath(pluginName)
					+ PreferencesConstants.Hints.preferencePath(hintType);
			final String defBase = PreferencesConstants.preferencePath(pluginName)
					+ PreferencesConstants.Hints.preferencePath(PreferencesConstants.Hints.DEFAULT_TYPE);

			store.setDefault(prefBase + PreferencesConstants.Hints.WhenToShow.USE_DEFAULT, true);
			store.setDefault(prefBase + PreferencesConstants.Hints.Font.USE_DEFAULT, true);
			store.setDefault(prefBase + PreferencesConstants.Hints.Display.USE_DEFAULT, true);

			for (final String pref : prefsToCopyFromDef) {
				store.setDefault(prefBase + pref, store.getDefaultString(defBase + pref));
			}
		}
	}

	private void defualtHighlights(final IPreferenceStore store, final IConfigurationElement element,
			final String pluginName) {
		/* the default */

		store.setDefault(PreferencesConstants.preferencePath(pluginName)
				+ PreferencesConstants.Brackets.Highlights.getAttrPath(0, true)
				+ PreferencesConstants.Brackets.Highlights.UseDefault, false);

		PreferenceConverter.setDefault(store,
				PreferencesConstants.preferencePath(pluginName)
						+ PreferencesConstants.Brackets.Highlights.getAttrPath(0, true)
						+ PreferencesConstants.Brackets.Highlights.Color,
				new RGB(255, 255, 255));

		store.setDefault(PreferencesConstants.preferencePath(pluginName)
				+ PreferencesConstants.Brackets.Highlights.getAttrPath(0, false)
				+ PreferencesConstants.Brackets.Highlights.UseDefault, true);

		/* the brackets... */

		for (int i = 1; i < PreferencesConstants.MAX_PAIRS + 2; i++) {

			store.setDefault(PreferencesConstants.preferencePath(pluginName)
					+ PreferencesConstants.Brackets.Highlights.getAttrPath(i, true)
					+ PreferencesConstants.Brackets.Highlights.UseDefault, true);

			store.setDefault(PreferencesConstants.preferencePath(pluginName)
					+ PreferencesConstants.Brackets.Highlights.getAttrPath(i, false)
					+ PreferencesConstants.Brackets.Highlights.UseDefault, false);

			if (i == PreferencesConstants.MAX_PAIRS + 1) {
				PreferenceConverter.setDefault(store,
						PreferencesConstants.preferencePath(pluginName)
								+ PreferencesConstants.Brackets.Highlights.getAttrPath(i, false)
								+ PreferencesConstants.Brackets.Highlights.Color,
						new RGB(250, 0, 0));
			} else {
				final int max = PreferencesConstants.MAX_PAIRS;
				// int val = (((max-i)+1)*255)/(max+1);
				final int val = i * 255 / (max + 1);
				PreferenceConverter.setDefault(store,
						PreferencesConstants.preferencePath(pluginName)
								+ PreferencesConstants.Brackets.Highlights.getAttrPath(i, false)
								+ PreferencesConstants.Brackets.Highlights.Color,
						new RGB(val, val, val));
			}

			store.setDefault(
					PreferencesConstants.preferencePath(pluginName)
							+ PreferencesConstants.Brackets.Highlights.getAttrPath(i, false)
							+ PreferencesConstants.Brackets.Highlights.HighlightTypeAttr,
					PreferencesConstants.Brackets.Highlights.HighlightTypeValSolid);

			// TODO: get the real editor background default
			/*
			 * Color def = Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
			 * PreferenceConverter.setDefault(store,
			 * PreferencesConstants.preferencePath(pluginName) +
			 * PreferencesConstants.Highlights.getAttrPath(i, false) +
			 * PreferencesConstants.Highlights.Color, def.getRGB());
			 */

			// TODO: get the real editor background default
			/*
			 * def = Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND);
			 * PreferenceConverter.setDefault(store,
			 * PreferencesConstants.preferencePath(pluginName) +
			 * PreferencesConstants.Highlights.getAttrPath(i, false) +
			 * PreferencesConstants.Highlights.Color, def.getRGB());
			 */
		}

		store.setDefault(
				PreferencesConstants.preferencePath(pluginName) + PreferencesConstants.Brackets.Annotations.Enable,
				true);

		store.setDefault(
				PreferencesConstants.preferencePath(pluginName) + PreferencesConstants.Brackets.Surrounding.Enable,
				true);
		store.setDefault(PreferencesConstants.preferencePath(pluginName)
				+ PreferencesConstants.Brackets.Surrounding.NumBracketsToShow, PreferencesConstants.MAX_PAIRS);
		store.setDefault(PreferencesConstants.preferencePath(pluginName)
				+ PreferencesConstants.Brackets.Surrounding.MinDistanceBetweenBrackets, 1);
		store.setDefault(
				PreferencesConstants.preferencePath(pluginName)
						+ PreferencesConstants.Brackets.Surrounding.ShowBrackets,
				element.getAttribute(ProcessorsRegistry.SUPPORTED_BRACKETS_ATTR));

		store.setDefault(
				PreferencesConstants.preferencePath(pluginName) + PreferencesConstants.Brackets.Hovering.Enable, true);
		store.setDefault(
				PreferencesConstants.preferencePath(pluginName) + PreferencesConstants.Brackets.Hovering.PopupEnable,
				true);
		store.setDefault(PreferencesConstants.preferencePath(pluginName)
				+ PreferencesConstants.Brackets.Hovering.PopupOnlyWithoutHint, true);
	}
}
