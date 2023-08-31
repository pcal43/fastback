package net.pcal.fastback.mod.forge;

import net.minecraft.client.gui.DrawContext;
import net.pcal.fastback.logging.UserMessage;

import static net.pcal.fastback.logging.SystemLogger.syslog;

/**
 * @author pcal
 * @since 0.16.0
 */
class ForgeDedicatedServerProvider extends BaseForgeProvider {

    @Override
    public boolean isClient() {
        return false;
    }

    @Override
    public void setHudText(UserMessage userMessage) {
        //syslog().debug("[HUD] "+ userMessage.getString());
        //FIXME
    }

    @Override
    public void clearHudText() {
        syslog().debug("[HUD] clear");
        //FIXME
    }

    @Override
    public void setMessageScreenText(UserMessage userMessage) {
        //syslog().debug("[SCREEN] "+ userMessage.getString());
        //FIXME
    }

    @Override
    void renderOverlayText(DrawContext drawContext) {

    }
}
