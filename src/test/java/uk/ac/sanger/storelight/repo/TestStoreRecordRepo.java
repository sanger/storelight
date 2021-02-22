package uk.ac.sanger.storelight.repo;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.ac.sanger.storelight.model.*;

import javax.transaction.Transactional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests {@link StoreRecordRepo}
 * @author dr6
 */
@SpringBootTest
public class TestStoreRecordRepo {
    @Autowired
    private StoreRecordRepo recordRepo;
    @Autowired
    private LocationRepo locationRepo;

    @ParameterizedTest
    @MethodSource("storeRecordArgs")
    @Transactional
    public void testStoreRecord(StoreRecord record) {
        if (record.getLocationId()!=null) {
            Location location = locationRepo.save(new Location(null, "STO-A1"));
            record.setLocationId(location.getId());
        }
        StoreRecord saved = recordRepo.save(record);
        assertNotNull(saved.getId());
        assertNotNull(saved.getRecorded());
        assertEquals(record.getBarcode(), saved.getBarcode());
        assertEquals(record.getAddress(), saved.getAddress());
        assertEquals(record.getLocationId(), saved.getLocationId());
        assertEquals(record.getUsername(), saved.getUsername());
        assertEquals(record.getApp(), saved.getApp());
    }

    static Stream<StoreRecord> storeRecordArgs() {
        return Stream.of(
                new StoreRecord("ITEM-A1", null, null, "user1", "app1"),
                new StoreRecord("ITEM-A2", null, 1, "user1", "app1"),
                new StoreRecord("ITEM-A3", new Address(3,4), 1, "user1", "app1")
        );
    }
}
