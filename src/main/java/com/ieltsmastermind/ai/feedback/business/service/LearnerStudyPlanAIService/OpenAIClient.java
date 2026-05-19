package com.ieltsmastermind.ai.feedback.business.service.LearnerStudyPlanAIService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltsmastermind.ai.feedback.domain.dto.StudyPlanAIResponse;
import com.ieltsmastermind.ai.feedback.domain.dto.TaskInput;
import com.ieltsmastermind.ai.feedback.domain.dto.TaskOutput;
import com.ieltsmastermind.ai.feedback.domain.entity.AIInput;
import com.ieltsmastermind.ai.feedback.domain.entity.AIOutput;
import com.ieltsmastermind.practice.studyplan.management.domain.enums.LearnerStudyPlanFocusType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class OpenAIClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private static final Logger log = LoggerFactory.getLogger(OpenAIClient.class);
    @Value("${openai.model}")
    private String model;

    private String callRawAI(String prompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", "You are an IELTS coach."),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.3,
                "max_tokens", 2000,
                "response_format", Map.of("type", "json_object")
        );

        String response = webClient.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(30));

        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode usage = root.path("usage");

            log.info("========== TOKEN USAGE ==========");
            log.info("Request Tokens (Prompt): {}", usage.path("prompt_tokens").asInt());
            log.info("Response Tokens (Completion): {}", usage.path("completion_tokens").asInt());
            log.info("Total Tokens: {}", usage.path("total_tokens").asInt());
            log.info("=================================");

        } catch (Exception e) {
            log.warn("Cannot parse token usage");
        }

        return response;
    }

    private StudyPlanAIResponse parse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);

            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new RuntimeException("Invalid AI response: no choices");
            }

            String content = choices.get(0)
                    .path("message")
                    .path("content")
                    .asText();



            return objectMapper.readValue(content, StudyPlanAIResponse.class);

        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", json);
            throw new RuntimeException("AI parse error", e);
        }
    }


    public StudyPlanAIResponse generateWithRetry(
            List<AIInput> weaknesses,
            List<AIInput> strengths,
            List<TaskInput> tasks,
            Boolean isWriting
    ) {
        int maxAttempts = 3;
        int attempt = 0;

        String prompt = buildFullPrompt(weaknesses, strengths, tasks, isWriting);

        while (attempt < maxAttempts) {
            try {
                attempt++;

                String raw = callRawAI(prompt);

                log.info("AI attempt {} success", attempt);
                log.debug("AI PROMPT:\n{}", prompt);
                StudyPlanAIResponse parsed = parse(raw);

                validateAIResponse(parsed);

                return parsed;

            } catch (Exception e) {
                log.warn("AI failed attempt {}: {}", attempt, e.getMessage());

                if (attempt == maxAttempts) {
                    log.error("AI failed after {} attempts", maxAttempts, e);
                    throw new RuntimeException("AI failed after retries", e);
                }

                prompt += "\n\nCRITICAL: RETURN VALID JSON ONLY. NO EXTRA TEXT.";
            }
        }

        throw new RuntimeException("Unexpected AI error");
    }

    private void validateAIResponse(StudyPlanAIResponse res) {

        if (res == null) {
            throw new RuntimeException("Response is null");
        }

        if (res.getWeaknesses() == null || res.getStrengths() == null || res.getTasks() == null) {
            throw new RuntimeException("Missing fields in AI response");
        }

        for (AIOutput w : res.getWeaknesses()) {
            if (w.getId() == null) {
                throw new RuntimeException("Weakness missing id");
            }
        }

        for (TaskOutput t : res.getTasks()) {
            if (t.getId() == null || t.getDescription() == null) {
                throw new RuntimeException("Invalid task output");
            }
        }
    }


    private String buildFullPrompt(
            List<AIInput> weaknesses,
            List<AIInput> strengths,
            List<TaskInput> tasks,
            boolean isWriting
    ) {
        try {

            String metricName = isWriting ? "overallBandScore" : "correctRate";

            String prompt = """
You are an IELTS coach.

Your task is to analyze a learner's study plan and return structured JSON feedback.

INPUT:
- weaknesses: areas learner struggles
- strengths: areas learner performs well
- tasks: practice activities with learning trend (INCREASE, REDUCE)

RULES:
- Keep responses concise, specific, and actionable
- Use %s as evidence when relevant
- Adjust task descriptions based on trend:
  - IMPROVING → increase difficulty
  - DECLINING → reinforce fundamentals
  - STABLE → maintain and optimize

OUTPUT FORMAT (STRICT JSON ONLY):
{
  "weaknesses": [
    {
      "id": "...",
      "description": "...",
      "explanation": "...",
      "evidence": "...",
      "recommendation": "..."
    }
  ],
  "strengths": [
    {
      "id": "...",
      "description": "...",
      "explanation": "...",
      "evidence": "...",
      "recommendation": "..."
    }
  ],
  "tasks": [
    {
      "id": "...",
      "description": "..."
    }
  ]
}

CONSTRAINTS:
- Do NOT change ids
- Do NOT skip any item
- Do NOT add text outside JSON

DATA:
WEAKNESSES: %s
STRENGTHS: %s
TASKS: %s
""".formatted(
                    metricName,
                    objectMapper.writeValueAsString(weaknesses),
                    objectMapper.writeValueAsString(strengths),
                    objectMapper.writeValueAsString(tasks)
            );

            return prompt;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}