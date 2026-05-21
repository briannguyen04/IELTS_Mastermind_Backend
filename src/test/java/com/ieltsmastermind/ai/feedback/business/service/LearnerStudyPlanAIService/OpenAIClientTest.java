package com.ieltsmastermind.ai.feedback.business.service.LearnerStudyPlanAIService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltsmastermind.ai.feedback.domain.dto.StudyPlanAIResponse;
import com.ieltsmastermind.ai.feedback.domain.dto.TaskInput;
import com.ieltsmastermind.ai.feedback.domain.dto.TaskOutput;
import com.ieltsmastermind.ai.feedback.domain.entity.AIInput;
import com.ieltsmastermind.ai.feedback.domain.entity.AIOutput;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeQuestionType;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeTopicTag;
import com.ieltsmastermind.practice.studyplan.management.domain.enums.LearnerStudyPlanFocusType;
import com.ieltsmastermind.practice.studyplan.management.domain.enums.LearnerStudyPlanTaskDirection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenAIClientTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private OpenAIClient openAIClient;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(openAIClient, "model", "gpt-4o-mini");
    }

    @Test
    void generateWithRetry_whenFirstAttemptSucceedsForListening_shouldCallAIValidateAndReturnResponse() {
        StudyPlanAIResponse expectedResponse = validResponse();
        String aiJson = openAiResponse(toJson(expectedResponse));

        configureSuccessfulWebClient(aiJson);

        StudyPlanAIResponse result = openAIClient.generateWithRetry(
                weaknesses(),
                strengths(),
                tasks(),
                false
        );

        assertThat(result).isNotNull();
        assertThat(result.getWeaknesses()).isEmpty();
        assertThat(result.getStrengths()).isEmpty();
        assertThat(result.getTasks()).hasSize(1);
        assertThat(result.getTasks().get(0).getId()).isEqualTo("task-1");
        assertThat(result.getTasks().get(0).getDescription()).isEqualTo("Task description.");

        ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
        verify(requestBodyUriSpec).bodyValue(bodyCaptor.capture());

        Map<String, Object> body = castMap(bodyCaptor.getValue());

        assertThat(body.get("model")).isEqualTo("gpt-4o-mini");
        assertThat(body.get("temperature")).isEqualTo(0.3);
        assertThat(body.get("max_tokens")).isEqualTo(2000);
        assertThat(castMap(body.get("response_format")).get("type")).isEqualTo("json_object");

        List<Map<String, Object>> messages = castList(body.get("messages"));
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).get("role")).isEqualTo("system");
        assertThat(messages.get(0).get("content")).isEqualTo("You are an IELTS coach.");
        assertThat(messages.get(1).get("role")).isEqualTo("user");

        String prompt = messages.get(1).get("content").toString();
        assertThat(prompt).contains("Your task is to analyze a learner's study plan");
        assertThat(prompt).contains("Use correctRate as evidence when relevant");
        assertThat(prompt).contains("OUTPUT FORMAT (STRICT JSON ONLY)");
        assertThat(prompt).contains("Do NOT change ids");
        assertThat(prompt).contains("Do NOT add text outside JSON");
        assertThat(prompt).contains("WEAKNESSES:");
        assertThat(prompt).contains("STRENGTHS:");
        assertThat(prompt).contains("TASKS:");
        assertThat(prompt).contains("weakness-1");
        assertThat(prompt).contains("strength-1");
        assertThat(prompt).contains("task-1");
        assertThat(prompt).doesNotContain("Use overallBandScore as evidence when relevant");

        verify(webClient).post();
        verify(requestBodyUriSpec).uri("/chat/completions");
        verify(responseSpec).bodyToMono(String.class);
    }

    @Test
    void generateWithRetry_whenFirstAttemptSucceedsForWriting_shouldBuildWritingPromptAndReturnResponse() {
        StudyPlanAIResponse expectedResponse = validResponse();

        configureSuccessfulWebClient(openAiResponse(toJson(expectedResponse)));

        StudyPlanAIResponse result = openAIClient.generateWithRetry(
                weaknesses(),
                strengths(),
                tasks(),
                true
        );

        assertThat(result).isNotNull();
        assertThat(result.getWeaknesses()).isEmpty();
        assertThat(result.getStrengths()).isEmpty();
        assertThat(result.getTasks()).hasSize(1);

        ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
        verify(requestBodyUriSpec).bodyValue(bodyCaptor.capture());

        Map<String, Object> body = castMap(bodyCaptor.getValue());
        List<Map<String, Object>> messages = castList(body.get("messages"));
        String prompt = messages.get(1).get("content").toString();

        assertThat(prompt).contains("Use overallBandScore as evidence when relevant");
        assertThat(prompt).contains("OUTPUT FORMAT (STRICT JSON ONLY)");
        assertThat(prompt).contains("weakness-1");
        assertThat(prompt).contains("strength-1");
        assertThat(prompt).contains("task-1");
        assertThat(prompt).doesNotContain("Use correctRate as evidence when relevant");
    }

    @Test
    void generateWithRetry_whenFirstTwoAttemptsFailAndThirdSucceeds_shouldRetryWithStrictJsonInstructionAndReturnResponse() {
        StudyPlanAIResponse expectedResponse = validResponse();

        configureSuccessfulWebClient(
                "not-json",
                openAiResponse("{\"weaknesses\":null,\"strengths\":[],\"tasks\":[]}"),
                openAiResponse(toJson(expectedResponse))
        );

        StudyPlanAIResponse result = openAIClient.generateWithRetry(
                weaknesses(),
                strengths(),
                tasks(),
                false
        );

        assertThat(result).isNotNull();
        assertThat(result.getWeaknesses()).isEmpty();
        assertThat(result.getTasks()).hasSize(1);

        ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
        verify(requestBodyUriSpec, times(3)).bodyValue(bodyCaptor.capture());

        List<Object> requestBodies = bodyCaptor.getAllValues();

        String firstPrompt = userPromptFromBody(requestBodies.get(0));
        String secondPrompt = userPromptFromBody(requestBodies.get(1));
        String thirdPrompt = userPromptFromBody(requestBodies.get(2));

        assertThat(firstPrompt).doesNotContain("CRITICAL: RETURN VALID JSON ONLY. NO EXTRA TEXT.");
        assertThat(secondPrompt).contains("CRITICAL: RETURN VALID JSON ONLY. NO EXTRA TEXT.");
        assertThat(thirdPrompt).contains("CRITICAL: RETURN VALID JSON ONLY. NO EXTRA TEXT.");
        assertThat(countOccurrences(thirdPrompt, "CRITICAL: RETURN VALID JSON ONLY. NO EXTRA TEXT.")).isEqualTo(2);

        verify(webClient, times(3)).post();
        verify(responseSpec, times(3)).bodyToMono(String.class);
    }

    @Test
    void generateWithRetry_whenAllAttemptsFail_shouldThrowRuntimeExceptionAfterThreeAttempts() {
        configureSuccessfulWebClient("not-json", "still-not-json", "bad-json-again");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> openAIClient.generateWithRetry(
                        weaknesses(),
                        strengths(),
                        tasks(),
                        false
                )
        );

        assertThat(exception.getMessage()).isEqualTo("AI failed after retries");
        assertThat(exception.getCause()).hasMessage("AI parse error");

        verify(webClient, times(3)).post();
        verify(responseSpec, times(3)).bodyToMono(String.class);
    }

    @Test
    void generateWithRetry_whenWebClientFails_shouldRetryThenThrowRuntimeException() {
        configureFailingWebClient(new RuntimeException("network failed"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> openAIClient.generateWithRetry(
                        weaknesses(),
                        strengths(),
                        tasks(),
                        false
                )
        );

        assertThat(exception.getMessage()).isEqualTo("AI failed after retries");
        assertThat(exception.getCause()).hasMessage("network failed");

        verify(webClient, times(3)).post();
        verify(responseSpec, times(3)).bodyToMono(String.class);
    }

    @Test
    void privateCallRawAI_whenWebClientSucceeds_shouldSendExpectedRequestBodyAndReturnRawResponse() {
        configureSuccessfulWebClient("raw-response");

        String result = invokePrivateMethod(
                "callRawAI",
                new Class<?>[]{String.class},
                "Prompt content."
        );

        assertThat(result).isEqualTo("raw-response");

        ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
        verify(requestBodyUriSpec).bodyValue(bodyCaptor.capture());

        Map<String, Object> body = castMap(bodyCaptor.getValue());
        List<Map<String, Object>> messages = castList(body.get("messages"));

        assertThat(body.get("model")).isEqualTo("gpt-4o-mini");
        assertThat(body.get("temperature")).isEqualTo(0.3);
        assertThat(body.get("max_tokens")).isEqualTo(2000);
        assertThat(castMap(body.get("response_format")).get("type")).isEqualTo("json_object");
        assertThat(messages.get(0).get("role")).isEqualTo("system");
        assertThat(messages.get(0).get("content")).isEqualTo("You are an IELTS coach.");
        assertThat(messages.get(1).get("role")).isEqualTo("user");
        assertThat(messages.get(1).get("content")).isEqualTo("Prompt content.");

        verify(webClient).post();
        verify(requestBodyUriSpec).uri("/chat/completions");
    }

    @Test
    void privateCallRawAI_whenResponseContainsTokenUsage_shouldParseUsageAndStillReturnRawResponse() {
        String response = openAiResponseWithUsage(toJson(validResponse()));
        configureSuccessfulWebClient(response);

        String result = invokePrivateMethod(
                "callRawAI",
                new Class<?>[]{String.class},
                "Prompt content."
        );

        assertThat(result).isEqualTo(response);

        verify(webClient).post();
        verify(responseSpec).bodyToMono(String.class);
    }

    @Test
    void privateParse_whenJsonIsValid_shouldReturnStudyPlanAIResponse() {
        StudyPlanAIResponse expectedResponse = validResponse();

        StudyPlanAIResponse result = invokePrivateMethod(
                "parse",
                new Class<?>[]{String.class},
                openAiResponse(toJson(expectedResponse))
        );

        assertThat(result).isNotNull();
        assertThat(result.getWeaknesses()).isEmpty();
        assertThat(result.getStrengths()).isEmpty();
        assertThat(result.getTasks()).hasSize(1);
        assertThat(result.getTasks().get(0).getDescription()).isEqualTo("Task description.");
    }

    @Test
    void privateParse_whenChoicesAreMissing_shouldThrowRuntimeException() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> invokePrivateMethod(
                        "parse",
                        new Class<?>[]{String.class},
                        "{}"
                )
        );

        assertThat(exception.getMessage()).isEqualTo("AI parse error");
        assertThat(exception.getCause()).hasMessage("Invalid AI response: no choices");
    }

    @Test
    void privateParse_whenChoicesAreEmpty_shouldThrowRuntimeException() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> invokePrivateMethod(
                        "parse",
                        new Class<?>[]{String.class},
                        "{\"choices\":[]}"
                )
        );

        assertThat(exception.getMessage()).isEqualTo("AI parse error");
        assertThat(exception.getCause()).hasMessage("Invalid AI response: no choices");
    }

    @Test
    void privateParse_whenContentIsInvalidJson_shouldThrowRuntimeException() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> invokePrivateMethod(
                        "parse",
                        new Class<?>[]{String.class},
                        openAiResponse("not-json")
                )
        );

        assertThat(exception.getMessage()).isEqualTo("AI parse error");
    }

    @Test
    void privateValidateAIResponse_whenResponseIsValid_shouldNotThrowException() {
        StudyPlanAIResponse response = validResponse();

        assertDoesNotThrow(() -> invokePrivateMethod(
                "validateAIResponse",
                new Class<?>[]{StudyPlanAIResponse.class},
                response
        ));
    }

    @Test
    void privateValidateAIResponse_whenResponseIsNull_shouldThrowRuntimeException() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> invokePrivateMethod(
                        "validateAIResponse",
                        new Class<?>[]{StudyPlanAIResponse.class},
                        new Object[]{null}
                )
        );

        assertThat(exception.getMessage()).isEqualTo("Response is null");
    }

    @Test
    void privateValidateAIResponse_whenWeaknessesAreNull_shouldThrowRuntimeException() {
        StudyPlanAIResponse response = validResponse();
        response.setWeaknesses(null);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> invokePrivateMethod(
                        "validateAIResponse",
                        new Class<?>[]{StudyPlanAIResponse.class},
                        response
                )
        );

        assertThat(exception.getMessage()).isEqualTo("Missing fields in AI response");
    }

    @Test
    void privateValidateAIResponse_whenStrengthsAreNull_shouldThrowRuntimeException() {
        StudyPlanAIResponse response = validResponse();
        response.setStrengths(null);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> invokePrivateMethod(
                        "validateAIResponse",
                        new Class<?>[]{StudyPlanAIResponse.class},
                        response
                )
        );

        assertThat(exception.getMessage()).isEqualTo("Missing fields in AI response");
    }

    @Test
    void privateValidateAIResponse_whenTasksAreNull_shouldThrowRuntimeException() {
        StudyPlanAIResponse response = validResponse();
        response.setTasks(null);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> invokePrivateMethod(
                        "validateAIResponse",
                        new Class<?>[]{StudyPlanAIResponse.class},
                        response
                )
        );

        assertThat(exception.getMessage()).isEqualTo("Missing fields in AI response");
    }

    @Test
    void privateValidateAIResponse_whenWeaknessIdIsNull_shouldThrowRuntimeException() {
        StudyPlanAIResponse response = validResponse();
        response.setWeaknesses(List.of(aiOutput(
                null,
                "Weakness description.",
                "Weakness explanation.",
                "Weakness evidence.",
                "Weakness recommendation."
        )));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> invokePrivateMethod(
                        "validateAIResponse",
                        new Class<?>[]{StudyPlanAIResponse.class},
                        response
                )
        );

        assertThat(exception.getMessage()).isEqualTo("Weakness missing id");
    }

    @Test
    void privateValidateAIResponse_whenTaskIdIsNull_shouldThrowRuntimeException() {
        StudyPlanAIResponse response = validResponse();
        TaskOutput taskOutput = taskOutput(null, "Task description.");
        response.setTasks(List.of(taskOutput));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> invokePrivateMethod(
                        "validateAIResponse",
                        new Class<?>[]{StudyPlanAIResponse.class},
                        response
                )
        );

        assertThat(exception.getMessage()).isEqualTo("Invalid task output");
    }

    @Test
    void privateValidateAIResponse_whenTaskDescriptionIsNull_shouldThrowRuntimeException() {
        StudyPlanAIResponse response = validResponse();
        TaskOutput taskOutput = taskOutput("task-1", null);
        response.setTasks(List.of(taskOutput));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> invokePrivateMethod(
                        "validateAIResponse",
                        new Class<?>[]{StudyPlanAIResponse.class},
                        response
                )
        );

        assertThat(exception.getMessage()).isEqualTo("Invalid task output");
    }

    @Test
    void privateBuildFullPrompt_whenIsWritingFalse_shouldUseCorrectRateMetricAndSerializeInputs() {
        String prompt = invokePrivateMethod(
                "buildFullPrompt",
                new Class<?>[]{List.class, List.class, List.class, boolean.class},
                weaknesses(),
                strengths(),
                tasks(),
                false
        );

        assertThat(prompt).contains("Use correctRate as evidence when relevant");
        assertThat(prompt).contains("weakness-1");
        assertThat(prompt).contains("strength-1");
        assertThat(prompt).contains("task-1");
        assertThat(prompt).contains("WEAKNESSES:");
        assertThat(prompt).contains("STRENGTHS:");
        assertThat(prompt).contains("TASKS:");
        assertThat(prompt).contains("OUTPUT FORMAT (STRICT JSON ONLY)");
        assertThat(prompt).contains("Do NOT change ids");
        assertThat(prompt).contains("Do NOT skip any item");
        assertThat(prompt).doesNotContain("Use overallBandScore as evidence when relevant");
    }

    @Test
    void privateBuildFullPrompt_whenIsWritingTrue_shouldUseOverallBandScoreMetricAndSerializeInputs() {
        String prompt = invokePrivateMethod(
                "buildFullPrompt",
                new Class<?>[]{List.class, List.class, List.class, boolean.class},
                weaknesses(),
                strengths(),
                tasks(),
                true
        );

        assertThat(prompt).contains("Use overallBandScore as evidence when relevant");
        assertThat(prompt).contains("weakness-1");
        assertThat(prompt).contains("strength-1");
        assertThat(prompt).contains("task-1");
        assertThat(prompt).contains("WEAKNESSES:");
        assertThat(prompt).contains("STRENGTHS:");
        assertThat(prompt).contains("TASKS:");
        assertThat(prompt).doesNotContain("Use correctRate as evidence when relevant");
    }

    private List<AIInput> weaknesses() {
        return List.of(new AIInput(
                "weakness-1",
                "WEAKNESS",
                LearnerStudyPlanFocusType.QUESTION_TYPE,
                PracticeQuestionType.MULTIPLE_CHOICE,
                null,
                45.0
        ));
    }

    private List<AIInput> strengths() {
        return List.of(new AIInput(
                "strength-1",
                "STRENGTH",
                LearnerStudyPlanFocusType.TOPIC,
                null,
                PracticeTopicTag.EDUCATION_AND_LEARNING,
                85.0
        ));
    }

    private List<TaskInput> tasks() {
        return List.of(new TaskInput(
                "task-1",
                LearnerStudyPlanFocusType.QUESTION_TYPE,
                PracticeQuestionType.MULTIPLE_CHOICE,
                null,
                LearnerStudyPlanTaskDirection.INCREASE
        ));
    }

    private StudyPlanAIResponse validResponse() {
        StudyPlanAIResponse response = new StudyPlanAIResponse();

        response.setWeaknesses(List.of());
        response.setStrengths(List.of());
        response.setTasks(List.of(taskOutput("task-1", "Task description.")));

        return response;
    }

    private AIOutput aiOutput(String id,
                              String description,
                              String explanation,
                              String evidence,
                              String recommendation) {
        return new AIOutput(id, description, explanation, evidence, recommendation);
    }

    private TaskOutput taskOutput(String id, String description) {
        TaskOutput taskOutput = new TaskOutput();

        taskOutput.setId(id);
        taskOutput.setDescription(description);

        return taskOutput;
    }

    private String openAiResponse(String contentJson) {
        return "{\"choices\":[{\"message\":{\"content\":\""
                + contentJson
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                + "\"}}]}";
    }

    private String openAiResponseWithUsage(String contentJson) {
        return "{\"choices\":[{\"message\":{\"content\":\""
                + contentJson
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                + "\"}}],\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":20,\"total_tokens\":30}}";
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void configureSuccessfulWebClient(String... responseBodies) {
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/chat/completions")).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        Mono<String>[] monos = java.util.Arrays.stream(responseBodies)
                .map(Mono::just)
                .toArray(Mono[]::new);

        when(responseSpec.bodyToMono(String.class))
                .thenReturn(monos[0], java.util.Arrays.copyOfRange(monos, 1, monos.length));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void configureFailingWebClient(RuntimeException exception) {
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/chat/completions")).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(
                Mono.error(exception),
                Mono.error(exception),
                Mono.error(exception)
        );
    }

    private String userPromptFromBody(Object requestBody) {
        Map<String, Object> body = castMap(requestBody);
        List<Map<String, Object>> messages = castList(body.get("messages"));

        return messages.get(1).get("content").toString();
    }

    private int countOccurrences(String text, String target) {
        int count = 0;
        int index = 0;

        while ((index = text.indexOf(target, index)) != -1) {
            count++;
            index += target.length();
        }

        return count;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object value) {
        return (List<Map<String, Object>>) value;
    }

    @SuppressWarnings("unchecked")
    private <T> T invokePrivateMethod(String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = OpenAIClient.class.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return (T) method.invoke(openAIClient, args);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();

            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }

            throw new RuntimeException("Failed to invoke " + methodName, cause);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to invoke " + methodName, exception);
        }
    }
}
