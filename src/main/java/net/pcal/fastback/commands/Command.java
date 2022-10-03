package net.pcal.fastback.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.ModContext;

public interface Command {

    void register(final LiteralArgumentBuilder<ServerCommandSource> argb, final ModContext ctx);

}
