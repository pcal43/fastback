package net.pcal.gitback.commands;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.pcal.gitback.tasks.TaskListener;

import static java.util.Objects.requireNonNull;

class CommandTaskListener implements TaskListener {

    private final ServerCommandSource scs;

    public static TaskListener taskListener(CommandContext<ServerCommandSource> cc) {
        return new CommandTaskListener(cc.getSource());
    }

    private CommandTaskListener(ServerCommandSource scs) {
        this.scs = requireNonNull(scs);
    }

    @Override
    public void feedback(String message) {
        scs.sendFeedback(Text.literal(message), false);
    }

    @Override
    public void broadcast(String message) {
        scs.sendFeedback(Text.literal(message), true);
    }

    @Override
    public void error(String message) {
        scs.sendError(Text.literal(message));
    }
}
