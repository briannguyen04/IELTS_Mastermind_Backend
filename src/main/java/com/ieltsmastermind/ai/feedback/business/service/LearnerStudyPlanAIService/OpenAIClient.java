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

    private String callRawAI(String prompt) {
        Map<String, Object> body = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                        Map.of("role", "system", "content", "You are an IELTS coach."),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.3,
                "response_format", Map.of("type", "json_object")
        );

        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(30));
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
            List<TaskInput> tasks
    ) {
        int maxAttempts = 3;
        int attempt = 0;

        String prompt = buildFullPrompt(weaknesses, strengths, tasks);

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
            List<TaskInput> tasks
    ) {
        try {
            String prompt = """
You are a senior IELTS coach and learning strategist.

You are working inside an AI-powered IELTS training platform.

Your role is to analyze a learner's study plan and generate structured feedback.

====================================
SYSTEM CONTEXT (IMPORTANT)
====================================

The platform tracks learner performance across:

- QUESTION TYPES (e.g. MATCHING, SENTENCE_COMPLETION, MAP_LABELING)
- TOPIC TAGS (e.g. EDUCATION, ENVIRONMENT, TECHNOLOGY)

Each learner has:

- Weakness blocks → areas they struggle with
- Strength blocks → areas they perform well
- Tasks → practice activities assigned to improve performance

====================================
INPUT DATA EXPLANATION
====================================

You will receive 3 datasets:

1. WEAKNESSES
Each item contains:
- id: unique identifier (MUST be preserved)
- focusType: QUESTION_TYPE or TOPIC
- questionType / topicTag
- correctRate: ratio of (correct answers / total questions attempted)
- effectiveAccuracy: ratio of (correct answers / non-skipped questions only)
- skipRate: percentage of skipped questions

Interpretation:
- Low correctRate → overall weak understanding
- High skipRate → avoidance, confusion, or lack of confidence
- Low effectiveAccuracy → inconsistent performance even when attempting questions

------------------------------------

2. STRENGTHS
Same structure as weaknesses.

Interpretation:
- High correctRate → strong overall mastery
- High effectiveAccuracy → stable and reliable performance
- Low skipRate → confident engagement

------------------------------------

3. TASKS
Each item contains:
- id: unique identifier (MUST be preserved)
- focusType
- questionType / topicTag
- direction: learning trend of the learner in this area

IMPORTANT ABOUT "direction":
- Indicates learning progression trend over time
- Possible meanings:
  - IMPROVING → learner is getting better
  - DECLINING → performance is getting worse
  - STABLE → performance is consistent
- You MUST use this to adjust task difficulty and focus
- Example:
  - IMPROVING → increase difficulty gradually
  - DECLINING → reinforce fundamentals first
  - STABLE → maintain + optimize performance

====================================
YOUR GOALS
====================================

1. WEAKNESSES
For each weakness:
- Identify the core issue
- Explain WHY the learner struggles
- Use correctRate, effectiveAccuracy, skipRate as evidence
- Provide actionable improvement strategy

------------------------------------

2. STRENGTHS
For each strength:
- Explain WHY the learner performs well
- Reinforce successful learning behaviors
- Suggest how to maintain or further leverage this strength

------------------------------------

3. TASKS (CRITICAL)
For each task:
- Generate a clear and concise description based ONLY on the provided data

====================================
WRITING STYLE RULES
====================================

- Be concise but insightful
- Avoid generic advice
- Use educational and analytical tone
- Focus on actionable feedback
- No filler sentences

====================================
STRICT OUTPUT FORMAT (MUST FOLLOW)
====================================

Return ONLY valid JSON:

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

====================================
CRITICAL CONSTRAINTS
====================================

- DO NOT change any id
- DO NOT skip any item
- DO NOT add extra text outside JSON
- DO NOT hallucinate missing fields
- ALWAYS use provided data exactly
- MUST follow JSON format strictly

====================================
INPUT DATA
====================================

WEAKNESSES:
%s

STRENGTHS:
%s

TASKS:
%s
""".formatted(
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