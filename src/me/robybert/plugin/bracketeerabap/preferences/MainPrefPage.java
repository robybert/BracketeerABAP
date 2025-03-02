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
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wb.swt.FieldLayoutPreferencePage;

import me.robybert.plugin.bracketeerabap.Activator;
import me.robybert.plugin.bracketeerabap.core.ProcessorsRegistry;

public class MainPrefPage extends FieldLayoutPreferencePage implements IWorkbenchPreferencePage {
	public MainPrefPage() {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
	}

	@Override
	public void init(final IWorkbench workbench) {
	}

	@Override
	protected Control createPageContents(final Composite parent) {
		final Composite container = new Composite(parent, SWT.NULL);
		container.setLayout(new GridLayout(1, false));

		final IConfigurationElement[] config = Platform.getExtensionRegistry()
				.getConfigurationElementsFor(ProcessorsRegistry.PROC_FACTORY_ID);

		if (config.length == 0) {
			final Text txtNoBracketeerEditor = new Text(container, SWT.BORDER | SWT.READ_ONLY | SWT.WRAP | SWT.MULTI);
			txtNoBracketeerEditor.setText(Messages.MainPrefPage_txtNoBracketeerEditor_text);

			return container;
		}

		final Composite composite = new Composite(container, SWT.NONE);
		final ModifiersKeySequenceText mks = new ModifiersKeySequenceText(
				PreferencesConstants.General.HYPERLINK_MODIFIERS, Messages.MainPrefPage_HyperlinkModifier, composite);
		addField(mks);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), "com.choockapp.org.bracketeer.main_pref"); //$NON-NLS-1$
		return container;
	}

}
