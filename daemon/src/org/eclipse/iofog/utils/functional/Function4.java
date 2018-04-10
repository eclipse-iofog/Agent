package org.eclipse.iofog.utils.functional;

/**
 * Created by ekrylovich
 * on 15.6.17.
 */
@FunctionalInterface
public interface Function4<A, B, C, D, E> {
    E apply(final A a, final B b, final C c, final D d);
}
