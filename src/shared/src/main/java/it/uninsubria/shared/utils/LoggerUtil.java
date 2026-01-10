package it.uninsubria.shared.utils;

import java.util.function.Supplier;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LoggerUtil {
    public static Logger getLogger(Class<?> clazz) {
        Logger logger = Logger.getLogger(clazz.getName());
        logger.setUseParentHandlers(false); // Evita duplicazione output su console

        if (logger.getHandlers().length == 0) {
            ConsoleHandler handler = new ConsoleHandler();
            handler.setLevel(Level.ALL); // Puoi cambiare in INFO, WARNING ecc.
            handler.setFormatter(new SimpleFormatter()); // Puoi creare anche un formatter personalizzato
            logger.addHandler(handler);
        }

        logger.setLevel(Level.ALL);
        return logger;
    }

    /**
     * Utility method to handle exceptions with consistent logging and fallback behavior.
     * @param operation Description of the operation being attempted
     * @param logger The logger to use for error reporting
     * @param operationSupplier The operation that might throw exceptions
     * @param defaultValue Value to return if operation fails
     * @return Result of operation or defaultValue if failed
     */
    public static <T> T handleException(String operation, Logger logger,
                                       Supplier<T> operationSupplier, T defaultValue) {
        try {
            return operationSupplier.get();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Remote operation failed: " + operation, e);
            return defaultValue;
        }
    }

    /**
     * Utility method to handle exceptions that returns boolean results.
     * @param operation Description of the operation being attempted
     * @param logger The logger to use for error reporting
     * @param operationSupplier The boolean operation that might throw exceptions
     * @return Result of operation or false if failed
     */
    public static boolean handleException(String operation, Logger logger,
                                         Supplier<Boolean> operationSupplier) {
        return handleException(operation, logger, operationSupplier, false);
    }

    /**
     * Check if debug logging is enabled for the given logger.
     * Useful for avoiding expensive string concatenation when debug is disabled.
     */
    public static boolean isDebugEnabled(Logger logger) {
        return logger.isLoggable(Level.FINE);
    }

    /**
     * Check if info logging is enabled for the given logger.
     */
    public static boolean isInfoEnabled(Logger logger) {
        return logger.isLoggable(Level.INFO);
    }

    /**
     * Logs debug messages only if debug is enabled.
     * This helps reduce noise in production while keeping debug info available when needed.
     *
     * @param logger the logger to use
     * @param message the debug message
     */
    public static void debug(Logger logger, String message) {
        if (isDebugEnabled(logger)) {
            logger.fine(message);
        }
    }

    /**
     * Logs debug messages with parameters only if debug is enabled.
     *
     * @param logger the logger to use
     * @param message the debug message with placeholders
     * @param params parameters to substitute in the message
     */
    public static void debug(Logger logger, String message, Object... params) {
        if (isDebugEnabled(logger)) {
            logger.fine(String.format(message, params));
        }
    }
}
