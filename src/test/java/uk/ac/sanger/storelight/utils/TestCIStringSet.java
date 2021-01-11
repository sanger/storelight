package uk.ac.sanger.storelight.utils;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests {@link CIStringSet}
 * @author dr6
 */
public class TestCIStringSet {
    @Test
    public void testSetBasics() {
        CIStringSet strings = new CIStringSet();
        assertThat(strings).hasSize(0);
        assertThat(strings).isEmpty();
        assertTrue(strings.add("Alpha"));
        assertTrue(strings.add("Beta"));
        assertFalse(strings.add("alpha"));
        assertFalse(strings.add("BETA"));

        assertThat(strings).hasSize(2);
        assertThat(strings).isNotEmpty();
        for (String s : new String[] { "Alpha", "ALPHA", "beta", "BETA"}) {
            assertTrue(strings.contains(s));
        }
        assertEquals(new ArrayList<>(strings), List.of("Alpha", "Beta"));
        CIStringSet other = new CIStringSet(List.of("BETA", "alpha"));
        assertEquals(strings, other);
        assertEquals(strings.hashCode(), other.hashCode());

        assertArrayEquals(strings.toArray(new String[0]), new String[] { "Alpha", "Beta" });
        assertEquals(strings.toString(), "CIStringSet{Alpha, Beta}");
        assertTrue(strings.containsAll(CIStringSet.of("BETA", "ALPHA")));
    }

    @Test
    public void testRemovals() {
        CIStringSet strings = CIStringSet.of("Alpha", "Beta", "Gamma", "Delta");
        assertTrue(strings.containsAll(List.of("ALPHA", "BETA", "gamma", "delta")));
        assertTrue(strings.remove("BETA"));
        assertFalse(strings.remove("BETA"));
        assertThat(strings.size()).isEqualTo(3);
        assertFalse(strings.contains("beta"));
        assertEquals(CIStringSet.of("alpha", "GAMMA", "delta"), strings);

        assertFalse(strings.containsAll(List.of("ALPHA", "BETA", "gamma", "delta")));
        strings.retainAll(List.of("Alpha", "ALPHA", "gamma", "DELTA", "banana"));
        assertEquals(CIStringSet.of("Alpha", "Gamma", "Delta"), strings);

        strings.retainAll(List.of("alpha", "banana", "echo", "alpha"));
        assertEquals(CIStringSet.of("alpha"), strings);

        strings.clear();
        assertThat(strings).isEmpty();

        strings.addAll(List.of("Alabama", "Alaska", "Arizona", "Arkansas"));
        strings.removeAll(List.of("California", "Colorado"));
        assertThat(strings).hasSize(4);

        strings.removeAll(List.of("alaska", "colorado"));
        assertEquals(CIStringSet.of("Alabama", "Arizona", "ARKANSAS"), strings);
    }

    @Test
    public void testIterator() {
        CIStringSet strings = CIStringSet.of("Alabama", "Alaska", "Arizona", "Arkansas");
        Iterator<String> iter = strings.iterator();
        assertTrue(iter.hasNext());
        assertEquals("Alabama", iter.next());
        assertTrue(iter.hasNext());
        assertEquals("Alaska", iter.next());
        iter.remove();
        assertThrows(IllegalStateException.class, iter::remove);
        assertTrue(iter.hasNext());
        assertEquals(iter.next(), "Arizona");
        assertTrue(iter.hasNext());
        assertEquals(iter.next(), "Arkansas");
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::next);
        assertEquals(CIStringSet.of("Alabama", "ARIZONA", "arkansas"), strings);
    }
}
