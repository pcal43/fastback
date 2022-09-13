package net.pcal.fastback.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.WorldConfig;

import static java.util.Objects.requireNonNull;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.commands.Commands.*;

public class RemoteCommand {

    public static void register(final LiteralArgumentBuilder<ServerCommandSource> argb, final ModContext ctx) {
        final RemoteCommand c = new RemoteCommand(ctx);
        argb.then(
                literal("remote").executes(c::showRemote).then(
                        literal("enable").executes(c::enable)).then(
                        literal("disable").executes(c::disable)).then(
                        argument("remote-url", StringArgumentType.greedyString()).
                                executes(c::setRemoteUrl))
        );
    }

    private final ModContext ctx;

    private RemoteCommand(ModContext context) {
        this.ctx = requireNonNull(context);
    }

    private int showRemote(final CommandContext<ServerCommandSource> cc) {
        return executeStandard(this.ctx, cc, (gitc, wc, log) -> {
            final String remoteUrl = wc.getRemotePushUrl();
            final boolean enabled = wc.isRemoteBackupEnabled();
            if (enabled && remoteUrl != null) {
                log.notify("Remote backups are enabled to:");
                log.notify(remoteUrl);
                return SUCCESS;
            } else {
                log.notifyError("Remote backups are disabled.");
                if (remoteUrl != null) {
                    log.notify("Run '/backup remote enable' to enable remote backups to:");
                    log.notify(remoteUrl);
                } else {
                    log.notify("Run '/backup remote <remote-url>' to enable remote backups.");
                }
                return FAILURE;
            }
        });
    }

    private int enable(final CommandContext<ServerCommandSource> cc) {
        return executeStandard(this.ctx, cc, (gitc, wc, log) -> {
            final String currentUrl = wc.getRemotePushUrl();
            final boolean currentEnabled = wc.isRemoteBackupEnabled();
            if (currentUrl == null) {
                log.notifyError("No remote URL is set.");
                log.notify("Run '/backup remote <remote-url>'");
                return FAILURE;
            } else if (currentEnabled) {
                log.notifyError("Remote backups are already enabled.");
                return FAILURE;
            } else {
                log.notify("Remote backups enabled to:");
                log.notify(currentUrl);
                WorldConfig.setRemoteBackupEnabled(gitc, true);
                gitc.save();
                return SUCCESS;
            }
        });
    }

    private int disable(final CommandContext<ServerCommandSource> cc) {
        return executeStandard(this.ctx, cc, (gitc, wc, log) -> {
            final boolean currentEnabled = wc.isRemoteBackupEnabled();
            if (!currentEnabled) {
                log.notifyError("Remote backups are already disabled.");
                return FAILURE;
            } else {
                WorldConfig.setRemoteBackupEnabled(gitc, false);
                gitc.save();
                log.notifyError("Remote backups disabled.");
                return SUCCESS;
            }
        });
    }

    private int setRemoteUrl(final CommandContext<ServerCommandSource> cc) {
        return executeStandard(this.ctx, cc, (gitc, wc, log) -> {
            final String newUrl = cc.getArgument("remote-url", String.class);
            final String currentUrl = wc.getRemotePushUrl();
            final boolean currentEnable = wc.isRemoteBackupEnabled();
            if (currentUrl != null && currentUrl.equals(newUrl)) {
                if (currentEnable) {
                    log.notify("Remote backups are already enabled to:");
                    log.notify(newUrl);
                    return SUCCESS;
                } else {
                    WorldConfig.setRemoteBackupEnabled(gitc, true);
                    gitc.save();
                    log.notify("Enabled remote backups to:");
                    log.notify(newUrl);
                }
            } else {
                WorldConfig.setRemoteUrl(gitc, newUrl);
                if (currentEnable) {
                    log.notify("Remote backup URL changed to " + newUrl);
                } else {
                    WorldConfig.setRemoteBackupEnabled(gitc, true);
                    log.notify("Enabled remote backups to:" + newUrl);
                    log.notify(newUrl);
                }
                gitc.save();
            }
            return SUCCESS;
        });
    }

}
