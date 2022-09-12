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

    public static void register(LiteralArgumentBuilder<ServerCommandSource> argb, ModContext ctx) {
        final StatusCommand rc = new StatusCommand(ctx);
        argb.then(CommandManager.literal("status").executes(rc::execute));
    }

    private final ModContext ctx;

    private StatusCommand(ModContext context) {
        this.ctx = requireNonNull(context);
    }

    private int execute(CommandContext<ServerCommandSource> cc) {
        return executeStandard(this.ctx, cc, (gitc, wc, log) -> {
            log.notify("Local backup:  " + (wc.isBackupEnabled() ? "enabled" : "disabled"));
            log.notify("Remote backup: " + (wc.isRemoteBackupEnabled() ? "enabled" : "disabled"));
            if (wc.isRemoteBackupEnabled()) {
                String url = wc.getRemotePushUrl();
                if (url == null) {
                    log.notifyError("Remote URL: Not Configured.  Run /backup remote [url]");
                } else {
                    log.notify("Remote URL: " + url);
                }
            }
            log.notify("On shutdown:  " + (wc.isShutdownBackupEnabled() ? "enabled" : "disabled"));
            return SUCCESS;
        });
    }
}
