package net.pcal.fastback.fabric;

import net.pcal.fastback.ModContext;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

class FabricLoggerAdapter implements ModContext.Logger {

    private final Logger log4j;

    FabricLoggerAdapter(Logger log4j) {
        this.log4j = requireNonNull(log4j);
    }


    @Override
    public void error(String message) {
        this.log4j.error(message);
    }

    @Override
    public void error(String message, Throwable t) {
        this.log4j.error(message, t);
    }

    @Override
    public void warn(String message) {
        this.log4j.warn(message);
    }

    @Override
    public void info(String message) {
        this.log4j.info(message);
    }

    @Override
    public void debug(String message) {
        this.log4j.debug(message);
    }

    @Override
    public void trace(String message) {
        this.log4j.trace(message);
    }

    @Override
    public void trace(Supplier<?> messageSupplier) {
        this.log4j.trace(messageSupplier);
    }
}
