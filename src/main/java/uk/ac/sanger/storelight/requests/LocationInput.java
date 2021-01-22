package uk.ac.sanger.storelight.requests;

import com.google.common.base.MoreObjects;
import uk.ac.sanger.storelight.model.*;

import java.util.Objects;

import static uk.ac.sanger.storelight.utils.BasicUtils.repr;

/**
 * Incoming specification of a {@link uk.ac.sanger.storelight.model.Location Location}
 * @author dr6
 */
public class LocationInput {
    private String description;
    private Integer parentId;
    private Address address;
    private Size size;
    private GridDirection direction;

    public LocationInput() {}

    public LocationInput(String description, Integer parentId, Address address, Size size, GridDirection direction) {
        this.description = description;
        this.parentId = parentId;
        this.address = address;
        this.size = size;
        this.direction = direction;
    }

    /**
     * The description of the location (optional)
     * @see uk.ac.sanger.storelight.model.Location#MAX_DESCRIPTION
     * @return the description of the location, or null
     */
    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * The id of the parent location (if any)
     * @return the id of the parent location, or null
     */
    public Integer getParentId() {
        return this.parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    /**
     * The address of this location inside its parent (optional)
     * @return the address of this location, or null
     */
    public Address getAddress() {
        return this.address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    /**
     * The size of this location in rows and columns (optional)
     * @return the size of this location, or null
     */
    public Size getSize() {
        return this.size;
    }

    public void setSize(Size size) {
        this.size = size;
    }

    /**
     * The preferred direction of grid iteration, if any.
     * @return the preferred direction of grid iteration, or null
     */
    public GridDirection getDirection() {
        return this.direction;
    }

    public void setDirection(GridDirection direction) {
        this.direction = direction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocationInput that = (LocationInput) o;
        return (Objects.equals(this.description, that.description)
                && Objects.equals(this.parentId, that.parentId)
                && Objects.equals(this.address, that.address)
                && Objects.equals(this.size, that.size)
                && this.direction==that.direction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, parentId, address, size);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("description", repr(description))
                .add("parentId", parentId)
                .add("address", address)
                .add("size", size)
                .add("direction", direction)
                .omitNullValues()
                .toString();
    }
}
