package com.frogdevelopment.micronaut.consul.watch.watcher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.frogdevelopment.micronaut.consul.watch.context.PropertiesChangeHandler;

import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.discovery.consul.client.v1.ConsulClient;
import io.micronaut.discovery.consul.client.v1.KeyValue;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * @param <V>
 * @author LE GALL Benoît
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
abstract sealed class AbstractWatcher<V> implements Watcher permits ConfigurationsWatcher, NativeWatcher {

    protected final List<String> kvPaths;
    private final ConsulClient consulClient;
    private final PropertiesChangeHandler propertiesChangeHandler;

    private final Base64.Decoder base64Decoder = Base64.getDecoder();
    private final Map<String, V> holder = new ConcurrentHashMap<>();

    @Override
    public void watchKVs() {
        try {
            log.info("Polling KVs");
            Flux.fromIterable(kvPaths)
                    .parallel()
                    .runOn(Schedulers.parallel())
                    .flatMap(this::watchKvPath)
                    .sequential()
                    .collectList()
                    .doOnSuccess(this::handleSuccess)
                    .then()
                    .block();
        } catch (final Exception e) {
            log.error("Error reading configurations from Consul", e);
        }
    }

    private Mono<WatchResult> watchKvPath(final String kvPath) {
        log.debug("Polling kvPath={}", kvPath);
        return Mono.from(consulClient.readValues(kvPath))
                .onErrorResume(throwable -> handleError(kvPath, throwable))
                .flatMap(kvs -> mapToData(kvPath, kvs))
                .flatMap(next -> handle(kvPath, next));
    }

    private static Mono<List<KeyValue>> handleError(String kvPath, Throwable throwable) {
        if (throwable instanceof final HttpClientResponseException e && e.getStatus() == HttpStatus.NOT_FOUND) {
            log.debug("No KV found with path={}", kvPath);
            return Mono.empty();
        }

        return Mono.error(new ConfigurationException("Error reading KV for path=" + kvPath, throwable));
    }

    private Mono<WatchResult> handle(final String kvPath, final V next) {
        final var previous = holder.put(kvPath, next);

        if (previous == null) {
            log.debug("Watcher initialisation for kv path={}", kvPath);
            return Mono.empty();
        }

        if (areEqual(previous, next)) {
            handleNoChange();
            return Mono.empty();
        } else {
            final var previousValue = readValue(previous);
            final var nextValue = readValue(next);

            return Mono.just(new WatchResult(kvPath, previousValue, nextValue));
        }
    }

    protected abstract Mono<V> mapToData(String kvPath, final List<KeyValue> kvs);

    protected abstract boolean areEqual(final V previous, final V next);

    protected abstract Map<String, Object> readValue(final V value);

    protected final byte[] decodeValue(final KeyValue keyValue) {
        return base64Decoder.decode(keyValue.getValue());
    }

    protected void handleNoChange() {
        log.debug("Nothing changed");
    }

    private void handleSuccess(final List<WatchResult> results) {
        if (results.isEmpty()) {
            return;
        }
        log.info("Consul poll successful");
        propertiesChangeHandler.handleChanges(results);
    }

}
