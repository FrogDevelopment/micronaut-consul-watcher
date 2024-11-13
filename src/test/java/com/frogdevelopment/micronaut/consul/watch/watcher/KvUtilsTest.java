package com.frogdevelopment.micronaut.consul.watch.watcher;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.micronaut.discovery.consul.client.v1.KeyValue;

class KvUtilsTest {

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
        final var actual = KvUtils.areEqual(kv1, kv2);

        // then
        assertThat(actual).isEqualTo(expected);
    }

}
