/*
 * FastBack - Fast, incremental Minecraft backups powered by Git.
 * Copyright (C) 2022 pcal.net
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package net.pcal.fastback.common.mod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.pcal.fastback.common.commands.Commands;
import net.pcal.fastback.common.commands.SchedulableAction;
import net.pcal.fastback.common.config.GitConfig;
import net.pcal.fastback.common.logging.Log4jLogger;
import net.pcal.fastback.common.logging.SystemLogger;
import net.pcal.fastback.common.logging.UserLogger;
import net.pcal.fastback.common.logging.UserMessage;
import net.pcal.fastback.common.mixins.ServerAccessors;
import net.pcal.fastback.common.mixins.SessionAccessors;
import net.pcal.fastback.common.repo.Repo;
import net.pcal.fastback.common.repo.RepoFactory;
import org.apache.logging.log4j.LogManager;
import org.eclipse.jgit.transport.SshSessionFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

import static java.nio.file.Files.createTempDirectory;
import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.common.config.FastbackConfigKey.IS_BACKUP_ENABLED;
import static net.pcal.fastback.common.config.FastbackConfigKey.SHUTDOWN_ACTION;
import static net.pcal.fastback.common.logging.SystemLogger.syslog;
import static net.pcal.fastback.common.logging.UserMessage.UserMessageStyle.ERROR;
import static net.pcal.fastback.common.logging.UserMessage.localized;
import static net.pcal.fastback.common.mod.UserMessageUtil.messageToText;
import static net.pcal.fastback.common.utils.EnvironmentUtils.getGitLfsVersion;
import static net.pcal.fastback.common.utils.EnvironmentUtils.getGitVersion;
import static net.pcal.fastback.common.utils.Executor.executor;

class ModImpl implements Mod {

    // ======================================================================
    // Fields

    private final LoaderHelper loaderHelper;
    private final ClientHelper clientHelper; // null on a dedicated server
    private final Runnable autoSaveListener;
    private MinecraftServer minecraftServer = null; // currently open world
    private boolean isWorldSaveEnabled = true;
    private Path tempRestoresDirectory = null;

    // ======================================================================
    // Factory — called by loader initializers

    /**
     * Creates, registers, and initializes a ModImpl.
     *
     * @param loaderHelper loader-specific services (always present)
     * @param clientHelper client-specific services, or null on a dedicated server
     */
    static Mod initialize(final LoaderHelper loaderHelper, final ClientHelper clientHelper) {
        SystemLogger.Singleton.register(new Log4jLogger(LogManager.getLogger("fastback")));
        final ModImpl mod = new ModImpl(loaderHelper, clientHelper);
        SingletonHolder.register(mod);
        mod.onInitialize();
        return mod;
    }

    private ModImpl(LoaderHelper loaderHelper, ClientHelper clientHelper) {
        this.loaderHelper = requireNonNull(loaderHelper);
        this.clientHelper = clientHelper; // nullable — null means dedicated server
        this.autoSaveListener = new AutosaveListener();
    }

    // ======================================================================
    // Mod implementation

    @Override
    public void onWorldStart(final MinecraftServer minecraftServer) {
        this.minecraftServer = requireNonNull(minecraftServer);
        executor().start();
        syslog().debug("onWorldStart complete");
    }

    @Override
    public void onWorldStop() {
        try (final UserLogger ulog = UserLogger.forShutdown()) {
            final Path worldSaveDir = this.getWorldDirectory();
            if (executor().getActiveCount() > 0) {
                this.setMessageScreenText(localized("fastback.chat.thread-waiting"));
            }
            executor().stop();
            this.clearHudText();
            final RepoFactory rf = RepoFactory.rf();
            if (rf.isGitRepo(worldSaveDir)) {
                try (final Repo repo = rf.load(worldSaveDir)) {
                    final GitConfig config = repo.getConfig();
                    if (config.getBoolean(IS_BACKUP_ENABLED)) {
                        final SchedulableAction action = SchedulableAction.forConfigValue(config, SHUTDOWN_ACTION);
                        if (action != null) {
                            this.setMessageScreenText(localized("fastback.message.backing-up"));
                            action.getTask(repo, ulog).call();
                            this.setMessageScreenText(localized("fastback.chat.backup-complete"));
                        }
                    }
                } catch (Exception e) {
                    syslog().error("Shutdown action failed.", e);
                }
            }
            syslog().debug("onWorldStop complete");
        }
        this.minecraftServer = null;
    }

    @Override
    public Path getDefaultRestoresDir() throws IOException {
        Path savesDir = this.loaderHelper.getSavesDir();
        if (savesDir != null) return savesDir;
        if (tempRestoresDirectory == null) {
            tempRestoresDirectory = createTempDirectory("fastback-restore");
        }
        return tempRestoresDirectory;
    }

    @Override
    public String getModVersion() {
        return this.loaderHelper.getModVersion();
    }

    @Override
    public void setWorldSaveEnabled(boolean enabled) {
        this.isWorldSaveEnabled = enabled;
    }

    @Override
    public void saveWorld() {
        if (this.minecraftServer == null) throw new IllegalStateException();
        this.minecraftServer.saveEverything(false, true, true);
    }

    @Override
    public void sendChat(UserMessage message, CommandSourceStack scs) {
        if (message.style() == ERROR) {
            scs.sendFailure(messageToText(message));
        } else {
            scs.sendSuccess(() -> messageToText(message), false);
        }
    }

    @Override
    public void sendBroadcast(UserMessage userMessage) {
        if (this.minecraftServer != null && this.minecraftServer.isDedicatedServer()) {
            this.minecraftServer.getPlayerList().broadcastSystemMessage(messageToText(userMessage), false);
        }
    }

    @Override
    public void setHudText(UserMessage message) {
        if (this.clientHelper == null) return;
        if (message == null) {
            syslog().debug("null unexpectedly passed to setHudText, ignoring");
            this.clearHudText();
        } else {
            this.clientHelper.setHudText(message);
        }
    }

    @Override
    public void clearHudText() {
        if (this.clientHelper != null) this.clientHelper.clearHudText();
    }

    @Override
    public void setMessageScreenText(UserMessage message) {
        if (this.clientHelper != null)
            this.clientHelper.setMessageScreenText(message);
    }

    @Override
    public Path getWorldDirectory() {
        if (this.minecraftServer == null) throw new IllegalStateException();
        final LevelStorageSource.LevelStorageAccess session =
                ((ServerAccessors) this.minecraftServer).getStorageSource();
        return ((SessionAccessors) session).getLevelDirectory().path();
    }

    @Override
    public String getWorldName() {
        if (this.minecraftServer == null) throw new IllegalStateException();
        return this.minecraftServer.getWorldData().getLevelName();
    }

    @Override
    public void addBackupProperties(Map<String, String> props) {
        props.put("fastback-version", this.getModVersion());
        if (this.minecraftServer != null) {
            props.put("minecraft-version", minecraftServer.getServerVersion());
            props.put("minecraft-game-mode", String.valueOf(minecraftServer.getWorldData().getGameType()));
            props.put("minecraft-level-name", minecraftServer.getWorldData().getLevelName());
        }
        this.loaderHelper.addLoaderBackupProperties(props);
    }

    @Override
    public Collection<Path> getModsBackupPaths() {
        return this.loaderHelper.getModsBackupPaths();
    }

    // ======================================================================
    // Mod implementation (continued)

    @Override
    public boolean isWorldSaveEnabled() {
        return this.isWorldSaveEnabled;
    }

    @Override
    public void autoSaveCompleted() {
        if (this.autoSaveListener != null) {
            this.autoSaveListener.run();
        } else {
            syslog().warn("Autosave just happened but, unexpectedly, no one is listening.");
        }
    }

    @Override
    public void renderMessageScreen(GuiGraphics drawContext) {
        if (this.clientHelper != null) {
            this.clientHelper.renderMessageScreen(drawContext);
        } else {
            syslog().warn("renderMessageScreen called when clientHelper not set.");
        }
    }

    @Override
    public void renderHud(GuiGraphics drawContext) {
        if (this.clientHelper != null) {
            this.clientHelper.renderHud(drawContext);
        } else {
            syslog().warn("renderHud called when clientHelper not set.");
        }
    }

    // ======================================================================
    // Private methods

    private void onInitialize() {
        final String gitVersion = getGitVersion();
        if (gitVersion == null) {
            syslog().warn("git is not installed.");
        } else {
            syslog().info("git is installed: " + gitVersion);
        }
        final String gitLfsVersion = getGitLfsVersion();
        if (gitLfsVersion == null) {
            syslog().warn("git-lfs is not installed.");
        } else {
            syslog().info("git-lfs is installed: " + gitLfsVersion);
        }
        if (SshSessionFactory.getInstance() == null) {
            syslog().warn("An ssh provider was not initialized for jgit.  Operations on a remote repo over ssh will fail.");
        } else {
            syslog().info("SshSessionFactory: " + SshSessionFactory.getInstance());
        }
        this.loaderHelper.registerBackupCommand(clientHelper != null, Commands::createBackupCommand);
        syslog().debug("onInitialize complete");
    }
}