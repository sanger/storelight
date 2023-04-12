package uk.ac.sanger.storelight.repo;

import org.springframework.data.repository.CrudRepository;
import uk.ac.sanger.storelight.model.Location;
import uk.ac.sanger.storelight.requests.LocationIdentifier;

import javax.persistence.EntityNotFoundException;
import java.util.Collection;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static uk.ac.sanger.storelight.utils.BasicUtils.repr;

public interface LocationRepo extends CrudRepository<Location, Integer> {
    Optional<Location> findByBarcode(String barcode);

    Iterable<Location> findAllByBarcodeIn(Collection<String> barcodes);

    default Location getById(final Integer id) throws EntityNotFoundException {
        return findById(id).orElseThrow(() -> new EntityNotFoundException("No location found with id "+id));
    }

    default Location getByBarcode(final String barcode) throws EntityNotFoundException {
        return findByBarcode(barcode).orElseThrow(() -> new EntityNotFoundException("No location found with barcode "+repr(barcode)));
    }

    default Location get(LocationIdentifier li) throws EntityNotFoundException {
        requireNonNull(li, "Null location identifier given.");
        if (li.getId()!=null) {
            Location loc = getById(li.getId());
            if (li.getBarcode()!=null && !li.getBarcode().equalsIgnoreCase(loc.getBarcode())) {
                throw new IllegalArgumentException("Inconsistent location identifiers given: "+li);
            }
            return loc;
        }
        if (li.getBarcode()!=null) {
            return getByBarcode(li.getBarcode());
        }
        throw new IllegalArgumentException("No location identifier given.");
    }
}
