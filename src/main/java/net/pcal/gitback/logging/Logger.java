package net.pcal.gitback.logging;

public interface Logger {

    void notify(String message);

    void progressComplete(String message, int percentage);

    void progressComplete(String message);

    void notifyError(String message);

    void internalError(String message, Throwable t);

    void warn(String message);

    void info(String message);

    void debug(String message);

    void debug(String message, Throwable t);

}
