/*
 * Copyright 2015-2016 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.platform.console.tasks;

import static org.junit.platform.commons.meta.API.Usage.Internal;

import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.meta.API;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.console.options.CommandLineOptions;
import org.junit.platform.console.options.Details;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

/**
 * @since 1.0
 */
@API(Internal)
public class ExecuteTestsTask implements ConsoleTask {

	private final CommandLineOptions options;
	private final Supplier<Launcher> launcherSupplier;

	public ExecuteTestsTask(CommandLineOptions options) {
		this(options, LauncherFactory::create);
	}

	// for tests only
	ExecuteTestsTask(CommandLineOptions options, Supplier<Launcher> launcherSupplier) {
		this.options = options;
		this.launcherSupplier = launcherSupplier;
	}

	@Override
	public int execute(PrintWriter out) throws Exception {
		return new CustomContextClassLoaderExecutor(createCustomClassLoader()).invoke(() -> executeTests(out));
	}

	private int executeTests(PrintWriter out) {
		Launcher launcher = launcherSupplier.get();
		SummaryGeneratingListener summaryListener = registerListeners(out, launcher);

		LauncherDiscoveryRequest discoveryRequest = new DiscoveryRequestCreator().toDiscoveryRequest(options);
		launcher.execute(discoveryRequest);

		TestExecutionSummary summary = summaryListener.getSummary();
		printSummary(summary, out);

		return computeExitCode(summary);
	}

	private Optional<ClassLoader> createCustomClassLoader() {
		List<Path> additionalClasspathEntries = options.getAdditionalClasspathEntries();
		if (!additionalClasspathEntries.isEmpty()) {
			URL[] urls = additionalClasspathEntries.stream().map(this::toURL).toArray(URL[]::new);
			ClassLoader parentClassLoader = ReflectionUtils.getDefaultClassLoader();
			ClassLoader customClassLoader = URLClassLoader.newInstance(urls, parentClassLoader);
			return Optional.of(customClassLoader);
		}
		return Optional.empty();
	}

	private URL toURL(Path path) {
		try {
			return path.toUri().toURL();
		}
		catch (Exception ex) {
			throw new JUnitException("Invalid classpath entry: " + path, ex);
		}
	}

	private SummaryGeneratingListener registerListeners(PrintWriter out, Launcher launcher) {
		SummaryGeneratingListener summaryListener = new SummaryGeneratingListener();
		launcher.registerTestExecutionListeners(summaryListener);
		switch (options.getDetails()) {
			case FLAT:
				launcher.registerTestExecutionListeners(
					new ColoredPrintingTestListener(out, options.isAnsiColorOutputDisabled()));
				break;
			case TREE:
				launcher.registerTestExecutionListeners(new TreePrinter(out, options.isAnsiColorOutputDisabled()));
				break;
			case VERBOSE:
				launcher.registerTestExecutionListeners(
					new VerboseTreePrinter(out, options.isAnsiColorOutputDisabled()));
				break;
		}
		options.getReportsDir().ifPresent(
			dir -> launcher.registerTestExecutionListeners(new XmlReportsWritingListener(dir, out)));
		return summaryListener;
	}

	private void printSummary(TestExecutionSummary summary, PrintWriter out) {
		if (options.getDetails() == Details.HIDDEN) { // Otherwise the failures have already been printed
			summary.printFailuresTo(out);
		}
		summary.printTo(out);
	}

	private int computeExitCode(TestExecutionSummary summary) {
		return (summary.getTotalFailureCount() == 0 ? SUCCESS : TESTS_FAILED);
	}

}
