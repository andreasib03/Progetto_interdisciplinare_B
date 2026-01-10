package it.uninsubria.shared.model;

import lombok.Value;

/**
 * Type-safe identifier for Book entities.
 * Prevents confusion with other numeric IDs.
 */
@Value
public class BookId {
    private final int value;

    /**
     * Constructs a BookId with validation.
     *
     * @param value the ID value
     * @throws IllegalArgumentException if value is negative
     */
    public BookId(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Book ID must be non-negative: " + value);
        }
        this.value = value;
    }

    /**
     * Gets the ID value.
     *
     * @return the ID value
     */
    public int getValue() {
        return value;
    }

    /**
     * Creates a BookId instance.
     *
     * @param value the ID value
     * @return the BookId instance
     */
    public static BookId of(int value) {
        return new BookId(value);
    }

    @Override
    public String toString() {
        return "BookId[" + value + "]";
    }
}
