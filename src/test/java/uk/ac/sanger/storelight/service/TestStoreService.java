package uk.ac.sanger.storelight.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InOrder;
import uk.ac.sanger.storelight.graphql.StoreRequestContext;
import uk.ac.sanger.storelight.model.*;
import uk.ac.sanger.storelight.repo.*;
import uk.ac.sanger.storelight.requests.LocationIdentifier;
import uk.ac.sanger.storelight.requests.StoreInput;
import uk.ac.sanger.storelight.utils.CIStringSet;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests {@link StoreService}
 * @author dr6
 */
public class TestStoreService {
    private EntityManager mockEntityManager;
    private ItemBarcodeValidator mockItemBarcodeValidator;
    private StoreAddressChecker mockStoreAddressChecker;
    private ItemRepo mockItemRepo;
    private LocationRepo mockLocationRepo;
    private StoreRecordRepo mockRecordRepo;

    private StoreService storeService;
    private StoreRequestContext ctxt;

    @BeforeEach
    void setup() {
        ctxt = new StoreRequestContext("apikey", "test", "tester");
        mockEntityManager = mock(EntityManager.class);
        mockItemBarcodeValidator = mock(ItemBarcodeValidator.class);
        mockStoreAddressChecker = mock(StoreAddressChecker.class);
        mockItemRepo = mock(ItemRepo.class);
        mockLocationRepo = mock(LocationRepo.class);
        mockRecordRepo = mock(StoreRecordRepo.class);
        final StoreDB mockDb = mock(StoreDB.class);
        when(mockDb.getLocationRepo()).thenReturn(mockLocationRepo);
        when(mockDb.getItemRepo()).thenReturn(mockItemRepo);
        when(mockDb.getStoreRecordRepo()).thenReturn(mockRecordRepo);

        when(mockItemRepo.save(any())).then(invocation -> invocation.getArgument(0));
        when(mockItemRepo.saveAll(any())).then(invocation -> invocation.getArgument(0));

        storeService = spy(new StoreService(mockEntityManager, mockDb, mockItemBarcodeValidator, mockStoreAddressChecker));
    }

    @ParameterizedTest
    @MethodSource("storeBarcodeTestData")
    public void testStoreBarcode(StoreBarcodeTestData data) {
        String itemBarcode = (data.expectedResult==null ? "ITEM-1" : data.expectedResult.getBarcode());
        LocationIdentifier li = new LocationIdentifier(data.location==null ? 1 : data.location.getId());
        if (data.location!=null) {
            when(mockLocationRepo.get(li)).thenReturn(data.location);
        } else {
            when(mockLocationRepo.get(any())).thenThrow(EntityNotFoundException.class);
        }
        if (data.itemBarcodeError!=null) {
            when(mockItemBarcodeValidator.validateItemBarcodes(any())).thenThrow(new IllegalArgumentException(data.itemBarcodeError));
        }

        if (data.expectedResult!=null) {
            assertEquals(data.expectedResult, storeService.storeBarcode(ctxt, itemBarcode, li, data.address));
            verify(mockItemBarcodeValidator).validateItemBarcodes(any());
            InOrder order = inOrder(mockItemRepo, mockEntityManager);
            order.verify(mockItemRepo).deleteAllByBarcodeIn(List.of(itemBarcode));
            order.verify(mockEntityManager).flush();
            order.verify(mockItemRepo).save(data.expectedResult);
            verify(mockRecordRepo).save(new StoreRecord(itemBarcode, data.address, data.location.getId(), ctxt.getUsername(), ctxt.getApp()));
            return;
        }
        Exception ex = assertThrows(data.expectedException, () -> storeService.storeBarcode(ctxt, itemBarcode, li, data.address));
        if (data.expectedErrorMessage!=null) {
            assertThat(ex).hasMessage(data.expectedErrorMessage);
        }
        verify(mockItemRepo, never()).deleteAllByBarcodeIn(any());
        verify(mockEntityManager, never()).flush();
        verify(mockItemRepo, never()).save(any());
        verify(mockRecordRepo, never()).save(any());
    }

