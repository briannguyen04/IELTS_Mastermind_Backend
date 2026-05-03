package com.ieltsmastermind.practice.content.management.business.parser;

import com.ieltsmastermind.practice.content.management.domain.model.doc.ImageNode;

public interface ImageParser {

    ImageNode parseImage(String tag);
}
