package uk.ac.sanger.storelight.requests;

import uk.ac.sanger.storelight.model.Address;

import java.util.Objects;

import static uk.ac.sanger.storelight.utils.BasicUtils.repr;

/**
 * @author dr6
 */
public class StoreInput {
    private String barcode;
    private LocationIdentifier location;
    private Address address;

    public StoreInput() {}

    public StoreInput(String barcode, LocationIdentifier location, Address address) {
        this.barcode = barcode;
        this.location = location;
        this.address = address;
    }

    public String getBarcode() {
        return this.barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public LocationIdentifier getLocation() {
        return this.location;
    }

    public void setLocation(LocationIdentifier location) {
        this.location = location;
    }

    public Address getAddress() {
        return this.address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StoreInput that = (StoreInput) o;
        return (Objects.equals(this.barcode, that.barcode)
                && Objects.equals(this.location, that.location)
                && Objects.equals(this.address, that.address));
    }

    @Override
    public int hashCode() {
        return Objects.hash(barcode, location, address);
    }

    @Override
    public String toString() {
        if (address==null) {
            return String.format("(barcode=%s, location=%s)", repr(barcode), location);
        }
        return String.format("(barcode=%s, location=%s, address=%s)",
                repr(barcode), location, address);
    }
}
