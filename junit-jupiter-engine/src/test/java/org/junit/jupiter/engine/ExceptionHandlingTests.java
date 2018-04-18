/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine;

import static org.assertj.core.api.Assertions.allOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.engine.test.ExecutionEventConditions.assertRecordedExecutionEventsContainsExactly;
import static org.junit.platform.engine.test.ExecutionEventConditions.container;
import static org.junit.platform.engine.test.ExecutionEventConditions.engine;
import static org.junit.platform.engine.test.ExecutionEventConditions.event;
import static org.junit.platform.engine.test.ExecutionEventConditions.finishedSuccessfully;
import static org.junit.platform.engine.test.ExecutionEventConditions.finishedWithFailure;
import static org.junit.platform.engine.test.ExecutionEventConditions.started;
import static org.junit.platform.engine.test.ExecutionEventConditions.test;
import static org.junit.platform.engine.test.TestExecutionResultConditions.isA;
import static org.junit.platform.engine.test.TestExecutionResultConditions.message;
import static org.junit.platform.engine.test.TestExecutionResultConditions.suppressed;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Optional;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.engine.test.ExecutionGraph;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.opentest4j.AssertionFailedError;

/**
 * Integration tests that verify correct exception handling in the {@link JupiterTestEngine}.
 *
 * @since 5.0
 */
class ExceptionHandlingTests extends AbstractJupiterTestEngineTests {

	@Test
	void failureInTestMethodIsRegistered() throws NoSuchMethodException {
		Method method = FailureTestCase.class.getDeclaredMethod("failingTest");
		LauncherDiscoveryRequest request = request().selectors(selectMethod(FailureTestCase.class, method)).build();

		ExecutionGraph executionGraph = executeTests(request).getExecutionGraph();

		assertEquals(1, executionGraph.getTestStartedCount(), "# tests started");
		assertEquals(1, executionGraph.getTestFailedCount(), "# tests failed");

		assertRecordedExecutionEventsContainsExactly(executionGraph.getFailedTestFinishedEvents(), //
			event(test("failingTest"),
				finishedWithFailure(allOf(isA(AssertionFailedError.class), message("always fails")))));
	}

	@Test
	void uncheckedExceptionInTestMethodIsRegistered() throws NoSuchMethodException {
		Method method = FailureTestCase.class.getDeclaredMethod("testWithUncheckedException");
		LauncherDiscoveryRequest request = request().selectors(selectMethod(FailureTestCase.class, method)).build();

		ExecutionGraph executionGraph = executeTests(request).getExecutionGraph();

		assertEquals(1, executionGraph.getTestStartedCount(), "# tests started");
		assertEquals(1, executionGraph.getTestFailedCount(), "# tests failed");

		assertRecordedExecutionEventsContainsExactly(executionGraph.getFailedTestFinishedEvents(), //
			event(test("testWithUncheckedException"),
				finishedWithFailure(allOf(isA(RuntimeException.class), message("unchecked")))));
	}

	@Test
	void checkedExceptionInTestMethodIsRegistered() throws NoSuchMethodException {
		Method method = FailureTestCase.class.getDeclaredMethod("testWithCheckedException");
		LauncherDiscoveryRequest request = request().selectors(selectMethod(FailureTestCase.class, method)).build();

		ExecutionGraph executionGraph = executeTests(request).getExecutionGraph();

		assertEquals(1, executionGraph.getTestStartedCount(), "# tests started");
		assertEquals(1, executionGraph.getTestFailedCount(), "# tests failed");

		assertRecordedExecutionEventsContainsExactly(executionGraph.getFailedTestFinishedEvents(), //
			event(test("testWithCheckedException"),
				finishedWithFailure(allOf(isA(IOException.class), message("checked")))));
	}

