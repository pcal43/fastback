package net.pcal.fastback.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.ModContext;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.executeStandard;

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
