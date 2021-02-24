package uk.ac.sanger.storelight.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.sanger.storelight.model.Location;
import uk.ac.sanger.storelight.repo.LocationRepo;
import uk.ac.sanger.storelight.requests.LocationIdentifier;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests {@link LocationCache}
 * @author dr6
 */
public class TestLocationCache {
    LocationRepo mockLocationRepo;
    Set<Integer> idsLookedUp;
    Set<String> bcsLookedUp;
    List<Location> locations;

    @BeforeEach
    void setup() {
        mockLocationRepo = mock(LocationRepo.class);
        locations = IntStream.range(1, 6)
                .mapToObj(id -> new Location(id, "STO-"+id))
                .collect(toList());
        when(mockLocationRepo.findAllById(any())).thenAnswer(invocation -> {
            Set<Integer> ids = invocation.getArgument(0);
            idsLookedUp = new HashSet<>(ids);
            return locations.stream().filter(loc -> ids.contains(loc.getId())).collect(toList());
        });
        when(mockLocationRepo.findAllByBarcodeIn(any())).thenAnswer(invocation -> {
            Set<String> bcs = invocation.getArgument(0);
            bcsLookedUp = new HashSet<>(bcs);
            return locations.stream().filter(loc -> bcs.contains(loc.getBarcode())).collect(toList());
        });
    }

    @Test
    public void testLocationCache() {
        Stream<LocationIdentifier> liStream = Stream.of(1, 2, "sto-3", "STO-4", "STO-5", "STO-1", "STO-3", 3)
                .map(obj -> {
                    if (obj instanceof String) {
                        return new LocationIdentifier((String) obj);
                    } else {
                        return new LocationIdentifier((Integer) obj);
                    }
                });
        LocationCache cache = new LocationCache(mockLocationRepo);
        cache.lookUp(liStream);
        verify(mockLocationRepo).findAllById(any());
        verify(mockLocationRepo).findAllByBarcodeIn(any());
        assertEquals(Set.of(1,2,3), idsLookedUp);
        assertEquals(Set.of("STO-4", "STO-5"), bcsLookedUp);

        for (Location loc : locations) {
            assertEquals(loc, cache.get(new LocationIdentifier(loc.getId())));
            assertEquals(loc, cache.get(new LocationIdentifier(loc.getBarcode())));
            assertEquals(loc, cache.get(new LocationIdentifier(loc.getBarcode().toLowerCase())));
        }

        assertThrows(IllegalArgumentException.class, () -> cache.get(new LocationIdentifier((String) null)));

        verifyNoMoreInteractions(mockLocationRepo);
    }

    @Test
    public void testUnknownLocations() {
        {
            Stream<LocationIdentifier> liStream = Stream.of(1, 2, 404, 405)
                    .map(LocationIdentifier::new);
            LocationCache cache = new LocationCache(mockLocationRepo);
            assertThat(assertThrows(IllegalArgumentException.class, () -> cache.lookUp(liStream)))
                    .hasMessage("Unknown location ids: [404, 405]");
        }
        {
            Stream<LocationIdentifier> liStream = Stream.of("STO-1", "STO-404")
                    .map(LocationIdentifier::new);
            LocationCache cache = new LocationCache(mockLocationRepo);
            assertThat(assertThrows(IllegalArgumentException.class, () -> cache.lookUp(liStream)))
                    .hasMessage("Unknown location barcode: [\"STO-404\"]");
        }
    }
}
