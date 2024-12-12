package com.frogdevelopment.micronaut.consul.watch;

import lombok.extern.slf4j.Slf4j;

import com.frogdevelopment.micronaut.consul.watch.watcher.Watcher;

import io.micronaut.context.annotation.Context;
import io.micronaut.context.event.ShutdownEvent;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;

@Slf4j
@Context
public class WatchersTrigger {

    @EventListener
    public void onStart(final StartupEvent event) {
        log.info("Starting watchers");
        event.getSource().getBean(Watcher.class).start();
    }

    @EventListener
    public void onShutdown(final ShutdownEvent event) {
        log.info("Shutting down watchers");
        event.getSource().getBean(Watcher.class).stop();
    }
}
