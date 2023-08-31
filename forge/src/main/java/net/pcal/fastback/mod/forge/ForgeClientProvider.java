package net.pcal.fastback.mod.forge;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.pcal.fastback.logging.UserMessage;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.logging.SystemLogger.syslog;
import static net.pcal.fastback.mod.MinecraftProvider.messageToText;

/**
 * @author pcal
 * @since 0.16.0
 */
final class ForgeClientProvider extends BaseForgeProvider {

    // ======================================================================
    // Constants

    private static final long TEXT_TIMEOUT = 10 * 1000;
    private final MinecraftClient client;

    // ======================================================================
    // Fields

    //private MinecraftClient client = null;
    private Text hudText;
    private long hudTextTime;

    ForgeClientProvider() {
        this.client = requireNonNull(MinecraftClient.getInstance(), "MinecraftClient.getInstance() returned null");
    }

    @Override
    public boolean isClient() {
        return true;
    }

    @Override
    public void setHudText(UserMessage userMessage) {
        if (userMessage == null) {
            clearHudText();
        } else {
            this.hudText = messageToText(userMessage); // so the hud renderer can find it
            this.hudTextTime = System.currentTimeMillis();
        }
    }

    @Override
    public void clearHudText() {
        this.hudText = null;
        // TODO someday it might be nice to bring back the fading text effect.  But getting to it properly
        // clean up 100% of the time is more than I want to deal with right now.
    }

    @Override
    public void setMessageScreenText(UserMessage userMessage) {
        final Text text = messageToText(userMessage);
        this.hudText = text;
        final Screen screen = client.currentScreen;
        if (screen != null) screen.title = text;
    }

    @Override
    void renderOverlayText(final DrawContext drawContext) {
        if (this.hudText == null) return;
        // if (!this.client.options.getShowAutosaveIndicator().getValue()) return; FIXME
        if (System.currentTimeMillis() - this.hudTextTime > TEXT_TIMEOUT) {
            // Don't leave it sitting up there forever if we fail to call clearHudText()
            this.hudText = null;
            syslog().debug("hud text timed out.  somebody forgot to clean up");
            return;
        }
        if (client != null) {
            drawContext.drawTextWithShadow(this.client.textRenderer, this.hudText, 2, 2, 1);
        }
    }
}