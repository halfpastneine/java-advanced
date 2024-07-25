package info.kgeorgiy.ja.mikhailov.arrayset;

import java.util.*;

public class ArraySet<E> extends AbstractSet<E> implements SortedSet<E> {

    private final List<E> arraySet;
    private final Comparator<? super E> comparator;

    public ArraySet() {
        this(null, null);
    }

    public ArraySet(Collection<E> collection) {
        this(collection, null);
    }

    public ArraySet(Collection<E> collection, Comparator<? super E> comparator) {
        TreeSet<E> treeSet = new TreeSet<>(comparator);
        if (collection != null) {
            treeSet.addAll(collection);
        }
        this.comparator = comparator;
        this.arraySet = new ArrayList<>(treeSet);
    }

    @Override
    public Iterator<E> iterator() {
        return arraySet.iterator();
    }

    @Override
    public int size() {
        return arraySet.size();
    }

    @Override
    public Comparator<? super E> comparator() {
        return this.comparator;
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        if (comparator.compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException("fromElement > Element");
        }
        return subSetImpl(binSearch(fromElement), binSearch(toElement));
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        return subSetImpl(0, binSearch(toElement));
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return subSetImpl(binSearch(fromElement), size());
    }

    private SortedSet<E> subSetImpl(int firstIndex, int lastIndex) {
        return new ArraySet<>(arraySet.subList(firstIndex, lastIndex), comparator());
    }

    @Override
    public E first() {
        return returnElementByIndex(0);
    }

    @Override
    public E last() {
        return returnElementByIndex(size() - 1);
    }

    private E returnElementByIndex(int index) {
        if (arraySet.isEmpty()) {
            throw new NoSuchElementException("ArraySet is empty");
        }
        return arraySet.get(index);
    }

    @Override
    public boolean contains(Object o) {
        @SuppressWarnings("unchecked")
        E obj = (E) o;
        return Collections.binarySearch(arraySet, obj, comparator()) >= 0;
    }

    public int binSearch(E e) {
        int index = Collections.binarySearch(arraySet, e, comparator());
        return index >= 0 ? index : (-index) - 1;
    }
}
