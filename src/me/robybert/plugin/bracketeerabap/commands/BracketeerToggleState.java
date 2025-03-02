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
package me.robybert.plugin.bracketeerabap.commands;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.commands.State;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.ISourceProvider;
import org.eclipse.ui.ISourceProviderListener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.ISourceProviderService;

import me.robybert.plugin.bracketeerabap.Activator;
import me.robybert.plugin.bracketeerabap.preferences.PreferencesConstants;

public class BracketeerToggleState extends State
		implements IPropertyChangeListener, IExecutableExtension, ISourceProviderListener {
	private String _attrName;
	private String _attrSuffix;
	private final IPreferenceStore _store;

	public final static String STATE_ID = "org.eclipse.ui.commands.toggleState"; //$NON-NLS-1$

	public BracketeerToggleState() {
		_attrName = null;

		_store = Activator.getDefault().getPreferenceStore();
		_store.addPropertyChangeListener(this);

		final ISourceProviderService srcService = PlatformUI.getWorkbench().getService(ISourceProviderService.class);
		final ISourceProvider src = srcService.getSourceProvider(SourceProvider.PLUGIN_NAME);
		if (src == null) {
			Activator.log(Messages.BracketeerToggleState_SrcProviderNotFound);
		} else {
			src.addSourceProviderListener(this);
		}

		super.setValue(false);
	}

	@Override
	public void propertyChange(final PropertyChangeEvent event) {
		if ((_attrName == null) || !event.getProperty().equals(_attrName)) {
			return;
		}

		setValue(_store.getBoolean(_attrName));
	}

	@Override
	public void setValue(final Object value) {
		if (!(value instanceof Boolean)) {
			throw new IllegalArgumentException("ToggleState takes a Boolean as a value"); //$NON-NLS-1$
		}

		if (_attrName == null) {
			throw new IllegalStateException(Messages.BracketeerToggleState_ErrAttrName);
		}

		_store.setValue(_attrName, (Boolean) value);

		super.setValue(value);
	}

	@Override
	public void setInitializationData(final IConfigurationElement config, final String propertyName, final Object data)
			throws CoreException {
		if (data == null || !(data instanceof String)) {
			throw new IllegalArgumentException(Messages.BracketeerToggleState_ErrAttrSuffix);
		}

		_attrSuffix = (String) data;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void sourceChanged(final int sourcePriority, final Map sourceValuesByName) {
		final Set set = sourceValuesByName.entrySet();
		for (final Iterator it = set.iterator(); it.hasNext();) {
			@SuppressWarnings("unchecked")
			final Map.Entry<String, Object> entry = (Map.Entry<String, Object>) it.next();
			sourceChanged(sourcePriority, entry.getKey(), entry.getValue());
		}
	}

	@Override
	public void sourceChanged(final int sourcePriority, final String sourceName, final Object sourceValue) {
		if (!sourceName.equals(SourceProvider.PLUGIN_NAME)) {
			return;
		}

		final String processorName = (String) sourceValue;
		if (processorName == null || processorName.isEmpty()) {
			_attrName = null;
		} else {
			_attrName = PreferencesConstants.preferencePath(processorName) + _attrSuffix;
			super.setValue(_store.getBoolean(_attrName));
		}
	}

}
