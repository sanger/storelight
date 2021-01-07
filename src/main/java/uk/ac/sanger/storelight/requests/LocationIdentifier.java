package uk.ac.sanger.storelight.requests;

import java.util.Objects;

import static uk.ac.sanger.storelight.utils.BasicUtils.repr;

/**
 * A like-union input type for a location identifier (either an id or a barcode)
 * @author dr6
 */
public class LocationIdentifier {
    private Integer id;
    private String barcode;

    public LocationIdentifier() {}

    public LocationIdentifier(Integer id) {
        this.id = id;
    }

    public LocationIdentifier(String barcode) {
        this.barcode = barcode;
    }

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getBarcode() {
        return this.barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocationIdentifier that = (LocationIdentifier) o;
        return (Objects.equals(this.id, that.id)
                && Objects.equals(this.barcode, that.barcode));
    }

    @Override
    public int hashCode() {
        return (id!=null ? id.hashCode() : barcode!=null ? barcode.hashCode() : 0);
    }

    @Override
    public String toString() {
        if (id!=null) {
            if (barcode!=null) {
                return String.format("(id=%s, barcode=%s)", id, repr(barcode));
            }
            return String.format("(id=%s)", id);
        }
        if (barcode!=null) {
            return String.format("(barcode=%s)", repr(barcode));
        }
        return "(no identifier)";
    }

    public boolean isSpecified() {
        return (id!=null || barcode!=null);
    }
}
