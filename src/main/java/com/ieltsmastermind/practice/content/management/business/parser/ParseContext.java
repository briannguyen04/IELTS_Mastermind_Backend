package com.ieltsmastermind.practice.content.management.business.parser;

public class ParseContext {

    private int nextQuestionNumber = 1;

    public int nextQuestionNumber() {
        return nextQuestionNumber++;
    }
}
