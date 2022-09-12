package net.pcal.fastback.logging;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static java.util.Objects.requireNonNull;

public class ChatLogger implements Logger {

    private final ServerCommandSource scs;

    public ChatLogger(ServerCommandSource scs) {
        this.scs = requireNonNull(scs);
    }

    @Override
    public void notify(String message) {
        scs.sendFeedback(Text.literal(message), false);
    }

    @Override
    public void notifyError(String message) {
        scs.sendError(Text.literal(message));
    }

    @Override
    public void internalError(String message, Throwable t) {
        scs.sendError(Text.literal("An unexpected error occurred. See log for details."));
    }

    @Override
    public void warn(String message) {
        scs.sendError(Text.literal(message));
    }

    @Override
    public void progressComplete(String message) {
    }

    @Override
    public void progressComplete(String message, int percentage) {
    }

    @Override
    public void info(String message) {
    }

    @Override
    public void debug(String message) {
    }

    @Override
    public void debug(String message, Throwable t) {
    }
}
