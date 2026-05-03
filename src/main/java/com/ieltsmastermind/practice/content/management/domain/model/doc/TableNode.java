package com.ieltsmastermind.practice.content.management.domain.model.doc;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

/** { type: "table"; rows } */
public record TableNode(List<TableRowNode> rows) implements DocNode {
    public TableNode {
        Objects.requireNonNull(rows, "rows");
        rows = List.copyOf(rows);
    }

    @JsonProperty("type")
    public String type() {
        return "table";
    }

    public record TableCellNode(Integer colspan, List<InlineNode> content) {
        public TableCellNode {
            Objects.requireNonNull(content, "content");
            content = List.copyOf(content);
        }
    }

    public record TableRowNode(List<TableCellNode> cells) {
        public TableRowNode {
            Objects.requireNonNull(cells, "cells");
            cells = List.copyOf(cells);
        }
    }
}
