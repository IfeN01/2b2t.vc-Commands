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

public class FirstSeen2b2t extends Command {
    private static final String API_ENDPOINT = "/seen?playerName=";

    public FirstSeen2b2t() {
        super("firstseen2b2t", "Shows first seen info for a player.", "fs", "firstseen");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("player", StringArgumentType.word())
            .executes(ctx -> {
                runFirstSeen(StringArgumentType.getString(ctx, "player"));
                return SINGLE_SUCCESS;
            })
        );
    }

    private void runFirstSeen(String playerName) {
        MeteorExecutor.execute(() -> {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player == null) return;

            String requestUrl = APIHandler.API_URL + API_ENDPOINT + playerName;
            String response = new APIHandler().fetch(requestUrl);
            if (response == null) return;

            JsonObject root;
            try {
                root = JsonParser.parseString(response).getAsJsonObject();
            } catch (Exception e) {
                ChatUtils.warning("Failed to parse JSON.", getName());
                return;
            }

            if (!root.has("firstSeen")) {
                ChatUtils.warning("Unexpected API output.", getName());
                return;
            }

            Instant firstSeen = Instant.parse(root.get("firstSeen").getAsString());
            ZonedDateTime firstSeenZdt = firstSeen.atZone(ZoneId.systemDefault());
            Duration duration = Duration.between(firstSeen, Instant.now());
            String line = String.format(
                "§7First seen §b%s §7%s ago (§f%s§7)",
                playerName,
                formatDuration(duration),
                firstSeenZdt.format(DateTimeFormatter.ofPattern("MMM dd yyyy, HH:mm", Locale.US))
            );

            sendMessageLines(player, "First seen §b" + playerName, line);
        });
    }

    private void sendMessageLines(ClientPlayerEntity player, String title, String... lines) {
        player.sendMessage(Text.of(""), false);
        player.sendMessage(Text.of("§8<§7-------- " + title + " §7--------§8>"), false);
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
        return String.format("§f%d days §f%d hours §f%d minutes §f%d seconds", days, hours, minutes, seconds);
    }
}
