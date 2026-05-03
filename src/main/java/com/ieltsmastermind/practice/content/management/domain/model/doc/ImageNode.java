package com.ieltsmastermind.practice.content.management.domain.model.doc;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/** { type: "image"; src; alt?; width? } */
public record ImageNode(String src, String alt, Integer width) implements DocNode {
    public ImageNode {
        Objects.requireNonNull(src, "src");
    }

    @JsonProperty("type")
    public String type() {
        return "image";
    }
}
