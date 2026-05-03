package com.ieltsmastermind.practice.content.management.business.parser;

import com.ieltsmastermind.practice.content.management.domain.model.doc.InlineNode;
import com.ieltsmastermind.practice.content.management.domain.model.doc.TableNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TableParserImpl implements TableParser {

    // [row]...[/row]
    private static final Pattern ROW_RE = Pattern.compile("\\[row\\]([\\s\\S]*?)\\[/row\\]");

    // [cell ...]...[/cell]
    private static final Pattern CELL_RE = Pattern.compile("\\[cell([^\\]]*)\\]([\\s\\S]*?)\\[/cell\\]");

    // exact [table]
    private static final String TABLE_OPEN = "[table]";
    private static final String TABLE_CLOSE = "[/table]";

    private final AttrsParser attrsParser;
    private final InlineParser inlineParser;

    public TableParserImpl(AttrsParser attrsParser,
                           InlineParser inlineParser) {
        this.attrsParser = attrsParser;
        this.inlineParser = inlineParser;
    }

    @Override
    public TableNode parseTable(String block, ParseContext context) {
        int openLen = block.startsWith(TABLE_OPEN) ? TABLE_OPEN.length() : 0;

        int closeIndex = block.lastIndexOf(TABLE_CLOSE);
        if (closeIndex < 0) closeIndex = block.length(); // defensive fallback

        String inner = block.substring(openLen, closeIndex);

        List<TableNode.TableRowNode> rows = new ArrayList<>();

        Matcher rm = ROW_RE.matcher(inner);
        while (rm.find()) {
            String rowInner = rm.group(1);

            List<TableNode.TableCellNode> cells = new ArrayList<>();

            Matcher cm = CELL_RE.matcher(rowInner);
            while (cm.find()) {
                String attrsStr = cm.group(1) != null ? cm.group(1) : "";
                Map<String, String> cellAttrs = attrsParser.parseAttrs(attrsStr);

                Integer colspan = null;
                String colspanStr = cellAttrs.get("colspan");
                if (colspanStr != null) {
                    try {
                        colspan = Integer.parseInt(colspanStr);
                    } catch (NumberFormatException ignored) {
                        colspan = null;
                    }
                }

                List<InlineNode> content = inlineParser.parseInline(cm.group(2).trim(), context);
                cells.add(new TableNode.TableCellNode(colspan, content));
            }

            // Optional shortcut: allow [row]Some text [gap][/row] (no explicit [cell])
            if (cells.isEmpty()) {
                cells.add(new TableNode.TableCellNode(
                        null,
                        inlineParser.parseInline(rowInner.trim(), context)
                ));
            }

            rows.add(new TableNode.TableRowNode(cells));
        }

        return new TableNode(rows);
    }
}