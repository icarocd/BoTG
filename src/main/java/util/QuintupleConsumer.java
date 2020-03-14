package util;

@FunctionalInterface
public interface QuintupleConsumer<A,B,C,D,E> {

    void accept(A a, B b, C c, D d, E e);
}
