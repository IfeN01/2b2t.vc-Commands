package com.ifen.addon.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;

import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import com.ifen.addon.utils.APIHandler;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.Duration;

public class Playtime2b2t extends Command {
    private static final String API_ENDPOINT_PLAYER = "/playtime?playerName=";
    private static final String API_ENDPOINT_TOP_MONTH = "/playtime/top/month";
    private static final String API_ENDPOINT_TOP_ALL = "/playtime/top";
    private static final int MAX_TOP = 10;

    public Playtime2b2t() {
        super("playtime2b2t", "Shows playtime stats.", "playtime", "pt");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("player", StringArgumentType.word())
            .executes(ctx -> {
                runPlayerPlaytime(StringArgumentType.getString(ctx, "player"));
                return SINGLE_SUCCESS;
            })
        );
        builder.then(literal("top")
            .then(literal("month")
                .executes(ctx -> {
                    runTopPlaytime(API_ENDPOINT_TOP_MONTH, true, "Top playtime this month");
                    return SINGLE_SUCCESS;
                })
            )
            .then(literal("all")
                .executes(ctx -> {
                    runTopPlaytime(API_ENDPOINT_TOP_ALL, false, "All time top playtime");
                    return SINGLE_SUCCESS;
                })
            )
        );
    }

    private void runPlayerPlaytime(String playerName) {
        MeteorExecutor.execute(() -> {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player == null) return;

            String requestUrl = APIHandler.API_URL + API_ENDPOINT_PLAYER + playerName;
            String response = new APIHandler().fetch(requestUrl);
            if (response == null) return;

            JsonObject root;
            try {
                root = JsonParser.parseString(response).getAsJsonObject();
            } catch (Exception e) {
                ChatUtils.warning("Failed to parse JSON.", getName());
                return;
            }

            if (!root.has("playtimeSeconds")) {
                ChatUtils.warning("Unexpected API output.", getName());
                return;
            }

            long totalSeconds = root.get("playtimeSeconds").getAsLong();
            Duration duration = Duration.ofSeconds(totalSeconds);

            sendMessageLines(player,
                "Playtime for §b" + playerName,
                "§f" + formatDuration(duration)
            );
        });
    }

    private void runTopPlaytime(String endpoint, boolean monthlyDays, String header) {
        MeteorExecutor.execute(() -> {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player == null) return;

            String requestUrl = APIHandler.API_URL + endpoint;
            String response = new APIHandler().fetch(requestUrl);
            if (response == null) return;

            JsonObject root;
            try {
                root = JsonParser.parseString(response).getAsJsonObject();
            } catch (Exception e) {
                ChatUtils.warning("Failed to parse JSON.", getName());
                return;
            }

            if (!root.has("players")) {
                ChatUtils.warning("Unexpected API output.", getName());
                return;
            }

            JsonArray players = root.getAsJsonArray("players");
            sendMessageLines(player, header);

            int count = 0;
            for (JsonElement e : players) {
                if (count >= MAX_TOP) break;

                JsonObject p = e.getAsJsonObject();
                String name = p.get("playerName").getAsString();
                long totalSeconds = monthlyDays
                    ? (long) (p.get("playtimeDays").getAsDouble() * 24 * 3600)
                    : p.get("playtimeSeconds").getAsLong();

                Duration duration = Duration.ofSeconds(totalSeconds);
                player.sendMessage(
                    Text.of("§7" + (count + 1) + ". §b" + name + " §f" + formatDuration(duration)),
                    false
                );
                count++;
            }
        });
    }

    private void sendMessageLines(ClientPlayerEntity player, String title, String... lines) {
        player.sendMessage(Text.of(""), false);
        player.sendMessage(Text.of("§8<§7-------- §7" + title + " §7--------§8>"), false);
        for (String line : lines) {
            player.sendMessage(Text.of(line), false);
        }
        player.sendMessage(Text.of(""), false);
    }

    private String formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;
        return String.format("%d days %d hours %d minutes %d seconds", days, hours, minutes, seconds);
    }
}
