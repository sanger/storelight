package uk.ac.sanger.storelight.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.sanger.storelight.graphql.StoreRequestContext;
import uk.ac.sanger.storelight.model.*;
import uk.ac.sanger.storelight.repo.ItemRepo;
import uk.ac.sanger.storelight.repo.StoreDB;
import uk.ac.sanger.storelight.requests.LocationIdentifier;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static uk.ac.sanger.storelight.utils.BasicUtils.iterableToString;

/**
 * Service for removing things from storage
 * @author dr6
 */
@Service
public class UnstoreService {
    private final Logger log = LoggerFactory.getLogger(UnstoreService.class);

    private final StoreDB db;

    public UnstoreService(StoreDB db) {
        this.db = db;
    }

    public Item unstoreBarcode(StoreRequestContext ctxt, String barcode) {
        requireNonNull(ctxt, "Request context is null");
        ItemRepo itemRepo = db.getItemRepo();
        Item item = itemRepo.findByBarcode(barcode).orElse(null);
        if (item!=null) {
            itemRepo.delete(item);
            recordUnstores(ctxt, List.of(item));
            log.info("Stored item deleted {} by {}.", item, ctxt);
        }
        return item;
    }

    public List<Item> unstoreBarcodes(StoreRequestContext ctxt, List<String> barcodes) {
        requireNonNull(ctxt, "Request context is null");
        ItemRepo itemRepo = db.getItemRepo();
        List<Item> items = itemRepo.findAllByBarcodeIn(barcodes);
        if (!items.isEmpty()) {
            itemRepo.deleteAll(items);
            recordUnstores(ctxt, items);
            log.info("Stored items deleted {} by {}.", iterableToString(items), ctxt);
        }
        return items;
    }

    public List<Item> empty(StoreRequestContext ctxt, LocationIdentifier li) {
        requireNonNull(ctxt, "Request context is null");
        Location location = db.getLocationRepo().get(li);
        if (location.getStored().isEmpty()) {
            return List.of();
        }
        List<Item> items = List.copyOf(location.getStored());
        db.getItemRepo().deleteAll(items);
        recordUnstores(ctxt, items);
        log.info("Stored items deleted {} by {}.", iterableToString(items), ctxt);
        return items;
    }

    private Iterable<StoreRecord> recordUnstores(StoreRequestContext ctxt, List<Item> items) {
        List<StoreRecord> records = new ArrayList<>(items.size());
        for (Item item : items) {
            records.add(new StoreRecord(item.getBarcode(), null, null, ctxt.getUsername(), ctxt.getApp()));
        }
        return db.getStoreRecordRepo().saveAll(records);
    }
}
