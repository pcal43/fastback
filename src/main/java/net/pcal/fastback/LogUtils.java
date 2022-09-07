package net.pcal.fastback;

import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public class LogUtils {

    public static void error(Loginator logger, String message) {
        logger.error(message);
    }

    public static void error(Loginator logger, String message, Throwable exception) {
        logger.error(message, exception);
    }

    public static void warn(Loginator logger, String message) {
        logger.warn(message);
    }

    public static void info(Loginator logger, String message) {
        logger.info(message);
    }

    public static void debug(Loginator logger, String message) {
        logger.debug(message);
    }

    public static void debug(Loginator logger, Supplier<?> messageSupplier) {
        logger.debug(() -> messageSupplier.get());
    }

    public static void trace(Loginator logger, String message) {
        logger.trace(message);
    }

    public static void trace(Logger logger, Supplier<?> messageSupplier) {
        logger.trace(() -> messageSupplier.get());
    }

}
