/*
 * Copyright 2015-2023 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.util;

import static org.apiguardian.api.API.Status.INTERNAL;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import org.apiguardian.api.API;

/**
 * Collection of utilities for working with exceptions.
 *
 * <h2>DISCLAIMER</h2>
 *
 * <p>These utilities are intended solely for usage within the JUnit framework
 * itself. <strong>Any usage by external parties is not supported.</strong>
 * Use at your own risk!
 *
 * @since 1.0
 */
@API(status = INTERNAL, since = "1.0")
public final class ExceptionUtils {

	private static final String JUNIT_PLATFORM_LAUNCHER_PACKAGE_PREFIX = "org.junit.platform.launcher.";
	private static final List<String> ALWAYS_INCLUDED_STACK_TRACE_ELEMENTS = Arrays.asList( //
		"org.junit.jupiter.api.Assertions", //
		"org.junit.jupiter.api.Assumptions" //
	);

	private ExceptionUtils() {
		/* no-op */
	}

	/**
	 * Throw the supplied {@link Throwable}, <em>masked</em> as an
	 * unchecked exception.
	 *
	 * <p>The supplied {@code Throwable} will not be wrapped. Rather, it
	 * will be thrown <em>as is</em> using an exploit of the Java language
	 * that relies on a combination of generics and type erasure to trick
	 * the Java compiler into believing that the thrown exception is an
	 * unchecked exception even if it is a checked exception.
	 *
	 * <h4>Warning</h4>
	 *
	 * <p>This method should be used sparingly.
	 *
	 * @param t the {@code Throwable} to throw as an unchecked exception;
	 * never {@code null}
	 * @return this method always throws an exception and therefore never
	 * returns anything; the return type is merely present to allow this
	 * method to be supplied as the operand in a {@code throw} statement
	 */
	public static RuntimeException throwAsUncheckedException(Throwable t) {
		Preconditions.notNull(t, "Throwable must not be null");
		ExceptionUtils.throwAs(t);

		// Appeasing the compiler: the following line will never be executed.
		return null;
	}

	@SuppressWarnings("unchecked")
	private static <T extends Throwable> void throwAs(Throwable t) throws T {
		throw (T) t;
	}

	/**
	 * Read the stacktrace of the supplied {@link Throwable} into a String.
	 */
	public static String readStackTrace(Throwable throwable) {
		Preconditions.notNull(throwable, "Throwable must not be null");
		StringWriter stringWriter = new StringWriter();
		try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
			throwable.printStackTrace(printWriter);
		}
		return stringWriter.toString();
	}

	/**
	 * Prune stack trace elements of the supplied {@link Throwable} using the
	 * supplied {@link Predicate}, except {@code org.junit.jupiter.api.Assertions}
	 * and {@code org.junit.jupiter.api.Assumptions} that will always remain
	 * present.
	 *
	 * <p>Additionally, all elements prior to and including the first
	 * JUnit Launcher call will be pruned.
	 *
	 * @param throwable the {@code Throwable} whose stack trace should be
	 * pruned; never {@code null}
	 * @param stackTraceElementFilter the {@code Predicate} used to filter
	 * elements of the stack trace
	 *
	 * @since 5.10
	 */
	public static void pruneStackTrace(Throwable throwable, Predicate<String> stackTraceElementFilter) {
		Preconditions.notNull(throwable, "Throwable must not be null");

		List<StackTraceElement> stackTrace = Arrays.asList(throwable.getStackTrace());
		List<StackTraceElement> prunedStackTrace = new ArrayList<>();

		Collections.reverse(stackTrace);

		for (StackTraceElement element : stackTrace) {
			String name = element.getClassName();
			if (name.startsWith(JUNIT_PLATFORM_LAUNCHER_PACKAGE_PREFIX)) {
				prunedStackTrace.clear();
			}
			else if (stackTraceElementFilter.test(name) || ALWAYS_INCLUDED_STACK_TRACE_ELEMENTS.contains(name)) {
				prunedStackTrace.add(element);
			}
		}

		Collections.reverse(prunedStackTrace);
		throwable.setStackTrace(prunedStackTrace.toArray(new StackTraceElement[0]));
	}

}
