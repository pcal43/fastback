package net.pcal.fastback.logging;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static java.util.Objects.requireNonNull;
import static net.minecraft.text.Text.translatable;

public class CommandSourceLogger implements Logger {

    private final ServerCommandSource scs;

    public CommandSourceLogger(ServerCommandSource scs) {
        this.scs = requireNonNull(scs);
    }

    @Override
    public void notify(Text message) {
        scs.sendFeedback(message, false);
    }

    @Override
    public void notifyError(Text message) {
        scs.sendError(message);
    }

    @Override
    public void internalError(String message, Throwable t) {
        scs.sendError(translatable("fastback.notify.internal-error"));
    }

    @Override
    public void warn(String message) {
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
