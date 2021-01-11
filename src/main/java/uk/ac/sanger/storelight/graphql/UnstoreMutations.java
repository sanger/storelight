package uk.ac.sanger.storelight.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.schema.DataFetcher;
import org.springframework.stereotype.Component;
import uk.ac.sanger.storelight.model.Item;
import uk.ac.sanger.storelight.repo.ItemRepo;
import uk.ac.sanger.storelight.repo.StoreDB;
import uk.ac.sanger.storelight.requests.LocationIdentifier;
import uk.ac.sanger.storelight.requests.UnstoreResult;
import uk.ac.sanger.storelight.service.UnstoreService;

import java.util.List;
import java.util.Optional;

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
            return unstoreService.unstoreBarcode(barcode);
        };
    }

    public DataFetcher<UnstoreResult> unstoreBarcodes() {
        return dfe -> {
            List<String> barcodes = dfe.getArgument("barcodes");
            return new UnstoreResult(unstoreService.unstoreBarcodes(barcodes));
        };
    }

    public DataFetcher<UnstoreResult> empty() {
        return dfe -> {
            LocationIdentifier li = getLocationIdentifier(dfe);
            return new UnstoreResult(unstoreService.empty(li));
        };
    }
}
