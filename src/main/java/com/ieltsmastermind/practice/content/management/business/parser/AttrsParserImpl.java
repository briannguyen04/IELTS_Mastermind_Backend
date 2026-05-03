package com.ieltsmastermind.practice.content.management.business.parser;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AttrsParserImpl implements AttrsParser{

    @Override
    public Map<String, String> parseAttrs(String attrStr) {
        Map<String, String> attrs = new LinkedHashMap<>();
        Pattern p = Pattern.compile("(\\w+)\\s*=\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(attrStr);

        while (m.find()) {
            attrs.put(m.group(1), m.group(2));
        }
        return attrs;
    }
}
