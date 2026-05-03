package com.ieltsmastermind.practice.content.management.business.parser;

import com.ieltsmastermind.practice.content.management.domain.model.doc.InlineNode;

import java.util.List;

public interface InlineParser {

    List<InlineNode> parseInline(String text, ParseContext context);
}
