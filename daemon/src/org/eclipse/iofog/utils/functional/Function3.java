package org.eclipse.iofog.utils.functional;

/**
 * Created by ekrylovich
 * on 13.6.17.
 */
@FunctionalInterface
public interface Function3<A, B, C, D> {
    D apply(final A a, final B b, final C c);
}