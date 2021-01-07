package uk.ac.sanger.storelight.repo;

import org.springframework.data.repository.CrudRepository;
import uk.ac.sanger.storelight.model.BarcodeSeed;

public interface BarcodeSeedRepo extends CrudRepository<BarcodeSeed, Integer> {
    default String createBarcode(String prefix) {
        return save(new BarcodeSeed()).toBarcode(prefix);
    }

    default String createStoreBarcode() {
        return createBarcode(BarcodeSeed.STORE_PREFIX);
    }
}
