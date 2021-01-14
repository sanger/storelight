package uk.ac.sanger.storelight.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.sanger.storelight.graphql.StoreRequestContext;
import uk.ac.sanger.storelight.model.*;
import uk.ac.sanger.storelight.repo.LocationRepo;
import uk.ac.sanger.storelight.repo.StoreDB;
import uk.ac.sanger.storelight.requests.LocationIdentifier;
import uk.ac.sanger.storelight.requests.LocationInput;

import java.util.*;

import static java.util.Objects.requireNonNull;
import static uk.ac.sanger.storelight.utils.BasicUtils.pluralise;

/**
 * Service for operations on {@link Location Locations}.
 * @author dr6
 */
@Service
public class LocationService {
    private final Logger log = LoggerFactory.getLogger(LocationService.class);

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
    public Location createLocation(StoreRequestContext context, LocationInput lin) {
        requireNonNull(context, "Request context is null.");
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
                throw new IllegalArgumentException("Location description is too long (max length: "+Location.MAX_DESCRIPTION+").");
            }
        }
        String barcode = db.getBarcodeSeedRepo().createStoreBarcode();
        Location loc = new Location(null, barcode, desc, parent, address, lin.getSize());
        Location savedLoc = db.getLocationRepo().save(loc);
        log.info("New location created {} by {}.", savedLoc, context);
        return savedLoc;
    }

    /**
     * Updates some fields in a location.
     * Any fields that are given in the map with null value indicate that the field
     * must be cleared in the existing location.
     * @param li the identifier for the location to be updated
     * @param fields a map of the fields that will be updated
     * @return an updated location (or an unupdated one if the changes were nop)
     * @exception IllegalArgumentException the requested changes were invalid
     */
    public Location editLocation(StoreRequestContext context, LocationIdentifier li, Map<String, ?> fields) {
        requireNonNull(context, "Request context is null.");
        LocationRepo locRepo = db.getLocationRepo();
        Location location = locRepo.get(li);
        validateChanges(location, fields);
        boolean changed = false;
        for (var entry : fields.entrySet()) {
            switch (entry.getKey()) {
                case "description": {
                    String desc = (String) entry.getValue();
                    if (desc!=null) {
                        desc = desc.trim();
                        if (desc.isEmpty()) {
                            desc = null;
                        }
                    }
                    if (!Objects.equals(location.getDescription(), desc)) {
                        changed = true;
                        location.setDescription(desc);
                    }
                    break;
                }
                case "parentId": {
                    Integer parentId = (Integer) entry.getValue();
                    Integer currentParentId = (location.getParent()==null ? null : location.getParent().getId());
                    if (!Objects.equals(currentParentId, parentId)) {
                        changed = true;
                        if (parentId==null) {
                            location.setParent(null);
                        } else {
                            location.setParent(locRepo.getById(parentId));
                        }
                    }
                    break;
                }
                case "address": {
                    Address address = (Address) entry.getValue();
                    if (!Objects.equals(location.getAddress(), address)) {
                        changed = true;
                        location.setAddress(address);
                    }
                    break;
                }
                case "size": {
                    //noinspection unchecked
                    Map<String, Integer> sizeMap = (Map<String, Integer>) entry.getValue();
                    Size size = (sizeMap==null ? null : new Size(sizeMap.get("numRows"), sizeMap.get("numColumns")));
                    if (!Objects.equals(location.getSize(), size)) {
                        changed = true;
                        location.setSize(size);
                    }
                    break;
                }
            }
        }
        if (changed) {
            location = locRepo.save(location);
            log.info("Location edited {} by {}.", location, context);
        }
        return location;
    }

    /**
     * Checks requested changes for problems.
     * @param location location being updated
     * @param changes requested changes
     * @exception IllegalArgumentException any detected problems
     */
    public void validateChanges(Location location, Map<String, ?> changes) {
        Set<String> invalidFields = new HashSet<>();
        Set<String> problems = new LinkedHashSet<>();

        Location parent = null;
        boolean newParent = false;
        boolean parentError = false;
        Address address = null;
        boolean newAddress = false;
        boolean addressError = false;

        for (var entry : changes.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            switch (key) {
                case "description": {
                    if (value==null) {
                        break;
                    }
                    if (!(value instanceof String)) {
                        problems.add(fieldTypeError("description", "a string", value));
                        break;
                    }
                    int len = ((String) value).trim().length();
                    if (len > Location.MAX_DESCRIPTION) {
                        problems.add(String.format("Description too long (%s). Max length is %s.", len, Location.MAX_DESCRIPTION));
                    }
                    break;
                }

                case "parentId": {
                    newParent = true;
                    if (value==null) {
                        break;
                    }
                    if (!(value instanceof Integer)) {
                        problems.add(fieldTypeError("parentId", "an integer", value));
                        parentError = true;
                        break;
                    }
                    Integer parentId = (Integer) value;
                    if (parentId.equals(location.getId())) {
                        problems.add("A location cannot be its own parent.");
                        parentError = true;
                        break;
                    }
                    var optLocation = db.getLocationRepo().findById(parentId);
                    if (optLocation.isEmpty()) {
                        problems.add("Invalid parent id: "+parentId+".");
                        parentError = true;
                        break;
                    }
                    parent = optLocation.get();
                    break;
                }

                case "address": {
                    newAddress = true;
                    if (value==null) {
                        break;
                    }
                    if (!(value instanceof Address)) {
                        problems.add(fieldTypeError("address", "an address", value));
                        addressError = true;
                        break;
                    }
                    address = (Address) value;
                    break;
                }

                case "size": {
                    if (value==null) {
                        continue;
                    }
                    if (!(value instanceof Map)) {
                        problems.add(fieldTypeError("size", "a mapping of numRows and numColumns", value));
                        break;
                    }
                    Map<?, ?> map = (Map<?,?>) value;
                    if (map.size()!=2 || !(map.get("numColumns") instanceof Integer) || !(map.get("numRows") instanceof Integer)) {
                        problems.add("Received size with invalid contents.");
                        break;
                    }
                    if ((Integer) (map.get("numColumns")) < 1 || (Integer) map.get("numRows") < 1) {
                        problems.add("Fields in size must be greater than zero.");
                        break;
                    }
                    break;
                }
                default:
                    invalidFields.add(key);
            }
        }
        if ((newAddress || newParent) && !parentError) {
            if (!newAddress) {
                address = location.getAddress();
            }
            String problem = null;
            if (newParent && parent!=null) {
                problem = checkCycle(location, parent);
            }
            if (problem==null && address!=null && !addressError) {
                if (!newParent) {
                    parent = location.getParent();
                }
                problem = checkParentWithAddress(location, parent, address);
            }
            if (problem!=null) {
                problems.add(problem);
            }
        }

        if (problems.isEmpty() && invalidFields.isEmpty()) {
            return;
        }
        String joinedProblems = String.join(" ", problems);
        if (!invalidFields.isEmpty()) {
            String problem = pluralise("Invalid field{s}: ", invalidFields.size()) + invalidFields + ".";
            if (joinedProblems.isEmpty()) {
                joinedProblems = problem;
            } else {
                joinedProblems = problem + " " + joinedProblems;
            }
        }
        throw new IllegalArgumentException(joinedProblems);
    }

    private static String fieldTypeError(String name, String expected, Object actual) {
        return String.format("Require %s to be %s, but received %s.", name, expected, actual.getClass().getName());
    }

    public String checkParentWithAddress(Location location, Location parent, Address address) {
        if (address==null) {
            return null;
        }
        if (parent==null) {
            return "A location without a parent cannot have an address.";
        }
        if (parent.getSize()!=null) {
            if (!parent.getSize().contains(address)) {
                return String.format("Address %s is out of bounds for the specified parent.", address);
            }
        }
        Optional<Location> optOccupant = parent.getChildren().stream()
                .filter(loc -> address.equals(loc.getAddress()))
                .findAny();
        if (optOccupant.isPresent() && !optOccupant.get().getId().equals(location.getId())) {
            return String.format("Address %s is occupied by another location.", address);
        }
        return null;
    }

    public String checkCycle(Location location, Location parent) {
        Integer locId = location.getId();
        if (parent.getId().equals(locId)) {
            return "Location cannot be the parent of itself.";
        }
        if (location.getChildren().isEmpty()) {
            return null; // don't need to traverse the tree if the location we're moving is a leaf
        }
        parent = parent.getParent();
        if (parent!=null && parent.getId().equals(locId)) {
            return "Location cannot be the parent of its own parent.";
        }
        while (parent!=null) {
            if (parent.getId().equals(locId)) {
                return "Location cannot be the parent of a location that indirectly contains it.";
            }
            parent = parent.getParent();
        }
        return null;
    }
}
