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

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

final class Right<E, A> extends Either<E, A> {

    private final A value;

    Right(final A value) {
        this.value = Objects.requireNonNull(value);
    }

    public <B> Either<E, B> map(final Function<A, B> f) {
        return new Right<>(f.apply(value));
    }

    public <B> Either<E, B> bind(final Function<A, Either<E, B>> f) {
        return f.apply(value);
    }

    public Either<E, A> orElse(final Supplier<Either<E, A>> a) {
        return this;
    }

    @Override
    public String toString() {
        return String.format("Right(%s)", value);
    }

    @Override
    public boolean isLeft() {
        return false;
    }

    @Override
    public boolean isRight() {
        return true;
    }

    @Override
    public E leftValue() {
        throw new IllegalStateException("getLeft called on Right");
    }

    @Override
    public A rightValue() {
        return this.value;
    }
}
