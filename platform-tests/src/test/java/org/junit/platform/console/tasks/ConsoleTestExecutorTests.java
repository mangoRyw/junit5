/*
 * Copyright 2015-2023 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.console.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.platform.commons.util.ClassLoaderUtils.getDefaultClassLoader;
import static org.junit.platform.launcher.core.LauncherFactoryForTestingPurposesOnly.createLauncher;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.platform.console.options.CommandLineOptions;
import org.junit.platform.console.options.Details;
import org.junit.platform.engine.support.hierarchical.DemoHierarchicalTestEngine;

/**
 * @since 1.0
 */
class ConsoleTestExecutorTests {

	private static final Runnable FAILING_BLOCK = () -> fail("should fail");
	private static final Runnable SUCCEEDING_TEST = () -> {
	};

	private final StringWriter stringWriter = new StringWriter();
	private final CommandLineOptions options = new CommandLineOptions();
	private final DemoHierarchicalTestEngine dummyTestEngine = new DemoHierarchicalTestEngine();

	{
		options.setScanClasspath(true);
	}

	@Test
	void printsSummary() {
		dummyTestEngine.addTest("succeedingTest", SUCCEEDING_TEST);
		dummyTestEngine.addTest("failingTest", FAILING_BLOCK);

		var task = new ConsoleTestExecutor(options, () -> createLauncher(dummyTestEngine));
		task.execute(new PrintWriter(stringWriter), Optional.empty());

		assertThat(stringWriter.toString()).contains("Test run finished after", "2 tests found", "0 tests skipped",
			"2 tests started", "0 tests aborted", "1 tests successful", "1 tests failed");
	}

	@Test
	void printsDetailsIfTheyAreNotHidden() {
		options.setDetails(Details.FLAT);

		dummyTestEngine.addTest("failingTest", FAILING_BLOCK);

		var task = new ConsoleTestExecutor(options, () -> createLauncher(dummyTestEngine));
		task.execute(new PrintWriter(stringWriter), Optional.empty());

		assertThat(stringWriter.toString()).contains("Test execution started.");
	}

	@Test
	void printsNoDetailsIfTheyAreHidden() {
		options.setDetails(Details.NONE);

		dummyTestEngine.addTest("failingTest", FAILING_BLOCK);

		var task = new ConsoleTestExecutor(options, () -> createLauncher(dummyTestEngine));
		task.execute(new PrintWriter(stringWriter), Optional.empty());

		assertThat(stringWriter.toString()).doesNotContain("Test execution started.");
	}

	@Test
	void printsFailuresEvenIfDetailsAreHidden() {
		options.setDetails(Details.NONE);

		dummyTestEngine.addTest("failingTest", FAILING_BLOCK);
		dummyTestEngine.addContainer("failingContainer", FAILING_BLOCK);

		var task = new ConsoleTestExecutor(options, () -> createLauncher(dummyTestEngine));
		task.execute(new PrintWriter(stringWriter), Optional.empty());

		assertThat(stringWriter.toString()).contains("Failures (2)", "failingTest", "failingContainer");
	}

	@Test
	void usesCustomClassLoaderIfAdditionalClassPathEntriesArePresent() {
		options.setAdditionalClasspathEntries(List.of(Paths.get(".")));

		var oldClassLoader = getDefaultClassLoader();
		dummyTestEngine.addTest("failingTest",
			() -> assertSame(oldClassLoader, getDefaultClassLoader(), "should fail"));

		var task = new ConsoleTestExecutor(options, () -> createLauncher(dummyTestEngine));
		task.execute(new PrintWriter(stringWriter), Optional.empty());

		assertThat(stringWriter.toString()).contains("failingTest", "should fail", "1 tests failed");
	}

	@Test
	void usesSameClassLoaderIfNoAdditionalClassPathEntriesArePresent() {
		options.setAdditionalClasspathEntries(List.of());

		var oldClassLoader = getDefaultClassLoader();
		dummyTestEngine.addTest("failingTest",
			() -> assertNotSame(oldClassLoader, getDefaultClassLoader(), "should fail"));

		var task = new ConsoleTestExecutor(options, () -> createLauncher(dummyTestEngine));
		task.execute(new PrintWriter(stringWriter), Optional.empty());

		assertThat(stringWriter.toString()).contains("failingTest", "should fail", "1 tests failed");
	}

	private PrintWriter dummyWriter() {
		return new PrintWriter(new StringWriter());
	}

}
