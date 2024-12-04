package com.frogdevelopment.micronaut.consul.watch.watcher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.frogdevelopment.micronaut.consul.watch.client.IndexConsulClient;
import com.frogdevelopment.micronaut.consul.watch.client.KeyValue;
import com.frogdevelopment.micronaut.consul.watch.context.PropertiesChangeHandler;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.client.exceptions.ReadTimeoutException;
import io.micronaut.http.client.exceptions.ResponseClosedException;
import io.micronaut.scheduling.TaskScheduler;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

/**
 * @author LE GALL Benoît
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
abstract sealed class AbstractWatcher<V> implements Watcher permits ConfigurationsWatcher, NativeWatcher {

    protected static final Integer NO_INDEX = null;
    private static final int WATCH_DELAY = 1000;

    private final List<String> kvPaths;
    private final TaskScheduler taskScheduler;
    protected final IndexConsulClient consulClient;
    private final PropertiesChangeHandler propertiesChangeHandler;

    protected final Map<String, V> kvHolder = new ConcurrentHashMap<>();
    private final Map<String, Disposable> listeners = new ConcurrentHashMap<>();

    private final Base64.Decoder base64Decoder = Base64.getDecoder();
    private volatile boolean watching = false;

    @Override
    public void start() {
        if (watching) {
            throw new IllegalStateException("Watcher is already started");
        }

        try {
            log.debug("Starting KVs watcher");
            watching = true;
            kvPaths.forEach(kvPath -> watchKvPath(kvPath, 0));
        } catch (final Exception e) {
            log.error("Error watching configurations", e);
            stop();
        }
    }

    @Override
    public void stop() {
        if (!watching) {
            log.warn("You tried to stop an unstarted Watcher");
            return;
        }

        log.debug("Stopping KVs watchers");
        listeners.forEach((key, value) -> {
            try {
                log.debug("Stopping watch for kvPath={}", key);
                value.dispose();
            } catch (final Exception e) {
                log.error("Error stopping configurations watcher for kvPath={}", key, e);
            }
        });
        listeners.clear();
        kvHolder.clear();
        watching = false;
    }

    private void watchKvPath(final String kvPath, final int nbFailures) {
        taskScheduler.schedule(Duration.ofMillis(WATCH_DELAY), () -> {
            if (!watching) {
                log.warn("Watcher is not started");
                return;
            }
            final var disposable = watchValue(kvPath)
                    .subscribe(next -> onNext(kvPath, next), throwable -> onError(kvPath, throwable, nbFailures));

            listeners.put(kvPath, disposable);
        });
    }

    protected abstract Mono<V> watchValue(String kvPath);

    private void onNext(String kvPath, final V next) {
        final var previous = kvHolder.put(kvPath, next);

        if (previous == null) {
            handleInit(kvPath);
        } else if (areEqual(previous, next)) {
            handleNoChange(kvPath);
        } else {
            final var previousValue = readValue(previous);
            final var nextValue = readValue(next);

            propertiesChangeHandler.handleChanges(new WatchResult(kvPath, previousValue, nextValue));
        }

        watchKvPath(kvPath, 0);
    }

    protected abstract boolean areEqual(final V previous, final V next);

    protected abstract Map<String, Object> readValue(final V keyValue);

    private void onError(String kvPath, Throwable throwable, int nbFailures) {
        if (throwable instanceof final HttpClientResponseException e && e.getStatus() == HttpStatus.NOT_FOUND) {
            log.debug("No KV found with kvPath={}", kvPath);
            listeners.remove(kvPath);
        } else if (throwable instanceof ReadTimeoutException || throwable instanceof ResponseClosedException) {
            log.debug("Exception [{}] for kvPath={}", throwable, kvPath);
            watchKvPath(kvPath, 0);
        } else {
            log.error("Watching kvPath={} failed", kvPath, throwable);
            if (nbFailures <= 3) {
                watchKvPath(kvPath, nbFailures + 1);
            }
        }
    }

    private void handleInit(final String kvPath) {
        log.debug("Init watcher for kvPath={}", kvPath);
    }

    private void handleNoChange(final String kvPath) {
        log.debug("Nothing changed for kvPath={}", kvPath);
    }

    protected final byte[] decodeValue(final KeyValue keyValue) {
        return base64Decoder.decode(keyValue.getValue());
    }

}
