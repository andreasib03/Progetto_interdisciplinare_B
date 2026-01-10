package it.uninsubria.shared.model;

import lombok.Value;

/**
 * Type-safe identifier for Library entities.
 * Prevents confusion with other numeric IDs.
 */
@Value
public class LibraryId {
    private final int value;

    public LibraryId(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Library ID must be non-negative: " + value);
        }
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static LibraryId of(int value) {
        return new LibraryId(value);
    }

    @Override
    public String toString() {
        return "LibraryId[" + value + "]";
    }
}
