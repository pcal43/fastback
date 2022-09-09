package net.pcal.gitback.fabric;

import net.pcal.gitback.Loggr;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

class Log4jLoggr implements Loggr {

    private final Logger log4j;

    Log4jLoggr(Logger log4j) {
        this.log4j = requireNonNull(log4j);
    }

    @Override
    public void error(String message) {
        this.log4j.error(message);
    }

    @Override
    public void error(Throwable t) {
        this.log4j.error(t);
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
    public void warn(String s, IOException ioe) {
        this.log4j.warn(s, ioe);
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
    public void debug(Supplier<?> messageSupplier) {
        this.log4j.debug(messageSupplier);
    }

    @Override
    public void trace(String message) {
        this.log4j.trace(message);
    }

    @Override
    public void trace(Supplier<?> messageSupplier) {
        this.log4j.trace(messageSupplier);
    }

    @Override
    public void trace(Supplier<?> s, Throwable t) {
        this.log4j.trace(s, t);

    }
}
