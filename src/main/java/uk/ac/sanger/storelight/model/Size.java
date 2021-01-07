package uk.ac.sanger.storelight.model;

import com.google.common.base.MoreObjects;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A grid size (num rows and num columns, both positive).
 * @author dr6
 */
@SuppressWarnings("JpaDataSourceORMInspection")
@Embeddable
public class Size {
    @Column(name="num_rows")
    private int numRows;
    @Column(name="num_columns")
    private int numColumns;

    public Size() {
        this(1,1);
    }

    public Size(int numRows, int numColumns) {
        setNumRows(numRows);
        setNumColumns(numColumns);
    }

    public int getNumRows() {
        return this.numRows;
    }

    public void setNumRows(int numRows) {
        if (numRows <= 0) {
            throw new IllegalArgumentException("numRows cannot be less than 1.");
        }
        this.numRows = numRows;
    }

    public int getNumColumns() {
        return this.numColumns;
    }

    public void setNumColumns(int numColumns) {
        if (numColumns <= 0) {
            throw new IllegalArgumentException("numColumns cannot be less than 1.");
        }
        this.numColumns = numColumns;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Size that = (Size) o;
        return (this.numRows == that.numRows
                && this.numColumns == that.numColumns);
    }

    @Override
    public int hashCode() {
        return numRows + 63*numColumns;
    }

    @Override
    public String toString() {
        return String.format("(numRows=%s, numColumns=%s)", numRows, numColumns);
    }

    public Stream<Address> addresses() {
        return Address.stream(this.numRows, this.numColumns);
    }

    public boolean contains(Address address) {
        return contains(address.getRow(), address.getColumn());
    }

    public boolean contains(int row, int column) {
        return (1 <= row && row <= numRows && 1 <= column && column <= numColumns);
    }
}
