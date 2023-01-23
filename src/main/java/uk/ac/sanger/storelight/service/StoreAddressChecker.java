package uk.ac.sanger.storelight.service;

import org.springframework.stereotype.Service;
import uk.ac.sanger.storelight.model.*;
import uk.ac.sanger.storelight.utils.CIStringSet;

import java.util.*;

import static java.util.stream.Collectors.toMap;
import static uk.ac.sanger.storelight.utils.BasicUtils.pluralise;
import static uk.ac.sanger.storelight.utils.BasicUtils.repr;

/**
 * Checks stuff can be stored in specific addresses in locations.
 * @author dr6
 */
@Service
public class StoreAddressChecker {

    public void checkItems(Collection<Item> items, CIStringSet barcodes) throws IllegalArgumentException {
        Map<Integer, Map<Address, Item>> contentCache = new HashMap<>();
        Set<Destination> seenDestinations = new HashSet<>();
        Set<Destination> repeated = new LinkedHashSet<>();
        Set<Destination> outOfBounds = new LinkedHashSet<>();
        Set<Destination> occupied = new LinkedHashSet<>();
        for (Item item : items) {
            Address address = item.getAddress();
            if (address ==null) {
                continue;
            }
            Destination destination = new Destination(item.getLocation(), address);
            if (!seenDestinations.add(destination)) {
                repeated.add(destination);
                continue;
            }
            Size size = item.getLocation().getSize();
            if (size!=null && !size.contains(address)) {
                outOfBounds.add(destination);
            } else {
                Item occupant = itemAt(destination, contentCache);
                if (occupant!=null && !barcodes.contains(occupant.getBarcode())) {
                    occupied.add(destination);
                }
            }
        }
        if (repeated.isEmpty() && outOfBounds.isEmpty() && occupied.isEmpty()) {
            return; // everything is fine
        }
        List<String> errors = new ArrayList<>(3);
        if (!outOfBounds.isEmpty()) {
            errors.add(storeDestinationErrorMessage("Address{es} outside of listed size for location:",
                    outOfBounds, true));
        }
        if (!occupied.isEmpty()) {
            errors.add(storeDestinationErrorMessage("Address{es} already occupied:",
                    occupied, false));
        }
        if (!repeated.isEmpty()) {
            errors.add(storeDestinationErrorMessage("Address{es} repeated in same location:",
                    repeated, false));
        }
        throw new IllegalArgumentException(String.join(" ", errors));
    }

    private static String storeDestinationErrorMessage(String preambleTemplate, Collection<Destination> destinations,
                                                       boolean includeSize) {
        StringBuilder sb = new StringBuilder(pluralise(preambleTemplate, destinations.size()));
        for (Destination dest : destinations) {
            sb.append(' ')
                    .append(dest.getAddress())
                    .append(" in location (id=")
                    .append(dest.getLocation().getId())
                    .append(", barcode=")
                    .append(repr(dest.getLocation().getBarcode()))
                    .append(')');
            if (includeSize) {
                sb.append(' ').append(dest.getLocation().getSize());
            }
            sb.append(',');
        }
        sb.setCharAt(sb.length()-1, '.');
        return sb.toString();
    }

    private Item itemAt(Destination destination, Map<Integer, Map<Address, Item>> contentCache) {
        Location location = destination.getLocation();
        Map<Address, Item> map = contentCache.get(location.getId());
        if (map==null) {
            map = location.getStored().stream()
                    .filter(item -> item.getAddress()!=null)
                    .collect(toMap(Item::getAddress, item -> item));
        }
        return map.get(destination.getAddress());
    }

    /**
     * A wrapper for a destination and an address, used for dupe detection
     */
    static class Destination {
        Location location;
        Address address;

        public Destination(Location location, Address address) {
            this.location = location;
            this.address = address;
        }

        public Location getLocation() {
            return this.location;
        }

        public Address getAddress() {
            return this.address;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Destination that = (Destination) o;
            return (Objects.equals(this.location, that.location)
                    && Objects.equals(this.address, that.address));
        }

        @Override
        public int hashCode() {
            return Objects.hash(location, address);
        }
    }
}
