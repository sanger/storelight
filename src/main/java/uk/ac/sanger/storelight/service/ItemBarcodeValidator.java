package uk.ac.sanger.storelight.service;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import uk.ac.sanger.storelight.model.BarcodeSeed;
import uk.ac.sanger.storelight.model.Item;
import uk.ac.sanger.storelight.utils.BasicUtils;
import uk.ac.sanger.storelight.utils.CIStringSet;

import java.util.*;
import java.util.stream.Stream;

import static uk.ac.sanger.storelight.utils.BasicUtils.pluralise;
import static uk.ac.sanger.storelight.utils.BasicUtils.repr;

/**
 * Checks a stream of item barcodes and generates an exception listing all the problems it finds.
 * @author dr6
 */
@Service
public class ItemBarcodeValidator {

    public CIStringSet validateItemBarcodes(Stream<String> itemBarcodes) {
        boolean anyNull = false;
        CIStringSet seen = new CIStringSet();
        Set<String> untrimmed = new CIStringSet();
        Set<String> tooShort = new CIStringSet();
        Set<String> tooLong = new CIStringSet();
        Set<String> wrongPrefix = new CIStringSet();
        Set<String> repeated = new CIStringSet();
        for (String barcode : (Iterable<String>) itemBarcodes::iterator) {
            if (barcode==null) {
                anyNull = true;
                continue;
            }
            if (!seen.add(barcode)) {
                repeated.add(barcode);
                continue;
            }
            if (barcode.length() < Item.MIN_BARCODE) {
                tooShort.add(barcode);
            } else if (barcode.length() > Item.MAX_BARCODE) {
                tooLong.add(barcode);
            } else if (BasicUtils.startsWithIgnoreCase(barcode, BarcodeSeed.STORE_PREFIX)) {
                wrongPrefix.add(barcode);
            } else if (!barcode.trim().equals(barcode)) {
                untrimmed.add(barcode);
            }
        }
        if (!anyNull && untrimmed.isEmpty() && tooShort.isEmpty() && tooLong.isEmpty() && wrongPrefix.isEmpty() && repeated.isEmpty()) {
            return seen;
        }
        List<String> errors = new ArrayList<>();
        if (anyNull) {
            errors.add("Null given as item barcode.");
        }
        if (!repeated.isEmpty()) {
            errors.add(barcodeError("Barcode{s} repeated:", repeated));
        }
        if (!tooShort.isEmpty()) {
            errors.add(barcodeError("Barcode{s} too short:", tooShort));
        }
        if (!tooLong.isEmpty()) {
            errors.add(barcodeError("Barcode{s} too long:", tooLong));
        }
        if (!untrimmed.isEmpty()) {
            errors.add(barcodeError("Barcode{s} {has|have} surrounding whitespace:", untrimmed));
        }
        if (!wrongPrefix.isEmpty()) {
            errors.add(barcodeError("Barcodes cannot start with STO-:", wrongPrefix));
        }
        throw new IllegalArgumentException(String.join(" ", errors));
    }

    private static String barcodeError(String template, Collection<String> barcodes) {
        StringBuilder sb = new StringBuilder(pluralise(template, barcodes.size()));
        sb.append(" [");
        boolean first = true;
        for (String bc : barcodes) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(repr(bc));
        }
        sb.append("].");
        return sb.toString();
    }
}
