package com.redhat.service.bridge.infra.models.filters;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class StringContains extends BaseFilter<List<String>> {

    public static final String FILTER_TYPE_NAME = "StringContains";

    @JsonProperty("values")
    private List<String> values;

    public StringContains() {
        super(FILTER_TYPE_NAME);
    }

    public StringContains(String key, List<String> values) {
        super(FILTER_TYPE_NAME, key);
        this.values = values;
    }

    @Override
    public List<String> getValue() {
        return values;
    }
}
