/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.dax.parser.ccc;

import org.eclipse.daanse.dax.parser.api.DaxParser;
import org.eclipse.daanse.dax.parser.api.DaxParserException;
import org.eclipse.daanse.dax.parser.api.DaxParserProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

@Component(scope = ServiceScope.SINGLETON, configurationPid = CCCDaxParserProvider.PID, service = DaxParserProvider.class)
public class CCCDaxParserProvider implements DaxParserProvider {

    public static final String PID = "daanse.dax.parser.ccc.CCCDaxParserProvider";

    @Override
    public DaxParser newParser(CharSequence dax) throws DaxParserException {
        return new DaxParserWrapper(dax);
    }
}
