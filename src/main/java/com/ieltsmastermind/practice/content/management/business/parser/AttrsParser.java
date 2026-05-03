package com.ieltsmastermind.practice.content.management.business.parser;

import java.util.Map;

public interface AttrsParser {

    Map<String, String> parseAttrs(String attrStr);
}
