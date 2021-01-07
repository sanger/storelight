package uk.ac.sanger.storelight.requests;

import com.google.common.base.MoreObjects;
import uk.ac.sanger.storelight.model.Item;

import java.util.Collection;
import java.util.Objects;

import static graphql.util.FpKit.toCollection;

/**
 * Type listing items that were stored
 * @author dr6
 */
public class StoreResult {
    private final Collection<Item> stored;

    public StoreResult(Iterable<Item> stored) {
        this.stored = toCollection(stored);
    }

    public int getNumStored() {
        return stored.size();
    }

    public Collection<Item> getStored() {
        return this.stored;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StoreResult that = (StoreResult) o;
        return Objects.equals(this.stored, that.stored);
    }

    @Override
    public int hashCode() {
        return stored.hashCode();
    }

    @Override
    public String toString() {
        return stored.toString();
    }
}
