package net.pcal.fastback.logging;

import net.minecraft.text.Text;
import net.pcal.fastback.ModContext;

import static java.util.Objects.requireNonNull;
import static net.minecraft.text.Text.translatable;

public class ChatLogger implements Logger {

    private final ModContext ctx;

    public ChatLogger(ModContext ctx) {
        this.ctx = requireNonNull(ctx);
    }

    @Override
    public void notify(Text message) {
        ctx.sendClientChatMessage(message);
    }

    @Override
    public void notifyError(Text message) {
        ctx.sendClientChatMessage(message);
    }

    @Override
    public void internalError(String message, Throwable t) {
        ctx.sendClientChatMessage(translatable("fastback.notify.internal-error"));
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
