package uk.ac.sanger.storelight.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.ac.sanger.storelight.model.*;
import uk.ac.sanger.storelight.utils.CIStringSet;

import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link StoreAddressChecker}
 * @author dr6
 */
public class TestStoreAddressChecker {

    @ParameterizedTest
    @MethodSource("checkItemsArguments")
    public void testCheckItems(Collection<Item> items, String expectedErrorMessage) {
        StoreAddressChecker checker = new StoreAddressChecker();
        CIStringSet barcodes = new CIStringSet();
        items.forEach(item -> barcodes.add(item.getBarcode()));
        if (expectedErrorMessage==null) {
            checker.checkItems(items, barcodes);
        } else {
            assertThat(assertThrows(IllegalArgumentException.class, () -> checker.checkItems(items, barcodes)))
                    .hasMessage(expectedErrorMessage);
        }
    }

    static Stream<Arguments> checkItemsArguments() {
        final Location loc1 = new Location(1, "STO-1", null, null, null, null);
        final Location loc2 = new Location(2, "STO-2", null, null, null, new Size(2,2));
        final String loc1desc = "location (id=1, barcode=\"STO-1\")";
        final String loc2desc = "location (id=2, barcode=\"STO-2\")";
        final String loc2descSize = loc2desc + " (numRows=2, numColumns=2)";
        Address A1 = new Address(1,1);
        Address A2 = new Address(1, 2);
        Address A3 = new Address(1, 3);
        Address B1 = new Address(2,1);
        Address C1 = new Address(3, 1);
        loc1.getStored().add(new Item(101, "Item-101", loc1, B1));
        loc2.getStored().add(new Item(100, "Item-100", loc2, A2));
        return Arrays.stream(new Object[][] {
                { null },
                { loc1, A1, loc1, null, loc1, null, loc2, A1, loc2, null, null },
                { loc1, A1, loc1, A2, loc2, A1, loc1, A2, loc2, A1, loc2, A1, "Addresses repeated in same location: A2 in "+loc1desc+", A1 in "+loc2desc+"." },
                { loc1, A3, loc2, A3, loc2, C1, "Addresses outside of listed size for location: A3 in "+loc2descSize+", C1 in "+loc2descSize+"." },
                { loc1, A1, loc1, B1, loc2, A2, "Addresses already occupied: B1 in "+loc1desc+", A2 in "+loc2desc+"." },
                { loc1, B1, loc1, B1, loc2, C1, "Address outside of listed size for location: C1 in "+loc2descSize+". " +
                        "Address already occupied: B1 in "+loc1desc+". " +
                        "Address repeated in same location: B1 in "+loc1desc+"." },
        }).map(TestStoreAddressChecker::arrToArgs);
    }

    private static Arguments arrToArgs(Object[] arr) {
        List<Item> items = new ArrayList<>(arr.length/2);
        for (int i = 0; i < arr.length-1; i += 2) {
            Location loc = (Location) arr[i];
            Address ad = (Address) arr[i+1];
            items.add(new Item(null, "CGAP-A"+items.size(), loc, ad));
        }
        String message = (String) arr[arr.length-1];
        return Arguments.of(items, message);
    }
}
