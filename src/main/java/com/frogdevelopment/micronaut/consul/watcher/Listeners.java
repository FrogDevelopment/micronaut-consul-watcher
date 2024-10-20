package com.frogdevelopment.micronaut.consul.watcher;

import io.micronaut.context.event.ShutdownEvent;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor
public class Listeners {

    private final ConsulKVWatcher consulWatcher;

    @EventListener
    public void onStartupEvent(StartupEvent event) {
        consulWatcher.start();
    }

    @EventListener
    public void onShutdownEvent(ShutdownEvent event) {
        consulWatcher.stop();
    }
}
