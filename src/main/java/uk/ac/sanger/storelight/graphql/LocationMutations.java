package uk.ac.sanger.storelight.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.schema.DataFetcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.sanger.storelight.model.Location;
import uk.ac.sanger.storelight.requests.LocationInput;
import uk.ac.sanger.storelight.service.LocationService;

/**
 * @author dr6
 */
@Component
public class LocationMutations extends BaseGraphQLResource {
    private final LocationService locationService;

    @Autowired
    public LocationMutations(ObjectMapper objectMapper, LocationService locationService) {
        super(objectMapper);
        this.locationService = locationService;
    }

    public DataFetcher<Location> addLocation() {
        return dfe -> {
            LocationInput lin = arg(dfe, "location", LocationInput.class);
            return locationService.createLocation(lin);
        };
    }
}
