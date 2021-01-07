package uk.ac.sanger.storelight.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.schema.DataFetcher;
import org.springframework.stereotype.Component;
import uk.ac.sanger.storelight.model.Item;
import uk.ac.sanger.storelight.repo.ItemRepo;
import uk.ac.sanger.storelight.repo.StoreDB;
import uk.ac.sanger.storelight.requests.UnstoreResult;

import java.util.List;
import java.util.Optional;

/**
 * @author dr6
 */
@Component
public class UnstoreMutations extends BaseGraphQLResource {
    private final StoreDB db;

    public UnstoreMutations(ObjectMapper objectMapper, StoreDB db) {
        super(objectMapper);
        this.db = db;
    }

    public DataFetcher<Item> unstoreBarcode() {
        return dfe -> {
            String barcode = dfe.getArgument("barcode");
            ItemRepo itemRepo = db.getItemRepo();
            Optional<Item> optItem = itemRepo.findByBarcode(barcode);
            if (optItem.isEmpty()) {
                return null;
            }
            Item item = optItem.get();
            itemRepo.delete(item);
            return item;
        };
    }

    public DataFetcher<UnstoreResult> unstoreBarcodes() {
        return dfe -> {
            List<String> barcodes = dfe.getArgument("barcodes");
            ItemRepo itemRepo = db.getItemRepo();
            List<Item> items = itemRepo.findAllByBarcodeIn(barcodes);
            itemRepo.deleteAllByBarcodeIn(barcodes);
            return new UnstoreResult(items);
        };
    }
}
