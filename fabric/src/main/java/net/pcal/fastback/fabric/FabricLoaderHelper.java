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

package net.pcal.fastback.fabric;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.api.EnvType;
import net.minecraft.commands.CommandSourceStack;
import net.pcal.fastback.common.mod.LoaderHelper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static net.pcal.fastback.common.commands.Commands.createBackupCommand;
import static net.pcal.fastback.common.logging.SystemLogger.syslog;

/**
 * Base Fabric implementation of {@link LoaderHelper}. Provides mod version and
 * loader-specific backup properties. Subclassed by client and server providers.
 *
 * @author pcal
 * @since 0.1.0
 */
class FabricLoaderHelper implements LoaderHelper {

    @Override
    public String getModVersion() {
        return FabricLoader.getInstance().getModContainer(MOD_ID)
                .orElseThrow(() -> new IllegalStateException("Could not find loader for " + MOD_ID))
                .getMetadata().getVersion().toString();
    }

    @Override
    public void addLoaderBackupProperties(Map<String, String> props) {
        try {
            final List<String> modList = new ArrayList<>();
            for (final ModContainer mc : FabricLoader.getInstance().getAllMods()) {
                modList.add(mc.getMetadata().getId() + ':' + mc.getMetadata().getVersion());
            }
            Collections.sort(modList);
            final StringBuilder modListProp = new StringBuilder();
            for (final String mod : modList) modListProp.append(mod).append(", ");
            props.put("fabric-mods", modListProp.toString());
        } catch (Exception ohwell) {
            syslog().error(ohwell);
        }
    }

    @Override
    public Path getSavesDir() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            return FabricLoader.getInstance().getGameDir().resolve("saves");
        }
        return null;
    }

    @Override
    public Collection<Path> getModsBackupPaths() {
        final Path gameDir = FabricLoader.getInstance().getGameDir();
        final List<Path> out = new ArrayList<>();
        out.add(gameDir.resolve("options.txt"));
        out.add(gameDir.resolve("mods"));
        out.add(gameDir.resolve("config"));
        out.add(gameDir.resolve("resourcepacks"));
        return out;
    }

    /**
     * Registers the /backup command. Called by both client and server initializers
     * after {@code ModImpl.initialize()} so the command system is ready.
     *
     * @param isClient true when running on an integrated (client-embedded) server
     */
    static void registerBackupCommand(boolean isClient) {
        final int requiredLevel = isClient ? 0 : 4;
        LiteralArgumentBuilder<CommandSourceStack> backupCommand = createBackupCommand(
                permName -> Permissions.require(permName, requiredLevel)
        );
        CommandRegistrationCallback.EVENT.register((dispatcher, regAccess, env) ->
                dispatcher.register(backupCommand));
        syslog().debug("registered backup command");
    }
}
