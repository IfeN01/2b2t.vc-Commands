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

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Kills2b2t extends Command {
    private static final String API_ENDPOINT_PLAYER = "/kills";
    private static final String API_ENDPOINT_TOP_MONTH = "/kills/top/month";

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 20;

    public Kills2b2t() {
        super("kills2b2t", "Fetches kill stats from 2b2t.", "kills");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("player")
            .then(argument("player", StringArgumentType.word())
                // .kills player name
                .executes(ctx -> {
                    runPlayerKills(
                        StringArgumentType.getString(ctx, "player"),
                        DEFAULT_PAGE,
                        DEFAULT_LIMIT
                    );
                    return SINGLE_SUCCESS;
                })
                // .kills player name page
                .then(argument("page", IntegerArgumentType.integer(1))
                    .executes(ctx -> {
                        runPlayerKills(
                            StringArgumentType.getString(ctx, "player"),
                            ctx.getArgument("page", Integer.class),
                            DEFAULT_LIMIT
                        );
                        return SINGLE_SUCCESS;
                    })
                    // .kills player name page limit
                    .then(argument("limit", IntegerArgumentType.integer(1, MAX_LIMIT))
                        .executes(ctx -> {
                            runPlayerKills(
                                StringArgumentType.getString(ctx, "player"),
                                ctx.getArgument("page", Integer.class),
                                ctx.getArgument("limit", Integer.class)
                            );
                            return SINGLE_SUCCESS;
                        })
                    )
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

    private void runPlayerKills(String playerName, int page, int limit) {
        MeteorExecutor.execute(() -> {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player == null) return;

            String requestUrl =
                APIHandler.API_URL + API_ENDPOINT_PLAYER +
                    "?playerName=" + playerName +
                    "&page=" + page +
                    "&pageSize=" + limit;

            String response = new APIHandler().fetch(requestUrl);
            if (response == null) return;

            JsonObject root;
            try {
                root = JsonParser.parseString(response).getAsJsonObject();
            } catch (Exception e) {
                ChatUtils.warning("Failed to parse JSON.", getName());
                return;
            }

            if (!root.has("kills")) {
                ChatUtils.warning("Unexpected API output.", getName());
                return;
            }

            JsonArray kills = root.getAsJsonArray("kills");
            sendMessageLines(
                player,
                "Kills for §b" + playerName + " §7(Page " + page + ")",
                getFormattedKills(kills)
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
                int killsCount = p.get("count").getAsInt();
                lines[i] = "§7" + (i + 1) + ". §b" + name + " §7kills: §f" + killsCount;
            }

            sendMessageLines(player, "Top kills this month", lines);
        });
    }

    private String[] getFormattedKills(JsonArray kills) {
        int count = kills.size();
        String[] lines = new String[count];
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd yyyy, HH:mm", Locale.US);

        for (int i = 0; i < count; i++) {
            JsonObject k = kills.get(i).getAsJsonObject();
            String killMsg = k.get("deathMessage").getAsString();
            ZonedDateTime zdt = Instant.parse(k.get("time").getAsString()).atZone(ZoneId.systemDefault());

            lines[i] = "§7[" + zdt.format(fmt) + "] §f" + killMsg;
        }

        return lines;
    }

    private void sendMessageLines(ClientPlayerEntity player, String title, String... lines) {
        player.sendMessage(Text.of(""), false);
        player.sendMessage(Text.of("§8<§7-------- §7" + title + " §7--------§8>"), false);
        for (String line : lines) {
            player.sendMessage(Text.of(line), false);
        }
        player.sendMessage(Text.of(""), false);
    }
}
