package uk.ac.sanger.storelight.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.sanger.storelight.graphql.StoreRequestContext;
import uk.ac.sanger.storelight.model.*;
import uk.ac.sanger.storelight.repo.*;
import uk.ac.sanger.storelight.requests.LocationIdentifier;

import javax.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests {@link UnstoreService}
 * @author dr6
 */
public class TestUnstoreService {
    private LocationRepo mockLocationRepo;
    private ItemRepo mockItemRepo;
    private StoreRecordRepo mockRecordRepo;
    private UnstoreService unstoreService;
    private StoreRequestContext ctxt;

    @BeforeEach
    void setup() {
        mockLocationRepo = mock(LocationRepo.class);
        mockItemRepo = mock(ItemRepo.class);
        mockRecordRepo = mock(StoreRecordRepo.class);
        StoreDB mockDb = mock(StoreDB.class);
        when(mockDb.getItemRepo()).thenReturn(mockItemRepo);
        when(mockDb.getLocationRepo()).thenReturn(mockLocationRepo);
        when(mockDb.getStoreRecordRepo()).thenReturn(mockRecordRepo);
        unstoreService = new UnstoreService(mockDb);
        ctxt = new StoreRequestContext("apikey", "test", "tester");
    }

    @Test
    public void testUnstoreBarcode() {
        when(mockItemRepo.findByBarcode("X")).thenReturn(Optional.empty());
        assertNull(unstoreService.unstoreBarcode(ctxt, "X"));
        verify(mockItemRepo, never()).delete(any());

        Item item = new Item(1, "ITEM-1", new Location(10, "STO-10"), null);
        when(mockItemRepo.findByBarcode(item.getBarcode())).thenReturn(Optional.of(item));

        assertEquals(item, unstoreService.unstoreBarcode(ctxt, item.getBarcode()));
        verify(mockItemRepo).delete(item);
        verifyUnstoreRecords(List.of(item));
    }

    @Test
    public void testUnstoreBarcodes() {
        List<String> list1 = List.of("X", "Y", "Z");
        when(mockItemRepo.findAllByBarcodeIn(list1)).thenReturn(List.of());
        assertThat(unstoreService.unstoreBarcodes(ctxt, list1)).isEmpty();
        verify(mockItemRepo, never()).deleteAll(any());
        Location loc = new Location(1, "STO-1");
        List<String> list2 = List.of("A", "B", "C");
        List<Item> items = List.of(new Item(1, "A", loc, null), new Item(2, "B", loc, null));
        when(mockItemRepo.findAllByBarcodeIn(list2)).thenReturn(items);

        assertThat(unstoreService.unstoreBarcodes(ctxt, list2)).hasSameElementsAs(items);
        verify(mockItemRepo).deleteAll(items);
        verifyUnstoreRecords(items);
    }

    @Test
    public void testEmpty() {
        LocationIdentifier li0 = new LocationIdentifier(0);
        when(mockLocationRepo.get(li0)).thenThrow(EntityNotFoundException.class);
        assertThrows(EntityNotFoundException.class, () -> unstoreService.empty(ctxt, li0));

        Location loc1 = new Location(1, "LOC-1");
        LocationIdentifier li1 = new LocationIdentifier(loc1.getId());
        when(mockLocationRepo.get(li1)).thenReturn(loc1);

        assertThat(unstoreService.empty(ctxt, li1)).isEmpty();
        verify(mockItemRepo, never()).deleteAll();

        Location loc2 = new Location(2, "LOC-2");
        Item item = new Item(100, "ITEM-100", loc2, null);
        loc2.getStored().add(item);
        LocationIdentifier li2 = new LocationIdentifier(loc2.getId());
        when(mockLocationRepo.get(li2)).thenReturn(loc2);
        assertThat(unstoreService.empty(ctxt, li2)).containsOnly(item);
        verify(mockItemRepo).deleteAll(List.of(item));
        verifyUnstoreRecords(List.of(item));
    }

    private void verifyUnstoreRecords(List<Item> items) {
        List<StoreRecord> records = items.stream()
                .map(item -> new StoreRecord(item.getBarcode(), null, null, ctxt.getUsername(), ctxt.getApp()))
                .collect(toList());
        verify(mockRecordRepo).saveAll(records);
    }
}
