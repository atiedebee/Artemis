/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.wynntils.core.consumers.commands.Command;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class TerritoryCommand extends Command {
    private final CompassCommand delagate = new CompassCommand();

    @Override
    public String getCommandName() {
        return "territory";
    }

    @Override
    public String getDescription() {
        return "Alias of /compass territory";
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getCommandBuilder(
            LiteralArgumentBuilder<CommandSourceStack> base) {
        return Commands.literal("territory")
                .then(Commands.argument("territory", StringArgumentType.greedyString())
                        .suggests(CompassCommand.TERRITORY_SUGGESTION_PROVIDER)
                        .executes(delagate::territory))
                .executes(delagate::syntaxError);
    }
}
