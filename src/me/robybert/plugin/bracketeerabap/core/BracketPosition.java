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
 *******************************************************************************/
package me.robybert.plugin.bracketeerabap.core;

import org.eclipse.jface.text.Position;

public class BracketPosition implements IPosition{
    private final Position _pos;
    private final int _colorCode;

    public BracketPosition(final int offset, final int colorCode) {
        _pos = new Position(offset, 1);
        _colorCode = colorCode;
    }

    public Position getPosition() {
        return _pos;
    }

    public int getColorCode() {
        return _colorCode;
    }
}
