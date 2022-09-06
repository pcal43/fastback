package net.pcal.fastback.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.pcal.fastback.ModContext;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;

import java.io.IOException;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

public class ListCommand {

    public static void register(final LiteralArgumentBuilder<ServerCommandSource> fastbackCmd, final ModContext ctx) {
        final ListCommand rc = new ListCommand(ctx);
        fastbackCmd.then(CommandManager.literal("list").executes(rc::execute));
    }

    private final ModContext context;

    private ListCommand(ModContext context) {
        this.context = requireNonNull(context);
    }

    private int execute(CommandContext<ServerCommandSource> cc) {
        final ModContext.WorldContext world = this.context.getWorldContext(cc.getSource().getServer());
        final Path worldSaveDir = world.getWorldSaveDirectory();


        try (final Git git = Git.open(worldSaveDir.toFile())) {
            for (Ref branch : git.branchList().call()) {
                cc.getSource().sendFeedback(Text.literal(branch.getName()), true);
            }
        } catch (GitAPIException | IOException e) {
            throw new RuntimeException(e);
        }
        return 1;
    }
}
