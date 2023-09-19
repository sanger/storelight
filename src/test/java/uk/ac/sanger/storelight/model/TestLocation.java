package uk.ac.sanger.storelight.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

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

    @ParameterizedTest
    @EnumSource(GridDirection.class)
    public void testAddressIndex(GridDirection direction) {
        int rows = 2, cols = 3;
        Size size = new Size(rows, cols);
        Location loc = new Location();
        loc.setSize(size);
        loc.setDirection(direction);
        Address[] addresses = new Address[rows*cols];
        for (int row = 1; row <= rows; ++row) {
            for (int col = 1; col <= cols; ++col) {
                final Address address = new Address(row, col);
                int i = loc.addressIndex(address);
                assertThat(i).isBetween(1, addresses.length);
                addresses[i-1] = address;
            }
        }
        assertNull(loc.addressIndex(new Address(1, cols+1)));
        assertNull(loc.addressIndex(new Address(rows+1, 1)));
        BiPredicate<Address, Address> predicate = addressOrderPredicate(direction);
        Address prev = null;
        for (Address address : addresses) {
            if (prev!=null) {
                assertTrue(predicate.test(prev, address));
            }
            prev = address;
        }
    }

    private static BiPredicate<Address, Address> addressOrderPredicate(GridDirection direction) {
        switch (direction) {
            case RightDown:
                return (a,b) -> b.getRow() > a.getRow() || b.getRow()==a.getRow() && b.getColumn() > a.getColumn();
            case DownRight:
                return (a,b) -> b.getColumn() > a.getColumn() || a.getColumn()==b.getColumn() && b.getRow() > a.getRow();
            case RightUp:
                return (a,b) -> b.getRow() < a.getRow() || b.getRow()==a.getRow() && b.getColumn() > a.getColumn();
            case UpRight:
                return (a,b) -> b.getColumn() > a.getColumn() || b.getColumn()==b.getColumn() && b.getRow() < a.getRow();
        }
        throw new IllegalArgumentException("Unknown direction "+direction);
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
