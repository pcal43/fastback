package net.pcal.gitback.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.gitback.ModContext;

import static java.util.Objects.requireNonNull;
import static net.pcal.gitback.commands.Commands.SUCCESS;
import static net.pcal.gitback.commands.Commands.executeStandard;

public class StatusCommand {

    public static void register(LiteralArgumentBuilder<ServerCommandSource> argb, ModContext ctx) {
        final StatusCommand rc = new StatusCommand(ctx);
        argb.then(CommandManager.literal("status").executes(rc::execute));
    }

    private final ModContext ctx;

    private StatusCommand(ModContext context) {
        this.ctx = requireNonNull(context);
    }

    private int execute(CommandContext<ServerCommandSource> cc) {
        return executeStandard(this.ctx, cc, (gitc, wc, tali) -> {
            tali.feedback("Local backup:  " + (wc.isBackupEnabled() ? "enabled" : "disabled"));
            tali.feedback("Remote backup: " + (wc.isRemoteBackupEnabled() ? "enabled" : "disabled"));
            if (wc.isRemoteBackupEnabled()) {
                String url = wc.getRemotePushUrl();
                if (url == null) {
                    tali.error("Remote URL: Not Configured.  Run /backup remote [url]");
                } else {
                    tali.feedback("Remote URL: " + url);
                }
            }
            tali.feedback("On shutdown:  " + (wc.isShutdownBackupEnabled() ? "enabled" : "disabled"));
            return SUCCESS;
        });
    }
}
