package com.ifen.addon.commands;

import com.ifen.addon.utils.APIHandler;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.player.ChatUtils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Connections2b2t extends Command {
    private static final String API_ENDPOINT = "/connections";
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 25;

    public Connections2b2t() {
        super("connections2b2t", "Show recent JOIN/LEAVE connections of a 2b2t player.", "connections", "conns");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("player", StringArgumentType.word())
            // .conns player
            .executes(ctx -> {
                run(
                    StringArgumentType.getString(ctx, "player"),
                    DEFAULT_PAGE,
                    DEFAULT_LIMIT
                );
                return SINGLE_SUCCESS;
            })
            // .conns player page
            .then(argument("page", IntegerArgumentType.integer(1))
                .executes(ctx -> {
                    run(
                        StringArgumentType.getString(ctx, "player"),
                        ctx.getArgument("page", Integer.class),
                        DEFAULT_LIMIT
                    );
                    return SINGLE_SUCCESS;
                })
                // .conns player page limit
                .then(argument("limit", IntegerArgumentType.integer(1, MAX_LIMIT))
                    .executes(ctx -> {
                        run(
                            StringArgumentType.getString(ctx, "player"),
                            ctx.getArgument("page", Integer.class),
                            ctx.getArgument("limit", Integer.class)
                        );
                        return SINGLE_SUCCESS;
                    })
                )
            )
        );
    }

    private void run(String playerName, int page, int limit) {
        final String name = playerName.trim();

        MeteorExecutor.execute(() -> {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player == null) return;

            String requestUrl =
                APIHandler.API_URL + API_ENDPOINT +
                    "?playerName=" + name +
                    "&page=" + page +
                    "&pageSize=" + limit;

            String response = new APIHandler().fetch(requestUrl);
            if (response == null) return;

            JsonObject root;
            try {
                root = JsonParser.parseString(response).getAsJsonObject();
            } catch (Exception e) {
                ChatUtils.warning("API said: Nuh Uh", getName());
                return;
            }

            if (!root.has("connections")) {
                ChatUtils.warning("API said: Nuh Uh", getName());
                return;
            }

            JsonArray connections = root.getAsJsonArray("connections");
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd yyyy, HH:mm", Locale.US);

            player.sendMessage(Text.of(
                "§8<§7-------- §7Connections for §f" + name +
                    " §7(Page " + page + ") §7--------§8>"
            ), false);

            for (JsonElement e : connections) {
                JsonObject conn = e.getAsJsonObject();
                String type = conn.get("connection").getAsString();
                String timeStr = conn.get("time").getAsString();

                String typeColor = switch (type) {
                    case "JOIN" -> "§a";
                    case "LEAVE" -> "§c";
                    default -> "§7";
                };

                ZonedDateTime zdt = Instant.parse(timeStr).atZone(ZoneId.systemDefault());
                player.sendMessage(
                    Text.of(" §8• " + typeColor + type + " §7at §f" + zdt.format(fmt)),
                    false
                );
            }

            player.sendMessage(Text.of(""), false);
        });
    }
}
