package uk.ac.sanger.storelight.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests {@link Item}
 * @author dr6
 */
public class TestItem {
    @Test
    public void testGetAddressIndex() {
        Location loc = new Location(1, "STO-1");
        loc.setDirection(GridDirection.RightDown);
        loc.setSize(new Size(2, 3));
        Item item = new Item(1, "ITEM-1", loc, new Address(2,3));
        assertEquals(6, item.getAddressIndex());
        item.setAddress(new Address(1,2));
        assertEquals(2, item.getAddressIndex());
        item.setAddress(null);
        assertNull(item.getAddressIndex());
    }
}
