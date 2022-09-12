package net.pcal.fastback.logging;

import net.minecraft.text.Text;

import java.util.List;

import static java.util.Objects.requireNonNull;

public class CompositeLogger implements Logger {

    private final Iterable<Logger> delegates;

    public static Logger of(Logger... loggers) {
        return new CompositeLogger(List.of(loggers));
    }

    public CompositeLogger(Iterable<Logger> delegates) {
        this.delegates = requireNonNull(delegates);
    }

    @Override
    public void notify(Text message) {
        this.delegates.forEach(d -> d.notify(message));
    }

    @Override
    public void progressComplete(String message, int percentage) {
        this.delegates.forEach(d -> d.progressComplete(message, percentage));
    }

    @Override
    public void progressComplete(String message) {
        this.delegates.forEach(d -> d.progressComplete(message));
    }

    @Override
    public void notifyError(Text message) {
        this.delegates.forEach(d -> d.notifyError(message));
    }

    @Override
    public void internalError(String message, Throwable t) {
        this.delegates.forEach(d -> d.internalError(message, t));
    }

    @Override
    public void warn(String message) {
        this.delegates.forEach(d -> d.warn(message));
    }

    @Override
    public void info(String message) {
        this.delegates.forEach(d -> d.info(message));
    }

    @Override
    public void debug(String message) {
        this.delegates.forEach(d -> d.debug(message));
    }

    @Override
    public void debug(String message, Throwable t) {
        this.delegates.forEach(d -> d.debug(message, t));
    }
}
