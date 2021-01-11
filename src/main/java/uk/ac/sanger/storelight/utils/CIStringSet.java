package uk.ac.sanger.storelight.utils;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A case-preserving collection of strings distinct by case.
 * Lacking any perfect canonical way to case-fold strings in Java,
 * just {@link String#toUpperCase} is used, and we hope for no weird Turkish characters.
 * @author dr6
 */
public class CIStringSet implements Set<String> {
    private Set<String> set;
    private List<String> list;

    public CIStringSet() {
        set = new HashSet<>();
        list = new ArrayList<>();
    }

    public CIStringSet(int capacity) {
        set = new HashSet<>(capacity);
        list = new ArrayList<>(capacity);
    }

    public CIStringSet(Collection<String> strings) {
        this(strings.size());
        this.addAll(strings);
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return (o instanceof String && set.contains(((String) o).toUpperCase()));
    }

    @NotNull
    @Override
    public Iterator<String> iterator() {
        return new Iter();
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return list.toArray();
    }

    @SuppressWarnings("SuspiciousToArrayCall")
    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
        return list.toArray(a);
    }

    @Override
    public boolean add(String s) {
        if (!set.add(s.toUpperCase())) {
            return false;
        }
        list.add(s);
        return true;
    }

    // This is O(N) if the string is found; O(1) if it is not found.
    @Override
    public boolean remove(Object o) {
        if (o instanceof String) {
            String s = (String) o;
            if (set.remove(s.toUpperCase())) {
                list.removeIf(s::equalsIgnoreCase);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        if (c instanceof CIStringSet) {
            return this.set.containsAll(((CIStringSet) c).set);
        }
        return c.stream().allMatch(this::contains);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends String> c) {
        boolean any = false;
        for (String s : c) {
            if (add(s)) {
                any = true;
            }
        }
        return any;
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        Set<String> uc = c.stream()
                .filter(x -> x instanceof String)
                .map(x -> ((String) x).toUpperCase())
                .collect(Collectors.toSet());
        Set<String> newSet = new HashSet<>();
        List<String> newList = new ArrayList<>();
        for (String s : list) {
            String su = s.toUpperCase();
            if (uc.contains(su)) {
                newSet.add(su);
                newList.add(s);
            }
        }
        if (newSet.size()==set.size()) {
            return false;
        }
        this.set = newSet;
        this.list = newList;
        return true;
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        Set<String> uc = c.stream()
                .filter(x -> x instanceof String)
                .map(x -> ((String) x).toUpperCase())
                .collect(Collectors.toSet());
        Set<String> newSet = new HashSet<>();
        List<String> newList = new ArrayList<>();
        for (String s : list) {
            String su = s.toUpperCase();
            if (!uc.contains(su)) {
                newSet.add(su);
                newList.add(s);
            }
        }
        if (newSet.size()==set.size()) {
            return false;
        }
        this.set = newSet;
        this.list = newList;
        return true;
    }

    @Override
    public void clear() {
        set.clear();
        list.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CIStringSet that = (CIStringSet) o;
        return this.set.equals(that.set);
        // Note that this set is not equal to any other implementation of set,
        // so it breaks the symmetry of equals with sets that think they can be equal
        // to any other set
    }

    @Override
    public int hashCode() {
        return set.hashCode();
    }

    public static CIStringSet of(String... strings) {
        return new CIStringSet(List.of(strings));
    }

    @Override
    public String toString() {
        return "CIStringSet{"+String.join(", ", list)+"}";
    }

    private class Iter implements Iterator<String> {
        int nextIndex = 0;   // index of next element to return
        int lastIndex = -1;  // index of last element returned, or -1

        public boolean hasNext() {
            return (nextIndex < size());
        }

        public String next() {
            int i = nextIndex;
            if (i < 0 || i >= size()) {
                throw new NoSuchElementException();
            }
            nextIndex = i + 1;
            lastIndex = i;
            return list.get(i);
        }

        public void remove() {
            if (lastIndex < 0) {
                throw new IllegalStateException();
            }

            String s = list.remove(lastIndex);
            set.remove(s.toUpperCase());
            nextIndex = lastIndex;
            lastIndex = -1;
        }
    }
}
