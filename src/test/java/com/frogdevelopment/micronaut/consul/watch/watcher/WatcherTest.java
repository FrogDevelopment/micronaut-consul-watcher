package com.frogdevelopment.micronaut.consul.watch.watcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.frogdevelopment.micronaut.consul.watch.context.PropertiesChangeHandler;

import io.micronaut.context.env.PropertySourceReader;
import io.micronaut.context.env.yaml.YamlPropertySourceLoader;
import io.micronaut.discovery.consul.client.v1.ConsulClient;
import io.micronaut.discovery.consul.client.v1.KeyValue;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class WatcherTest {

    private Watcher watcher;
    @Mock
    private ConsulClient consulClient;
    @Mock
    private PropertiesChangeHandler propertiesChangeHandler;
    private final PropertySourceReader propertySourceReader = new YamlPropertySourceLoader();

    private final Base64.Encoder base64Encoder = Base64.getEncoder();

    @Captor
    private ArgumentCaptor<Map<String, Object>> previousDataCaptor;
    @Captor
    private ArgumentCaptor<Map<String, Object>> nextDataCaptor;

    @Test
    void should_publish_data_change_yaml() {
        // given
        watcher = new ConfigurationsWatcher("path/to/yaml", consulClient, propertiesChangeHandler, propertySourceReader);

        final var keyValue = new KeyValue("path/to/yaml", base64Encoder.encodeToString("foo.bar: value".getBytes()));
        final var newKeyValue = new KeyValue("path/to/yaml", base64Encoder.encodeToString("foo.bar: value_2".getBytes()));
        given(consulClient.readValues("path/to/yaml"))
                .willReturn(Mono.just(List.of(keyValue))) // init
                .willReturn(Mono.just(List.of(keyValue))) // no change
                .willReturn(Mono.just(List.of(newKeyValue))); // change

        // when
        watcher.watchKvPath(); // init
        watcher.watchKvPath(); // no change
        watcher.watchKvPath(); // change

        // then
        then(propertiesChangeHandler).should().handleChanges(eq("path/to/yaml"), previousDataCaptor.capture(), nextDataCaptor.capture());
        final var previousValue = previousDataCaptor.getValue();
        assertThat(previousValue)
                .hasSize(1)
                .containsEntry("foo.bar", "value");
        final var nextValue = nextDataCaptor.getValue();
        assertThat(nextValue)
                .hasSize(1)
                .containsEntry("foo.bar", "value_2");
    }

    @Test
    void should_publish_data_change_native() {
        // given
        watcher = new NativeWatcher("path/to/", consulClient, propertiesChangeHandler);

        final var previousKeyValue1 = new KeyValue("path/to/foo.bar", base64Encoder.encodeToString("value_a".getBytes()));
        final var previousKeyValue2 = new KeyValue("path/to/other.key", base64Encoder.encodeToString("value_b".getBytes()));
        final var previousKvs = new ArrayList<>(List.of(previousKeyValue1, previousKeyValue2));

        final var nextKeyValue1 = new KeyValue("path/to/foo.bar", base64Encoder.encodeToString("value_c".getBytes()));
        final var nextKeyValue2 = new KeyValue("path/to/other.key", base64Encoder.encodeToString("value_b".getBytes()));
        final var nextKvs = new ArrayList<>(List.of(nextKeyValue1, nextKeyValue2));
        given(consulClient.readValues("path/to/"))
                .willReturn(Mono.just(previousKvs)) // init
                .willReturn(Mono.just(previousKvs)) // no change
                .willReturn(Mono.just(nextKvs)); // change

        // when
        watcher.watchKvPath(); // init
        watcher.watchKvPath(); // no change
        watcher.watchKvPath(); // change

        // then
        then(propertiesChangeHandler).should().handleChanges(eq("path/to/"), previousDataCaptor.capture(), nextDataCaptor.capture());
        final var previousValue = previousDataCaptor.getValue();
        assertThat(previousValue)
                .hasSize(2)
                .containsEntry("foo.bar", "value_a")
                .containsEntry("other.key", "value_b");
        final var nextValue = nextDataCaptor.getValue();
        assertThat(nextValue)
                .hasSize(2)
                .containsEntry("foo.bar", "value_c")
                .containsEntry("other.key", "value_b");
    }

    @Test
    void should_log_global_error() {
        // given
        watcher = new ConfigurationsWatcher("path/to/yaml", consulClient, propertiesChangeHandler, propertySourceReader);

        final var keyValue = new KeyValue("path/to/yaml", "");
        final var newKeyValue = new KeyValue("path/to/yaml", "incorrect data");
        given(consulClient.readValues("path/to/yaml"))
                .willReturn(Mono.just(List.of(keyValue))) // init
                .willReturn(Mono.just(List.of(keyValue))) // no change
                .willReturn(Mono.just(List.of(newKeyValue))); // change

        // when
        watcher.watchKvPath(); // init
        watcher.watchKvPath(); // no change
        watcher.watchKvPath(); // change

        // then
        then(propertiesChangeHandler).shouldHaveNoInteractions();
        // todo assert error logs
    }

    @Test
    void should_handle_client_error_NOT_FOUND() {
        // given
        watcher = new ConfigurationsWatcher("path/to/yaml", consulClient, propertiesChangeHandler, propertySourceReader);

        final var response = mock(HttpResponse.class);
        final var exception = new HttpClientResponseException("boom", response);
        given(consulClient.readValues("path/to/yaml")).willReturn(Mono.error(exception));
        given(response.getStatus()).willReturn(HttpStatus.NOT_FOUND);

        // when
        watcher.watchKvPath();

        // then
        then(propertiesChangeHandler).shouldHaveNoInteractions();
        // todo assert no error logs
    }

    @Test
    void should_handle_client_error_http_error() {
        // given
        watcher = new ConfigurationsWatcher("path/to/yaml", consulClient, propertiesChangeHandler, propertySourceReader);

        final var response = mock(HttpResponse.class);
        final var exception = new HttpClientResponseException("boom", response);
        given(consulClient.readValues("path/to/yaml")).willReturn(Mono.error(exception));
        given(response.getStatus()).willReturn(HttpStatus.INTERNAL_SERVER_ERROR);

        // when
        watcher.watchKvPath();

        // then
        then(propertiesChangeHandler).shouldHaveNoInteractions();
        // todo assert error logs
    }

    @Test
    void should_handle_client_error_other() {
        // given
        watcher = new ConfigurationsWatcher("path/to/yaml", consulClient, propertiesChangeHandler, propertySourceReader);

        final var exception = new RuntimeException("boom");
        given(consulClient.readValues("path/to/yaml")).willReturn(Mono.error(exception));

        // when
        watcher.watchKvPath();
        // then
        then(propertiesChangeHandler).shouldHaveNoInteractions();
        // todo assert error logs
    }

    private static Stream<Arguments> provideKV() {
        return Stream.of(
                Arguments.of(null, null, true),
                Arguments.of(new KeyValue("key", "value"), null, false),
                Arguments.of(null, new KeyValue("key", "value"), false),
                Arguments.of(new KeyValue("key", "value"), new KeyValue("key_2", "value"), false),
                Arguments.of(new KeyValue("key", "value"), new KeyValue("key", "value_2"), false),
                Arguments.of(new KeyValue("key", "value"), new KeyValue("key", "value"), true)
        );
    }

    @ParameterizedTest
    @MethodSource("provideKV")
    void should_compare_KV_both_null(final KeyValue kv1, final KeyValue kv2, final boolean expected) {
        // when
        final var actual = AbstractWatcher.areEqual(kv1, kv2);

        // then
        assertThat(actual).isEqualTo(expected);
    }

}
