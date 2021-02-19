package uk.ac.sanger.storelight.repo;

import org.springframework.data.repository.CrudRepository;
import uk.ac.sanger.storelight.model.StoreRecord;

public interface StoreRecordRepo extends CrudRepository<StoreRecord, Integer> {
}
