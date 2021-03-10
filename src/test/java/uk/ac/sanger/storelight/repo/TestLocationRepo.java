package uk.ac.sanger.storelight.repo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.ac.sanger.storelight.model.*;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests {@link LocationRepo}
 * @author dr6
 */
@SpringBootTest
public class TestLocationRepo {
    @Autowired
    LocationRepo locationRepo;
    @Autowired
    EntityManager entityManager;

    @Test
    @Transactional
    public void testSaveLocation() {
        Location parent = new Location(null, "STO-001F", "parent", "parent desc", null, null, null, null);
        parent = locationRepo.save(parent);
        checkLocation(parent, null, "STO-001F", "parent desc", null, null, null, null);
        assertThat(parent.getChildren()).isEmpty();
        assertThat(parent.getStored()).isEmpty();
        Integer parentId = parent.getId();
        parent = locationRepo.getById(parentId);
        checkLocation(parent, parentId, "STO-001F", "parent desc", null, null, null, null);

        Location loc1 = new Location(null, "STO-002E", "loc1", "loc1 desc", parent,
                new Address(2,3), new Size(4,5), GridDirection.RightDown);
        loc1 = locationRepo.save(loc1);
        checkLocation(loc1, null, "STO-002E", "loc1 desc", parentId, new Address(2,3),
                new Size(4,5), GridDirection.RightDown);
        assertThat(loc1.getStored()).isEmpty();
        assertThat(loc1.getChildren()).isEmpty();
        Integer loc1Id = loc1.getId();
        loc1 = locationRepo.getById(loc1Id);
        checkLocation(loc1, loc1Id, "STO-002E", "loc1 desc", parentId, new Address(2,3),
                new Size(4,5), GridDirection.RightDown);

        entityManager.refresh(parent);
        assertThat(parent.getChildren()).containsOnly(loc1);
    }

    private void checkLocation(Location location, Integer id, String barcode, String description, Integer parentId,
                               Address address, Size size, GridDirection direction) {
        if (id == null) {
            assertNotNull(location.getId());
        } else {
            assertEquals(id, location.getId());
        }
        assertEquals(barcode, location.getBarcode());
        assertEquals(description, location.getDescription());
        if (parentId == null) {
            assertNull(location.getParent());
        } else {
            assertNotNull(location.getParent());
            assertEquals(parentId, location.getParent().getId());
        }
        assertEquals(address, location.getAddress());
        assertEquals(size, location.getSize());
        assertEquals(direction, location.getDirection());
    }
}
