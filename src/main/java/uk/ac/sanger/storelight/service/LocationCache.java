package uk.ac.sanger.storelight.service;

import uk.ac.sanger.storelight.model.Location;
import uk.ac.sanger.storelight.repo.LocationRepo;
import uk.ac.sanger.storelight.requests.LocationIdentifier;
import uk.ac.sanger.storelight.utils.BasicUtils;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.ac.sanger.storelight.utils.BasicUtils.pluralise;

/**
 * A helper to cache {@link Location Locations} from {@link LocationIdentifier LocationIdentifiers}
 * @author dr6
 */
public class LocationCache {
    private final Map<Integer, Location> idMap = new HashMap<>();
    private final Map<String, Location> bcMap = new HashMap<>();
    private final LocationRepo locationRepo;

    public LocationCache(LocationRepo locationRepo) {
        this.locationRepo = locationRepo;
    }

    public void cache(Location location) {
        idMap.put(location.getId(), location);
        bcMap.put(location.getBarcode().toUpperCase(), location);
    }

    public void lookUp(Stream<LocationIdentifier> lis) {
        Set<Integer> ids = new HashSet<>();
        Set<String> bcs = new HashSet<>();
        lis.forEach(li -> {
            if (li.getId() != null) {
                ids.add(li.getId());
            } else if (li.getBarcode() != null) {
                bcs.add(li.getBarcode().toUpperCase());
            }
        });
        ids.removeAll(idMap.keySet());
        bcs.removeAll(bcMap.keySet());
        Consumer<Location> receive = loc -> {
            cache(loc);
            ids.remove(loc.getId());
            bcs.remove(loc.getBarcode().toUpperCase());
        };
        if (!ids.isEmpty()) {
            locationRepo.findAllById(ids).forEach(receive);
            if (!ids.isEmpty()) {
                throw new IllegalArgumentException(pluralise("Unknown location id{s}: ", ids.size())
                        + ids);
            }
        }
        if (!bcs.isEmpty()) {
            locationRepo.findAllByBarcodeIn(bcs).forEach(receive);
            if (!bcs.isEmpty()) {
                List<String> bcReprs = bcs.stream().map(BasicUtils::repr).collect(Collectors.toList());
                throw new IllegalArgumentException(pluralise("Unknown location barcode{s}: ", bcs.size())
                        + bcReprs);
            }
        }
    }

    public Location get(LocationIdentifier li) {
        if (li.getId()!=null) {
            return idMap.get(li.getId());
        }
        if (li.getBarcode()!=null) {
            return bcMap.get(li.getBarcode().toUpperCase());
        }
        throw new IllegalArgumentException("Missing location identifier.");
    }
}
