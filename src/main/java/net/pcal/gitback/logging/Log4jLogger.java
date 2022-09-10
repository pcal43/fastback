package net.pcal.gitback.logging;

import static java.util.Objects.requireNonNull;

public class Log4jLogger implements Logger {

    private final org.apache.logging.log4j.Logger log4j;

    public Log4jLogger(org.apache.logging.log4j.Logger log4j) {
        this.log4j = requireNonNull(log4j);
    }

    @Override
    public void notify(String message) {
        this.log4j.info("[NOTIFY] " + message);
    }

    @Override
    public void notifyError(String message) {
        this.log4j.error("[NOTIFY-ERROR]" + message);
    }

    @Override
    public void progressComplete(String message, int percent) {
        this.log4j.info("[PROGRESS " + message + " " + percent);
    }

    @Override
    public void progressComplete(String message) {
        this.log4j.info("[PROGRESS-COMPLETE] " + message);
    }

    @Override
    public void internalError(String message, Throwable t) {
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
    public void debug(String message, Throwable t) {
        this.log4j.debug(message, t);
    }
}
