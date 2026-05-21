package com.ieltsmastermind.ai.feedback.business.service.LearnerPracticeAIService;

import com.ieltsmastermind.ai.feedback.domain.dto.ListeningFeedbackResponseDto;
import com.ieltsmastermind.ai.feedback.domain.dto.TaskOutput;
import com.ieltsmastermind.ai.feedback.domain.dto.WritingFeedbackResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static com.ieltsmastermind.common.constants.FileStorageConstants.IMAGE_PUBLIC_BASE_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AIFeedbackServiceImplTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private AIFeedbackServiceImpl aiFeedbackService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(aiFeedbackService, "model", "gpt-4o-mini");
    }

    @Test
    void callAI_whenWebClientSucceeds_shouldSendExpectedBodyAndReturnResponse() {
        configureSuccessfulWebClient("ai-response");

        String result = aiFeedbackService.callAI("Evaluate this answer.");

        assertThat(result).isEqualTo("ai-response");

        ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
        verify(requestBodyUriSpec).bodyValue(bodyCaptor.capture());

        Map<String, Object> body = castMap(bodyCaptor.getValue());
        assertThat(body.get("model")).isEqualTo("gpt-4o-mini");
        assertThat(body.get("temperature")).isEqualTo(0.2);
        assertThat(body.get("max_tokens")).isEqualTo(4000);

        Map<String, Object> responseFormat = castMap(body.get("response_format"));
        assertThat(responseFormat.get("type")).isEqualTo("json_object");

        List<Map<String, Object>> messages = castList(body.get("messages"));
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).get("role")).isEqualTo("system");
        assertThat(messages.get(0).get("content")).isEqualTo("You are an IELTS examiner.");
        assertThat(messages.get(1).get("role")).isEqualTo("user");
        assertThat(messages.get(1).get("content")).isEqualTo("Evaluate this answer.");

        verify(webClient).post();
        verify(requestBodyUriSpec).uri("/chat/completions");
        verify(responseSpec).bodyToMono(String.class);
    }

    @Test
    void callAI_whenBodyMonoFails_shouldPropagateRuntimeException() {
        configureFailingWebClient(new RuntimeException("AI timeout"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> aiFeedbackService.callAI("Evaluate this answer.")
        );

        assertThat(exception.getMessage()).isEqualTo("AI timeout");

        verify(webClient).post();
        verify(responseSpec).bodyToMono(String.class);
    }

    @Test
    void callAIWithImages_whenImageUrlsProvided_shouldSendTextAndImageContentAndReturnResponse() {
        configureSuccessfulWebClient("image-ai-response");

        String result = aiFeedbackService.callAIWithImages(
                "Evaluate this image task.",
                List.of("https://cdn.example.com/a.png", "https://cdn.example.com/b.png")
        );

        assertThat(result).isEqualTo("image-ai-response");

        ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
        verify(requestBodyUriSpec).bodyValue(bodyCaptor.capture());

        Map<String, Object> body = castMap(bodyCaptor.getValue());
        assertThat(body.get("model")).isEqualTo("gpt-4o-mini");
        assertThat(body.get("temperature")).isEqualTo(0.2);

        Map<String, Object> responseFormat = castMap(body.get("response_format"));
        assertThat(responseFormat.get("type")).isEqualTo("json_object");

        List<Map<String, Object>> messages = castList(body.get("messages"));

        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).get("role")).isEqualTo("system");

        Map<String, Object> userMessage = messages.get(1);
        assertThat(userMessage.get("role")).isEqualTo("user");

        List<Map<String, Object>> content = castList(userMessage.get("content"));
        assertThat(content).hasSize(3);
        assertThat(content.get(0).get("type")).isEqualTo("text");
        assertThat(content.get(0).get("text")).isEqualTo("Evaluate this image task.");

        assertThat(content.get(1).get("type")).isEqualTo("image_url");
        assertThat(castMap(content.get(1).get("image_url")).get("url"))
                .isEqualTo("https://cdn.example.com/a.png");

        assertThat(content.get(2).get("type")).isEqualTo("image_url");
        assertThat(castMap(content.get(2).get("image_url")).get("url"))
                .isEqualTo("https://cdn.example.com/b.png");

        verify(webClient).post();
        verify(requestBodyUriSpec).uri("/chat/completions");
    }

    @Test
    void callAIUnified_whenPublicUrlsAreNull_shouldCallTextOnlyRequest() {
        configureSuccessfulWebClient("text-only-response");

        String result = aiFeedbackService.callAIUnified("Prompt only.", null);

        assertThat(result).isEqualTo("text-only-response");

        ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
        verify(requestBodyUriSpec).bodyValue(bodyCaptor.capture());

        Map<String, Object> body = castMap(bodyCaptor.getValue());
        List<Map<String, Object>> messages = castList(body.get("messages"));

        assertThat(messages.get(1).get("content")).isEqualTo("Prompt only.");
    }

    @Test
    void callAIUnified_whenPublicUrlsAreEmpty_shouldCallTextOnlyRequest() {
        configureSuccessfulWebClient("text-only-response");

        String result = aiFeedbackService.callAIUnified("Prompt only.", List.of());

        assertThat(result).isEqualTo("text-only-response");

        ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
        verify(requestBodyUriSpec).bodyValue(bodyCaptor.capture());

        Map<String, Object> body = castMap(bodyCaptor.getValue());
        List<Map<String, Object>> messages = castList(body.get("messages"));

        assertThat(messages.get(1).get("content")).isEqualTo("Prompt only.");
    }

    @Test
    void callAIUnified_whenPublicUrlsArePresent_shouldCallImageRequest() {
        configureSuccessfulWebClient("image-response");

        String result = aiFeedbackService.callAIUnified(
                "Prompt with images.",
                List.of("https://cdn.example.com/a.png")
        );

        assertThat(result).isEqualTo("image-response");

        ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
        verify(requestBodyUriSpec).bodyValue(bodyCaptor.capture());

        Map<String, Object> body = castMap(bodyCaptor.getValue());
        List<Map<String, Object>> messages = castList(body.get("messages"));
        Map<String, Object> userMessage = messages.get(1);
        List<Map<String, Object>> content = castList(userMessage.get("content"));

        assertThat(content).hasSize(2);
        assertThat(content.get(0).get("text")).isEqualTo("Prompt with images.");
        assertThat(castMap(content.get(1).get("image_url")).get("url"))
                .isEqualTo("https://cdn.example.com/a.png");
    }

    @Test
    void parseResponse_whenJsonIsValid_shouldParseNestedContentIntoDto() {
        String contentJson = """
                {
                  "summary": "Good listening performance.",
                  "questions": [
                    {
                      "questionNumber": 1,
                      "result": "CORRECT",
                      "correctAnswer": ["A"],
                      "evidence": {
                        "quote": "The answer is stated directly.",
                        "reason": "The recording mentions option A."
                      }
                    }
                  ]
                }
                """;

        ListeningFeedbackResponseDto result =
                aiFeedbackService.parseResponse(openAiResponse(contentJson), ListeningFeedbackResponseDto.class);

        assertThat(result).isNotNull();
        assertThat(result.getSummary()).isEqualTo("Good listening performance.");
        assertThat(result.getQuestions()).hasSize(1);
        assertThat(result.getQuestions().get(0).getQuestionNumber()).isEqualTo(1);
        assertThat(result.getQuestions().get(0).getResult()).isEqualTo("CORRECT");
        assertThat(result.getQuestions().get(0).getCorrectAnswer()).containsExactly("A");
        assertThat(result.getQuestions().get(0).getEvidence().getQuote())
                .isEqualTo("The answer is stated directly.");
        assertThat(result.getQuestions().get(0).getEvidence().getReason())
                .isEqualTo("The recording mentions option A.");
    }

    @Test
    void parseResponse_whenJsonIsInvalid_shouldThrowRuntimeException() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> aiFeedbackService.parseResponse("not-json", ListeningFeedbackResponseDto.class)
        );

        assertThat(exception.getMessage()).startsWith("Parse error for JSON:");
    }

    @Test
    void parseResponse_whenChoicesAreMissing_shouldThrowRuntimeException() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> aiFeedbackService.parseResponse("{}", ListeningFeedbackResponseDto.class)
        );

        assertThat(exception.getMessage()).startsWith("Parse error for JSON:");
    }

    @Test
    void parseWritingResponse_whenJsonIsValidAndAnswersHaveFeedbacks_shouldReturnWritingFeedbackResponse() {
        String contentJson = """
                {
                  "answers": [
                    {
                      "orderIndex": 1,
                      "feedbacks": [
                        {
                          "criterionName": "TASK_RESPONSE",
                          "feedbackType": "STRENGTH",
                          "label": "CLEAR_POSITION",
                          "description": "Clear position.",
                          "explanation": "The writer presents a consistent opinion.",
                          "evidenceSentences": ["I strongly agree with this view."],
                          "recommendedActionDescription": "Keep the position clear.",
                          "recommendedActionExplanation": "A clear stance improves task response."
                        }
                      ]
                    }
                  ]
                }
                """;

        WritingFeedbackResponseDto result =
                aiFeedbackService.parseWritingResponse(openAiResponse(contentJson));

        assertThat(result).isNotNull();
        assertThat(result.getAnswers()).hasSize(1);
        assertThat(result.getAnswers().get(0).getOrderIndex()).isEqualTo(1);
        assertThat(result.getAnswers().get(0).getFeedbacks()).hasSize(1);
        assertThat(result.getAnswers().get(0).getFeedbacks().get(0).getCriterionName())
                .isEqualTo("TASK_RESPONSE");
        assertThat(result.getAnswers().get(0).getFeedbacks().get(0).getFeedbackType())
                .isEqualTo("STRENGTH");
        assertThat(result.getAnswers().get(0).getFeedbacks().get(0).getEvidenceSentences())
                .containsExactly("I strongly agree with this view.");
        assertThat(result.getAnswers().get(0).getFeedbacks().get(0).getRecommendedActionExplanation())
                .isEqualTo("A clear stance improves task response.");
    }

    @Test
    void parseWritingResponse_whenAnswersAreEmpty_shouldThrowRuntimeException() {
        String contentJson = """
                {
                  "answers": []
                }
                """;

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> aiFeedbackService.parseWritingResponse(openAiResponse(contentJson))
        );

        assertThat(exception.getMessage()).startsWith("Parse WRITING error:");
        assertThat(exception.getCause()).hasMessage("AI returned empty answers");
    }

    @Test
    void parseWritingResponse_whenAnswersAreMissing_shouldThrowRuntimeException() {
        String contentJson = "{}";

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> aiFeedbackService.parseWritingResponse(openAiResponse(contentJson))
        );

        assertThat(exception.getMessage()).startsWith("Parse WRITING error:");
        assertThat(exception.getCause()).hasMessage("AI returned empty answers");
    }

    @Test
    void parseWritingResponse_whenFeedbacksAreNull_shouldThrowRuntimeException() {
        String contentJson = """
                {
                  "answers": [
                    {
                      "orderIndex": 1
                    }
                  ]
                }
                """;

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> aiFeedbackService.parseWritingResponse(openAiResponse(contentJson))
        );

        assertThat(exception.getMessage()).startsWith("Parse WRITING error:");
        assertThat(exception.getCause()).hasMessage("Invalid feedback size: 0");
    }

    @Test
    void parseWritingResponse_whenJsonIsInvalid_shouldThrowRuntimeException() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> aiFeedbackService.parseWritingResponse("not-json")
        );

        assertThat(exception.getMessage()).startsWith("Parse WRITING error:");
    }

    @Test
    void extractJson_whenRawContainsJsonObject_shouldReturnOuterJsonObject() {
        String result = aiFeedbackService.extractJson("prefix {\"a\":1,\"b\":{\"c\":2}} suffix");

        assertThat(result).isEqualTo("{\"a\":1,\"b\":{\"c\":2}}");
    }

    @Test
    void extractJson_whenRawDoesNotContainOpeningBrace_shouldThrowRuntimeException() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> aiFeedbackService.extractJson("no json here}")
        );

        assertThat(exception.getMessage()).isEqualTo("Invalid JSON format");
    }

    @Test
    void extractJson_whenRawDoesNotContainClosingBrace_shouldThrowRuntimeException() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> aiFeedbackService.extractJson("{no json here")
        );

        assertThat(exception.getMessage()).isEqualTo("Invalid JSON format");
    }

    @Test
    void toJson_whenObjectIsSerializable_shouldReturnJsonString() {
        TaskOutput taskOutput = new TaskOutput();
        taskOutput.setId("task-1");
        taskOutput.setDescription("Practice matching questions.");

        String result = aiFeedbackService.toJson(taskOutput);

        assertThat(result).contains("\"id\":\"task-1\"");
        assertThat(result).contains("\"description\":\"Practice matching questions.\"");
    }

    @Test
    void toJson_whenObjectCannotBeSerialized_shouldThrowRuntimeException() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> aiFeedbackService.toJson(new BrokenJsonObject())
        );

        assertThat(exception).isNotNull();
    }

    @Test
    void extractImages_whenInstructionContainsImageTags_shouldReturnImageSourcesInOrder() {
        String instruction = """
                Question text [img src="/uploads/a.png" alt="A"] more text
                [img src="/uploads/b.jpg"]
                """;

        List<String> result = aiFeedbackService.extractImages(instruction);

        assertThat(result).containsExactly("/uploads/a.png", "/uploads/b.jpg");
    }

    @Test
    void extractImages_whenInstructionDoesNotContainImageTags_shouldReturnEmptyList() {
        List<String> result = aiFeedbackService.extractImages("Question without images.");

        assertThat(result).isEmpty();
    }

    @Test
    void toPublicUrls_whenLocalPathsAreProvided_shouldConvertToBackendPublicUrlsUsingFileNames() {
        ReflectionTestUtils.setField(aiFeedbackService, "backendUrl", "https://backend.example.com");

        List<String> result = aiFeedbackService.toPublicUrls(
                List.of("/tmp/uploads/a.png", "b.jpg")
        );

        assertThat(result).containsExactly(
                "https://backend.example.com" + IMAGE_PUBLIC_BASE_PATH + "a.png",
                "https://backend.example.com" + IMAGE_PUBLIC_BASE_PATH + "b.jpg"
        );
    }

    @Test
    void toPublicUrls_whenLocalPathsAreEmpty_shouldReturnEmptyList() {
        ReflectionTestUtils.setField(aiFeedbackService, "backendUrl", "https://backend.example.com");

        List<String> result = aiFeedbackService.toPublicUrls(List.of());

        assertThat(result).isEmpty();
    }

    @Test
    void stripCustomTags_whenCustomTagsExist_shouldRemoveSupportedTagsAndReplaceImages() {
        String input = """
                [f]Hello[/f][table][row][cell]A[/cell][/row][/table]
                [multiple-choice][option]One[/option][/multiple-choice][img src="/a.png"]
                """;

        String result = aiFeedbackService.stripCustomTags(input);

        assertThat(result).contains("Hello");
        assertThat(result).contains("A");
        assertThat(result).contains("One");
        assertThat(result).contains("[Image provided]");
        assertThat(result).doesNotContain("[f]");
        assertThat(result).doesNotContain("[/f]");
        assertThat(result).doesNotContain("[table]");
        assertThat(result).doesNotContain("[row]");
        assertThat(result).doesNotContain("[cell]");
        assertThat(result).doesNotContain("[multiple-choice]");
        assertThat(result).doesNotContain("[option]");
        assertThat(result).doesNotContain("[img");
    }

    @Test
    void stripCustomTags_whenInputHasNoCustomTags_shouldReturnSameText() {
        String result = aiFeedbackService.stripCustomTags("Plain instruction.");

        assertThat(result).isEqualTo("Plain instruction.");
    }

    @Test
    void normalizeInstruction_whenRawContainsCustomTags_shouldTransformToReadablePromptText() {
        String raw = """
                  [f bold]Hello?[/f][table][row][cell]A[/cell][cell]B[/cell][/row][/table]
                  [multiple-choice id="1"][option]One[/option][option]Two[/option][/multiple-choice]
                  [img src="/a.png"][gap:1]
                """;

        String result = aiFeedbackService.normalizeInstruction(raw);

        assertThat(result).contains("Hello\u2013");
        assertThat(result).contains("A B");
        assertThat(result).contains("- One");
        assertThat(result).contains("- Two");
        assertThat(result).contains("Refer to the image.");
        assertThat(result).contains("_____");
        assertThat(result).doesNotContain("[f");
        assertThat(result).doesNotContain("[table]");
        assertThat(result).doesNotContain("[row]");
        assertThat(result).doesNotContain("[cell]");
        assertThat(result).doesNotContain("[multiple-choice");
        assertThat(result).doesNotContain("[option");
        assertThat(result).doesNotContain("[img");
        assertThat(result).doesNotContain("[gap:");
        assertThat(result).isEqualTo(result.trim());
    }

    @Test
    void normalizeInstruction_whenRawIsPlainText_shouldTrimAndReplaceQuestionMarks() {
        String result = aiFeedbackService.normalizeInstruction("  Is this correct?  ");

        assertThat(result).isEqualTo("Is this correct\u2013");
    }

    @Test
    void logTokenUsage_whenResponseContainsUsage_shouldPrintTokenCounts() {
        String response = """
                {
                  "usage": {
                    "prompt_tokens": 120,
                    "completion_tokens": 80,
                    "total_tokens": 200
                  }
                }
                """;

        String output = captureSystemOut(() -> aiFeedbackService.logTokenUsage(response));

        assertThat(output).contains("========== TOKEN USAGE ==========");
        assertThat(output).contains("Request Tokens (Prompt): 120");
        assertThat(output).contains("Response Tokens (Completion): 80");
        assertThat(output).contains("Total Tokens: 200");
        assertThat(output).contains("=================================");
    }

    @Test
    void logTokenUsage_whenResponseCannotBeParsed_shouldPrintFallbackMessage() {
        String output = captureSystemOut(() -> aiFeedbackService.logTokenUsage("not-json"));

        assertThat(output).contains("Cannot parse token usage");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void configureSuccessfulWebClient(String responseBody) {
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/chat/completions")).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(responseBody));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void configureFailingWebClient(RuntimeException exception) {
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/chat/completions")).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.error(exception));
    }

    private String captureSystemOut(Runnable action) {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (PrintStream capture = new PrintStream(outputStream, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            action.run();
        } finally {
            System.setOut(originalOut);
        }

        return outputStream.toString(StandardCharsets.UTF_8);
    }

    private String openAiResponse(String contentJson) {
        return "{\"choices\":[{\"message\":{\"content\":\""
                + contentJson
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                + "\"}}]}";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object value) {
        return (List<Map<String, Object>>) value;
    }

    private static class BrokenJsonObject {
        public String getValue() {
            throw new RuntimeException("Broken getter");
        }
    }
}
