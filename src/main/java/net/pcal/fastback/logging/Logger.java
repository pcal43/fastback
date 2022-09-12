package net.pcal.fastback.logging;

import net.minecraft.text.Text;

public interface Logger {

    void progressComplete(String message, int percentage);

    void progressComplete(String message);

    void notify(Text message);

    void notifyError(Text message);

    void internalError(String message, Throwable t);

    void warn(String message);

    void info(String message);

    void debug(String message);

    void debug(String message, Throwable t);

    @Deprecated
    default void notify(String message) {
        this.notify(Text.literal(message));
    }

    @Deprecated
    default void notifyError(String message) {
        this.notifyError(Text.literal(message));
    }
}
