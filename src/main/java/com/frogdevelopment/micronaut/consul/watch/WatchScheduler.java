package com.frogdevelopment.micronaut.consul.watch;

import com.frogdevelopment.micronaut.consul.watch.watcher.Watcher;

import io.micronaut.context.event.ShutdownEvent;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.TaskScheduler;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ScheduledFuture;

/**
 * Schedule the polling for all watcher in 1 unique Runnable.
 *
 * @author LE GALL Benoît
 * @since 1.0.0
 */
@Slf4j
@Singleton
final class WatchScheduler {

    private final TaskScheduler taskScheduler;
    private final WatchConfiguration watchConfiguration;
    private final Watcher watcher;

    private volatile boolean started = false;
    private volatile boolean stopped = false;
    private ScheduledFuture<?> scheduledFuture;

    /**
     * @param taskScheduler      taskScheduler
     * @param watchConfiguration configuration for the scheduled task
     * @param watcher            {@link Watcher} to schedule
     */
    WatchScheduler(@Named(TaskExecutors.SCHEDULED) final TaskScheduler taskScheduler,
                   final WatchConfiguration watchConfiguration,
                   final Watcher watcher) {
        this.taskScheduler = taskScheduler;
        this.watchConfiguration = watchConfiguration;
        this.watcher = watcher;
    }

    /**
     * Schedule the polling task
     */
    @EventListener
    public synchronized void start(final StartupEvent ignored) {
        if (!started) {
            scheduledFuture = taskScheduler.scheduleWithFixedDelay(
                    watchConfiguration.getInitialDelay(),
                    watchConfiguration.getPeriod(),
                    watcher::watchKVs);
            started = true;
        } else {
            throw new IllegalStateException("Watcher scheduler already started");
        }
    }


    /**
     * Cancel the scheduled task
     */
    @EventListener
    public synchronized void stop(final ShutdownEvent ignored) {
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
