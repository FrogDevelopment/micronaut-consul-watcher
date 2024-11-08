package com.frogdevelopment.micronaut.consul.watch;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.frogdevelopment.micronaut.consul.watch.watcher.Watcher;

import io.micronaut.context.event.ShutdownEvent;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.TaskScheduler;

/**
 * Schedule the polling for all watcher in 1 unique Runnable.
 *
 * @author benoit.legall
 * @since 1.0.0
 */
@Slf4j
@Singleton
final class WatchScheduler {

    private final TaskScheduler taskScheduler;
    private final WatchConfiguration watchConfiguration;
    private final List<Watcher> watchers;

    private volatile boolean started = false;
    private volatile boolean stopped = false;
    private ScheduledFuture<?> scheduledFuture;

    /**
     * @param taskScheduler      taskScheduler
     * @param watchConfiguration configuration for the scheduled task
     * @param watchers           List of watcher to schedule
     */
    WatchScheduler(@Named(TaskExecutors.SCHEDULED) final TaskScheduler taskScheduler,
                   final WatchConfiguration watchConfiguration,
                   final List<Watcher> watchers) {
        this.taskScheduler = taskScheduler;
        this.watchConfiguration = watchConfiguration;
        this.watchers = watchers;
    }

    /**
     * Schedule the polling task
     *
     * @param startupEvent event sent when the application is started
     */
    @EventListener
    public synchronized void start(final StartupEvent startupEvent) {
        if (!started) {
            scheduledFuture = taskScheduler.scheduleAtFixedRate(
                    watchConfiguration.getInitialDelay(),
                    watchConfiguration.getPeriod(),
                    watch());
            started = true;
        } else {
            throw new IllegalStateException("Watcher scheduler already started");
        }
    }


    private Runnable watch() {
        return () -> watchers.forEach(Watcher::watchKvPath);
    }

    /**
     * Cancel the scheduled task
     *
     * @param shutdownEvent event sent when application will be shutdown
     */
    @EventListener
    public synchronized void stop(final ShutdownEvent shutdownEvent) {
        if (!started) {
            log.warn("You tried to stop an unstarted Watcher scheduler");
            return;
        }
        if (stopped) {
            log.warn("Watcher scheduler is already stopped");
            return;
        }

        stopped = scheduledFuture.cancel(true);
        log.info("Stopped watch: {}", stopped);
        if (Boolean.FALSE.equals(stopped)) {
            log.warn("Watcher scheduler could not be stopped");
        }
    }

}
