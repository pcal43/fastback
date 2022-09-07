package net.pcal.fastback.tasks;

import net.pcal.fastback.Loggr;

import static java.util.Objects.requireNonNull;

public final class TaskLogger {

    private final TaskListener listener;
    private final Loggr logger;

    public TaskLogger(TaskListener listener, Loggr logger) {
        this.listener = requireNonNull(listener);
        this.logger = requireNonNull(logger);
    }

    void userFeedback(String message) {
        this.listener.feedback(message);
    }

    void userError(String message) {
        this.listener.error(message);
    }

    void unexpectedError() {
        userError("An unexpected error occurred. See log for details.");
    }

    void warn(String msg) {
        this.logger.warn(msg);
        this.listener.error(msg);
    }
}
