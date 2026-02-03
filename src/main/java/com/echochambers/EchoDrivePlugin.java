package com.echochambers;

import com.echochambers.listeners.EntityRemoveListener;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.entity.EntityRemoveEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

public class EchoDrivePlugin extends JavaPlugin {

    private EventRegistry eventRegistry;

    public EchoDrivePlugin(@Nonnull JavaPluginInit init) {
        super(init);
        getLogger().at(Level.INFO).log("EchoDriveCharger plugin initialized");
    }

    @Override
    protected void start() {
        eventRegistry = new EventRegistry(
            new CopyOnWriteArrayList<>(),
            this::isEnabled,
            "EchoDriveCharger is not active!",
            HytaleServer.get().getEventBus()
        );
        eventRegistry.enable();
        
        eventRegistry.registerGlobal(
            EntityRemoveEvent.class,
            new EntityRemoveListener()
        );
        
        getLogger().at(Level.INFO).log("Plugin started successfully");
    }

    @Override
    protected void shutdown() {
        if (eventRegistry != null) {
            eventRegistry.shutdown();
            eventRegistry = null;
        }
        getLogger().at(Level.INFO).log("Plugin disabled");
    }
}
