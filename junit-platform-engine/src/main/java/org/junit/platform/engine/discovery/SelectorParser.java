/*
 * Copyright 2015-2024 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.discovery;

import java.net.URI;
import java.util.stream.Stream;

import org.junit.platform.engine.DiscoverySelector;

public interface SelectorParser {
	String getPrefix();

	Stream<DiscoverySelector> parse(URI selector, SelectorParserContext context);
}
