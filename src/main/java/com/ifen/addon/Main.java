package com.ifen.addon;

import com.ifen.addon.commands.*;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import org.slf4j.Logger;

public class Main extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        LOG.info("Initializing Meteor Addon Template");

        Commands.add(new Chats2b2t());
        Commands.add(new Connections2b2t());
        Commands.add(new Deaths2b2t());
        Commands.add(new FirstSeen2b2t());
        Commands.add(new Kills2b2t());
        Commands.add(new LastSeen2b2t());
        Commands.add(new Playtime2b2t());
        Commands.add(new Stats2b2t());
    }

    @Override
    public String getPackage() {
        return "com.ifen.addon";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("MeteorDevelopment", "core-2b2t.vc");
    }
}
