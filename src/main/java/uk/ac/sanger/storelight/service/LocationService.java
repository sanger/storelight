package uk.ac.sanger.storelight.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.sanger.storelight.model.Address;
import uk.ac.sanger.storelight.model.Location;
import uk.ac.sanger.storelight.repo.StoreDB;
import uk.ac.sanger.storelight.requests.LocationInput;

/**
 * Service for operations on {@link Location Locations}.
 * @author dr6
 */
@Service
public class LocationService {
    private final StoreDB db;

    @Autowired
    public LocationService(StoreDB db) {
        this.db = db;
    }

    /**
     * Creates a new location from the given details.
     * @param lin the details about hte new location
     * @return the new location
     */
    public Location createLocation(LocationInput lin) {
        if (lin.getAddress()!=null && lin.getParentId()==null) {
            throw new IllegalArgumentException("A location with no parent cannot have an address.");
        }
        Location parent = (lin.getParentId()==null ? null : db.getLocationRepo().getById(lin.getParentId()));
        Address address = lin.getAddress();
        if (parent!=null && address!=null && parent.getSize()!=null) {
            if (!parent.getSize().contains(address)) {
                throw new IllegalArgumentException("The address " + address + " is outside the " +
                        "listed size " + parent.getSize() + " for the parent.");
            }
        }
        if (parent!=null && address!=null && parent.getChildren().stream().anyMatch(c -> address.equals(c.getAddress()))) {
            throw new IllegalArgumentException("There is already a location at address "+address+" in the parent.");
        }
        String desc = lin.getDescription();
        if (desc!=null) {
            desc = desc.trim();
            if (desc.isEmpty()) {
                desc = null;
            } else if (desc.length() > Location.MAX_DESCRIPTION) {
                throw new IllegalArgumentException("Location description too long (max length: "+Location.MAX_DESCRIPTION+")");
            }
        }
        String barcode = db.getBarcodeSeedRepo().createStoreBarcode();
        Location loc = new Location(null, barcode, desc, parent, address, lin.getSize());
        return db.getLocationRepo().save(loc);
    }
}
