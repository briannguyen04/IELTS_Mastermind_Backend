package com.ieltsmastermind.practice.content.management.domain.model.doc;

import com.fasterxml.jackson.annotation.JsonProperty;

/** { type: "gap"; n } */
public record GapInline(int n) implements InlineNode {
    @JsonProperty("type")
    public String type() {
        return "gap";
    }
}