	@Test
	void checkedExceptionInBeforeEachIsRegistered() throws NoSuchMethodException {
		Method method = FailureTestCase.class.getDeclaredMethod("succeedingTest");
		LauncherDiscoveryRequest request = request().selectors(selectMethod(FailureTestCase.class, method)).build();

		FailureTestCase.exceptionToThrowInBeforeEach = Optional.of(new IOException("checked"));

		ExecutionGraph executionGraph = executeTests(request).getExecutionGraph();

		assertEquals(1, executionGraph.getTestStartedCount(), "# tests started");
		assertEquals(1, executionGraph.getTestFailedCount(), "# tests failed");

		assertRecordedExecutionEventsContainsExactly(executionGraph.getFailedTestFinishedEvents(),
			event(test("succeedingTest"), finishedWithFailure(allOf(isA(IOException.class), message("checked")))));
	}

	@Test
	void checkedExceptionInAfterEachIsRegistered() throws NoSuchMethodException {
		Method method = FailureTestCase.class.getDeclaredMethod("succeedingTest");
		LauncherDiscoveryRequest request = request().selectors(selectMethod(FailureTestCase.class, method)).build();

		FailureTestCase.exceptionToThrowInAfterEach = Optional.of(new IOException("checked"));

		ExecutionGraph executionGraph = executeTests(request).getExecutionGraph();

		assertEquals(1, executionGraph.getTestStartedCount(), "# tests started");
		assertEquals(1, executionGraph.getTestFailedCount(), "# tests failed");

		assertRecordedExecutionEventsContainsExactly(executionGraph.getFailedTestFinishedEvents(),
			event(test("succeedingTest"), finishedWithFailure(allOf(isA(IOException.class), message("checked")))));
	}

