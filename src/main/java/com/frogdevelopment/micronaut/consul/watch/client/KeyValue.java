package com.frogdevelopment.micronaut.consul.watch.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
@ReflectiveAccess
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class KeyValue {

    private final Integer modifyIndex;
    private final String key;
    private final String value;

    /**
     * @param key   The key
     * @param value The value
     */
    @JsonCreator
    public KeyValue(@JsonProperty("ModifyIndex") final Integer modifyIndex, @JsonProperty("Key") final String key, @JsonProperty("Value") final String value) {
        this.modifyIndex = modifyIndex;
        this.key = key;
        this.value = value;
    }

    /**
     * @return The modifyIndex
     */
    public Integer getModifyIndex() {
        return modifyIndex;
    }

    /**
     * @return The key
     */
    public String getKey() {
        return key;
    }

    /**
     * @return The value
     */
    public String getValue() {
        return value;
    }
}
