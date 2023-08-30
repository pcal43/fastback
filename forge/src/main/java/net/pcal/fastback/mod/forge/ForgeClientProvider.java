package net.pcal.fastback.mod.forge;

import net.minecraft.text.Text;

import static net.pcal.fastback.logging.SystemLogger.syslog;

/**
 * @author pcal
 * @since 0.16.0
 */
class ForgeClientProvider extends BaseForgeProvider {

    @Override
    public boolean isClient() {
        return true;
    }

    @Override
    public void setHudText(Text text) {
        syslog().debug("[HUD] "+text.getString());
        //FIXME
    }

    @Override
    public void clearHudText() {
        syslog().debug("[HUD] clear");
        //FIXME
    }

    @Override
    public void setMessageScreenText(Text text) {
        syslog().debug("[SCREEN] "+text.getString());
        //FIXME
    }

}
