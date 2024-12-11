package com.frogdevelopment.micronaut.consul.watch.watcher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.frogdevelopment.micronaut.consul.watch.WatchConfiguration;
import com.frogdevelopment.micronaut.consul.watch.client.KeyValue;
import com.frogdevelopment.micronaut.consul.watch.client.WatchConsulClient;
import com.frogdevelopment.micronaut.consul.watch.context.PropertiesChangeHandler;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.client.exceptions.ReadTimeoutException;
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

    private final List<String> kvPaths;
    protected final WatchConsulClient consulClient;
    private final WatchConfiguration watchConfiguration;
    private final PropertiesChangeHandler propertiesChangeHandler;

    protected final Map<String, V> kvHolder = new ConcurrentHashMap<>();
    private final Map<String, Disposable> listeners = new ConcurrentHashMap<>();

    private final Base64.Decoder base64Decoder = Base64.getDecoder();
    private volatile boolean started = false;
    private volatile boolean isInit = false;

    @Override
    public void start() {
        if (started) {
            throw new IllegalStateException("Watcher is already started");
        }

        try {
            log.debug("Starting KVs watcher");
            started = true;
            kvPaths.parallelStream()
                    .forEach(this::watchKvPath);
        } catch (final Exception e) {
            log.error("Error watching configurations", e);
            stop();
        }
    }

    @Override
    public boolean isWatching() {
        return started && isInit;
    }

    @Override
    public void stop() {
        if (!started) {
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
        started = false;
        isInit = false;
    }

    private void watchKvPath(final String kvPath) {
            if (!started) {
                log.warn("Watcher is not started");
                return;
            }
            // delaying to avoid flood caused by multiple consecutive calls
            final var disposable = Mono.delay(watchConfiguration.getWatchDelay())
                    .then(watchValue(kvPath))
                    .subscribe(next -> onNext(kvPath, next), throwable -> onError(kvPath, throwable));

            listeners.put(kvPath, disposable);
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

        watchKvPath(kvPath);
    }

    protected abstract boolean areEqual(final V previous, final V next);

    protected abstract Map<String, Object> readValue(final V keyValue);

    private void onError(String kvPath, Throwable throwable) {
        if (throwable instanceof final HttpClientResponseException e && e.getStatus() == HttpStatus.NOT_FOUND) {
            log.trace("No KV found with kvPath={}", kvPath);
            listeners.remove(kvPath);
        } else if (throwable instanceof ReadTimeoutException) {
            log.debug("Timeout for kvPath={}", kvPath);
            watchKvPath(kvPath);
        } else {
            log.error("Watching kvPath={} failed", kvPath, throwable);
            listeners.remove(kvPath);
        }
    }

    private void handleInit(final String kvPath) {
        log.debug("Init watcher for kvPath={}", kvPath);
        this.isInit = true;
    }

    private void handleNoChange(final String kvPath) {
        log.debug("Nothing changed for kvPath={}", kvPath);
    }

    protected final byte[] decodeValue(final KeyValue keyValue) {
        return base64Decoder.decode(keyValue.getValue());
    }

}
