package net.pcal.fastback.fabric;

import net.minecraft.text.Text;

import java.nio.file.Path;

interface FabricClientProvider {

    void consumeSaveScreenText(Text text);

    Path getClientRestoreDir();

    void sendClientChatMessage(Text text);
}
