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

package me.robybert.plugin.bracketeerabap.common;

import java.util.List;

public class Utils {

    private static final String _openingBrackets = "([{<"; //$NON-NLS-1$
    private static final List<String> _openingStatements = List.of(	"if", "IF", "loop", "LOOP", "do"	, "DO",
    																"while", "WHILE", "try", "TRY", "case", "CASE");

    public static boolean isOpenningBracket(final char prevChar) {
        return _openingBrackets.indexOf(prevChar) != -1;
    }
    
    public static boolean isOpeningStatement(final String prevString) {
		return _openingStatements.contains(prevString);
	}

}
