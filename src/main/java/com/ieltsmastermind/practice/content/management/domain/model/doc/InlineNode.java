package com.ieltsmastermind.practice.content.management.domain.model.doc;

import com.fasterxml.jackson.annotation.JsonProperty;

public sealed interface InlineNode permits TextInline, GapInline {
    @JsonProperty("type")
    String type();
}
