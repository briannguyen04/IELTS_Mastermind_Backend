package com.ieltsmastermind.practice.content.management.business.parser;

import com.ieltsmastermind.practice.content.management.domain.model.doc.DocNode;

import java.util.List;

public interface InstructionParser {

    List<DocNode> parseInstruction(String input);
}
