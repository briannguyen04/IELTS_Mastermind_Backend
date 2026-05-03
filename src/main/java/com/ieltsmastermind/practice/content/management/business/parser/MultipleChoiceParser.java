package com.ieltsmastermind.practice.content.management.business.parser;

import com.ieltsmastermind.practice.content.management.domain.model.doc.MultipleChoiceNode;

public interface MultipleChoiceParser {

    MultipleChoiceNode parseMultipleChoice(String block, ParseContext context);
}
