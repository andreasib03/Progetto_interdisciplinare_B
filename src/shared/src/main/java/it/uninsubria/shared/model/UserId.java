package it.uninsubria.shared.model;

import lombok.Value;

/**
 * Type-safe identifier for User entities.
 * Prevents confusion with other numeric IDs.
 */
@Value
public class UserId {
    private final int value;

    public UserId(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("User ID must be non-negative: " + value);
        }
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static UserId of(int value) {
        return new UserId(value);
    }

    @Override
    public String toString() {
        return "UserId[" + value + "]";
    }
}
