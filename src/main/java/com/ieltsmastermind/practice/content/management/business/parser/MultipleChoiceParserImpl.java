package com.ieltsmastermind.practice.content.management.business.parser;

import com.ieltsmastermind.practice.content.management.domain.model.doc.MultipleChoiceNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MultipleChoiceParserImpl implements MultipleChoiceParser {

    // /^\[multiple-choice([^\]]*)\]/
    private static final Pattern MC_OPEN_RE = Pattern.compile("^\\[multiple-choice([^\\]]*)\\]");

    // /\[option([^\]]*)\]([\s\S]*?)\[\/option\]/g
    private static final Pattern OPTION_RE =
            Pattern.compile("\\[option([^\\]]*)\\]([\\s\\S]*?)\\[/option\\]");

    private final AttrsParser attrsParser;

    public MultipleChoiceParserImpl(AttrsParser attrsParser) {
        this.attrsParser = attrsParser;
    }

    @Override
    public MultipleChoiceNode parseMultipleChoice(String block, ParseContext context) {
        Matcher openM = MC_OPEN_RE.matcher(block);
        int openLen = 0;
        String openAttrsStr = "";

        if (openM.find()) {
            openLen = openM.group(0).length();
            openAttrsStr = openM.group(1) != null ? openM.group(1) : "";
        }

        int closeIndex = block.lastIndexOf("[/multiple-choice]");
        if (closeIndex < 0) closeIndex = block.length(); // defensive fallback

        String inner = block.substring(openLen, closeIndex).trim();

        Map<String, String> attrs = attrsParser.parseAttrs(openAttrsStr);

        int pick = 1;
        String pickStr = attrs.get("pick");
        if (pickStr != null) {
            try {
                pick = Integer.parseInt(pickStr);
            } catch (NumberFormatException ignored) {
                pick = 1;
            }
        }
        if (pick < 1) pick = 1;

        int n = context.nextQuestionNumber();
        for (int i = 1; i < pick; i++) {
            context.nextQuestionNumber();
        }

        List<MultipleChoiceNode.MultipleChoiceOption> options = new ArrayList<>();

        Matcher om = OPTION_RE.matcher(inner);
        while (om.find()) {
            String optAttrsStr = om.group(1) != null ? om.group(1) : "";
            Map<String, String> optAttrs = attrsParser.parseAttrs(optAttrsStr);

            String key = optAttrs.get("key");
            if (key != null) key = key.trim();
            if (key == null || key.isEmpty()) {
                // fallback A, B, C...
                key = String.valueOf((char) ('A' + options.size()));
            }

            String label = om.group(2) != null ? om.group(2).trim() : "";
            options.add(new MultipleChoiceNode.MultipleChoiceOption(key, label));
        }

        return new MultipleChoiceNode(n, pick, options);
    }

}