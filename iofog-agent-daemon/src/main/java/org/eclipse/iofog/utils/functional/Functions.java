/*
 * *******************************************************************************
 *  * Copyright (c) 2018-2022 Edgeworx, Inc.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v. 2.0 which is available at
 *  * http://www.eclipse.org/legal/epl-2.0
 *  *
 *  * SPDX-License-Identifier: EPL-2.0
 *  *******************************************************************************
 *
 */

package org.eclipse.iofog.utils.functional;


import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Optional.empty;
import static org.eclipse.iofog.utils.functional.Unit.UNIT;


/**
 * Created by ekrylovich
 * on 13.6.17.
 */
public final class Functions {
	private Functions() {
		throw new UnsupportedOperationException("com.digitalfuel.util.Functions could not be instantiated as it is util class");
	}

	public static <V> Function<V, Optional<V>> alwaysPresent() {
		return Optional::of;
	}

	public static <V> Function<V, Optional<V>> alwaysAbsent() {
		return value -> empty();
	}

	public static <A, B> Optional<B> bind(final Optional<? extends A> value, final Function<? super A, Optional<B>> f) {
		return value.flatMap(f::apply);
	}

	public static <A, B> Function<Optional<A>, Function<Function<A, Optional<B>>, Optional<B>>> bind() {
		return value -> f -> bind(value, f);
	}

	public static <A, B, C> Function<A, Optional<C>> composeM(final Function<B, Optional<C>> g, final Function<A, Optional<B>> f) {
		return value -> bind(f.apply(value), g);
	}

	public static <A, B, C> Function<Function<B, Optional<C>>, Function<Function<A, Optional<B>>, Function<A, Optional<C>>>> composeM() {
		return g -> f -> composeM(g, f);
	}

	public static <A, B, C> BiFunction<B, A, C> flip(final BiFunction<A, B, C> f) {
		return (b, a) -> f.apply(a, b);
	}

	public static <A, B, C> Function<B, Function<A, C>> flip(final Function<A, Function<B, C>> f) {
		return first -> second -> f.apply(second).apply(first);
	}

	public static <A, B, C> Function<A, Function<B, C>> curry(final BiFunction<A, B, C> f) {
		return first -> (Function<B, C>) second -> f.apply(first, second);
	}

	public static <A, B, C> BiFunction<A, B, C> unCurry(final Function<A, Function<B, C>> f) {
		return (first, second) -> f.apply(first).apply(second);
	}

	public static <A, B, C, D> Function<A, Function<B, Function<C, D>>> curry(final Function3<A, B, C, D> f) {
		return a -> (Function<B, Function<C, D>>) b -> (Function<C, D>) c -> f.apply(a, b, c);
	}
	public static <A, B, C> Function<A, C> andThen(final Function<A, ? extends B> f, final Function<B, C> g) {
		return f.andThen(g);
	}
	public static <A, B, C> Optional<C> apply(final Optional<A> val1, final Optional<B> val2, final Function<A, Function<B, C>> f) {
		return val1.flatMap(a -> val2.map(b -> f.apply(a).apply(b)));
	}

	public static <A, B, C, D> Function<C, D> apply(final Function3<A, B, C, D> f, final A a, final B b) {
		return c -> f.apply(a, b, c);
	}

	public static <B> Either<Unit, B> fromOptional(final Optional<B> value) {
		return value.map((Function<B, Either<Unit, B>>) Either::right).orElse(Either.left(UNIT));
	}

	public static <A, B> Function<A, Function<B, A>> constant() {
		return curry((a, b) -> a);
	}

	public static <A> Predicate<List<A>> nullOrEmpty() {
		return list -> list == null || list.isEmpty();
	}
}