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
package me.robybert.plugin.bracketeerabap.common;

import org.eclipse.jface.text.Position;

public class SingleBracket extends SingleObject {
	char _char;

	public SingleBracket(final int offset, final boolean isOpening, final char ch) {
		_position = new Position(offset, 1);
		_isOpening = isOpening;
		_char = ch;
	}

	public char[] getChar() {
		char[] ret = { _char };
		return ret;
	}

}
