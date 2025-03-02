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

public interface IProcessingContainerListener {
    void containerUpdated(boolean bracketsPairsTouched, boolean singleBracketsTouched, boolean hintsTouched, boolean matchingStatementsTouched, boolean singleStatementsTouched);
}