    static Stream<StoreBarcodeTestData> storeBarcodeTestData() {
        final Address A1 = new Address(1, 1);
        final Address A2 = new Address(1, 2);
        final Address A3 = new Address(1, 3);
        Location loc1 = new Location(1, "STO-1");
        Item occupant = new Item(100, "ITEM-100", loc1, A2);
        loc1.getStored().add(occupant);
        Location loc2 = new Location(2, "STO-2", null, null, null, null, new Size(2, 2), null);
        return Stream.of(
                StoreBarcodeTestData.make()
                        .location(loc1)
                        .expectedResult(new Item(null, "ITEM-1", loc1, null)),
                StoreBarcodeTestData.make()
                        .location(loc1)
                        .address(A1)
                        .expectedResult(new Item(null, "ITEM-1", loc1, A1)),
                StoreBarcodeTestData.make()
                        .location(loc2)
                        .address(A1)
                        .expectedResult(new Item(null, "ITEM-1", loc2, A1)),

                StoreBarcodeTestData.make()
                        .location(loc1)
                        .itemBarcodeError("Barcodes are bad.")
                        .expectedErrorMessage("Barcodes are bad."),

                StoreBarcodeTestData.make()
                        .location(loc1)
                        .address(A2)
                        .expectedErrorMessage("There is another item at address A2 in location (id=1)."),

                StoreBarcodeTestData.make()
                        .location(loc2)
                        .address(A3)
                        .expectedErrorMessage("The address A3 is outside the listed size (numRows=2, numColumns=2) " +
                                "for location (id=2)."),

                StoreBarcodeTestData.make()
                        .expectedException(EntityNotFoundException.class) // no location
        );
    }

    @ParameterizedTest
    @MethodSource("storeBarcodesTestData")
    public void testStoreBarcodes(StoreBarcodesTestData data) {
        LocationIdentifier li = new LocationIdentifier(data.location==null ? 1 : data.location.getId());
        CIStringSet barcodeSet;
        if (data.itemBarcodeError==null) {
            barcodeSet = new CIStringSet(data.barcodes);
            when(mockItemBarcodeValidator.validateItemBarcodes(any())).thenReturn(barcodeSet);
        } else {
            barcodeSet = null;
            when(mockItemBarcodeValidator.validateItemBarcodes(any()))
                    .thenThrow(new IllegalArgumentException(data.itemBarcodeError));
        }
        if (data.location!=null) {
            when(mockLocationRepo.get(new LocationIdentifier(data.location.getId())))
                    .thenReturn(data.location);
        } else {
            when(mockLocationRepo.get(any())).thenThrow(EntityNotFoundException.class);
        }
        doAnswer(invocation -> invocation.getArgument(1))
                .when(storeService).storeItems(any(), any(), any());

        if (data.expectedResult!=null) {
            assertEquals(data.expectedResult, storeService.storeBarcodes(ctxt, data.barcodes, li));
            if (data.expectedResult.isEmpty()) {
                verify(storeService, never()).storeItems(any(), any(), any());
            } else {
                verify(storeService).storeItems(ctxt, data.expectedResult, barcodeSet);
            }
            return;
        }

        Exception ex = assertThrows(data.expectedException, () -> storeService.storeBarcodes(ctxt, data.barcodes, li));
        if (data.expectedErrorMessage!=null) {
            assertThat(ex).hasMessage(data.expectedErrorMessage);
        }
        verify(storeService, never()).storeItems(any(), any(), any());
    }

    static Stream <StoreBarcodesTestData> storeBarcodesTestData() {
        Location loc = new Location(1, "STO-1");
        List<Item> items = List.of(new Item("ITEM-1", loc), new Item("ITEM-2", loc));
        List<String> barcodes = items.stream().map(Item::getBarcode).collect(toList());
        return Stream.of(
                new StoreBarcodesTestData(barcodes)
                        .expectedResult(items)
                        .location(loc),
                new StoreBarcodesTestData(barcodes)
                        .itemBarcodeError("Barcodes are bad.")
                        .expectedErrorMessage("Barcodes are bad.")
                        .location(loc),
                new StoreBarcodesTestData(List.of())
                        .location(loc)
                        .expectedResult(List.of()),
                new StoreBarcodesTestData(barcodes)
                        .expectedException(EntityNotFoundException.class)
        );
    }

