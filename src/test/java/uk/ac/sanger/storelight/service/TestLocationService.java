package uk.ac.sanger.storelight.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import uk.ac.sanger.storelight.graphql.StoreRequestContext;
import uk.ac.sanger.storelight.model.*;
import uk.ac.sanger.storelight.repo.*;
import uk.ac.sanger.storelight.requests.LocationIdentifier;
import uk.ac.sanger.storelight.requests.LocationInput;

import javax.persistence.EntityNotFoundException;
import java.util.*;
import java.util.stream.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests {@link LocationService}
 * @author dr6
 */
public class TestLocationService {
    private static final String NEWBC = "STO-88";
    private LocationRepo mockLocationRepo;
    private LocationService locationService;
    private StoreRequestContext ctxt;

    @BeforeEach
    void setup() {
        StoreDB mockDb = mock(StoreDB.class);
        ctxt = new StoreRequestContext("apikey", "test", "tester");
        mockLocationRepo = mock(LocationRepo.class);
        BarcodeSeedRepo mockBarcodeSeedRepo = mock(BarcodeSeedRepo.class);
        when(mockDb.getBarcodeSeedRepo()).thenReturn(mockBarcodeSeedRepo);
        when(mockDb.getLocationRepo()).thenReturn(mockLocationRepo);

        when(mockBarcodeSeedRepo.createStoreBarcode()).thenReturn(NEWBC);
        when(mockLocationRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        locationService = spy(new LocationService(mockDb));
    }

    @ParameterizedTest
    @MethodSource("createLocationArguments")
    public void testCreateLocation(Object expectedResult, LocationInput lin, Location existingLocation) {
        if (existingLocation!=null) {
            when(mockLocationRepo.getById(existingLocation.getId())).thenReturn(existingLocation);
        } else {
            when(mockLocationRepo.getById(any())).thenThrow(EntityNotFoundException.class);
        }
        if (expectedResult instanceof Location) {
            Location result = locationService.createLocation(ctxt, lin);
            assertEquals(expectedResult, result);
            verify(mockLocationRepo).save(result);
        } else if (expectedResult instanceof Class) {
            //noinspection unchecked
            assertThrows((Class<? extends Throwable>) expectedResult, () -> locationService.createLocation(ctxt, lin));
            verify(mockLocationRepo, never()).save(any());
        } else if (expectedResult instanceof String) {
            assertThat(assertThrows(IllegalArgumentException.class, () -> locationService.createLocation(ctxt, lin)))
                    .hasMessage((String) expectedResult);
            verify(mockLocationRepo, never()).save(any());
        }
    }

    static Stream<Arguments> createLocationArguments() {
        Location par1 = new Location(1, "STO-1", null, null, null, null);
        Location par2 = new Location(2, "STO-2", null, null, null, new Size(2, 2));
        Address A1 = new Address(1, 1);
        Address A2 = new Address(1, 2);
        Address A3 = new Address(1, 3);
        Location loc3 = new Location(3, "STO-3", null, par1, A2, null);
        par1.getChildren().add(loc3);
        String longDescription = IntStream.range(0, Location.MAX_DESCRIPTION/2).mapToObj(String::valueOf).collect(Collectors.joining());

        return Arrays.stream(new Object[][]{
                {
                        new Location(null, NEWBC, null, null, null, null),
                        new LocationInput(null, null, null, null)
                },
                {
                        new Location(null, NEWBC, null, null, null, null),
                        new LocationInput("    ", null, null, null)
                },
                {
                        new Location(null, NEWBC, "Bananas", par1, A1, new Size(3, 4)),
                        new LocationInput("Bananas ", par1.getId(), A1, new Size(3, 4)),
                        par1
                },
                {
                        EntityNotFoundException.class,
                        new LocationInput(null, 404, null, null),
                },
                {
                        "A location with no parent cannot have an address.",
                        new LocationInput(null, null, A1, null)
                },
                {
                        "The address A3 is outside the listed size (numRows=2, numColumns=2) for the parent.",
                        new LocationInput(null, par2.getId(), A3, null),
                        par2
                },
                {
                        "There is already a location at address A2 in the parent.",
                        new LocationInput(null, par1.getId(), A2, null),
                        par1
                },
                {
                        "Location description is too long (max length: "+Location.MAX_DESCRIPTION+").",
                        new LocationInput(longDescription, null, null, null)
                },
        }).map(arr -> (arr.length==2 ? Arguments.of(arr[0], arr[1], null) : Arguments.of(arr)));
    }

    @ParameterizedTest
    @MethodSource("editLocationsArguments")
    public void testEditLocation(Location location, Map<String, ?> changes, Location parent, Object result) {
        LocationIdentifier li = new LocationIdentifier(location.getId());
        when(mockLocationRepo.get(li)).thenReturn(location);
        if (parent!=null) {
            when(mockLocationRepo.getById(parent.getId())).thenReturn(parent);
        }
        String validationError = (result instanceof String ? (String) result : null);
        Location expectedSaved = (result instanceof Location ? (Location) result : null);
        Location expectedResult = (expectedSaved!=null ? expectedSaved : location);

        if (validationError!=null) {
            doThrow(new IllegalArgumentException(validationError)).when(locationService).validateChanges(location, changes);
            assertThat(assertThrows(IllegalArgumentException.class, () -> locationService.editLocation(ctxt, li, changes)))
                    .hasMessage(validationError);
        } else {
            doNothing().when(locationService).validateChanges(any(), any());
            assertEquals(expectedResult, locationService.editLocation(ctxt, li, changes));
            verify(locationService).validateChanges(location, changes);
        }
        if (expectedSaved!=null) {
            ArgumentCaptor<Location> captor = ArgumentCaptor.forClass(Location.class);
            verify(mockLocationRepo).save(captor.capture());
            Location saved = captor.getValue();
            assertEquals(expectedSaved, saved);
            assertEquals(expectedSaved.getId(), saved.getId());
            assertEquals(expectedSaved.getAddress(), saved.getAddress());
            assertEquals(expectedSaved.getSize(), saved.getSize());
            assertEquals(expectedSaved.getBarcode(), saved.getBarcode());
            if (expectedSaved.getParent()==null) {
                assertNull(saved.getParent());
            } else {
                assertEquals(expectedSaved.getParent().getId(), saved.getParent().getId());
            }
        } else {
            verify(mockLocationRepo, never()).save(any());
        }
    }

    static Stream<Arguments> editLocationsArguments() {
        final Address A2 = new Address(1,2);
        final Address B3 = new Address(2,3);
        final Size size34 = new Size(3, 4);
        final Size size45 = new Size(4, 5);

        Location parent = new Location(1, "STO-1");
        Location newParent = new Location(2, "STO-2");

        Map<String, Object> noChange = Map.of();
        Map<String, Object> changeAll = Map.of("description", "  New description.  ",
                "parentId", newParent.getId(), "address", B3, "size", sizeMap(4,5));
        Map<String, Object> blankAll = new HashMap<>(changeAll.size());
        changeAll.keySet().forEach(key -> blankAll.put(key, null));

        return Stream.of(
                Arguments.of(new Location(3, "STO-3"), Map.of("description", "foo"), null, "Validation failed."),
                Arguments.of(new Location(3, "STO-3"), noChange, null, null),
                Arguments.of(new Location(3, "STO-3"), blankAll, null, null),
                Arguments.of(new Location(3, "STO-3"), Map.of("description", "    "), null, null),
                Arguments.of(new Location(3, "STO-3"), changeAll, newParent, new Location(3, "STO-3", "New description.", newParent, B3, size45)),

                Arguments.of(new Location(3, "STO-3", "Complex location.", parent, A2, size34), noChange, null, null),
                Arguments.of(new Location(3, "STO-3", "Complex location.", parent, A2, size34), Map.of("description", "   "),
                        parent, new Location(3, "STO-3", null, parent, A2, size34)),
                Arguments.of(new Location(3, "STO-3", "Complex location.", parent, A2, size34), changeAll, newParent,
                        new Location(3, "STO-3", "New description.", newParent, B3, size45)),
                Arguments.of(new Location(3, "STO-3", "Complex location.", parent, A2, size34), blankAll, null,
                        new Location(3, "STO-3"))
        );
    }

    private static Map<String, Integer> sizeMap(int numRows, int numColumns) {
        return Map.of("numRows", numRows, "numColumns", numColumns);
    }

    @ParameterizedTest
    @MethodSource("validateChangesArguments")
    public void testValidateChanges(ValidateChangesData data) {
        if (data.parent!=null) {
            when(mockLocationRepo.findById(data.parent.getId())).thenReturn(Optional.of(data.parent));
        } else {
            when(mockLocationRepo.findById(any())).thenReturn(Optional.empty());
        }
        doReturn(data.cycleError).when(locationService).checkCycle(data.location, data.parent!=null ? data.parent : data.location.getParent());
        doReturn(data.parentError).when(locationService).checkParentWithAddress(data.location, data.parent!=null ? data.parent : data.location.getParent(),
                data.newAddress!=null ? data.newAddress: data.location.getAddress());

        if (data.expectedError==null) {
            locationService.validateChanges(data.location, data.changes);
            if (data.parent!=null) {
                verify(locationService).checkCycle(data.location, data.parent);
            }
            if (data.newAddress!=null || !data.changes.containsKey("address") && data.location.getAddress()!=null) {
                Location parent = (data.parent!=null ? data.parent : data.location.getParent());
                verify(locationService).checkParentWithAddress(data.location, parent, data.newAddress!=null ? data.newAddress : data.location.getAddress());
            }
        } else {
            assertThat(assertThrows(IllegalArgumentException.class, () -> locationService.validateChanges(data.location, data.changes)))
                    .hasMessage(data.expectedError);
        }
    }

    static Stream<ValidateChangesData> validateChangesArguments() {
        Location a = new Location(1, "STO-1");
        Location b = new Location(2, "STO-2");
        Location c = new Location(3, "STO-3");
        c.setParent(a);
        a.getChildren().add(c);
        final Address A1 = new Address(1,1);
        final Map<String, Integer> sizeMap = Map.of("numRows", 2, "numColumns", 3);
        final Map<String, Object> fullFields = Map.of("address", A1, "description", "Ilikepie", "parentId", b.getId(), "size", sizeMap);
        final Map<String, Object> nullFields = new HashMap<>(fullFields.size());
        for (String key : fullFields.keySet()) {
            nullFields.put(key, null);
        }
        String longDesc = IntStream.range(0, Location.MAX_DESCRIPTION/2).mapToObj(String::valueOf).collect(Collectors.joining());
        String longDescError = "Description too long ("+longDesc.length()+"). Max length is "+Location.MAX_DESCRIPTION+".";
        return Stream.of(
                new ValidateChangesData(c).changes(Map.of()),
                new ValidateChangesData(c).changes(fullFields).parent(b).newAddress(A1),
                new ValidateChangesData(c).changes(nullFields),
                new ValidateChangesData(c).change("parentId", b.getId()).parent(b),
                new ValidateChangesData(c).change("address", A1).newAddress(A1),

                new ValidateChangesData(c).changes(fullFields).parent(b).newAddress(A1).cycleError("Problem with cycles.").expectedError("Problem with cycles."),
                new ValidateChangesData(c).changes(fullFields).parent(b).newAddress(A1).parentError("Problem with parent and address.").expectedError("Problem with parent and address."),
                new ValidateChangesData(c).change("description", longDesc).expectedError(longDescError),
                new ValidateChangesData(c).change("description", 17).expectedError("Require description to be a string, but received java.lang.Integer."),
                new ValidateChangesData(c).change("parentId", c.getId()).expectedError("A location cannot be its own parent."),
                new ValidateChangesData(c).change("parentId", "Bananas").expectedError("Require parentId to be an integer, but received java.lang.String."),
                new ValidateChangesData(c).change("parentId", 404).expectedError("Invalid parent id: 404."),
                new ValidateChangesData(c).change("address", "bananas").expectedError("Require address to be an address, but received java.lang.String."),
                new ValidateChangesData(c).change("size", "bananas").expectedError("Require size to be a mapping of numRows and numColumns, but received java.lang.String."),
                new ValidateChangesData(c).change("size", Map.of("x", 1)).expectedError("Received size with invalid contents."),
                new ValidateChangesData(c).change("size", Map.of("numRows", 1, "numColumns", "boop")).expectedError("Received size with invalid contents."),
                new ValidateChangesData(c).change("size", Map.of("numRows", 4, "numColumns", 0)).expectedError("Fields in size must be greater than zero."),
                new ValidateChangesData(c).change("Bananas", "Alpha").change("description", "Foo").change("Rhubarb", 4).expectedError("Invalid fields: [Bananas, Rhubarb]."),
                new ValidateChangesData(c).change("description", 17).change("parentId", b.getId()).change("address", A1).change("coke", "pepsi").parent(b).newAddress(A1).cycleError("Cycle bad.")
                        .expectedError("Invalid field: [coke]. Require description to be a string, but received java.lang.Integer. Cycle bad.")
        );
    }

    @ParameterizedTest
    @MethodSource("checkParentWithAddressArguments")
    public void testCheckParentWithAddress(Location loc, Location parent, Address address,
                                           String expectedError) {
        assertEquals(expectedError, locationService.checkParentWithAddress(loc, parent, address));
    }

    static Stream<Arguments> checkParentWithAddressArguments() {
        Location loc1 = new Location(1, "STO-1", null, null, null, null);
        Location loc2 = new Location(2, "STO-2", null, null, null, new Size(2,2));
        Location loc3 = new Location(3, "STO-3", null, loc1, new Address(3,4), null);
        loc1.getChildren().add(loc3);
        Location loc4 = new Location(4, "STO-4", null, null, null, null);
        return Stream.of(
                Arguments.of(loc4, loc1, new Address(1,2), null),
                Arguments.of(loc4, loc1, null, null, null),
                Arguments.of(loc4, loc2, new Address(1,2), null),
                Arguments.of(loc4, loc2, null, null, null),

                Arguments.of(loc4, loc1, new Address(3,4), "Address C4 is occupied by another location."),
                Arguments.of(loc4, loc2, new Address(3,4), "Address C4 is out of bounds for the specified parent."),
                Arguments.of(loc4, null, new Address(3,4), "A location without a parent cannot have an address.")
        );
    }

    @ParameterizedTest
    @MethodSource("checkCycleArguments")
    public void testCheckCycle(Location loc, Location parent, String expectedError) {
        assertEquals(expectedError, locationService.checkCycle(loc, parent));
    }

    static Stream<Arguments> checkCycleArguments() {
        Location a = new Location(1, "STO-1", null, null, null, null);
        Location a1 = new Location(2, "STO-2", null, a, null, null);
        Location a2 = new Location(3, "STO-3", null, a, null, null);
        Location b = new Location(4, "STO-4", null, null, null, null);
        Location a11 = new Location(5, "STO-5", null, a1, null, null);

        a.getChildren().add(a1);
        a.getChildren().add(a2);
        a1.getChildren().add(a11);

        return Stream.of(
                Arguments.of(a, a, "Location cannot be the parent of itself."),
                Arguments.of(a1, a, null),
                Arguments.of(a11, a, null),
                Arguments.of(a, b, null),
                Arguments.of(a11, b, null),
                Arguments.of(a1, a2, null),
                Arguments.of(a11, a2, null),

                Arguments.of(a, a2, "Location cannot be the parent of its own parent."),
                Arguments.of(a, a11, "Location cannot be the parent of a location that indirectly contains it.")
        );
    }

    static class ValidateChangesData {
        Location location;
        Map<String, Object> changes;
        Location parent;
        Address newAddress;
        String cycleError;
        String parentError;
        String expectedError;

        public ValidateChangesData(Location location) {
            this.location = location;
        }

        public ValidateChangesData location(Location location) {
            this.location = location;
            return this;
        }

        public ValidateChangesData changes(Map<String, Object> changes) {
            this.changes = changes;
            return this;
        }

        public ValidateChangesData change(String key, Object value) {
            if (changes==null) {
                changes = new HashMap<>();
            }
            changes.put(key, value);
            return this;
        }

        public ValidateChangesData parent(Location parent) {
            this.parent = parent;
            return this;
        }

        public ValidateChangesData newAddress(Address newAddress) {
            this.newAddress = newAddress;
            return this;
        }

        public ValidateChangesData cycleError(String cycleError) {
            this.cycleError = cycleError;
            return this;
        }

        public ValidateChangesData parentError(String parentError) {
            this.parentError = parentError;
            return this;
        }

        public ValidateChangesData expectedError(String expectedError) {
            this.expectedError = expectedError;
            return this;
        }

        @Override
        public String toString() {
            return expectedError==null ? "success" : expectedError;
        }
    }
}
