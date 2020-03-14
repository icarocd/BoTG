package util;

import java.util.ArrayList;

public class ListCollector<T> implements Collector<T> {

	private ArrayList<T> elements;

	public ListCollector() {
	    elements = new ArrayList<>();
    }
	public ListCollector(int initialCapacity) {
	    elements = new ArrayList<>(initialCapacity);
    }

	@Override
	public void collect(T element) {
		elements.add(element);
	}

	public ArrayList<T> getElements() {
		return elements;
	}

	public T getElement(int index) {
        return elements.get(index);
    }
}
