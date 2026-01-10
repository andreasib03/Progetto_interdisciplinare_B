package it.uninsubria.shared.utils;

/**
 * Application constants for configuration values.
 */
public final class AppConstants {

    private AppConstants() {
    }

    /**
     * Time-related constants.
     */
    public static class Time {
        /** Milliseconds per second. */
        public static final long MILLIS_PER_SECOND = 1000L;
        /** Milliseconds per minute. */
        public static final long MILLIS_PER_MINUTE = 60 * MILLIS_PER_SECOND;
        /** Milliseconds per hour. */
        public static final long MILLIS_PER_HOUR = 60 * MILLIS_PER_MINUTE;
        /** Milliseconds per day. */
        public static final long MILLIS_PER_DAY = 24 * MILLIS_PER_HOUR;

        /** Timeout of 2 minutes in milliseconds. */
        public static final long TIMEOUT_2_MINUTES = 2 * MILLIS_PER_MINUTE;
        /** Timeout of 5 minutes in milliseconds. */
        public static final long TIMEOUT_5_MINUTES = 5 * MILLIS_PER_MINUTE;
        /** Timeout of 10 minutes in milliseconds. */
        public static final long TIMEOUT_10_MINUTES = 10 * MILLIS_PER_MINUTE;
        /** Timeout of 15 minutes in milliseconds. */
        public static final long TIMEOUT_15_MINUTES = 15 * MILLIS_PER_MINUTE;
        /** Timeout of 30 minutes in milliseconds. */
        public static final long TIMEOUT_30_MINUTES = 30 * MILLIS_PER_MINUTE;
        /** Timeout of 1 hour in milliseconds. */
        public static final long TIMEOUT_1_HOUR = MILLIS_PER_HOUR;
        /** Timeout of 1 day in milliseconds. */
        public static final long TIMEOUT_1_DAY = MILLIS_PER_DAY;
        /** Timeout of 30 days in milliseconds. */
        public static final long TIMEOUT_30_DAYS = 30 * MILLIS_PER_DAY;
    }

    /**
     * Network-related constants.
     */
    public static class Network {
        /** Default RMI registry port. */
        public static final int RMI_REGISTRY_PORT = 1099;
        /** Default RMI host address. */
        public static final String RMI_HOST_DEFAULT = "localhost";
        /** Default PostgreSQL port. */
        public static final int POSTGRESQL_DEFAULT_PORT = 5432;
    }

    /**
     * Cache-related constants.
     */
    public static class Cache {
        /** Maximum cache entries per user. */
        public static final int MAX_CACHE_ENTRIES_PER_USER = 1000;
        /** Maximum concurrent users. */
        public static final int MAX_CONCURRENT_USERS = 1000;
        /** Default cache TTL in milliseconds. */
        public static final long DEFAULT_TTL_MS = Time.TIMEOUT_5_MINUTES;
    }

    /**
     * Batch processing constants.
     */
    public static class Batch {
        /** Default batch size. */
        public static final int DEFAULT_SIZE = 10000;
        /** Threshold for large dataset. */
        public static final int LARGE_DATASET_THRESHOLD = 100000;
        /** Size threshold for large dataset. */
        public static final int LARGE_DATASET_SIZE_THRESHOLD = 50000;
        /** Batch size for data seeder. */
        public static final int DATA_SEEDER_BATCH_SIZE = 5000;
    }

    /**
     * Validation-related constants.
     */
    public static class Validation {
        /** Minimum valid year. */
        public static final int MIN_YEAR = 1000;
        /** Maximum year offset from current year. */
        public static final int MAX_YEAR_OFFSET = 10;
        /** Maximum string length for validation. */
        public static final int MAX_STRING_LENGTH = 10000;
    }

    /**
     * Database-related constants.
     */
    public static class Database {
        /** Connection idle timeout in milliseconds. */
        public static final long IDLE_TIMEOUT_MS = Time.TIMEOUT_5_MINUTES;
        /** Connection keepalive time in milliseconds. */
        public static final long KEEPALIVE_TIME_MS = Time.TIMEOUT_5_MINUTES;
        /** Leak detection threshold in milliseconds. */
        public static final long LEAK_DETECTION_THRESHOLD_MS = Time.MILLIS_PER_MINUTE;
        /** Maximum connection pool size. */
        public static final int CONNECTION_POOL_MAX_SIZE = 50;
        /** Minimum idle connections in pool. */
        public static final int CONNECTION_POOL_MIN_IDLE = 10;
        /** Connection timeout in milliseconds. */
        public static final int CONNECTION_TIMEOUT_MS = 20000;
        /** Maximum connection lifetime in milliseconds. */
        public static final int MAX_LIFETIME_MS = 1200000; // 20 minutes
        /** Validation timeout in milliseconds. */
        public static final int VALIDATION_TIMEOUT_MS = 5000;
    }

    /**
     * UI-related constants.
     */
    public static class UI {
        /** Default window width. */
        public static final int WINDOW_WIDTH = 1000;
        /** Default window height. */
        public static final int WINDOW_HEIGHT = 700;
    }
}
