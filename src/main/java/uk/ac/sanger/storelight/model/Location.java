package uk.ac.sanger.storelight.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;

import javax.persistence.*;
import java.util.*;

import static uk.ac.sanger.storelight.utils.BasicUtils.newArrayList;
import static uk.ac.sanger.storelight.utils.BasicUtils.repr;

/**
 * A location that might contain stored items or other locations
 * @author dr6
 */
@Entity
public class Location {
    public static final int MAX_DESCRIPTION = 256, MAX_NAME = 64;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String barcode;
    private String name;
    private String description;
    @ManyToOne(fetch = FetchType.LAZY)
    private Location parent;
    @Embedded
    private Address address;
    @OneToMany(fetch = FetchType.LAZY, mappedBy="parent")
    private List<Location> children;
    @Embedded
    private Size size;

    @Column(columnDefinition = "enum('RightDown', 'DownRight', 'RightUp', 'UpRight')")
    @Enumerated(EnumType.STRING)
    private GridDirection direction;

    @OneToMany(fetch = FetchType.LAZY, mappedBy="location")
    private List<Item> stored;

    public Location() {
        this(null, null, null, null, null, null, null, null);
    }

    public Location(Integer id, String barcode) {
        this(id, barcode, null, null, null, null, null, null);
    }

    public Location(Integer id, String barcode, String name, String description, Location parent, Address address, Size size, GridDirection direction) {
        this.id = id;
        this.barcode = barcode;
        this.name = name;
        this.description = description;
        this.parent = parent;
        this.address = address;
        this.size = size;
        this.children = new ArrayList<>();
        this.stored = new ArrayList<>();
        this.direction = direction;
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

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
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

    public GridDirection getDirection() {
        return this.direction;
    }

    public void setDirection(GridDirection direction) {
        this.direction = direction;
    }

    @JsonIgnore
    public String getQualifiedNameWithFirstBarcode() {
        List<Location> hierarchy = getHierarchy();
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Location loc : hierarchy) {
            String name = loc.getName();
            if (name!=null && name.isEmpty()) {
                name = null;
            }
            if (first) {
                sb.append(loc.getBarcode());
                if (name!=null) {
                    sb.append(' ').append(name);
                }
                first = false;
            } else {
                sb.append(" / ");
                if (name!=null) {
                    sb.append(name);
                } else if (loc.getAddress()!=null) {
                    sb.append(loc.getAddress());
                } else {
                    sb.append(loc.getBarcode());
                }
            }
        }
        return sb.toString();
    }

    /**
     * Gets the parent hierarchy of this location, starting with the root, and ending with {@code this}.
     * @return a nonempty list of locations
     */
    public List<Location> getHierarchy() {
        ArrayList<Location> hierarchy = new ArrayList<>();
        Location cur = this;
        while (cur != null) {
            hierarchy.add(cur);
            cur = cur.getParent();
        }
        Collections.reverse(hierarchy);
        return hierarchy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Location that = (Location) o;
        return (Objects.equals(this.id, that.id)
                && Objects.equals(this.barcode, that.barcode)
                && Objects.equals(this.name, that.name)
                && Objects.equals(this.description, that.description)
                && Objects.equals(this.address, that.address)
                && Objects.equals(this.size, that.size)
                && this.direction==that.direction);
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
                .add("name", repr(name))
                .add("description", repr(description))
                .add("address", address)
                .add("size", size)
                .add("direction", direction)
                .omitNullValues()
                .toString();
    }

    /**
     * The index of the given address in this location, if such a thing can be deduced.
     * @param address the address inside this location
     * @return the index of the given address, starting from 1; or null
     */
    public Integer addressIndex(Address address) {
        if (address!=null && size!=null && direction!=null && size.contains(address)) {
            switch (direction) {
                case RightDown: return (address.getRow()-1) * size.getNumColumns() + address.getColumn();
                case DownRight: return (address.getColumn()-1) * size.getNumRows() + address.getRow();
                case RightUp: return (size.getNumRows()-address.getRow()) * size.getNumColumns() + address.getColumn();
                case UpRight: return (address.getColumn()-1) * size.getNumRows() + size.getNumRows() - address.getRow() + 1;
            }
        }
        return null;
    }

    /** Number of items stored directly in this location */
    public int getNumStored() {
        var stored = getStored();
        return stored==null ? 0 : stored.size();
    }

    /** Number of locations directly inside this location */
    public int getNumChildren() {
        var children = getChildren();
        return (children==null ? 0 : children.size());
    }
}
