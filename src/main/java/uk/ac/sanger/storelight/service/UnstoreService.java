package uk.ac.sanger.storelight.service;

import org.springframework.stereotype.Service;
import uk.ac.sanger.storelight.model.Item;
import uk.ac.sanger.storelight.model.Location;
import uk.ac.sanger.storelight.repo.ItemRepo;
import uk.ac.sanger.storelight.repo.StoreDB;
import uk.ac.sanger.storelight.requests.LocationIdentifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for removing things from storage
 * @author dr6
 */
@Service
public class UnstoreService {
    private final StoreDB db;

    public UnstoreService(StoreDB db) {
        this.db = db;
    }

    public Item unstoreBarcode(String barcode) {
        ItemRepo itemRepo = db.getItemRepo();
        Item item = itemRepo.findByBarcode(barcode).orElse(null);
        if (item!=null) {
            itemRepo.delete(item);
        }
        return item;
    }

    public List<Item> unstoreBarcodes(List<String> barcodes) {
        ItemRepo itemRepo = db.getItemRepo();
        List<Item> items = itemRepo.findAllByBarcodeIn(barcodes);
        if (!items.isEmpty()) {
            itemRepo.deleteAll(items);
        }
        return items;
    }

    public List<Item> empty(LocationIdentifier li) {
        Location location = db.getLocationRepo().get(li);
        if (location.getStored().isEmpty()) {
            return List.of();
        }
        List<Item> items = new ArrayList<>(location.getStored());
        db.getItemRepo().deleteAll(items);
        return items;
    }
}
