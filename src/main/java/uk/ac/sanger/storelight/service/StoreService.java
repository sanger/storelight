package uk.ac.sanger.storelight.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.sanger.storelight.model.*;
import uk.ac.sanger.storelight.repo.ItemRepo;
import uk.ac.sanger.storelight.repo.StoreDB;
import uk.ac.sanger.storelight.requests.LocationIdentifier;
import uk.ac.sanger.storelight.requests.StoreInput;
import uk.ac.sanger.storelight.utils.CIStringSet;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * Service for storing items in locations
 * @author dr6
 */
@Service
public class StoreService {
    private final EntityManager entityManager;
    private final StoreDB db;
    private final ItemBarcodeValidator itemBarcodeValidator;
    private final StoreAddressChecker storeAddressChecker;

    @Autowired
    public StoreService(EntityManager entityManager, StoreDB db, ItemBarcodeValidator itemBarcodeValidator, StoreAddressChecker storeAddressChecker) {
        this.entityManager = entityManager;
        this.db = db;
        this.itemBarcodeValidator = itemBarcodeValidator;
        this.storeAddressChecker = storeAddressChecker;
    }

    public Item storeBarcode(String barcode, LocationIdentifier li, Address address) {
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
        Item item = new Item(null, barcode, location, address);
        return itemRepo.save(item);
    }

    public Iterable<Item> storeBarcodes(List<String> barcodes, LocationIdentifier li) {
        validateItemBarcodes(barcodes.stream());
        Location location = db.getLocationRepo().get(li);
        if (barcodes.isEmpty()) {
            return List.of();
        }
        List<Item> newItems = barcodes.stream()
                .map(bc -> new Item(bc, location))
                .collect(toList());
        return storeItems(newItems, barcodes);
    }

    public Iterable<Item> store(List<StoreInput> storeInputs, LocationIdentifier defaultLi) {
        if (storeInputs.isEmpty()) {
            return List.of();
        }
        CIStringSet barcodeSet = validateItemBarcodes(storeInputs.stream().map(StoreInput::getBarcode));
        LocationCache locationCache = new LocationCache(db.getLocationRepo());
        locationCache.lookUp(Stream.concat(Stream.of(defaultLi), storeInputs.stream().map(StoreInput::getLocation))
                .filter(Objects::nonNull));
        List<Item> newItems = storeInputs.stream()
                .map(sin -> new Item(null, sin.getBarcode(), locationCache.get(sin.getLocation()), sin.getAddress()))
                .collect(toList());
        storeAddressChecker.checkItems(newItems, barcodeSet);
        return storeItems(newItems, barcodeSet);
    }

    private Iterable<Item> storeItems(Collection<Item> items, Collection<String> barcodes) {
        ItemRepo itemRepo = db.getItemRepo();
        itemRepo.deleteAllByBarcodeIn(barcodes);
        entityManager.flush();
        return itemRepo.saveAll(items);
    }

    private CIStringSet validateItemBarcodes(Stream<String> barcodes) {
        return itemBarcodeValidator.validateItemBarcodes(barcodes);
    }
}
