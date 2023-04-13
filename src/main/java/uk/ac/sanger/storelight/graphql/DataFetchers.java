package uk.ac.sanger.storelight.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.schema.DataFetcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.sanger.storelight.model.Item;
import uk.ac.sanger.storelight.model.Location;
import uk.ac.sanger.storelight.repo.StoreDB;
import uk.ac.sanger.storelight.requests.LocationIdentifier;
import uk.ac.sanger.storelight.service.LocationService;

import java.util.List;

/**
 * @author dr6
 */
@Component
public class DataFetchers extends BaseGraphQLResource {
    final StoreDB db;
    final LocationService locationService;

    @Autowired
    public DataFetchers(ObjectMapper objectMapper, StoreDB db, LocationService locationService) {
        super(objectMapper);
        this.db = db;
        this.locationService = locationService;
    }

    public DataFetcher<Location> getLocation() {
        return dfe -> {
            LocationIdentifier li = getLocationIdentifier(dfe);
            return db.getLocationRepo().get(li);
        };
    }

    public DataFetcher<Iterable<Item>> getStored() {
        return dfe -> {
            Iterable<String> barcodes = dfe.getArgument("barcodes");
            return db.getItemRepo().findAllByBarcodeIn(barcodes);
        };
    }

    public DataFetcher<List<Location>> getLocationHierarchy() {
        return dfe -> {
            LocationIdentifier li = getLocationIdentifier(dfe);
            Location loc = db.getLocationRepo().get(li);
            return loc.getHierarchy();
        };
    }
}
