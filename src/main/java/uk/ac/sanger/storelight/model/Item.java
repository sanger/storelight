package uk.ac.sanger.storelight.model;

import com.google.common.base.MoreObjects;

import javax.persistence.*;
import java.util.Objects;

import static uk.ac.sanger.storelight.utils.BasicUtils.repr;

/**
 * A barcoded item stored inside a location
 * @author dr6
 */
@Entity
public class Item {
    public static final int MIN_BARCODE = 2, MAX_BARCODE = 64;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String barcode;

    @ManyToOne
    private Location location;

    @Embedded
    private Address address;

    public Item() {
        this(null, null, null, null);
    }

    public Item(String barcode, Location location) {
        this(null, barcode, location, null);
    }

    public Item(Integer id, String barcode, Location location, Address address) {
        this.id = id;
        this.barcode = barcode;
        this.location = location;
        this.address = address;
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

    public Location getLocation() {
        return this.location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Address getAddress() {
        return this.address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    private boolean sameLocationAs(Item that) {
        return (this.location == that.location ||
                this.location != null && that.location != null && this.location.getId() != null
                        && this.location.getId().equals(that.location.getId()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Item that = (Item) o;
        return (Objects.equals(this.id, that.id)
                && Objects.equals(this.barcode, that.barcode)
                && this.sameLocationAs(that)
                && Objects.equals(this.address, that.address));
    }

    @Override
    public int hashCode() {
        return (id!=null ? id.hashCode() : barcode!=null ? barcode.hashCode() : 0);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("barcode", repr(barcode))
                .add("address", address)
                .add("locationId", location==null ? null : location.getId())
                .toString();
    }
}
