package net.pcal.fastback.logging;

import net.minecraft.text.Text;
import net.pcal.fastback.ModContext;

import static java.util.Objects.requireNonNull;
import static net.minecraft.text.Text.literal;
import static net.minecraft.text.Text.translatable;

public class SaveScreenLogger implements Logger {

    private final ModContext ctx;

    public SaveScreenLogger(final ModContext ctx) {
        this.ctx = requireNonNull(ctx);
    }

    @Override
    public void progressComplete(String message, int percentage) {
        Text text = null;
        if (message.contains("Finding sources")) {
            text = translatable("fastback.savescreen.remote-preparing", percentage);
        } else if (message.contains("Writing objects")) {
            text = translatable("fastback.savescreen.remote-uploading", percentage);
        }
        if (text == null) text = literal(message + " " + percentage + "%");
        this.ctx.setSavingScreenText(text);
    }

    @Override
    public void progressComplete(String message) {
        Text text = null;
        if (message.contains("Writing objects")) {
            text = translatable("fastback.savescreen.remote-done");
        }
        if (text == null) text = literal(message);
        this.ctx.setSavingScreenText(text);
    }

    @Override
    public void notify(Text message) {
        this.ctx.setSavingScreenText(message);
    }

    @Override
    public void notifyError(Text message) {
        this.ctx.setSavingScreenText(message);
    }

    @Override
    public void internalError(String message, Throwable t) {
    }

    @Override
    public void warn(String message) {
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
