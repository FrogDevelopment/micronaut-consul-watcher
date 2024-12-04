package com.frogdevelopment.micronaut.consul.watch.watcher;

import java.util.Comparator;
import java.util.List;

import com.frogdevelopment.micronaut.consul.watch.client.KeyValue;

/**
 * Utils class to compare {@link KeyValue}
 *
 * @author LE GALL Benoît
 * @since 1.0.0
 */
class KvUtils {

    /**
     * Private constructor
     */
    private KvUtils() {
    }

    /**
     * Compare 2 {@link KeyValue} by key and value
     *
     * @param left 1st {@link KeyValue} to compare
     * @param right 2d {@link KeyValue} to compare
     * @return {@code true} if they are equals
     */
    static boolean areEqual(final KeyValue left, final KeyValue right) {
        if (left == null && right == null) {
            return true;
        }

        if (left == null || right == null) {
            return false;
        }

        return left.getKey().equals(right.getKey()) && left.getValue().equals(right.getValue());
    }

    /**
     * Compare 2 list of {@link KeyValue}
     *
     * @param left 1st list of {@link KeyValue} to compare
     * @param right 2d list of {@link KeyValue} to compare
     * @return {@code true} if the list are equals
     * @see #areEqual(KeyValue, KeyValue)
     */
    static boolean areEqual(final List<KeyValue> left, final List<KeyValue> right) {
        if (left.size() != right.size()) {
            return false;
        }

        left.sort(Comparator.comparing(KeyValue::getKey));
        right.sort(Comparator.comparing(KeyValue::getKey));

        for (int i = 0; i < left.size(); i++) {
            final var leftKV = left.get(i);
            final var rightKV = right.get(i);
            if (!areEqual(rightKV, leftKV)) {
                return false;
            }
        }

        return true;
    }
}
