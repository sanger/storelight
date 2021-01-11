package uk.ac.sanger.storelight.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.schema.DataFetcher;
import org.springframework.stereotype.Component;
import uk.ac.sanger.storelight.model.Item;
import uk.ac.sanger.storelight.requests.LocationIdentifier;
import uk.ac.sanger.storelight.requests.UnstoreResult;
import uk.ac.sanger.storelight.service.UnstoreService;

import java.util.List;

/**
 * @author dr6
 */
@Component
public class UnstoreMutations extends BaseGraphQLResource {
    private final UnstoreService unstoreService;

    public UnstoreMutations(ObjectMapper objectMapper, UnstoreService unstoreService) {
        super(objectMapper);
        this.unstoreService = unstoreService;
    }

    public DataFetcher<Item> unstoreBarcode() {
        return dfe -> {
            String barcode = dfe.getArgument("barcode");
            return unstoreService.unstoreBarcode(auth(dfe), barcode);
        };
    }

    public DataFetcher<UnstoreResult> unstoreBarcodes() {
        return dfe -> {
            List<String> barcodes = dfe.getArgument("barcodes");
            return new UnstoreResult(unstoreService.unstoreBarcodes(auth(dfe), barcodes));
        };
    }

    public DataFetcher<UnstoreResult> empty() {
        return dfe -> {
            LocationIdentifier li = getLocationIdentifier(dfe);
            return new UnstoreResult(unstoreService.empty(auth(dfe), li));
        };
    }
}
