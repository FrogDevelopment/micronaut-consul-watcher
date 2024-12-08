package com.frogdevelopment.micronaut.consul.watch.watcher;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.frogdevelopment.micronaut.consul.watch.client.KeyValue;


class KvUtilsTest {

    private static Stream<Arguments> provideKV() {
        return Stream.of(
                Arguments.of(null, null, true),
                Arguments.of(new KeyValue(0, "key", "value"), null, false),
                Arguments.of(null, new KeyValue(0, "key", "value"), false),
                Arguments.of(new KeyValue(0, "key", "value"), new KeyValue(0, "key_2", "value"), false),
                Arguments.of(new KeyValue(0, "key", "value"), new KeyValue(0, "key", "value_2"), false),
                Arguments.of(new KeyValue(0, "key", "value"), new KeyValue(0, "key", "value"), true)
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
