package uk.ac.sanger.storelight.graphql;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.schema.DataFetcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.sanger.storelight.model.*;
import uk.ac.sanger.storelight.repo.StoreDB;
import uk.ac.sanger.storelight.requests.*;
import uk.ac.sanger.storelight.service.LocationService;
import uk.ac.sanger.storelight.service.StoreService;

import java.util.List;

/**
 * @author dr6
 */
@Component
public class StoreMutations extends BaseGraphQLResource {
    private final StoreDB db;
    private final StoreService storeService;

    @Autowired
    public StoreMutations(ObjectMapper objectMapper, StoreDB db,
                          StoreService storeService) {
        super(objectMapper);
        this.db = db;
        this.storeService = storeService;
    }

    public DataFetcher<Item> storeBarcode() {
        return dfe -> {
            String barcode = dfe.getArgument("barcode");
            Address address = dfe.getArgument("address");
            LocationIdentifier li = getLocationIdentifier(dfe);
            return storeService.storeBarcode(barcode, li, address);
        };
    }

    public DataFetcher<StoreResult> storeBarcodes() {
        return dfe -> {
            List<String> barcodes = dfe.getArgument("barcodes");
            LocationIdentifier li = getLocationIdentifier(dfe);
            return new StoreResult(storeService.storeBarcodes(barcodes, li));
        };
    }

    public DataFetcher<StoreResult> store() {
        return dfe -> {
            List<StoreInput> storeInputs = arg(dfe, "store", new TypeReference<List<StoreInput>>() {});
            LocationIdentifier li = getLocationIdentifier(dfe);
            return new StoreResult(storeService.store(storeInputs, li));
        };
    }
}
