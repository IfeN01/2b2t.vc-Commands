package com.ifen.addon.utils;

import meteordevelopment.meteorclient.utils.player.ChatUtils;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * The file is based on 0xTas's <a href="https://github.com/0xTas/stardust/blob/main/src/main/java/dev/stardust/util/commands/ApiHandler.java">ApiHandler.java</a> and heavily modified by IfeN
 * credit to <a href="https://github.com/rfresh2">rfresh for the 2b api</a>
 **/


public class APIHandler {
    public static final String API_URL = "https://api.2b2t.vc";

    @Nullable
    public String fetch(String requestString) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(requestString))
                .header("Accept", "*/*")
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

            HttpResponse<String> res = HttpClient.newHttpClient()
                .send(req, HttpResponse.BodyHandlers.ofString());

            return switch (res.statusCode()) {
                case 200 -> res.body();
                case 204 -> "204 Undocumented";
                default -> {
                    ChatUtils.warning("Unexpected response: " + res, "ApiHandler");
                    yield null;
                }
            };
        } catch (Exception err) {
            ChatUtils.error(err.toString(), "ApiHandler");
            return null;
        }
    }

}
