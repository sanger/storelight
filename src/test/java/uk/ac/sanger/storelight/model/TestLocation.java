package uk.ac.sanger.storelight.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests {@link Location}
 * @author dr6
 */
public class TestLocation {
    @ParameterizedTest
    @MethodSource("locNameData")
    public void testGetQualifiedNameWithFirstBarcode(Location loc, String expectedString) {
        assertEquals(expectedString, loc.getQualifiedNameWithFirstBarcode());
    }

    static Stream<Arguments> locNameData() {
        Location freezer = makeLoc(1, null, "Freezer", null);
        Location shelf = makeLoc(2, freezer, "Shelf 1", null);
        Location box = makeLoc(3, shelf, null, new Address(2,3));
        Location folder = makeLoc(4, shelf, null, null);
        Location cupboard = makeLoc(5, null, null, null);
        Location drawer = makeLoc(6, cupboard, null, new Address(1,4));

        return Arrays.stream(new Object[][] {
                { freezer, "STO-1 Freezer" },
                { shelf, "STO-1 Freezer / Shelf 1" },
                { box, "STO-1 Freezer / Shelf 1 / B3" },
                { folder, "STO-1 Freezer / Shelf 1 / STO-4" },
                { cupboard, "STO-5" },
                { drawer, "STO-5 / A4" },
        }).map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("getHierarchyArgs")
    public void testGetHierarchy(List<Location> expected) {
        assertThat(expected.get(expected.size()-1).getHierarchy()).containsExactlyElementsOf(expected);
    }

    static Stream<Arguments> getHierarchyArgs() {
        Location freezer = makeLoc(1, null, "Freezer", null);
        Location shelf = makeLoc(2, freezer, "Shelf 1", null);
        Location box = makeLoc(3, shelf, null, new Address(2,3));
        Location folder = makeLoc(4, shelf, null, null);
        Location cupboard = makeLoc(5, null, null, null);
        Location drawer = makeLoc(6, cupboard, null, new Address(1,4));

        return Arrays.stream(new Object[][] {
                {freezer},
                {freezer,shelf},
                {freezer,shelf,box},
                {freezer,shelf,folder},
                {cupboard},
                {cupboard,drawer},
        }).map(locs -> Arguments.of(Arrays.asList(locs)));
    }

    private static Location makeLoc(int id, Location parent, String name, Address address) {
        Location loc = new Location(id, "STO-"+id, name, null, parent, address, null, null);
        if (parent!=null) {
            parent.getChildren().add(loc);
        }
        return loc;
    }
}
