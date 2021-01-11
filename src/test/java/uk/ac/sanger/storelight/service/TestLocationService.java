package uk.ac.sanger.storelight.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.ac.sanger.storelight.graphql.StoreRequestContext;
import uk.ac.sanger.storelight.model.*;
import uk.ac.sanger.storelight.repo.*;
import uk.ac.sanger.storelight.requests.LocationInput;

import javax.persistence.EntityNotFoundException;

import java.util.Arrays;
import java.util.stream.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        when(mockLocationRepo.save(any())).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

        locationService = new LocationService(mockDb);
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
        String longDescription = IntStream.range(0, 40).mapToObj(String::valueOf).collect(Collectors.joining());

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
                        "Location description is too long (max length: 64).",
                        new LocationInput(longDescription, null, null, null)
                },
        }).map(arr -> (arr.length==2 ? Arguments.of(arr[0], arr[1], null) : Arguments.of(arr)));
    }
}