    @ParameterizedTest
    @MethodSource("storeTestData")
    public void testStore(StoreTestData data) {
        CIStringSet barcodeSet;
        LocationCache mockCache = mock(LocationCache.class);
        doReturn(mockCache).when(storeService).makeLocationCache();
        if (data.locations!=null) {
            when(mockCache.get(any())).then(invocation -> {
                LocationIdentifier li = invocation.getArgument(0);
                Predicate<Location> predicate = (li.getId()!=null ? (loc -> loc.getId().equals(li.getId())) : (loc -> loc.getBarcode().equalsIgnoreCase(li.getBarcode())));
                return data.locations.stream().filter(predicate)
                        .findAny()
                        .orElse(null);
            });
        }
        if (data.locationLookupError!=null) {
            doThrow(new IllegalArgumentException(data.locationLookupError))
                    .when(mockCache).lookUp(any());
        }
        when(mockItemRepo.findAllById(any())).then(invocation -> {
            if (data.locations==null) {
                return List.of();
            }
            Set<Integer> ids = iterableToSet(invocation.getArgument(0));
            return data.locations.stream().filter(loc -> ids.contains(loc.getId()));
        });
        when(mockItemRepo.findAllByBarcodeIn(any())).then(invocation -> {
            if (data.locations==null) {
                return List.of();
            }
            CIStringSet bcs = new CIStringSet();
            Iterable<String> bcsIterable = invocation.getArgument(0);
            for (String bc : bcsIterable) {
                bcs.add(bc);
            }
            return data.locations.stream().filter(loc -> bcs.contains(loc.getBarcode()));
        });
        if (data.itemBarcodeError==null) {
            barcodeSet = new CIStringSet();
            data.storeInputs.forEach(si -> barcodeSet.add(si.getBarcode()));
            when(mockItemBarcodeValidator.validateItemBarcodes(any())).thenReturn(barcodeSet);
        } else {
            barcodeSet = null;
            when(mockItemBarcodeValidator.validateItemBarcodes(any()))
                    .thenThrow(new IllegalArgumentException(data.itemBarcodeError));
        }
        if (data.addressError!=null) {
            doThrow(new IllegalArgumentException(data.addressError))
                    .when(mockStoreAddressChecker).checkItems(any(), any());
        }

        if (data.expectedResult!=null) {
            doReturn(data.expectedResult).when(storeService).storeItems(any(), any(), any());
            assertEquals(data.expectedResult, storeService.store(ctxt, data.storeInputs, data.defaultLi));
            if (data.storeInputs.isEmpty()) {
                verifyNoInteractions(mockCache);
                verifyNoInteractions(mockStoreAddressChecker);
                verify(storeService, never()).storeItems(any(), any(), any());
                return;
            }
            verify(mockCache).lookUp(any());
            List<Item> expectedStoreItems = data.expectedResult.stream()
                    .map(item -> new Item(null, item.getBarcode(), item.getLocation(), item.getAddress()))
                    .collect(toList());
            verify(storeService).storeItems(ctxt, expectedStoreItems, barcodeSet);
            return;
        }
        Exception ex = assertThrows(data.expectedException, () -> storeService.store(ctxt, data.storeInputs, data.defaultLi));
        if (data.expectedErrorMessage!=null) {
            assertThat(ex).hasMessage(data.expectedErrorMessage);
        }
        verify(storeService, never()).storeItems(any(), any(), any());
    }

