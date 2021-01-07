package uk.ac.sanger.storelight.model;

import com.google.common.base.MoreObjects;

import javax.persistence.*;
import java.util.*;

import static java.util.stream.Collectors.toMap;
import static uk.ac.sanger.storelight.utils.BasicUtils.newArrayList;
import static uk.ac.sanger.storelight.utils.BasicUtils.repr;

/**
 * A location that might contain stored items or other locations
 * @author dr6
 */
@Entity
public class Location {
    public static final int MAX_DESCRIPTION = 64;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String barcode;
    private String description;
    @ManyToOne(fetch = FetchType.LAZY)
    private Location parent;
    @Embedded
    private Address address;
    @OneToMany(fetch = FetchType.LAZY, mappedBy="parent")
    private List<Location> children;
    @Embedded
    private Size size;
    @OneToMany(fetch = FetchType.LAZY, mappedBy="location")
    private List<Item> stored;

    public Location() {
        this(null, null, null, null, null, null);
    }

    public Location(Integer id, String barcode, String description, Location parent, Address address, Size size) {
        this.id = id;
        this.barcode = barcode;
        this.description = description;
        this.parent = parent;
        this.address = address;
        this.size = size;
        this.children = new ArrayList<>();
        this.stored = new ArrayList<>();
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

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Location getParent() {
        return this.parent;
    }

    public void setParent(Location parent) {
        this.parent = parent;
    }

    public Address getAddress() {
        return this.address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public List<Location> getChildren() {
        return this.children;
    }

    public void setChildren(Iterable<Location> children) {
        this.children = newArrayList(children);
    }

    public Size getSize() {
        return this.size;
    }

    public void setSize(Size size) {
        this.size = size;
    }

    public List<Item> getStored() {
        return this.stored;
    }

    public void setStored(Iterable<Item> stored) {
        this.stored = newArrayList(stored);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Location that = (Location) o;
        return (Objects.equals(this.id, that.id)
                && Objects.equals(this.barcode, that.barcode)
                && Objects.equals(this.description, that.description)
                && Objects.equals(this.address, that.address)
                && Objects.equals(this.size, that.size));
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
                .add("description", repr(description))
                .add("address", address)
                .add("size", size)
                .toString();
    }
}
