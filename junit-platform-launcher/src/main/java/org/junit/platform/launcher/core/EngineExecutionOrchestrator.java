/*
 * Copyright 2015-2022 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.core;

import static org.apiguardian.api.API.Status.INTERNAL;
import static org.junit.platform.launcher.core.ListenerRegistry.forEngineExecutionListeners;

import java.util.Optional;
import java.util.function.Consumer;

import org.apiguardian.api.API;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.UnrecoverableExceptions;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

/**
 * Orchestrates test execution using the configured test engines.
 *
 * @since 1.7
 */
@API(status = INTERNAL, since = "1.7", consumers = { "org.junit.platform.testkit", "org.junit.platform.suite.engine" })
public class EngineExecutionOrchestrator {

	private final ListenerRegistry<TestExecutionListener> listenerRegistry;

	public EngineExecutionOrchestrator() {
		this(ListenerRegistry.forTestExecutionListeners());
	}

	EngineExecutionOrchestrator(ListenerRegistry<TestExecutionListener> listenerRegistry) {
		this.listenerRegistry = listenerRegistry;
	}

	void execute(InternalTestPlan internalTestPlan, TestExecutionListener... listeners) {
		ConfigurationParameters configurationParameters = internalTestPlan.getConfigurationParameters();
		ListenerRegistry<TestExecutionListener> testExecutionListenerListeners = buildListenerRegistryForExecution(
			listeners);
		withInterceptedStreams(configurationParameters, testExecutionListenerListeners,
			testExecutionListener -> execute(internalTestPlan, EngineExecutionListener.NOOP, testExecutionListener));
	}

	/**
	 * Executes tests for the supplied {@linkplain LauncherDiscoveryResult
	 * discoveryResult} and notifies the supplied {@linkplain
	 * EngineExecutionListener engineExecutionListener} and
	 * {@linkplain TestExecutionListener testExecutionListener} of execution
	 * events.
	 */
	@API(status = INTERNAL, since = "1.9", consumers = { "org.junit.platform.suite.engine" })
	public void execute(LauncherDiscoveryResult discoveryResult, EngineExecutionListener engineExecutionListener,
			TestExecutionListener testExecutionListener) {
		Preconditions.notNull(discoveryResult, "discoveryResult must not be null");
		Preconditions.notNull(engineExecutionListener, "engineExecutionListener must not be null");
		Preconditions.notNull(testExecutionListener, "testExecutionListener must not be null");

		// #2838: The discovery result from a suite may have been filtered by
		// post discovery filters from the launcher. The discovery result should
		// be pruned accordingly
		InternalTestPlan internalTestPlan = InternalTestPlan.from(discoveryResult.withPrunedEngines());
		execute(internalTestPlan, engineExecutionListener, testExecutionListener);
	}

	private void execute(InternalTestPlan internalTestPlan, EngineExecutionListener parentEngineExecutionListener,
			TestExecutionListener testExecutionListener) {
		internalTestPlan.markStarted();

		// Do not directly pass the internal test plan to test execution listeners.
		// Hyrum's Law indicates that someone will eventually come to depend on it.
		TestPlan testPlan = internalTestPlan.getDelegate();
		LauncherDiscoveryResult discoveryResult = internalTestPlan.getDiscoveryResult();

		ListenerRegistry<EngineExecutionListener> engineExecutionListenerRegistry = forEngineExecutionListeners();
		engineExecutionListenerRegistry.add(new ExecutionListenerAdapter(testPlan, testExecutionListener));
		engineExecutionListenerRegistry.add(parentEngineExecutionListener);

		testExecutionListener.testPlanExecutionStarted(testPlan);
		execute(discoveryResult, engineExecutionListenerRegistry.getCompositeListener());
		testExecutionListener.testPlanExecutionFinished(testPlan);
	}

	private void withInterceptedStreams(ConfigurationParameters configurationParameters,
			ListenerRegistry<TestExecutionListener> listenerRegistry, Consumer<TestExecutionListener> action) {

		TestExecutionListener testExecutionListener = listenerRegistry.getCompositeListener();
		Optional<StreamInterceptingTestExecutionListener> streamInterceptingTestExecutionListener = StreamInterceptingTestExecutionListener.create(
			configurationParameters, testExecutionListener::reportingEntryPublished);
		streamInterceptingTestExecutionListener.ifPresent(listenerRegistry::add);
		try {
			action.accept(listenerRegistry.getCompositeListener());
		}
		finally {
			streamInterceptingTestExecutionListener.ifPresent(StreamInterceptingTestExecutionListener::unregister);
		}
	}

	/**
	 * Executes tests for the supplied {@linkplain LauncherDiscoveryResult
	 * discovery results} and notifies the supplied {@linkplain
	 * EngineExecutionListener listener} of execution events.
	 */
	@API(status = INTERNAL, since = "1.7", consumers = { "org.junit.platform.testkit" })
	public void execute(LauncherDiscoveryResult discoveryResult, EngineExecutionListener engineExecutionListener) {
		Preconditions.notNull(discoveryResult, "discoveryResult must not be null");
		Preconditions.notNull(engineExecutionListener, "engineExecutionListener must not be null");

		for (TestEngine testEngine : discoveryResult.getTestEngines()) {
			TestDescriptor engineDescriptor = discoveryResult.getEngineTestDescriptor(testEngine);
			if (engineDescriptor instanceof EngineDiscoveryErrorDescriptor) {
				engineExecutionListener.executionStarted(engineDescriptor);
				engineExecutionListener.executionFinished(engineDescriptor,
					TestExecutionResult.failed(((EngineDiscoveryErrorDescriptor) engineDescriptor).getCause()));
			}
			else {
				execute(engineDescriptor, engineExecutionListener, discoveryResult.getConfigurationParameters(),
					testEngine);
			}
		}
	}

	private ListenerRegistry<TestExecutionListener> buildListenerRegistryForExecution(
			TestExecutionListener... listeners) {
		if (listeners.length == 0) {
			return this.listenerRegistry;
		}
		return ListenerRegistry.copyOf(this.listenerRegistry).addAll(listeners);
	}

	private void execute(TestDescriptor engineDescriptor, EngineExecutionListener listener,
			ConfigurationParameters configurationParameters, TestEngine testEngine) {

		OutcomeDelayingEngineExecutionListener delayingListener = new OutcomeDelayingEngineExecutionListener(listener,
			engineDescriptor);
		try {
			testEngine.execute(new ExecutionRequest(engineDescriptor, delayingListener, configurationParameters));
			delayingListener.reportEngineOutcome();
		}
		catch (Throwable throwable) {
			UnrecoverableExceptions.rethrowIfUnrecoverable(throwable);
			delayingListener.reportEngineFailure(new JUnitException(
				String.format("TestEngine with ID '%s' failed to execute tests", testEngine.getId()), throwable));
		}
	}
}