    static Stream<StoreTestData> storeTestData() {
        Location loc1 = new Location(1, "STO-1");
        Location loc2 = new Location(2, "STO-2");
        Address A2 = new Address(1, 2);
        List<StoreInput> sins = List.of(
                new StoreInput("ITEM-1", null, A2),
                new StoreInput("ITEM-2", new LocationIdentifier(loc2.getBarcode()), null)
        );
        List<Item> result = List.of(
                new Item(1, "ITEM-1", loc1, A2),
                new Item(2, "ITEM-2", loc2, null)
        );
        LocationIdentifier li1 = new LocationIdentifier(loc1.getId());
        List<Location> locations = List.of(loc1, loc2);
        return Stream.of(
                new StoreTestData(List.of())
                        .expectedResult(List.of()),
                new StoreTestData(sins)
                        .defaultLi(li1)
                        .expectedResult(result)
                        .locations(locations),
                new StoreTestData(sins)
                        .expectedErrorMessage("A location must be specified for each item."),
                new StoreTestData(sins)
                        .defaultLi(li1)
                        .addressError("Bad addresses in locations.")
                        .expectedErrorMessage("Bad addresses in locations."),
                new StoreTestData(sins).defaultLi(li1)
                        .itemBarcodeError("Bad item barcodes.")
                        .expectedErrorMessage("Bad item barcodes."),
                new StoreTestData(sins).defaultLi(li1)
                        .locationLookupError("No such thing.")
                        .expectedErrorMessage("No such thing.")
        );
    }

    @Test
    public void testMakeLocationCache() {
        assertNotNull(storeService.makeLocationCache());
    }

    private static <E> Set<E> iterableToSet(Iterable<E> iterable) {
        if (iterable==null) {
            return null;
        }
        if (iterable instanceof Set) {
            return (Set<E>) iterable;
        }
        Set<E> set = new HashSet<>();
        iterable.forEach(set::add);
        return set;
    }

    @Test
    public void testStoreItems() {
        Location loc = new Location(1, "STO-1");
        List<Item> items = List.of(new Item("ITEM-1", loc), new Item("ITEM-2", loc));
        List<Item> savedItems = List.of(new Item(1, "ITEM-1", loc, null), new Item(2, "ITEM-2", loc, null));
        CIStringSet barcodeSet = CIStringSet.of("ITEM-1", "ITEM-2");
        InOrder inOrder = inOrder(mockItemRepo, mockEntityManager);
        when(mockItemRepo.saveAll(any())).thenReturn(savedItems);

        assertSame(savedItems, storeService.storeItems(ctxt, items, barcodeSet));
        inOrder.verify(mockItemRepo).deleteAllByBarcodeIn(barcodeSet);
        inOrder.verify(mockEntityManager).flush();
        inOrder.verify(mockItemRepo).saveAll(items);
        List<StoreRecord> records = items.stream()
                .map(item -> new StoreRecord(item.getBarcode(), item.getAddress(), item.getLocation().getId(),
                        ctxt.getUsername(), ctxt.getApp()))
                .collect(toList());
        verify(mockRecordRepo).saveAll(records);
    }

    @ParameterizedTest
    @MethodSource("transferTestData")
    public void testTransfer(TransferTestData data) {
        StoreRequestContext ctxt = new StoreRequestContext(null, "STAN", "user1");
        if (data.source==null) {
            when(mockLocationRepo.get(data.sourceLi)).thenThrow(new EntityNotFoundException("No such location."));
        } else {
            when(mockLocationRepo.get(data.sourceLi)).thenReturn(data.source);
        }
        if (data.destination==null) {
            when(mockLocationRepo.get(data.destinationLi)).thenThrow(new EntityNotFoundException("No such location."));
        } else {
            when(mockLocationRepo.get(data.destinationLi)).thenReturn(data.destination);
        }

        CIStringSet barcodes;
        if (data.expectedResult!=null) {
            barcodes = data.expectedResult.stream()
                    .map(Item::getBarcode)
                    .collect(Collectors.toCollection(CIStringSet::new));
        } else {
            barcodes = null;
        }

        if (data.expectedErrorMessage==null) {
            assert data.expectedResult != null;
            if (!data.expectedResult.isEmpty()) {
                doAnswer(invocation -> invocation.getArgument(1))
                        .when(storeService).storeItems(any(), any(), any());
            }
            assertEquals(data.expectedResult, storeService.transfer(ctxt, data.sourceLi, data.destinationLi));
            if (data.expectedResult.isEmpty()) {
                verifyNoInteractions(mockStoreAddressChecker);
                verify(storeService, never()).storeItems(any(), any(), any());
            } else {
                verify(mockStoreAddressChecker).checkItems(data.expectedResult, barcodes);
                verify(storeService).storeItems(ctxt, data.expectedResult, barcodes);
            }
            return;
        }

        if (data.checkerError!=null) {
            doThrow(new IllegalArgumentException(data.checkerError))
                    .when(mockStoreAddressChecker).checkItems(any(), any());

        }

        assertThat(assertThrows(RuntimeException.class, () -> storeService.transfer(ctxt, data.sourceLi, data.destinationLi)))
                .hasMessage(data.expectedErrorMessage);

        if (data.checkerError!=null) {
            verify(mockStoreAddressChecker).checkItems(data.expectedResult, barcodes);
        } else {
            verifyNoInteractions(mockStoreAddressChecker);
        }
        verify(storeService, never()).storeItems(any(), any(), any());

    }