	@Test
	void checkedExceptionInAfterEachIsSuppressedByExceptionInTest() throws NoSuchMethodException {
		Method method = FailureTestCase.class.getDeclaredMethod("testWithUncheckedException");
		LauncherDiscoveryRequest request = request().selectors(selectMethod(FailureTestCase.class, method)).build();

		FailureTestCase.exceptionToThrowInAfterEach = Optional.of(new IOException("checked"));

		ExecutionGraph executionGraph = executeTests(request).getExecutionGraph();

		assertRecordedExecutionEventsContainsExactly(executionGraph.getExecutionEvents(), //
			event(engine(), started()), //
			event(container(FailureTestCase.class), started()), //
			event(test("testWithUncheckedException"), started()), //
			event(test("testWithUncheckedException"), //
				finishedWithFailure(allOf( //
					isA(RuntimeException.class), //
					message("unchecked"), //
					suppressed(0, allOf(isA(IOException.class), message("checked")))))), //
			event(container(FailureTestCase.class), finishedSuccessfully()), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void checkedExceptionInBeforeAllIsRegistered() throws NoSuchMethodException {
		Method method = FailureTestCase.class.getDeclaredMethod("succeedingTest");
		LauncherDiscoveryRequest request = request().selectors(selectMethod(FailureTestCase.class, method)).build();

		FailureTestCase.exceptionToThrowInBeforeAll = Optional.of(new IOException("checked"));

		ExecutionGraph executionGraph = executeTests(request).getExecutionGraph();

		assertRecordedExecutionEventsContainsExactly(executionGraph.getExecutionEvents(), //
			event(engine(), started()), //
			event(container(FailureTestCase.class), started()), //
			event(container(FailureTestCase.class),
				finishedWithFailure(allOf(isA(IOException.class), message("checked")))), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void checkedExceptionInAfterAllIsRegistered() throws NoSuchMethodException {
		Method method = FailureTestCase.class.getDeclaredMethod("succeedingTest");
		LauncherDiscoveryRequest request = request().selectors(selectMethod(FailureTestCase.class, method)).build();

		FailureTestCase.exceptionToThrowInAfterAll = Optional.of(new IOException("checked"));

		ExecutionGraph executionGraph = executeTests(request).getExecutionGraph();

		assertRecordedExecutionEventsContainsExactly(executionGraph.getExecutionEvents(), //
			event(engine(), started()), //
			event(container(FailureTestCase.class), started()), //
			event(test("succeedingTest"), started()), //
			event(test("succeedingTest"), finishedSuccessfully()), //
			event(container(FailureTestCase.class),
				finishedWithFailure(allOf(isA(IOException.class), message("checked")))), //
			event(engine(), finishedSuccessfully()));
	}

	@Test
	void exceptionInAfterAllCallbackDoesNotHideFailureWhenTestInstancePerClassIsUsed() {
		LauncherDiscoveryRequest request = request().selectors(
			selectClass(TestCaseWithInvalidConstructorAndThrowingAfterAllCallback.class)).build();

		FailureTestCase.exceptionToThrowInAfterAll = Optional.of(new IOException("after"));

		ExecutionGraph executionGraph = executeTests(request).getExecutionGraph();

		assertRecordedExecutionEventsContainsExactly(executionGraph.getExecutionEvents(), //
			event(engine(), started()), //
			event(container(TestCaseWithInvalidConstructorAndThrowingAfterAllCallback.class), started()), //
			event(container(TestCaseWithInvalidConstructorAndThrowingAfterAllCallback.class), finishedWithFailure(allOf( //
				message(m -> m.contains("constructor")), //
				suppressed(0, message("callback"))))), //
			event(engine(), finishedSuccessfully()));
	}

	@AfterEach
	void cleanUpExceptions() {
		FailureTestCase.exceptionToThrowInBeforeAll = Optional.empty();
		FailureTestCase.exceptionToThrowInAfterAll = Optional.empty();
		FailureTestCase.exceptionToThrowInBeforeEach = Optional.empty();
		FailureTestCase.exceptionToThrowInAfterEach = Optional.empty();
	}

	static class FailureTestCase {

		static Optional<Throwable> exceptionToThrowInBeforeAll = Optional.empty();
		static Optional<Throwable> exceptionToThrowInAfterAll = Optional.empty();
		static Optional<Throwable> exceptionToThrowInBeforeEach = Optional.empty();
		static Optional<Throwable> exceptionToThrowInAfterEach = Optional.empty();

		@BeforeAll
		static void beforeAll() throws Throwable {
			if (exceptionToThrowInBeforeAll.isPresent())
				throw exceptionToThrowInBeforeAll.get();
		}

		@AfterAll
		static void afterAll() throws Throwable {
			if (exceptionToThrowInAfterAll.isPresent())
				throw exceptionToThrowInAfterAll.get();
		}

		@BeforeEach
		void beforeEach() throws Throwable {
			if (exceptionToThrowInBeforeEach.isPresent())
				throw exceptionToThrowInBeforeEach.get();
		}

		@AfterEach
		void afterEach() throws Throwable {
			if (exceptionToThrowInAfterEach.isPresent())
				throw exceptionToThrowInAfterEach.get();
		}

		@Test
		void succeedingTest() {
		}

		@Test
		void failingTest() {
			Assertions.fail("always fails");
		}

		@Test
		void testWithUncheckedException() {
			throw new RuntimeException("unchecked");
		}

		@Test
		void testWithCheckedException() throws IOException {
			throw new IOException("checked");
		}

	}

	@TestInstance(PER_CLASS)
	@ExtendWith(ThrowingAfterAllCallback.class)
	static class TestCaseWithInvalidConstructorAndThrowingAfterAllCallback {
		TestCaseWithInvalidConstructorAndThrowingAfterAllCallback() {
		}

		@SuppressWarnings("unused")
		TestCaseWithInvalidConstructorAndThrowingAfterAllCallback(String unused) {
		}

		@Test
		void test() {
		}
	}

	static class ThrowingAfterAllCallback implements AfterAllCallback {
		@Override
		public void afterAll(ExtensionContext context) {
			throw new IllegalStateException("callback");
		}
	}

}
