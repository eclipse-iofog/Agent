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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.eclipse.iofog.utils.functional.Functions.curry;


/**
 * Created by ekrylovich
 * on 13.6.17.
 */
public final class Pair<A, B> {
	private final A first;
	private final B second;

	private Pair(final A first, final B second) {
		this.first = Objects.requireNonNull(first);
		this.second = Objects.requireNonNull(second);
	}

	public A _1() {
		return first;
	}

	public B _2() {
		return second;
	}

	public static <T, V> Pair<T, V> of(final T first, final V second) {
		return new Pair<>(first, second);
	}

	/**
	 * Map the first element of the product.
	 *
	 * @param f The function to map with.
	 * @return A product with the given function applied.
	 */
	public <C> Pair<C, B> map1(final Function<A, C> f) {
		return bimap(f, Function.identity());
	}

	/**
	 * Map the second element of the product.
	 *
	 * @param f The function to map with.
	 * @return A product with the given function applied.
	 */
	public <C> Pair<A, C> map2(final Function<B, C> f) {
		return bimap(Function.identity(), f);
	}

	/**
	 * Swaps this product-2 (pair)
	 *
	 * @return the swapped pair
	 */
	public Pair<B, A> swap() {
		return of(_2(), _1());
	}

	/**
	 * Split this product between two argument functions and combine their
	 * output.
	 *
	 * @param f A function that will map the first element of this product.
	 * @param g A function that will map the second element of this product.
	 * @return A new product with the first function applied to the second
	 * element and the second function applied to the second element.
	 */
	public <C, D> Pair<C, D> bimap(final Function<A, C> f, final Function<B, D> g) {
		return of(f.apply(_1()), g.apply(_2()));
	}

	/**
	 * Transforms a curried function of arity-2 to a function of a product-2
	 *
	 * @param f a curried function of arity-2 to transform into a function of
	 *          a product-2
	 * @return The function, transformed to operate on on a product-2
	 */
	public static <A, B, C> Function<Pair<A, B>, C> tuple(final Function<A, Function<B, C>> f) {
		return pair -> f.apply(pair._1()).apply(pair._2());
	}

	/**
	 * Transforms an uncurried function of arity-2 to a function of a product-2
	 *
	 * @param f an uncurried function of arity-2 to transform into a function
	 *          of a product-2
	 * @return The function, transformed to operate on on a product-2
	 */
	public static <A, B, C> Function<Pair<A, B>, C> tuple(final BiFunction<A, B, C> f) {
		return tuple(curry(f));
	}

	/**
	 * Transforms a function of a product-2 to an uncurried function or arity-2.
	 *
	 * @param f A function of a product-2 to transform into an uncurried
	 *          function.
	 * @return The function, transformed to an uncurried function of arity-2.
	 */
	public static <A, B, C> BiFunction<A, B, C> untuple(final Function<Pair<A, B>, C> f) {
		return (a, b) -> f.apply(of(a, b));
	}

	/**
	 * Returns a new Pair (Product2) with the first element is the application
	 * of the given function to this pair (current instance) and the second
	 * element is the same as this instance. It duplicates this product2 (pair)
	 * in the first element and then apply the given function to generate the
	 * new first element of the result pair and leave the second element
	 * unchanged.
	 *
	 * @param f A function to map over the duplicated product.
	 * @return A new product with the result of the given function applied to
	 * this product as the first element, and with the second element
	 * unchanged.
	 */
	public <C> Pair<C, B> cobind(final Function<Pair<A, B>, C> f) {
		return Pair.<A, B, C>extend().apply(f).apply(this);
	}

	/**
	 * Defines the comonad coextension operator ((D A -> B)-> (D A -> D B) A
	 * computation of type <code> D A </code> can be seen as a context-dependent
	 * computation that produces a value
	 * <code>A</value> if some context <code>D</code>. Thus this method
	 * propagates the context-dependence to the result. This function is the
	 * implementation of coextension comonad operator for the Product comonad.
	 * In this case, this function duplicates the give Product2 (Pair) in the
	 * first element and then apply the given function.
	 * <p>
	 * A function to map over the duplicated product.
	 *
	 * @return A new product with the result of the given function applied to
	 * this product as the first element, and with the second element
	 * unchanged.
	 */
	public static <A, B, C> Function<Function<Pair<A, B>, C>, Function<Pair<A, B>, Pair<C, B>>> extend() {
		return f -> pair -> of(f.apply(pair), pair._2());
	}

	/**
	 * Defines the first natural transformation(epsi:: D -> 1) required to
	 * define a comonad. The intuition for this function is, given a
	 * context-dependent computation <code>D A</code> run it in the `empty` or
	 * `current` context and get the result.
	 *
	 * @return the function that runs a given context-dependent computation in
	 * the `empty` or `current` context.
	 */
	public static <A, B> Function<Pair<A, B>, A> extract() {
		return Pair::_1;
	}

	/**
	 * Duplicates this product into the first element. This function corresponds
	 * to the second natural transformation (d :: D -> D D) required to define a
	 * comonad. The intuition for this function is : given a context-dependent
	 * computation returns context-dependent context depend computation where
	 * the context of the inner computation given by the outer.
	 *
	 * @return A new product with this product in its first element and with the
	 * second element unchanged
	 */
	public Pair<Pair<A, B>, B> duplicate() {
		return cobind(Function.identity());
	}

	/**
	 * Replaces the first element of this product with the given value.
	 *
	 * @param c The value with which to replace the first element of this
	 *          product.
	 * @return A new product with the first element replaced with the given
	 * value.
	 */
	public <C> Pair<C, B> replace1(final C c) {
		final Function<Pair<A, B>, C> co = Functions.<C, Pair<A, B>>constant().apply(c);
		return cobind(co);
	}

	/**
	 * Replaces the first element of this product with the given value.
	 *
	 * @param c The value with which to replace the first element of this
	 *          product.
	 * @return A new product with the first element replaced with the given
	 * value.
	 */
	public <C> Pair<A, C> replace2(final C c) {
		return swap().replace1(c).swap();
	}

	/**
	 * Returns a first projection as a function. To be used with higher order
	 * functions.
	 *
	 * @return function that will project the given Pair to the first component
	 */
	public static <A, B> Function<Pair<A, B>, A> fst() {
		return Pair::_1;
	}

	/**
	 * Returns a second projection as a function. To be used with higher order
	 * functions.
	 *
	 * @return function that will project the given Pair to the second component
	 */
	public static <A, B> Function<Pair<A, B>, B> snd() {
		return Pair::_2;
	}

	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}

	@Override
	public boolean equals(final Object obj) {
		return EqualsBuilder.reflectionEquals(this, obj);
	}

	@Override
	public String toString() {
		return "(" + first + "," + second + ")";
	}
}

