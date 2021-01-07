package uk.ac.sanger.storelight.repo;

import org.springframework.data.repository.CrudRepository;
import uk.ac.sanger.storelight.model.Item;

import java.util.*;

public interface ItemRepo extends CrudRepository<Item, Integer> {
    Optional<Item> findByBarcode(String barcode);
    List<Item> findAllByBarcodeIn(Iterable<String> barcodes);
    void deleteAllByBarcodeIn(Iterable<String> barcodes);

}
