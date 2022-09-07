package net.pcal.fastback.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.GitUtils;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.tasks.TaskListener;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.URIish;

import java.io.IOException;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

public class StatusCommand {

    public static void register(LiteralArgumentBuilder<ServerCommandSource> fastbackCmd, ModContext ctx) {
        final StatusCommand rc = new StatusCommand(ctx);
        fastbackCmd.then(CommandManager.literal("status").executes(rc::execute));
    }

    private final ModContext ctx;

    private StatusCommand(ModContext context) {
        this.ctx = requireNonNull(context);
    }

    private int execute(CommandContext<ServerCommandSource> cc) {
        final MinecraftServer server = cc.getSource().getServer();
        final Path worldSaveDir = this.ctx.getWorldSaveDirectory(server);
        final TaskListener tl = new ServerTaskListener(cc.getSource());
        tl.feedback("Local Backup:  enabled"); //FIXME
        tl.feedback("Remote Backup: disabled");
        final URIish remoteUri;
        try(Git git = Git.open(worldSaveDir.toFile())) {
            remoteUri = GitUtils.getRemoteUri(git, "origin", ctx.getLogger());
            tl.feedback("Remote URI:    "+remoteUri);
        } catch (IOException | GitAPIException e) {
            tl.error("An unexpected error occurred.  See log for details");
            ctx.getLogger().error("could not look up remote uri", e);
        }
        return 1;
    }
}
