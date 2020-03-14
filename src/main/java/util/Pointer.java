package util;

public class Pointer<T> {
    private T element;

    public Pointer() {
    }

    public Pointer(T element) {
        this.element = element;
    }

    public T get() {
        return element;
    }

    public void set(T element) {
        this.element = element;
    }
}
