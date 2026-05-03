package com.ieltsmastermind.practice.attempt.management.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum LearnerTestActivityType {
    GAP_FOCUS("GF"),
    GAP_INPUT("GI"),
    GAP_BLUR("GB"),
    MCQ_SELECT("MS"),
    MCQ_DESELECT("MD"),
    TEST_START("TS"),
    TEST_SUBMIT("TE");

    private final String code;

    LearnerTestActivityType(String code) {
        this.code = code;
    }

    @JsonValue
    public String toJson() {
        return code;
    }

    @JsonCreator
    public static LearnerTestActivityType fromJson(String value) {
        if (value == null) {
            return null;
        }

        return Arrays.stream(values())
                .filter(type ->
                        type.code.equalsIgnoreCase(value) ||
                                type.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown LearnerTestActivityType: " + value
                ));
    }
}
