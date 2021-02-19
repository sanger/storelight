package uk.ac.sanger.storelight.repo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author dr6
 */
@Component
public class StoreDB {
    private final BarcodeSeedRepo barcodeSeedRepo;
    private final ItemRepo itemRepo;
    private final LocationRepo locationRepo;
    private final StoreRecordRepo storeRecordRepo;

    @Autowired
    public StoreDB(BarcodeSeedRepo barcodeSeedRepo, ItemRepo itemRepo, LocationRepo locationRepo,
                   StoreRecordRepo storeRecordRepo) {
        this.barcodeSeedRepo = barcodeSeedRepo;
        this.itemRepo = itemRepo;
        this.locationRepo = locationRepo;
        this.storeRecordRepo = storeRecordRepo;
    }

    public BarcodeSeedRepo getBarcodeSeedRepo() {
        return this.barcodeSeedRepo;
    }

    public ItemRepo getItemRepo() {
        return this.itemRepo;
    }

    public LocationRepo getLocationRepo() {
        return this.locationRepo;
    }

    public StoreRecordRepo getStoreRecordRepo() {
        return this.storeRecordRepo;
    }
}
