package uk.ac.sanger.storelight.utils;

import java.util.Iterator;
import java.util.ListIterator;

/**
 * An iterator that follows a list iterator in reverse.
 * @author dr6
 */
public class ReverseIterator<E> implements Iterator<E> {
    private final ListIterator<? extends E> listIter;

    public ReverseIterator(ListIterator<? extends E> listIter) {
        this.listIter = listIter;
    }

    @Override
    public boolean hasNext() {
        return this.listIter.hasPrevious();
    }

    @Override
    public E next() {
        return this.listIter.previous();
    }

    @Override
    public void remove() {
        this.listIter.remove();
    }
}
