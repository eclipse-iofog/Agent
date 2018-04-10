package org.eclipse.iofog.utils.functional;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

final class Left<E, A> extends Either<E, A> {

    private final E value;

    Left(E value) {
        this.value = Objects.requireNonNull(value);
    }

    public <B> Either<E, B> map(final Function<A, B> f) {
        return new Left<>(value);
    }

    public <B> Either<E, B> bind(final Function<A, Either<E, B>> f) {
        return new Left<>(value);
    }

    public Either<E, A> orElse(final Supplier<Either<E, A>> a) {
        return a.get();
    }

    @Override
    public String toString() {
        return String.format("Left(%s)", value);
    }

    @Override
    public boolean isLeft() {
        return true;
    }

    @Override
    public boolean isRight() {
        return false;
    }

    @Override
    public E leftValue() {
        return this.value;
    }

    @Override
    public A rightValue() {
        throw new IllegalStateException("getRight called on Left");
    }
}