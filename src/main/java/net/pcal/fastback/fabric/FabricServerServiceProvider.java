package net.pcal.fastback.fabric;

import net.pcal.fastback.logging.Message;

import java.nio.file.Path;

public class FabricServerServiceProvider extends FabricServiceProvider {

    @Override
    public boolean isClient() {
        return false;
    }

    @Override
    public Path getClientSavesDir() {
        return null;
    }

    @Override
    public void setClientSavingScreenText(Message message) {}

    @Override
    public void sendClientChatMessage(Message message) {}

    @Override
    public void renderBackupIndicator(Message message) {}
}
