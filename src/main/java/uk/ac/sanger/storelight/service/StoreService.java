package uk.ac.sanger.storelight.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.sanger.storelight.graphql.StoreRequestContext;
import uk.ac.sanger.storelight.model.*;
import uk.ac.sanger.storelight.repo.ItemRepo;
import uk.ac.sanger.storelight.repo.StoreDB;
import uk.ac.sanger.storelight.requests.LocationIdentifier;
import uk.ac.sanger.storelight.requests.StoreInput;
import uk.ac.sanger.storelight.utils.CIStringSet;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static uk.ac.sanger.storelight.utils.BasicUtils.coalesce;
import static uk.ac.sanger.storelight.utils.BasicUtils.iterableToString;

/**
 * Service for storing items in locations
 * @author dr6
 */
@Service
public class StoreService {
    private final Logger log = LoggerFactory.getLogger(StoreService.class);

    private final EntityManager entityManager;
    private final StoreDB db;
    private final ItemBarcodeValidator itemBarcodeValidator;
    private final StoreAddressChecker storeAddressChecker;

    @Autowired
    public StoreService(EntityManager entityManager, StoreDB db, ItemBarcodeValidator itemBarcodeValidator,
                        StoreAddressChecker storeAddressChecker) {
        this.entityManager = entityManager;
        this.db = db;
        this.itemBarcodeValidator = itemBarcodeValidator;
        this.storeAddressChecker = storeAddressChecker;
    }

    public Item storeBarcode(StoreRequestContext ctxt, String barcode, LocationIdentifier li, Address address) {
        requireNonNull(ctxt, "Request context is null.");
        validateItemBarcodes(Stream.of(barcode));
        Location location = db.getLocationRepo().get(li);
        if (location.getSize()!=null && address!=null && !location.getSize().contains(address)) {
            throw new IllegalArgumentException(String.format("The address %s is outside the listed size %s " +
                    "for location %s.", address, location.getSize(), li));
        }
        if (address != null) {
            Item occupant = location.getStored().stream().filter(x -> address.equals(x.getAddress()))
                    .findAny().orElse(null);
            if (occupant!=null && !occupant.getBarcode().equalsIgnoreCase(barcode)) {
                throw new IllegalArgumentException(String.format("There is another item at address %s " +
                        "in location %s.", address, li));
            }
        }
        final ItemRepo itemRepo = db.getItemRepo();
        itemRepo.deleteAllByBarcodeIn(List.of(barcode));
        entityManager.flush();
        Item item = itemRepo.save(new Item(null, barcode, location, address));
        db.getStoreRecordRepo().save(new StoreRecord(item.getBarcode(), item.getAddress(), item.getLocation().getId(), ctxt.getUsername(), ctxt.getApp()));
        log.info("Item stored {} by {}.", item, ctxt);
        entityManager.refresh(item.getLocation()); // Sometimes location doesn't have updated contents without this
        return item;
    }

    public Iterable<Item> storeBarcodes(StoreRequestContext ctxt, List<String> barcodes, LocationIdentifier li) {
        requireNonNull(ctxt, "Request context is null.");
        CIStringSet barcodeSet = validateItemBarcodes(barcodes.stream());
        Location location = db.getLocationRepo().get(li);
        if (barcodes.isEmpty()) {
            return List.of();
        }
        List<Item> newItems = barcodes.stream()
                .map(bc -> new Item(bc, location))
                .collect(toList());
        return storeItems(ctxt, newItems, barcodeSet);
    }

    public Iterable<Item> store(StoreRequestContext ctxt, List<StoreInput> storeInputs, LocationIdentifier defaultLi) {
        requireNonNull(ctxt, "Request context is null.");
        if (storeInputs.isEmpty()) {
            return List.of();
        }
        CIStringSet barcodeSet = validateItemBarcodes(storeInputs.stream().map(StoreInput::getBarcode));
        LocationCache locationCache = makeLocationCache();
        if (defaultLi==null && storeInputs.stream().anyMatch(sin -> sin.getLocation()==null)) {
            throw new IllegalArgumentException("A location must be specified for each item.");
        }
        locationCache.lookUp(Stream.concat(Stream.of(defaultLi), storeInputs.stream().map(StoreInput::getLocation))
                .filter(Objects::nonNull));
        List<Item> newItems = storeInputs.stream()
                .map(sin -> new Item(null, sin.getBarcode(), locationCache.get(coalesce(sin.getLocation(), defaultLi)), sin.getAddress()))
                .collect(toList());
        storeAddressChecker.checkItems(newItems, barcodeSet);
        return storeItems(ctxt, newItems, barcodeSet);
    }

    LocationCache makeLocationCache() {
        return new LocationCache(db.getLocationRepo());
    }

    Iterable<Item> storeItems(StoreRequestContext ctxt, Collection<Item> items, Collection<String> barcodes) {
        ItemRepo itemRepo = db.getItemRepo();
        itemRepo.deleteAllByBarcodeIn(barcodes);
        entityManager.flush();
        Iterable<Item> saved = itemRepo.saveAll(items);
        if (log.isInfoEnabled()) {
            log.info("Items stored {} by {}.", iterableToString(saved), ctxt);
        }
        List<StoreRecord> records = new ArrayList<>(items.size());
        Map<Integer, Location> refreshedLocations = new HashMap<>();
        for (Item item : saved) {
            Location refreshedLoc = refreshedLocations.get(item.getLocation().getId());
            if (refreshedLoc==null) {
                refreshedLoc = item.getLocation();
                entityManager.refresh(refreshedLoc);
                refreshedLocations.put(refreshedLoc.getId(), refreshedLoc);
            } else {
                item.setLocation(refreshedLoc);
            }
            records.add(new StoreRecord(item.getBarcode(), item.getAddress(), item.getLocation().getId(), ctxt.getUsername(), ctxt.getApp()));
        }
        db.getStoreRecordRepo().saveAll(records);
        return saved;
    }

    private CIStringSet validateItemBarcodes(Stream<String> barcodes) {
        return itemBarcodeValidator.validateItemBarcodes(barcodes);
    }
}
