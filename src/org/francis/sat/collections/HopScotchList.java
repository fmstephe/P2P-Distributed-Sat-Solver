package org.francis.sat.collections;

import java.util.Arrays;

@SuppressWarnings("unchecked")
public class HopScotchList<T> {

    private Object[] elems;
    private int size;
    
    public HopScotchList() {
        this(10);
    }
    
    public HopScotchList(int initialCapacity) {
        elems = new Object[initialCapacity];
        size = 0;
    }
    
    public void remove(int i) {
        assert i < size;
        assert size != 0;
        elems[i] = getLast();
        size--;
    }
    
    public T get(int i) {
        assert i < size;
        assert size != 0;
        return (T)elems[i];
    }
    
    public void add(T elem) {
        if (elems.length == size) enlarge();
        elems[size] = elem;
        size++;
    }
    
    public int size() {
        return size;
    }
    
    private void enlarge() {
        Object[] newElems = new Object[size*2];
        System.arraycopy(elems, 0, newElems, 0, elems.length);
        elems = newElems;
    }
    
    private T getLast() {
        return (T) elems[size-1];
    }
    
    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        out.append("[");
        for (int i = 0; i < size-1; i++) {
            out.append(elems[i]+",");
        }
        if (size != 0) out.append(elems[size-1]);
        out.append("]");
        return out.toString();
    }
}
