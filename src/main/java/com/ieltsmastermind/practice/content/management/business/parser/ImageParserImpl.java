package com.ieltsmastermind.practice.content.management.business.parser;

import com.ieltsmastermind.practice.content.management.domain.model.doc.ImageNode;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ImageParserImpl implements ImageParser {

    private static final Pattern IMG_TAG_RE = Pattern.compile("^\\[img([^\\]]*)\\]$");

    private final AttrsParser attrsParser;

    public ImageParserImpl(AttrsParser attrsParser) {
        this.attrsParser = attrsParser;
    }

    @Override
    public ImageNode parseImage(String tag) {
        Matcher m = IMG_TAG_RE.matcher(tag);

        String attrStr = "";
        if (m.matches() && m.group(1) != null) {
            attrStr = m.group(1);
        }

        Map<String, String> attrs = attrsParser.parseAttrs(attrStr);

        Integer width = null;
        String widthStr = attrs.get("width");
        if (widthStr != null) {
            try {
                width = Integer.parseInt(widthStr);
            } catch (NumberFormatException ignored) {
                width = null; // mirrors TS Number.isFinite check -> undefined
            }
        }

        String src = attrs.get("src"); // TS expects attrs.src
        String alt = attrs.get("alt");

        // If you want to enforce src required:
        // if (src == null || src.isBlank()) throw new IllegalArgumentException("img src is required");

        return new ImageNode(src, alt, width);
    }

}