    static Stream<TransferTestData> transferTestData() {
        Location source = new Location(100, "STO-100");
        source.setStored(List.of(
                new Item(10, "ITEM-10", source, new Address(1,1)),
                new Item(11, "ITEM-11", source, null)
        ));
        Location emptySource = new Location(101, "STO-101");
        Location destination = new Location(200, "STO-200");
        List<Item> newItems = List.of(
                new Item(null, "ITEM-10", destination, new Address(1,1)),
                new Item(null, "ITEM-11", destination, null)
        );
        LocationIdentifier unknownLi = new LocationIdentifier(404);

        return Arrays.stream(new TransferTestData[] {
                new TransferTestData("success", source, destination).expectedResult(newItems),
                new TransferTestData("empty source", emptySource, destination).expectedResult(List.of()),
                new TransferTestData("same location", source, source).expectedErrorMessage("The source cannot be the destination."),
                new TransferTestData("same location with different identifier", source, source).destinationLi(new LocationIdentifier(source.getBarcode())).expectedErrorMessage("The source cannot be the destination."),
                new TransferTestData("unknown destination", source, null).destinationLi(unknownLi).expectedErrorMessage("No such location."),
                new TransferTestData("unknown source", null, destination).sourceLi(unknownLi).expectedErrorMessage("No such location."),
                new TransferTestData("checker error", source, destination).expectedResult(newItems).checkerError("Cannot put that in there."),
        });
    }

    private static class StoreBarcodeTestData {
        private Address address;
        private String itemBarcodeError;
        private String expectedErrorMessage;
        private Class<? extends Exception> expectedException;
        private Location location;
        private Item expectedResult;

        public StoreBarcodeTestData address(Address address) {
            this.address = address;
            return this;
        }

        public StoreBarcodeTestData expectedResult(Item expectedResult) {
            this.expectedResult = expectedResult;
            return this;
        }

        public StoreBarcodeTestData itemBarcodeError(String itemBarcodeError) {
            this.itemBarcodeError = itemBarcodeError;
            return this;
        }

        public StoreBarcodeTestData expectedErrorMessage(String expectedErrorMessage) {
            this.expectedErrorMessage = expectedErrorMessage;
            if (expectedException==null) {
                expectedException = IllegalArgumentException.class;
            }
            return this;
        }

        public StoreBarcodeTestData expectedException(Class<? extends Exception> expectedException) {
            this.expectedException = expectedException;
            return this;
        }

        public StoreBarcodeTestData location(Location location) {
            this.location = location;
            return this;
        }

        public static StoreBarcodeTestData make() {
            return new StoreBarcodeTestData();
        }

        @Override
        public String toString() {
            if (this.expectedResult!=null) {
                return this.expectedResult.toString();
            }
            if (expectedErrorMessage!=null) {
                return this.expectedErrorMessage;
            }
            return this.expectedException.toString();
        }
    }

    private static class StoreBarcodesTestData {
        List<String> barcodes;
        Location location;
        List<Item> expectedResult;
        String itemBarcodeError;
        String expectedErrorMessage;
        Class<? extends Exception> expectedException;

        StoreBarcodesTestData(List<String> barcodes) {
            this.barcodes = barcodes;
        }

