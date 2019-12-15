package me.lucko.spark.fabric;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.fabric.plugin.FabricServerSparkPlugin;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class SparkCommand implements Command<ServerCommandSource>, SuggestionProvider<ServerCommandSource> {

    private FabricServerSparkPlugin plugin;
    private SparkPlatform platform;

    public void init(FabricServerSparkPlugin plugin, SparkPlatform platform) {
        this.plugin = plugin;
        this.platform = platform;
    }

    @Override
    public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String[] args = processArgs(context);
        if (args == null) {
            return 0;
        }

        if (plugin == null || platform == null) {
            return 0; // No platform available
        }

        if (plugin.getServer() != context.getSource().getMinecraftServer()) {
            return 0; // Wrong plugin
        }

        this.platform.executeCommand(new FabricCommandSender(context.getSource().getPlayer(), this.plugin), args);
        return Command.SINGLE_SUCCESS;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder)
            throws CommandSyntaxException {
        String[] args = processArgs(context);
        if (args == null) {
            return Suggestions.empty();
        }

        if (plugin == null || platform == null) {
            return Suggestions.empty(); // No platform available
        }

        if (plugin.getServer() != context.getSource().getMinecraftServer()) {
            return Suggestions.empty(); // Wrong plugin
        }

        ServerPlayerEntity player = context.getSource().getPlayer();

        return CompletableFuture.supplyAsync(() -> {
            for (String suggestion : this.platform.tabCompleteCommand(new FabricCommandSender(player, this.plugin), args)) {
                builder.suggest(suggestion);
            }
            return builder.build();
        });
    }

    private static String /*Nullable*/[] processArgs(CommandContext<ServerCommandSource> context) {
        String[] split = context.getInput().split(" ");
        if (split.length == 0 || !split[0].equals("/spark")) {
            return null;
        }

        return Arrays.copyOfRange(split, 1, split.length);
    }
}
