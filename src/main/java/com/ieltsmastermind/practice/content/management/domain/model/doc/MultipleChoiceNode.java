package com.ieltsmastermind.practice.content.management.domain.model.doc;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

/** { type: "multiple-choice"; n; pick; options } */
public record MultipleChoiceNode(int n, int pick, List<MultipleChoiceOption> options) implements DocNode {
    public MultipleChoiceNode {
        Objects.requireNonNull(options, "options");
        options = List.copyOf(options);
    }

    @JsonProperty("type")
    public String type() {
        return "multiple-choice";
    }

    public record MultipleChoiceOption(String key, String label) {
        public MultipleChoiceOption {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(label, "label");
        }
    }
}
