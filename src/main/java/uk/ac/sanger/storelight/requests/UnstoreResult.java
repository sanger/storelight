package uk.ac.sanger.storelight.requests;

import uk.ac.sanger.storelight.model.Item;

import java.util.Collection;
import java.util.Objects;

import static graphql.util.FpKit.toCollection;

/**
 * Type listing items that were stored
 * @author dr6
 */
public class UnstoreResult {
    private final Collection<Item> unstored;

    public UnstoreResult(Iterable<Item> unstored) {
        this.unstored = toCollection(unstored);
    }

    public int getNumUnstored() {
        return unstored.size();
    }

    public Collection<Item> getUnstored() {
        return this.unstored;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnstoreResult that = (UnstoreResult) o;
        return Objects.equals(this.unstored, that.unstored);
    }

    @Override
    public int hashCode() {
        return unstored.hashCode();
    }

    @Override
    public String toString() {
        return unstored.toString();
    }
}
