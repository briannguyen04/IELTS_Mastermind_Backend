package com.ieltsmastermind.practice.content.management.domain.model.doc;

import com.fasterxml.jackson.annotation.JsonProperty;

public sealed interface DocNode permits ParagraphNode, TableNode, ImageNode, MultipleChoiceNode {
    @JsonProperty("type")
    String type();
}
