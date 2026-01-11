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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.Instant;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Stats2b2t extends Command {
    private static final String API_ENDPOINT = "/stats/player";

    public Stats2b2t() {
        super("stats2b2t", "Shows full stats for a player.", "stats");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("player", StringArgumentType.word())
            .executes(ctx -> {
                runStats(StringArgumentType.getString(ctx, "player"));
                return SINGLE_SUCCESS;
            })
        );
    }

    private void runStats(String playerName) {
        MeteorExecutor.execute(() -> {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player == null) return;

            String requestUrl = APIHandler.API_URL + API_ENDPOINT + "?playerName=" + playerName;
            String response = new APIHandler().fetch(requestUrl);
            if (response == null) return;

            JsonObject root;
            try {
                root = JsonParser.parseString(response).getAsJsonObject();
            } catch (Exception e) {
                ChatUtils.warning("Failed to parse JSON.", getName());
                return;
            }

            try {
                int chats = root.get("chatsCount").getAsInt();
                int deaths = root.get("deathCount").getAsInt();
                int kills = root.get("killCount").getAsInt();
                int joins = root.get("joinCount").getAsInt();
                int leaves = root.get("leaveCount").getAsInt();
                boolean prio = root.get("prio").getAsBoolean();
                long playtimeSeconds = root.get("playtimeSeconds").getAsLong();

                ZonedDateTime firstSeen = Instant.parse(root.get("firstSeen").getAsString())
                    .atZone(ZoneId.systemDefault());
                ZonedDateTime lastSeen = Instant.parse(root.get("lastSeen").getAsString())
                    .atZone(ZoneId.systemDefault());

                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd yyyy, HH:mm", Locale.US);
                String playtimeStr = formatDuration(Duration.ofSeconds(playtimeSeconds));

                sendStat(player, "Stats for §b" + playerName,
                    "§7Chats: §f" + chats,
                    "§7Deaths: §f" + deaths,
                    "§7Kills: §f" + kills,
                    "§7Joins: §f" + joins,
                    "§7Leaves: §f" + leaves,
                    "§7First Seen: §f" + firstSeen.format(fmt),
                    "§7Last Seen: §f" + lastSeen.format(fmt),
                    "§7Playtime: §f" + playtimeStr,
                    "§7Priority: §f" + prio
                );

            } catch (Exception e) {
                ChatUtils.warning("Missing fields in API response.", getName());
            }
        });
    }

    private void sendStat(ClientPlayerEntity player, String title, String... lines) {
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
