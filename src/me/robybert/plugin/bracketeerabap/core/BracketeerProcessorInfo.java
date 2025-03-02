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

import me.robybert.plugin.bracketeerabap.extensionpoint.BracketeerProcessor;

public class BracketeerProcessorInfo {
	private final BracketeerProcessor _processor;
	private final ProcessorConfiguration _configuration;

	public BracketeerProcessorInfo(final BracketeerProcessor processor, final ProcessorConfiguration configuration) {
		_processor = processor;
		_configuration = configuration;
	}

	public BracketeerProcessor getProcessor() {
		return _processor;
	}

	public ProcessorConfiguration getConfiguration() {
		return _configuration;
	}
}