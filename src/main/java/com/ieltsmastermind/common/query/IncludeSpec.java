package com.ieltsmastermind.common.query;

import java.util.Set;

public record IncludeSpec(Set<String> values) {
    public boolean has(String key) {
        return values != null && values.contains(key.toLowerCase());
    }

    public static IncludeSpec of(Set<String> values) {
        return new IncludeSpec(values);
    }
}