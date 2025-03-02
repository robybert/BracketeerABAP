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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.State;

public class BracketeerToggleCommandHendler extends AbstractHandler {
    public BracketeerToggleCommandHendler() {
    }

    @Override
    public Object execute(final ExecutionEvent event) throws ExecutionException {
        final State state = event.getCommand().getState(BracketeerToggleState.STATE_ID);
        state.setValue(!(Boolean) state.getValue());
        return null;
    }

}
