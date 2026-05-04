package com.ieltsmastermind.practice.content.management.business.parser;

import com.ieltsmastermind.practice.content.management.domain.model.doc.DocNode;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

@Disabled("Temporarily excluded from test suite")
public class InstructionParserImplTest {

    // mvn -Dtest=InstructionParserImplTest#parseInstruction_showResult test
    @Test
    void parseInstruction_showResult() {
        AttrsParser attrsParser = new AttrsParserImpl();
        InlineParser inlineParser = new InlineParserImpl(attrsParser);
        TableParser tableParser = new TableParserImpl(attrsParser, inlineParser);

        ImageParser imageParser = new ImageParserImpl(attrsParser);
        MultipleChoiceParser multipleChoiceParser = new MultipleChoiceParserImpl(attrsParser);

        InstructionParserImpl instructionParser = new InstructionParserImpl(
                inlineParser,
                tableParser,
                imageParser,
                multipleChoiceParser
        );

        String input = "[f style=\"normal\" color=\"#0f172a\" size=\"22\" weight=\"800\"]Section 1[/f]\n" +
                "\n" +
                "[f style=\"normal\" color=\"#1d4ed8\" size=\"18\" weight=\"700\"]Questions 1-6[/f][f style=\"normal\" color=\"#0f172a\" size=\"14\" weight=\"400\"] Complete the form below.[/f]\n" +
                "\n" +
                "[f style=\"italic\" color=\"#0f172a\" size=\"14\" weight=\"600\"]Write NO MORE THAN TWO WORDS AND/OR A NUMBER for each answer.[/f]\n" +
                "\n" +
                "[table]\n" +
                "[row]\n" +
                "[cell][f style=\"normal\" color=\"#334155\" size=\"14\" weight=\"600\"]Survey reference number[/f][/cell]\n" +
                "[cell][gap][/cell]\n" +
                "[/row]\n" +
                "[row]\n" +
                "[cell][f style=\"normal\" color=\"#334155\" size=\"14\" weight=\"600\"]Name[/f][/cell]\n" +
                "[cell][gap][/cell]\n" +
                "[/row]\n" +
                "[row]\n" +
                "[cell][f style=\"normal\" color=\"#334155\" size=\"14\" weight=\"600\"]Postcode[/f][/cell]\n" +
                "[cell][gap][/cell]\n" +
                "[/row]\n" +
                "[row]\n" +
                "[cell][f style=\"normal\" color=\"#334155\" size=\"14\" weight=\"600\"]Main purpose of travel[/f][/cell]\n" +
                "[cell][gap][/cell]\n" +
                "[/row]\n" +
                "[row]\n" +
                "[cell][f style=\"normal\" color=\"#334155\" size=\"14\" weight=\"600\"]Usual time of travel (a.m./p.m.)[/f][/cell]\n" +
                "[cell][gap][/cell]\n" +
                "[/row]\n" +
                "[row]\n" +
                "[cell][f style=\"normal\" color=\"#334155\" size=\"14\" weight=\"600\"]Number of trips per week[/f][/cell]\n" +
                "[cell][gap][/cell]\n" +
                "[/row]\n" +
                "[/table]\n" +
                "\n" +
                "[f style=\"normal\" color=\"#1d4ed8\" size=\"18\" weight=\"700\"]Question 7[/f][f style=\"normal\" color=\"#0f172a\" size=\"14\" weight=\"400\"] Look at the map image.[/f]\n" +
                "\n" +
                "[img src=\"https://www.gstatic.com/webp/gallery3/2.png\" alt=\"Transport map (sample)\" width=\"320\"]\n" +
                "\n" +
                "[f style=\"italic\" color=\"#0f172a\" size=\"14\" weight=\"600\"]Choose ONE answer.[/f]\n" +
                "\n" +
                "[multiple-choice pick=\"1\"]\n" +
                "[option key=\"A\"]The bus stop is next to the library.[/option]\n" +
                "[option key=\"B\"]The bus stop is opposite the supermarket.[/option]\n" +
                "[option key=\"C\"]The bus stop is behind the station.[/option]\n" +
                "[option key=\"D\"]The bus stop is beside the car park.[/option]\n" +
                "[/multiple-choice]\n" +
                "\n" +
                "[f style=\"normal\" color=\"#1d4ed8\" size=\"18\" weight=\"700\"]Questions 8–10[/f][f style=\"normal\" color=\"#0f172a\" size=\"14\" weight=\"600\"] Choose THREE answers.[/f]\n" +
                "\n" +
                "[multiple-choice pick=\"3\"]\n" +
                "[option key=\"A\"]Cheaper weekly tickets[/option]\n" +
                "[option key=\"B\"]More frequent buses[/option]\n" +
                "[option key=\"C\"]Longer operating hours[/option]\n" +
                "[option key=\"D\"]Cleaner vehicles[/option]\n" +
                "[option key=\"E\"]More routes to suburbs[/option]\n" +
                "[/multiple-choice]";

        List<DocNode> result = instructionParser.parseInstruction(input);

        System.out.println(result);
    }
}
