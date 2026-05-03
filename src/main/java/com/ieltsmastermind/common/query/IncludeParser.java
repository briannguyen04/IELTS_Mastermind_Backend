package com.ieltsmastermind.common.query;

import java.util.Arrays;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

public final class IncludeParser {
    private IncludeParser() {}

    public static IncludeSpec parse(String includeParam) {
        if (includeParam == null || includeParam.isBlank()) {
            return IncludeSpec.of(Set.of());
        }

        Set<String> requested = Arrays.stream(includeParam.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return IncludeSpec.of(requested);
    }
}
