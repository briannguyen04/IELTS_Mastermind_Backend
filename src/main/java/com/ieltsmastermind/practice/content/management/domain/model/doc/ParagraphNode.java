package com.ieltsmastermind.practice.content.management.domain.model.doc;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

/** { type: "paragraph"; inlines } */
public record ParagraphNode(List<InlineNode> inlines) implements DocNode {
    public ParagraphNode {
        Objects.requireNonNull(inlines, "inlines");
        inlines = List.copyOf(inlines);
    }

    @JsonProperty("type")
    public String type() {
        return "paragraph";
    }
}
