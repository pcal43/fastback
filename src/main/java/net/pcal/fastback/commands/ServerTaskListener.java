package net.pcal.fastback.commands;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.pcal.fastback.tasks.TaskListener;

import static java.util.Objects.requireNonNull;

class ServerTaskListener implements TaskListener {

    private final boolean sendToOps = true; //??
    private final ServerCommandSource scs;

    ServerTaskListener(ServerCommandSource scs) {
        this.scs = requireNonNull(scs);
    }

    @Override
    public void feedback(String message) {
        scs.sendFeedback(Text.literal(message), sendToOps);
    }

    @Override
    public void error(String message) {
        scs.sendError(Text.literal(message));
    }
}
