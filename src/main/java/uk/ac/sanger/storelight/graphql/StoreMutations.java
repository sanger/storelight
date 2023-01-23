package uk.ac.sanger.storelight.graphql;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.schema.DataFetcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.sanger.storelight.model.Address;
import uk.ac.sanger.storelight.model.Item;
import uk.ac.sanger.storelight.requests.*;
import uk.ac.sanger.storelight.service.StoreService;

import java.util.List;

/**
 * @author dr6
 */
@Component
public class StoreMutations extends BaseGraphQLResource {
    private final StoreService storeService;

    @Autowired
    public StoreMutations(ObjectMapper objectMapper, StoreService storeService) {
        super(objectMapper);
        this.storeService = storeService;
    }

    public DataFetcher<Item> storeBarcode() {
        return dfe -> {
            String barcode = dfe.getArgument("barcode");
            Address address = dfe.getArgument("address");
            LocationIdentifier li = getLocationIdentifier(dfe);
            return storeService.storeBarcode(auth(dfe), barcode, li, address);
        };
    }

    public DataFetcher<StoreResult> storeBarcodes() {
        return dfe -> {
            List<String> barcodes = dfe.getArgument("barcodes");
            LocationIdentifier li = getLocationIdentifier(dfe);
            return new StoreResult(storeService.storeBarcodes(auth(dfe), barcodes, li));
        };
    }

    public DataFetcher<StoreResult> store() {
        return dfe -> {
            List<StoreInput> storeInputs = arg(dfe, "store", new TypeReference<>() {});
            LocationIdentifier li = getLocationIdentifier(dfe);
            return new StoreResult(storeService.store(auth(dfe), storeInputs, li));
        };
    }

    public DataFetcher<StoreResult> transfer() {
        return dfe -> {
            LocationIdentifier srcLi = getLocationIdentifier(dfe, "source");
            LocationIdentifier dstLi = getLocationIdentifier(dfe, "destination");
            return new StoreResult(storeService.transfer(auth(dfe), srcLi, dstLi));
        };
    }
}
