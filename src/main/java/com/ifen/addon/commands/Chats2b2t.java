package com.ifen.addon.commands;

import com.ifen.addon.utils.APIHandler;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
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

public class Chats2b2t extends Command {
    private static final String API_ENDPOINT = "/chats?playerName=";
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 25;

    public Chats2b2t() {
        super("chats2b2t", "Show recent chat messages of a 2b2t player.", "chats");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(
            argument("player", StringArgumentType.word()).executes(ctx -> {
                run(ctx, null, DEFAULT_LIMIT);
                return SINGLE_SUCCESS;
            }).then(
                argument("keywordOrLimit", StringArgumentType.word()).executes(ctx -> {
                    String arg = ctx.getArgument("keywordOrLimit", String.class);
                    String keyword = null;
                    int limit = DEFAULT_LIMIT;
                    try {
                        limit = Integer.parseInt(arg);
                        if (limit > MAX_LIMIT) limit = MAX_LIMIT;
                    } catch (NumberFormatException e) {
                        keyword = arg;
                    }

                    run(ctx, keyword, limit);
                    return SINGLE_SUCCESS;
                }).then(
                    argument("limit", IntegerArgumentType.integer(1, MAX_LIMIT)).executes(ctx -> {
                        String keyword = ctx.getArgument("keywordOrLimit", String.class);
                        int limit = ctx.getArgument("limit", Integer.class);
                        run(ctx, keyword, limit);
                        return SINGLE_SUCCESS;
                    })
                )
            )
        );
    }

    private void run(CommandContext<CommandSource> ctx, String keyword, int limit) {
        MeteorExecutor.execute(() -> {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player == null) return;

            String playerName = ctx.getArgument("player", String.class).trim();
            String requestUrl = APIHandler.API_URL + API_ENDPOINT + playerName + "&sort=desc";
            if (keyword != null) requestUrl += "&word=" + keyword;

            String response = new APIHandler().fetch(requestUrl);
            if (response == null) return;

            JsonElement root;
            try {
                root = JsonParser.parseString(response);
            } catch (Exception e) {
                ChatUtils.warning("Failed to parse JSON.", getName());
                return;
            }

            if (!root.getAsJsonObject().has("chats")) {
                ChatUtils.warning("Unexpected API output.", getName());
                return;
            }

            JsonArray chats = root.getAsJsonObject().getAsJsonArray("chats");
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd yyyy, HH:mm", Locale.US);

            player.sendMessage(
                Text.of("§8<§7-------- §7Chats for §b" + playerName + "§7: §7--------§8>"),
                false
            );

            int printed = 0;
            for (JsonElement e : chats) {
                if (printed >= limit) break;

                String chatMsg = e.getAsJsonObject().get("chat").getAsString();
                String timeStr = e.getAsJsonObject().get("time").getAsString();
                ZonedDateTime zdt = Instant.parse(timeStr).atZone(ZoneId.systemDefault());

                String displayMsg = chatMsg;
                if (keyword != null && !keyword.isEmpty()) {
                    displayMsg = displayMsg.replaceAll("(?i)(" + keyword + ")", "§e$1§f");
                }

                player.sendMessage(
                    Text.of("§7[" + zdt.format(fmt) + "] <§b" + playerName + "§7> §f" + displayMsg),
                    false
                );
                printed++;
            }
        });
    }
}
