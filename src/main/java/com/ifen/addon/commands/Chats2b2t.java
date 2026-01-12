package com.ifen.addon.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import com.ifen.addon.utils.APIHandler;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Chats2b2t extends Command {
    private static final String API_ENDPOINT = "/chats";
    private static final int MAX_LIMIT = 100;

    public Chats2b2t() {
        super("chats2b2t", "Search 2b2t chat messages by player or keyword.", "chats");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {

        builder.then(literal("player")
            .then(argument("playerName", StringArgumentType.word())
                .executes(ctx -> {
                    run(ctx, StringArgumentType.getString(ctx, "playerName"), null, 1, 10);
                    return SINGLE_SUCCESS;
                })
                .then(argument("page", IntegerArgumentType.integer(1))
                    .executes(ctx -> {
                        run(ctx, StringArgumentType.getString(ctx, "playerName"), null,
                            ctx.getArgument("page", Integer.class), 10);
                        return SINGLE_SUCCESS;
                    })
                    .then(argument("limit", IntegerArgumentType.integer(1, MAX_LIMIT))
                        .executes(ctx -> {
                            run(ctx, StringArgumentType.getString(ctx, "playerName"), null,
                                ctx.getArgument("page", Integer.class), ctx.getArgument("limit", Integer.class));
                            return SINGLE_SUCCESS;
                        })
                    )
                )
                .then(literal("keyword")
                    .then(argument("keyword", StringArgumentType.word())
                        .executes(ctx -> {
                            run(ctx, StringArgumentType.getString(ctx, "playerName"),
                                StringArgumentType.getString(ctx, "keyword"), 1, 10);
                            return SINGLE_SUCCESS;
                        })
                        .then(argument("page", IntegerArgumentType.integer(1))
                            .executes(ctx -> {
                                run(ctx, StringArgumentType.getString(ctx, "playerName"),
                                    StringArgumentType.getString(ctx, "keyword"),
                                    ctx.getArgument("page", Integer.class), 10);
                                return SINGLE_SUCCESS;
                            })
                            .then(argument("limit", IntegerArgumentType.integer(1, MAX_LIMIT))
                                .executes(ctx -> {
                                    run(ctx, StringArgumentType.getString(ctx, "playerName"),
                                        StringArgumentType.getString(ctx, "keyword"),
                                        ctx.getArgument("page", Integer.class),
                                        ctx.getArgument("limit", Integer.class));
                                    return SINGLE_SUCCESS;
                                })
                            )
                        )
                    )
                )
            )
        );

        // KEYWORD-only subcommand
        builder.then(literal("keyword")
            .then(argument("keyword", StringArgumentType.word())
                .executes(ctx -> {
                    run(ctx, null, StringArgumentType.getString(ctx, "keyword"), 1, 10);
                    return SINGLE_SUCCESS;
                })
                .then(argument("page", IntegerArgumentType.integer(1))
                    .executes(ctx -> {
                        run(ctx, null, StringArgumentType.getString(ctx, "keyword"),
                            ctx.getArgument("page", Integer.class), 10);
                        return SINGLE_SUCCESS;
                    })
                    .then(argument("limit", IntegerArgumentType.integer(1, MAX_LIMIT))
                        .executes(ctx -> {
                            run(ctx, null, StringArgumentType.getString(ctx, "keyword"),
                                ctx.getArgument("page", Integer.class),
                                ctx.getArgument("limit", Integer.class));
                            return SINGLE_SUCCESS;
                        })
                    )
                )
            )
        );
    }

    private void run(CommandContext<CommandSource> ctx, String playerName, String keyword, int page, int limit) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;

        final String finalPlayerName = playerName;
        final String finalKeyword = keyword;
        final int finalPage = page;
        final int finalLimit = limit;

        MeteorExecutor.execute(() -> {
            StringBuilder url = new StringBuilder(APIHandler.API_URL + API_ENDPOINT
                + "?sort=desc&pageSize=" + finalLimit + "&page=" + finalPage);

            if (finalPlayerName != null) url.append("&playerName=").append(finalPlayerName);
            if (finalKeyword != null && !finalKeyword.isEmpty()) {
                try {
                    url.append("&word=").append(URLEncoder.encode(finalKeyword, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    ChatUtils.warning("Failed to encode keyword", getName());
                }
            }

            String response = new APIHandler().fetch(url.toString());
            if (response == null) {
                ChatUtils.warning("No response from API", getName());
                return;
            }

            JsonObject root;
            try {
                root = JsonParser.parseString(response).getAsJsonObject();
            } catch (Exception e) {
                ChatUtils.warning("Failed to parse JSON", getName());
                return;
            }

            if (!root.has("chats")) {
                ChatUtils.warning("Unexpected API output", getName());
                return;
            }

            JsonArray chats = root.getAsJsonArray("chats");
            if (chats.size() == 0) {
                ChatUtils.info("No chat messages found.", getName());
                return;
            }

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd yyyy, HH:mm", Locale.US);
            player.sendMessage(Text.of("§8<§7-------- §7Chats §7--------§8>"), false);

            for (JsonElement e : chats) {
                JsonObject obj = e.getAsJsonObject();
                String msg = obj.has("chat") ? obj.get("chat").getAsString() : "";
                String timeStr = obj.has("time") ? obj.get("time").getAsString() : Instant.now().toString();
                String chatPlayer = obj.has("playerName") ? obj.get("playerName").getAsString() : "Unknown";
                ZonedDateTime zdt = Instant.parse(timeStr).atZone(ZoneId.systemDefault());
                if (finalPlayerName != null && !finalPlayerName.equalsIgnoreCase(chatPlayer)) continue;

                String display = msg;
                if (finalKeyword != null && !finalKeyword.isEmpty()) {
                    display = display.replaceAll("(?i)(" + finalKeyword + ")", "§e$1§f");
                }
                player.sendMessage(Text.of("§7[" + zdt.format(fmt) + "] <§b" + chatPlayer + "§7> §f" + display), false);
            }
        });
    }
}
