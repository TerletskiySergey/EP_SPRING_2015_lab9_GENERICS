package maps;

import java.util.Iterator;

public interface MyMap<K, V> {

    interface Entry<K, V> {
        boolean equals(Object o);

        K getKey();

        V getValue();

        int hashCode();

        V setValue(V value);
    }

    void clear();

    boolean containsKey(K key);

    boolean containsValue(V value);

    V get(K key);

    boolean isEmpty();

    V put(K key, V value);

    V remove(K key);

    int size();

    Iterator entryIterator();
}
