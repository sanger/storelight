package uk.ac.sanger.storelight.model;

import com.google.common.base.MoreObjects;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.Entity;
import java.sql.Timestamp;
import java.util.Objects;

import static uk.ac.sanger.storelight.utils.BasicUtils.repr;

/**
 * A storage record, created when something is stored or unstored.
 * @author dr6
 */
@Entity
@DynamicInsert
public class StoreRecord {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer id;

    @Column(columnDefinition="TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    @Generated(GenerationTime.INSERT)
    private Timestamp recorded;

    private String barcode;
    @Embedded
    private Address address;
    private Integer locationId;
    private String username;
    private String app;

    public StoreRecord() {}

    public StoreRecord(String barcode, Address address, Integer locationId, String username, String app) {
        this.barcode = barcode;
        this.address = address;
        this.locationId = locationId;
        this.username = username;
        this.app = app;
    }

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    /** When the storage was recorded. */
    public Timestamp getRecorded() {
        return this.recorded;
    }

    /** Sets when the storage was recorded. */
    public void setRecorded(Timestamp recorded) {
        this.recorded = recorded;
    }

    /** The barcode of the stored item. */
    public String getBarcode() {
        return this.barcode;
    }

    /** Sets the barcode of the stored item. */
    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    /** The address of the item in storage (may be null). */
    public Address getAddress() {
        return this.address;
    }

    /** Sets the address of the item in storage (may be null). */
    public void setAddress(Address address) {
        this.address = address;
    }

    /** The id of the storage location (null when an item is being unstored). */
    public Integer getLocationId() {
        return this.locationId;
    }

    /** Sets the id of the storage location (null when an item is being unstored). */
    public void setLocationId(Integer locationId) {
        this.locationId = locationId;
    }

    /** The username of the person updating storage (may be null). */
    public String getUsername() {
        return this.username;
    }

    /** Sets the username of the person updating storage (may be null). */
    public void setUsername(String username) {
        this.username = username;
    }

    /** The app responsible for the request */
    public String getApp() {
        return this.app;
    }

    /** Sets the app responsible for the request */
    public void setApp(String app) {
        this.app = app;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StoreRecord that = (StoreRecord) o;
        return (Objects.equals(this.id, that.id)
                && Objects.equals(this.recorded, that.recorded)
                && Objects.equals(this.barcode, that.barcode)
                && Objects.equals(this.address, that.address)
                && Objects.equals(this.locationId, that.locationId)
                && Objects.equals(this.username, that.username)
                && Objects.equals(this.app, that.app));
    }

    @Override
    public int hashCode() {
        return (id!=null ? id.hashCode() : Objects.hash(recorded, barcode, address, locationId, username, app));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("recorded", recorded)
                .add("barcode", repr(barcode))
                .add("address", address)
                .add("locationId", locationId)
                .add("username", username)
                .add("app", app)
                .omitNullValues()
                .toString();
    }
}
