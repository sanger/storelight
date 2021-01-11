package uk.ac.sanger.storelight.utils;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Much copied from the corresponding class in CGAP lims
 * @author dr6
 */
public class BasicUtils {
    private BasicUtils() {}

    public static <E> E coalesce(E a, E b) {
        return (a==null ? b : a);
    }

    /**
     * Returns a string representation of the given object.
     * If it is a string it will be in quote marks and unprintable
     * characters will be shown as unicode insertions.
     * @param o object to represent
     * @return a string
     */
    public static String repr(Object o) {
        if (o==null) {
            return "null";
        }
        if (o instanceof CharSequence) {
            return StringRepr.repr((CharSequence) o);
        }
        if (o instanceof Character) {
            return StringRepr.repr((char) o);
        }
        return o.toString();
    }

    /**
     * Does the given string start with the given substring (case insensitive)
     * @param string the container string
     * @param sub the substring
     * @return true if {@code string} starts with {@code sub} ignoring case
     */
    public static boolean startsWithIgnoreCase(String string, String sub) {
        return StringUtils.startsWithIgnoreCase(string, sub);
    }

    /**
     * Pluralise a message with {@link MessageVar} and add an unordered list.
     * @param template the {@code MessageVar} template
     * @param items the items
     * @param stringFn an optional function to convert the items to strings
     * @param <E> the type of items being listed
     * @return a string including the message, listing the items
     */
    public static <E> String messageAndList(String template, Collection<? extends E> items,
                                            Function<? super E, String> stringFn) {
        return StringUtils.messageAndList(template, items, stringFn);
    }

    /**
     * Using {@link MessageVar} pluralise a message with a template.
     * @param template a template with substitutions baked in
     * @param number the number indicating whether the message should be pluralised or singularised
     * @return the processed string
     */
    public static String pluralise(String template, int number) {
        return MessageVar.process(template, number);
    }

    /**
     * Pluralise a message with {@link MessageVar} and add an unordered list.
     * @param template the {@code MessageVar} template
     * @param items the items
     * @return a string including the message, listing the items
     */
    public static String messageAndList(String template, Collection<?> items) {
        return StringUtils.messageAndList(template, items, null);
    }

    /**
     * Call the given {@code consumer} <i>once</i> for every element of {@code items} that is repeated.
     * That means that if the items are <code>1, 2, 1, 3, 1, 2, 1</code>
     * then the consumer will receive 1 (the first time 1 is repeated), and then 2 (the first time 2 is repeated);
     * and this method will return 2.
     * This is used for constructing error messages when repeated arguments are being rejected.
     * @return the number of repeated items
     */
    public static <T> int forRepeatedItems(Stream<? extends T> items, Consumer<? super T> consumer) {
        final Set<T> s = new HashSet<>();
        final Set<T> consumed = new HashSet<>();
        items.sequential()
                .filter(item -> !s.add(item))
                .filter(consumed::add)
                .forEachOrdered(consumer);
        return consumed.size();
    }

    /**
     * Creates a new ArrayList using the given items as contents.
     * If {@code items} is null, an empty list is created and returned.
     * @param items the contents (if any) of the new list
     * @param <E> the element type of the new list
     * @return a new list containing the given contents (if any)
     */
    public static <E> List<E> newArrayList(Iterable<? extends E> items) {
        if (items==null) {
            return new ArrayList<>();
        }
        if (items instanceof Collection) {
            //noinspection unchecked
            return new ArrayList<>((Collection<? extends E>) items);
        }
        List<E> list = new ArrayList<>();
        items.forEach(list::add);
        return list;
    }

    public static <E> String iterableToString(Iterable<E> items, Function<? super E, String> stringFunction) {
        if (items==null) {
            return "null";
        }
        Iterator<E> iter = items.iterator();
        if (!iter.hasNext()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[")
                .append(stringFunction.apply(iter.next()));
        while (iter.hasNext()) {
            sb.append(", ");
            sb.append(stringFunction.apply(iter.next()));
        }
        return sb.append(']').toString();
    }

    public static <E> String iterableToString(Iterable<E> items) {
        return iterableToString(items, String::valueOf);
    }

    /**
     * Do two collections have the same contents (maybe in a different order)?
     * If the collections contain repetitions, this method does <i>not</i> check
     * that they have the same number of repetitions.
     * @param a a collection
     * @param b a collection
     * @return {@code true} if the two collections have the same size and contents
     */
    @SuppressWarnings("SuspiciousMethodCalls")
    public static boolean sameContents(Collection<?> a, Collection<?> b) {
        if (a==b) {
            return true;
        }
        if (a instanceof Set && b instanceof Set) {
            return a.equals(b);
        }
        if (a==null || b==null || a.size()!=b.size()) {
            return false;
        }
        if (a.size() <= 3) {
            return (a.containsAll(b) && b.containsAll(a));
        }
        if (!(a instanceof Set)) {
            a = new HashSet<>(a);
        }
        if (!(b instanceof Set)) {
            b = new HashSet<>(b);
        }
        return a.equals(b);
    }
}
