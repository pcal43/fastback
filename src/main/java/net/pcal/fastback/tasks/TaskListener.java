package net.pcal.fastback.tasks;

public interface TaskListener {

    void feedback(String message);

    void error(String message);

    default void unexpectedError() {
        error("An unexpected error occurred. See log for details.");
    }
}
