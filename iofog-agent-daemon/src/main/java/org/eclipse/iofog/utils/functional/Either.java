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


import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Class that models Sum types, or binary CoProducts.Note that the universal
 * mapping property (UMP) of this binary CoProduct is not enforced inside the
 * implementation, but must be validate in tests. A binary CoProduct of
 * <code>A, B</code> is The triple
 * <code>(Either<A, B), {left()}:: A -> Either<A,B>, {right()}:: B -> Either<A,B>) </code>
 * such that if there is another triple with the same shape:
 * <code>(C, pi1():: A ->Either<A, B>, pi2():: B -> Either<A, B>)</code> then
 * there is <code>UNIQUE</code> function (really need not be a function, just
 * arrow) from
 * <code> <f,g> :: Either<A, B> -> C, such that pi1() = <f,g> o f and pi2()= <f,g> o g </code>
 * Note that : right() and left() are called injection functions and Either<A,
 * B> is written (A + B)
 *
 * Created by ekrylovich
 * on 13.6.17.
 */
public abstract class Either<A, B> {

    /**
     * Applies the given function for the value of type <code>B</code> it's
     * {@link Right} otherwise do nothing
     *
     * @param f
     *            the function to apply when this is instance of {@link Right}
     * @return An new instance of Either<A, C> by applying f to the value inside
     *         a Right instance otherwise return this
     */
    public abstract <C> Either<A, C> map(final Function<B, C> f);

    /**
     * Applies the given function if this instance is {@link Right} and then
     * flatten the result, because when applying the function we add one layer
     * of Either<?,?>, returns this otherwise.
     *
     * @param f
     *            function to apply to the value inside a {@link Right} instance
     * @return The flatten result after applying f it this is instance of
     *         {@link Right} otherwise returns this unmodified
     */
    public abstract <C> Either<A, C> bind(final Function<B, Either<A, C>> f);

    /**
     * Return this instance if it's {@link Right} otherwise f.app();
     *
     * @param f
     *            the function that will return a fall-back value
     * @return this instance if it's {@link Right} f.apply() otherwise
     */
    public abstract Either<A, B> orElse(final Supplier<Either<A, B>> f);

    /**
     * Tells whether this instance is {@link Left} or not.
     *
     * @return true if this is instance of {@link Left}
     */
    public abstract boolean isLeft();

    /**
     * Tells whether this instance is {@link Right} or not.
     *
     * @return true if this is instance of {@link Right}
     */
    public abstract boolean isRight();

    /**
     * Returns the value inside a {@link Left} instance otherwise throws an
     * exception if this is an instance of {@link Right}
     *
     * @return the value inside a {@link Left} instance
     */
    public abstract A leftValue();

    /**
     * Returns the value inside a {@link Right} instance otherwise throws an
     * exception if this is an instance of {@link Left}
     *
     * @return the value inside a {@link Right} instance
     */
    public abstract B rightValue();

    /**
     * Left injection function
     *
     * @return
     */
    public static <A, B> Either<A, B> left(final A value) {
        return new Left<>(value);
    }

    /**
     * Right injection function
     *
     * @return
     */
    public static <A, B> Either<A, B> right(final B value) {
        return new Right<>(value);
    }

    public static <A, B> Function<B, Either<A, B>> pointRight() {
        return Either::right;
    }

    /**
     * Returns a Function that injects on the left
     *
     * @return
     */
    public static <A, B> Function<A, Either<A, B>> pointLeft() {
        return Either::left;
    }

    /**
     * Applies one of the given functions depending on whether this is Left or
     * Right. If this is instance of Left then apply <code>left</code> otherwise
     * apply <code>right</code>
     *
     * @param left
     *            The function to call if this is left.
     * @param right
     *            The function to call if this is right.
     * @return The resulting value.
     */
    public final <C> C either(final Function<A, C> left, final Function<B, C> right) {
        return isLeft() ? left.apply(leftValue()) : right.apply(rightValue());
    }
}
