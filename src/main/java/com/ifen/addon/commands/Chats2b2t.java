package com.ifen.addon.commands;

import com.google.gson.*;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.player.ChatUtils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import com.ifen.addon.utils.APIHandler;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Pattern;

public class Chats2b2t extends Command {
    private static final String API_ENDPOINT = "/chats";

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 100;

    private static final DateTimeFormatter CHAT_TIME =
        DateTimeFormatter.ofPattern("MMM dd yyyy, HH:mm", Locale.US);

    public Chats2b2t() {
        super("chats2b2t", "Search 2b2t chat messages.", "chats");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        // PLAYER — kills-style design
        builder.then(literal("player")
            .then(argument("playerName", StringArgumentType.word())
                // default
                .executes(ctx -> run(
                    StringArgumentType.getString(ctx, "playerName"),
                    null,
                    DEFAULT_PAGE,
                    DEFAULT_LIMIT,
                    null,
                    null
                ))
                // page
                .then(argument("page", IntegerArgumentType.integer(1))
                    .executes(ctx -> run(
                        StringArgumentType.getString(ctx, "playerName"),
                        null,
                        ctx.getArgument("page", Integer.class),
                        DEFAULT_LIMIT,
                        null,
                        null
                    ))
                    // page + limit
                    .then(argument("limit", IntegerArgumentType.integer(1, MAX_LIMIT))
                        .executes(ctx -> run(
                            StringArgumentType.getString(ctx, "playerName"),
                            null,
                            ctx.getArgument("page", Integer.class),
                            ctx.getArgument("limit", Integer.class),
                            null,
                            null
                        ))
                        // optional date pair
                        .then(argument("startDate", StringArgumentType.word())
                            .then(argument("endDate", StringArgumentType.word())
                                .executes(ctx -> run(
                                    StringArgumentType.getString(ctx, "playerName"),
                                    null,
                                    ctx.getArgument("page", Integer.class),
                                    ctx.getArgument("limit", Integer.class),
                                    StringArgumentType.getString(ctx, "startDate"),
                                    StringArgumentType.getString(ctx, "endDate")
                                ))
                            )
                        )
                    )
                )
            )
        );

        // KEYWORD — same paging design
        builder.then(literal("keyword")
            .then(argument("keyword", StringArgumentType.word())
                .executes(ctx -> run(
                    null,
                    StringArgumentType.getString(ctx, "keyword"),
                    DEFAULT_PAGE,
                    DEFAULT_LIMIT,
                    null,
                    null
                ))
                .then(argument("page", IntegerArgumentType.integer(1))
                    .executes(ctx -> run(
                        null,
                        StringArgumentType.getString(ctx, "keyword"),
                        ctx.getArgument("page", Integer.class),
                        DEFAULT_LIMIT,
                        null,
                        null
                    ))
                    .then(argument("limit", IntegerArgumentType.integer(1, MAX_LIMIT))
                        .executes(ctx -> run(
                            null,
                            StringArgumentType.getString(ctx, "keyword"),
                            ctx.getArgument("page", Integer.class),
                            ctx.getArgument("limit", Integer.class),
                            null,
                            null
                        ))
                        .then(argument("startDate", StringArgumentType.word())
                            .then(argument("endDate", StringArgumentType.word())
                                .executes(ctx -> run(
                                    null,
                                    StringArgumentType.getString(ctx, "keyword"),
                                    ctx.getArgument("page", Integer.class),
                                    ctx.getArgument("limit", Integer.class),
                                    StringArgumentType.getString(ctx, "startDate"),
                                    StringArgumentType.getString(ctx, "endDate")
                                ))
                            )
                        )
                    )
                )
            )
        );
    }

    private int run(
        String playerName,
        String keyword,
        int page,
        int limit,
        String startDate,
        String endDate
    ) {
        ClientPlayerEntity mcPlayer = MinecraftClient.getInstance().player;
        if (mcPlayer == null) return SINGLE_SUCCESS;

        if ((startDate == null) != (endDate == null)) {
            ChatUtils.warning("startDate + endDate must be provided together.", getName());
            return SINGLE_SUCCESS;
        }

        MeteorExecutor.execute(() -> {
            String url = buildUrl(playerName, keyword, page, limit, startDate, endDate);
            String response = new APIHandler().fetch(url);
            if (response == null) return;

            JsonObject root;
            try {
                root = JsonParser.parseString(response).getAsJsonObject();
            } catch (Exception e) {
                ChatUtils.warning("Invalid JSON.", getName());
                return;
            }

            if (!root.has("chats")) {
                ChatUtils.warning("Unexpected API output.", getName());
                return;
            }

            JsonArray chats = root.getAsJsonArray("chats");
            if (chats.isEmpty()) {
                ChatUtils.info("No chat messages found.", getName());
                return;
            }

            Pattern highlight =
                keyword != null && !keyword.isEmpty()
                    ? Pattern.compile("(?i)" + Pattern.quote(keyword))
                    : null;

            mcPlayer.sendMessage(Text.of(""), false);
            mcPlayer.sendMessage(Text.of(
                "§8<§7-------- §7Chats §7(Page " + page + ") §7--------§8>"
            ), false);

            for (JsonElement el : chats) {
                JsonObject obj = el.getAsJsonObject();

                String msg = obj.get("chat").getAsString();
                String chatPlayer = obj.get("playerName").getAsString();

                if (playerName != null && !playerName.equalsIgnoreCase(chatPlayer)) continue;

                ZonedDateTime time = Instant.parse(obj.get("time").getAsString())
                    .atZone(ZoneId.systemDefault());

                if (highlight != null) {
                    msg = highlight.matcher(msg).replaceAll("§e$0§f");
                }

                mcPlayer.sendMessage(Text.of(
                    "§7[" + CHAT_TIME.format(time) + "] <§b" + chatPlayer + "§7> §f" + msg
                ), false);
            }

            mcPlayer.sendMessage(Text.of(""), false);
        });

        return SINGLE_SUCCESS;
    }

    private String buildUrl(
        String playerName,
        String keyword,
        int page,
        int limit,
        String startDate,
        String endDate
    ) {
        StringBuilder url = new StringBuilder(APIHandler.API_URL)
            .append(API_ENDPOINT)
            .append("?sort=desc")
            .append("&page=").append(page)
            .append("&pageSize=").append(limit);

        if (playerName != null) {
            url.append("&playerName=").append(playerName);
        }

        if (keyword != null && !keyword.isEmpty()) {
            url.append("&word=")
                .append(URLEncoder.encode(keyword, StandardCharsets.UTF_8));
        }

        if (startDate != null && endDate != null) {
            url.append("&startDate=").append(startDate);
            url.append("&endDate=").append(endDate);
        }

        return url.toString();
    }
}
