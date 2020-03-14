package util;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * An extension of {@link Predicate} for 2 parameters.
 */
@FunctionalInterface
public interface BiPredicate<A,B> {

	boolean test(A a, B b);

	default BiPredicate<A,B> and(BiPredicate<? super A, ? super B> other) {
        Objects.requireNonNull(other);
        return (a,b) -> test(a,b) && other.test(a,b);
    }

	default BiPredicate<A,B> negate() {
        return (a,b) -> !test(a,b);
    }

	default BiPredicate<A,B> or(BiPredicate<? super A, ? super B> other) {
        Objects.requireNonNull(other);
        return (a,b) -> test(a,b) || other.test(a,b);
    }
}
