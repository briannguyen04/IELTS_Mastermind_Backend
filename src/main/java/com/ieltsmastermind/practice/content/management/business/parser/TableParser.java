package com.ieltsmastermind.practice.content.management.business.parser;

import com.ieltsmastermind.practice.content.management.domain.model.doc.TableNode;

public interface TableParser {

    TableNode parseTable(String block, ParseContext context);
}
