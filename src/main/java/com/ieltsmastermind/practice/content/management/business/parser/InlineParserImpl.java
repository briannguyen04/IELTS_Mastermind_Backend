package com.ieltsmastermind.practice.content.management.business.parser;

import com.ieltsmastermind.practice.content.management.domain.model.doc.GapInline;
import com.ieltsmastermind.practice.content.management.domain.model.doc.InlineNode;
import com.ieltsmastermind.practice.content.management.domain.model.doc.TextInline;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class InlineParserImpl implements InlineParser {

    private final AttrsParser attrsParser;

    public InlineParserImpl(AttrsParser attrsParser) {
        this.attrsParser = attrsParser;
    }

    @Getter
    public static final class Format {
        private final String style;
        private final String color;
        private final Integer size;
        private final Integer weight;

        public Format(String style, String color, Integer size, Integer weight) {
            this.style = style;
            this.color = color;
            this.size = size;
            this.weight = weight;
        }
    }

    private static final Pattern GAP_RE = Pattern.compile("^\\[gap\\]");
    private static final Pattern F_OPEN_RE = Pattern.compile("^\\[f([^\\]]*)\\]");

    @Override
    public List<InlineNode> parseInline(String text, ParseContext context) {
        List<InlineNode> nodes = new ArrayList<>();
        Format active = null;

        int i = 0;

        while (i < text.length()) {
            int b = text.indexOf('[', i);
            if (b == -1) {
                pushText(nodes, text.substring(i), active);
                break;
            }

            if (b > i) {
                pushText(nodes, text.substring(i, b), active);
            }

            String rest = text.substring(b);

            // [gap]
            Matcher gapM = GAP_RE.matcher(rest);
            if (gapM.find()) {
                nodes.add(new GapInline(context.nextQuestionNumber()));
                i = b + gapM.group(0).length();
                continue;
            }

            // [f ...]
            Matcher fOpen = F_OPEN_RE.matcher(rest);
            if (fOpen.find()) {
                String attrStr = fOpen.group(1) != null ? fOpen.group(1) : "";
                Map<String, String> attrs = attrsParser.parseAttrs(attrStr);

                String style = attrs.get("style");
                String color = attrs.get("color");

                Integer size = null;
                String sizeStr = attrs.get("size");
                if (sizeStr != null) {
                    try {
                        size = Integer.parseInt(sizeStr);
                    } catch (NumberFormatException ignored) {
                        size = null;
                    }
                }

                Integer weight = null;
                String weightStr = attrs.get("weight");
                if (weightStr != null) {
                    try {
                        weight = Integer.parseInt(weightStr);
                    } catch (NumberFormatException ignored) {
                        weight = null;
                    }
                }

                active = new Format(style, color, size, weight);
                i = b + fOpen.group(0).length();
                continue;
            }

            // [/f]
            if (rest.startsWith("[/f]")) {
                active = null;
                i = b + "[/f]".length();
                continue;
            }

            // unknown tag => treat '[' as text
            pushText(nodes, "[", active);
            i = b + 1;
        }

        return nodes;
    }

    private static void pushText(List<InlineNode> nodes, String value, Format active) {
        if (value == null || value.isEmpty()) return;

        nodes.add(new TextInline(
                value,
                active != null ? active.getStyle() : null,
                active != null ? active.getColor() : null,
                active != null ? active.getSize() : null,
                active != null ? active.getWeight() : null
        ));
    }
}