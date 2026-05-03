package com.ieltsmastermind.practice.content.management.business.parser;

import com.ieltsmastermind.practice.content.management.domain.model.doc.DocNode;
import com.ieltsmastermind.practice.content.management.domain.model.doc.ParagraphNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class InstructionParserImpl implements InstructionParser {

    private final InlineParser inlineParser;
    private final TableParser tableParser;
    private final ImageParser imageParser;
    private final MultipleChoiceParser multipleChoiceParser;

    public InstructionParserImpl(InlineParser inlineParser,
                                 TableParser tableParser,
                                 ImageParser imageParser,
                                 MultipleChoiceParser multipleChoiceParser) {
        this.inlineParser = inlineParser;
        this.tableParser = tableParser;
        this.imageParser = imageParser;
        this.multipleChoiceParser = multipleChoiceParser;
    }

    @Override
    public List<DocNode> parseInstruction(String input) {
        List<DocNode> nodes = new ArrayList<>();
        ParseContext context = new ParseContext();

        int i = 0;

        final String TABLE_OPEN = "[table";
        final String TABLE_CLOSE = "[/table]";
        final String IMG_OPEN = "[img";
        final String IMG_CLOSE = "]";
        final String MC_OPEN = "[multiple-choice";
        final String MC_CLOSE = "[/multiple-choice]";

        while (i < input.length()) {
            int nextTable = input.indexOf(TABLE_OPEN, i);
            int nextImg = input.indexOf(IMG_OPEN, i);
            int nextMc = input.indexOf(MC_OPEN, i);

            int nextPos = minNonNegative(nextTable, nextImg, nextMc);
            if (nextPos == -1) {
                pushParagraphNodes(nodes, input.substring(i), context);
                break;
            }

            if (nextPos > i) {
                pushParagraphNodes(nodes, input.substring(i, nextPos), context);
            }

            if (nextPos == nextTable) {
                int end = input.indexOf(TABLE_CLOSE, nextPos);
                if (end == -1) {
                    pushParagraphNodes(nodes, input.substring(nextPos), context);
                    break;
                }
                String block = input.substring(nextPos, end + TABLE_CLOSE.length());
                nodes.add(tableParser.parseTable(block, context));
                i = end + TABLE_CLOSE.length();

            } else if (nextPos == nextImg) {
                int end = input.indexOf(IMG_CLOSE, nextPos);
                if (end == -1) {
                    pushParagraphNodes(nodes, input.substring(nextPos), context);
                    break;
                }
                String tag = input.substring(nextPos, end + 1);
                nodes.add(imageParser.parseImage(tag));
                i = end + 1;

            } else {
                int end = input.indexOf(MC_CLOSE, nextPos);
                if (end == -1) {
                    pushParagraphNodes(nodes, input.substring(nextPos), context);
                    break;
                }
                String block = input.substring(nextPos, end + MC_CLOSE.length());
                nodes.add(multipleChoiceParser.parseMultipleChoice(block, context));
                i = end + MC_CLOSE.length();
            }
        }

        return nodes;
    }

    public void pushParagraphNodes(List<DocNode> out, String text, ParseContext context) {
        if (text == null || text.isBlank()) return;

        String[] rawParts = text.split("\\n\\s*\\n");

        for (String part : rawParts) {
            String p = part == null ? "" : part.trim();
            if (p.isEmpty()) continue;

            out.add(new ParagraphNode(inlineParser.parseInline(p, context)));
        }
    }

    private int minNonNegative(int... values) {
        int best = Integer.MAX_VALUE;
        for (int v : values) {
            if (v >= 0 && v < best) best = v;
        }
        return best == Integer.MAX_VALUE ? -1 : best;
    }
}