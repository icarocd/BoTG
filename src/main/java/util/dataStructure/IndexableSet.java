package util.dataStructure;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import com.google.common.base.Preconditions;

/**
 * Given a initial informed set, it provides one index per element, and both value-access and index-access in O(1).
 */
public class IndexableSet<X> {

    private final int indexOffset;
    private final Map<X,Integer> indexByElement;
    private final X[] elementByIndex;

    public IndexableSet(Set<X> initialData, Class<X> type, int indexOffset) {
        Preconditions.checkArgument(indexOffset >= 0);
        this.indexOffset = indexOffset;
        indexByElement = new HashMap<>(initialData.size(), 1);
        elementByIndex = (X[])Array.newInstance(type, initialData.size());
        int i = 0;
        for (X element : initialData) {
            indexByElement.put(element, i + indexOffset);
            elementByIndex[i] = element;
            i++;
        }
    }

    public int getIndex(X element){
        return indexByElement.get(element);
    }

    public X getValue(int index){
        return elementByIndex[index - indexOffset];
    }

    public static void main(String[] args) {
        Set<String> initialData = new LinkedHashSet<>();
        initialData.add("bla");
        initialData.add("ble");
        initialData.add("bli");
        int offset = 1;
        IndexableSet<String> set = new IndexableSet<>(initialData, String.class, offset);
        System.out.println(set.getIndex("bla") == (0+offset));
        System.out.println(set.getIndex("ble") == (1+offset));
        System.out.println(set.getIndex("bli") == (2+offset));
        System.out.println("bla".equals(set.getValue(0+offset)));
        System.out.println("ble".equals(set.getValue(1+offset)));
        System.out.println("bli".equals(set.getValue(2+offset)));
    }
}
