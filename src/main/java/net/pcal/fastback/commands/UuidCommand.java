package net.pcal.fastback.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.Loggr;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.WorldConfig;
import net.pcal.fastback.tasks.TaskListener;

import java.io.IOException;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.commands.CommandTaskListener.taskListener;
import static net.pcal.fastback.commands.Commands.FAILURE;
import static net.pcal.fastback.commands.Commands.SUCCESS;

public class UuidCommand {

    public static void register(LiteralArgumentBuilder<ServerCommandSource> fastbackCmd, ModContext ctx) {
        final UuidCommand rc = new UuidCommand(ctx);
        fastbackCmd.then(CommandManager.literal("uuid").executes(rc::execute));
    }

    private final ModContext ctx;
    private final Loggr logger;

    private UuidCommand(ModContext context) {
        this.logger = requireNonNull(context.getLogger());
        this.ctx = requireNonNull(context);
    }

    private int execute(CommandContext<ServerCommandSource> cc) {
        final MinecraftServer server = cc.getSource().getServer();
        final Path worldSaveDir = this.ctx.getWorldSaveDirectory(server);
        final TaskListener tl = taskListener(cc);
        try {
            tl.feedback(WorldConfig.getWorldUuid(worldSaveDir));
        } catch (IOException e) {
            logger.error(e);
            tl.internalError();
            return FAILURE;
        }
        return SUCCESS;
    }
}
