
/*
 * Copyright 2015-2022 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;

/**
 * Simple test case that is used to verify proper support for classpath scanning
 * within the <em>default</em> package.
 *
 * @since 5.0
 */
@Disabled("Only used reflectively by other tests")
class DefaultPackageTestCase {

	//	// Set stopFirstFail as true, 10 tests all passed.
	//	@RepeatedTest(value = 10, stopFirstFail = true)
	//	void test0() {
	//		assertTrue(true);
	//	}
	//
	//	// Set stopFirstFail as default (false), 10 tests all passed.
	//	@RepeatedTest(value = 10)
	//	void test1() {
	//		assertTrue(true);
	//	}
	//
	//	// Set stopFirstFail as true, this set of test will stop after test1 failed.
	//	@RepeatedTest(value = 10, stopFirstFail = true)
	//	void test2() {
	//		assertEquals(1, 2);
	//	}

}