        public StoreBarcodesTestData location(Location location) {
            this.location = location;
            return this;
        }

        public StoreBarcodesTestData expectedResult(List<Item> expectedResult) {
            this.expectedResult = expectedResult;
            return this;
        }

        public StoreBarcodesTestData itemBarcodeError(String itemBarcodeError) {
            this.itemBarcodeError = itemBarcodeError;
            return this;
        }

        public StoreBarcodesTestData expectedErrorMessage(String expectedErrorMessage) {
            this.expectedErrorMessage = expectedErrorMessage;
            if (expectedException==null) {
                expectedException = IllegalArgumentException.class;
            }
            return this;
        }

        public StoreBarcodesTestData expectedException(Class<? extends Exception> expectedException) {
            this.expectedException = expectedException;
            return this;
        }

        @Override
        public String toString() {
            if (this.expectedResult!=null) {
                return this.expectedResult.toString();
            }
            if (expectedErrorMessage!=null) {
                return this.expectedErrorMessage;
            }
            return this.expectedException.toString();
        }
    }

    private static class StoreTestData {
        List<StoreInput> storeInputs;
        LocationIdentifier defaultLi;
        String itemBarcodeError;
        List<Location> locations;
        String addressError;
        String locationLookupError;
        List<Item> expectedResult;
        String expectedErrorMessage;
        Class<? extends Exception> expectedException;

        public StoreTestData(List<StoreInput> storeInputs) {
            this.storeInputs = storeInputs;
        }

        public StoreTestData defaultLi(LocationIdentifier defaultLi) {
            this.defaultLi = defaultLi;
            return this;
        }

        public StoreTestData itemBarcodeError(String itemBarcodeError) {
            this.itemBarcodeError = itemBarcodeError;
            return this;
        }

        public StoreTestData addressError(String addressError) {
            this.addressError = addressError;
            return this;
        }

        public StoreTestData expectedResult(List<Item> expectedResult) {
            this.expectedResult = expectedResult;
            return this;
        }

        public StoreTestData expectedErrorMessage(String expectedErrorMessage) {
            this.expectedErrorMessage = expectedErrorMessage;
            if (expectedException==null) {
                expectedException = IllegalArgumentException.class;
            }
            return this;
        }

        public StoreTestData locations(List<Location> locations) {
            this.locations = locations;
            return this;
        }

        public StoreTestData locationLookupError(String locationLookupError) {
            this.locationLookupError = locationLookupError;
            return this;
        }
    }

    private static class TransferTestData {
        String desc;
        Location source;
        Location destination;
        LocationIdentifier sourceLi;
        LocationIdentifier destinationLi;
        String checkerError;
        String expectedErrorMessage;
        List<Item> expectedResult;

        public TransferTestData(String desc) {
            this.desc = desc;
        }

        public TransferTestData(String desc, Location source, Location destination) {
            this(desc);
            source(source);
            destination(destination);
        }

        public TransferTestData source(Location source) {
            this.source = source;
            if (source!=null) {
                this.sourceLi = new LocationIdentifier(source.getId());
            }
            return this;
        }

        public TransferTestData destination(Location destination) {
            this.destination = destination;
            if (destination!=null) {
                this.destinationLi = new LocationIdentifier(destination.getId());
            }
            return this;
        }

        public TransferTestData sourceLi(LocationIdentifier sourceLi) {
            this.sourceLi = sourceLi;
            return this;
        }

        public TransferTestData destinationLi(LocationIdentifier destinationLi) {
            this.destinationLi = destinationLi;
            return this;
        }

        public TransferTestData checkerError(String checkerError) {
            this.checkerError = checkerError;
            return expectedErrorMessage(checkerError);
        }

        public TransferTestData expectedErrorMessage(String expectedErrorMessage) {
            this.expectedErrorMessage = expectedErrorMessage;
            return this;
        }

        public TransferTestData expectedResult(List<Item> expectedResult) {
            this.expectedResult = expectedResult;
            return this;
        }

        @Override
        public String toString() {
            return this.desc;
        }
    }
}
