package com.ifen.addon.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import com.ifen.addon.utils.APIHandler;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Deaths2b2t extends Command {
    private static final String API_ENDPOINT_PLAYER = "/deaths?playerName=";
    private static final String API_ENDPOINT_TOP_MONTH = "/deaths/top/month";
    private static final int MAX_LIMIT = 20;

    public Deaths2b2t() {
        super("deaths2b2t", "Fetches death stats from 2b2t.", "deaths");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("player")
            .then(argument("player", StringArgumentType.word())
                .executes(ctx -> {
                    runPlayerDeaths(StringArgumentType.getString(ctx, "player"), 5);
                    return SINGLE_SUCCESS;
                })
                .then(argument("limit", IntegerArgumentType.integer(1, MAX_LIMIT))
                    .executes(ctx -> {
                        int limit = ctx.getArgument("limit", Integer.class);
                        runPlayerDeaths(StringArgumentType.getString(ctx, "player"), limit);
                        return SINGLE_SUCCESS;
                    })
                )
            )
        );

        builder.then(literal("top")
            .then(literal("month")
                .executes(ctx -> {
                    runTopMonth();
                    return SINGLE_SUCCESS;
                })
            )
        );
    }

    private void runPlayerDeaths(String playerName, int limit) {
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

            if (!root.has("deaths")) {
                ChatUtils.warning("Unexpected API output.", getName());
                return;
            }

            JsonArray deaths = root.getAsJsonArray("deaths");
            sendMessageLines(player,
                "Deaths for §b" + playerName,
                getFormattedDeaths(deaths, limit)
            );
        });
    }

    private void runTopMonth() {
        MeteorExecutor.execute(() -> {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player == null) return;

            String requestUrl = APIHandler.API_URL + API_ENDPOINT_TOP_MONTH;
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
            String[] lines = new String[Math.min(10, players.size())];

            for (int i = 0; i < lines.length; i++) {
                JsonObject p = players.get(i).getAsJsonObject();
                String name = p.get("playerName").getAsString();
                int deathsCount = p.get("count").getAsInt();
                lines[i] = "§7" + (i + 1) + ". §b" + name + " §7deaths: §f" + deathsCount;
            }

            sendMessageLines(player, "Top deaths this month", lines);
        });
    }

    private String[] getFormattedDeaths(JsonArray deaths, int limit) {
        int count = Math.min(deaths.size(), limit);
        String[] lines = new String[count];
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd yyyy, HH:mm", Locale.US);

        for (int i = 0; i < count; i++) {
            JsonObject d = deaths.get(i).getAsJsonObject();
            String deathMsg = d.get("deathMessage").getAsString();
            ZonedDateTime zdt = Instant.parse(d.get("time").getAsString()).atZone(ZoneId.systemDefault());

            lines[i] = "§7[" + zdt.format(fmt) + "] §f" + deathMsg;
        }

        return lines;
    }

    private void sendMessageLines(ClientPlayerEntity player, String title, String... lines) {
        player.sendMessage(Text.of(""), false);
        player.sendMessage(Text.of("§8<§7-------- " + title + " §7--------§8>"), false);
        for (String line : lines) {
            player.sendMessage(Text.of(line), false);
        }
        player.sendMessage(Text.of(""), false);
    }
}
