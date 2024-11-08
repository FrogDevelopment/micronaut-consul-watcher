package com.frogdevelopment.micronaut.consul.watch.watcher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import com.frogdevelopment.micronaut.consul.watch.context.PropertiesChangeHandler;

import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.discovery.consul.client.v1.ConsulClient;
import io.micronaut.discovery.consul.client.v1.KeyValue;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @param <V>
 * @author benoit.legall
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
abstract sealed class AbstractWatcher<V> implements Watcher permits ConfigurationsWatcher, NativeWatcher {

    protected final String kvPath;
    private final ConsulClient consulClient;
    private final PropertiesChangeHandler propertiesChangeHandler;

    private final Base64.Decoder base64Decoder = Base64.getDecoder();
    private final AtomicReference<V> kvHolder = new AtomicReference<>();

    @Override
    public void watchKvPath() {
        try {
            log.debug("Polling kvPath={}", kvPath);
            Flux.from(consulClient.readValues(kvPath))
                    .onErrorResume(handleError())
                    .doOnNext(this::handleConfigurations)
                    .then()
                    .block();
        } catch (final Exception e) {
            log.error("Error reading configuration from Consul", e);
        }
    }

    protected abstract void handleConfigurations(final List<KeyValue> nextKVs);

    private Function<? super Throwable, ? extends Mono<? extends List<KeyValue>>> handleError() {
        return throwable -> {
            if (throwable instanceof final HttpClientResponseException e && e.getStatus() == HttpStatus.NOT_FOUND) {
                log.debug("No KV found with path={}", kvPath);
                return Mono.empty();
            }

            return Mono.error(new ConfigurationException("Error reading distributed configuration from Consul: " + throwable.getMessage(), throwable));
        };
    }

    protected V getAndSetPrevious(final V next) {
        return kvHolder.getAndSet(next);
    }

    protected static boolean areEqual(final KeyValue kv1, final KeyValue kv2) {
        if (kv1 == null && kv2 == null) {
            return true;
        }

        if (kv1 == null || kv2 == null) {
            return false;
        }

        return kv1.getKey().equals(kv2.getKey()) && kv1.getValue().equals(kv2.getValue());
    }

    protected byte[] decodeValue(final KeyValue keyValue) {
        return base64Decoder.decode(keyValue.getValue());
    }

    protected void handleNoChange() {
        log.debug("Nothing changed");
    }

    protected void handleSuccess(final Map<String, Object> previousValue, final Map<String, Object> nextValue) {
        log.info("SUCCESS - previousValue={}, nextValue={}", previousValue, nextValue);
        propertiesChangeHandler.handleChanges(kvPath, previousValue, nextValue);
    }

}
