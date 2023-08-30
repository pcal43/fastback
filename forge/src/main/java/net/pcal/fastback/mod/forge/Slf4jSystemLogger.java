package net.pcal.fastback.mod.forge;

import net.pcal.fastback.logging.SystemLogger;
import org.slf4j.Logger;

import static java.util.Objects.requireNonNull;

/**
 * @author pcal
 * @since 0.15.0
 */
class Slf4jSystemLogger implements SystemLogger {

    private final Logger slf4j;
    private boolean forceDebugEnabled = false;

    Slf4jSystemLogger(Logger slf4j) {
        this.slf4j = requireNonNull(slf4j);
    }

    @Override
    public void setForceDebugEnabled(boolean forceDebugEnabled) {
        this.forceDebugEnabled = forceDebugEnabled;
    }

    @Override
    public void error(String message) {
        this.slf4j.error(message);
    }

    @Override
    public void error(String message, Throwable t) {
        this.slf4j.error(message, t);
    }

    @Override
    public void warn(String message) {
        this.slf4j.warn(message);
    }

    @Override
    public void info(String message) {
        this.slf4j.info(message);
    }

    @Override
    public void debug(String message) {
        if (this.forceDebugEnabled) {
            this.slf4j.info("[DEBUG] " + message);
        } else {
            this.slf4j.debug(message);
        }
    }

    @Override
    public void debug(String message, Throwable t) {
        if (this.forceDebugEnabled) {
            this.slf4j.info("[DEBUG] " + message, t);
        } else {
            this.slf4j.debug(message, t);
        }
    }
}
