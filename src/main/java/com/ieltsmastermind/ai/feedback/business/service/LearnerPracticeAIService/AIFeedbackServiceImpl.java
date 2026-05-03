package com.ieltsmastermind.ai.feedback.business.service.LearnerPracticeAIService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltsmastermind.ai.feedback.domain.dto.WritingAnswerFeedbackDto;
import com.ieltsmastermind.ai.feedback.domain.dto.WritingFeedbackResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.ieltsmastermind.common.constants.FileStorageConstants.IMAGE_PUBLIC_BASE_PATH;

@Service
public class AIFeedbackServiceImpl {

    @Autowired
    private WebClient webClient;

    @Value("${app.ngrok-url}")
    private String backendUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();


    public  String callAI(String prompt) {

        Map<String, Object> body = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                        Map.of("role", "system", "content", "You are an IELTS examiner."),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.2,
                "response_format", Map.of("type", "json_object")
        );

        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        res -> res.bodyToMono(String.class)
                                .map(err -> new RuntimeException("AI error: " + err))
                )
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(80));
    }

    public String callAIWithImages(String prompt, List<String> imageUrls) {

        List<Map<String, Object>> content = new ArrayList<>();

        // text
        content.add(Map.of(
                "type", "text",
                "text", prompt
        ));

        // images
        for (String url : imageUrls) {
            content.add(Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", url)
            ));
        }

        Map<String, Object> message = Map.of(
                "role", "user",
                "content", content
        );

        Map<String, Object> body = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                        Map.of("role", "system", "content", "You are an IELTS examiner."),
                        message
                ),
                "temperature", 0.2,
                "response_format", Map.of("type", "json_object")
        );

        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        res -> res.bodyToMono(String.class)
                                .map(err -> new RuntimeException("AI error: " + err))
                )
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(80));
    }


    public <T> T parseResponse(String json, Class<T> clazz) {
        try {
            JsonNode root = objectMapper.readTree(json);

            String content = root
                    .path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();

            JsonNode actualJson = objectMapper.readTree(content);

            return objectMapper.treeToValue(actualJson, clazz);

        } catch (Exception e) {
            throw new RuntimeException("Parse error for JSON:\n" + json, e);
        }
    }

    public WritingFeedbackResponseDto parseWritingResponse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);

            String content = root
                    .path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();

            System.out.println("===== AI CONTENT =====");
            System.out.println(content);

            JsonNode actualJson = objectMapper.readTree(content);

            WritingFeedbackResponseDto response =
                    objectMapper.treeToValue(actualJson, WritingFeedbackResponseDto.class);

            if (response.getAnswers() == null || response.getAnswers().isEmpty()) {
                throw new RuntimeException("AI returned empty answers");
            }

            for (WritingAnswerFeedbackDto answer : response.getAnswers()) {

                if (answer.getFeedbacks() == null ) {
                    throw new RuntimeException(
                            "Invalid feedback size: " +
                                    (answer.getFeedbacks() == null ? 0 : answer.getFeedbacks().size())
                    );
                }
            }
            return response;

        } catch (Exception e) {
            throw new RuntimeException("Parse WRITING error:\n" + json, e);
        }
    }

    public  String extractJson(String raw) {
        int start = raw.indexOf("{");
        int end = raw.lastIndexOf("}");

        if (start == -1 || end == -1) {
            throw new RuntimeException("Invalid JSON format");
        }

        return raw.substring(start, end + 1);
    }

    public  String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public List<String> extractImages(String instruction) {
        Pattern pattern = Pattern.compile("\\[img\\s+src=\"(.*?)\".*?\\]");
        Matcher matcher = pattern.matcher(instruction);

        List<String> images = new ArrayList<>();
        while (matcher.find()) {
            images.add(matcher.group(1));
        }
        return images;
    }

    public List<String> toPublicUrls(List<String> localPaths) {
        return localPaths.stream()
                .map(path -> {
                    String fileName = path.substring(path.lastIndexOf("/") + 1);
                    return backendUrl + IMAGE_PUBLIC_BASE_PATH + fileName;
                })
                .toList();
    }

    public String stripCustomTags(String input) {
        return input
                .replaceAll("\\[/?f\\]", "")
                .replaceAll("\\[/?table.*?\\]", "")
                .replaceAll("\\[/?row.*?\\]", "")
                .replaceAll("\\[/?cell.*?\\]", "")
                .replaceAll("\\[/?multiple-choice.*?\\]", "")
                .replaceAll("\\[/?option.*?\\]", "")
                .replaceAll("\\[img.*?\\]", "[Image provided]");
    }

    public String normalizeInstruction(String raw) {

        String text = raw;

        // 1. Remove style tags nhưng giữ content
        text = text.replaceAll("\\[f[^\\]]*\\]", ""); // remove [f ...]
        text = text.replaceAll("\\[/f\\]", "");

        // 2. Handle structure tags → xuống dòng
        text = text.replaceAll("\\[table\\]", "\n");
        text = text.replaceAll("\\[/table\\]", "\n");

        text = text.replaceAll("\\[row\\]", "\n");
        text = text.replaceAll("\\[/row\\]", "");

        text = text.replaceAll("\\[cell\\]", "");
        text = text.replaceAll("\\[/cell\\]", " ");

        // 3. Multiple choice
        text = text.replaceAll("\\[multiple-choice[^\\]]*\\]", "\n");
        text = text.replaceAll("\\[/multiple-choice\\]", "\n");

        // option → mỗi cái 1 dòng
        text = text.replaceAll("\\[option[^\\]]*\\]", "\n- ");
        text = text.replaceAll("\\[/option\\]", "");

        // 4. Image
        text = text.replaceAll("\\[img[^\\]]*\\]", "\nRefer to the image.\n");

        // 5. Remove gap
        text = text.replaceAll("\\[gap:\\d+\\]", "_____");

        // 6. Fix encoding lỗi
        text = text.replace("?", "–");

        // 7. Normalize newline
        text = text.replaceAll("\\n+", "\n");

        // 8. Trim
        return text.trim();
    }

    public String callAIUnified(String prompt, List<String> publicUrls) {
        if (publicUrls == null || publicUrls.isEmpty()) {
            return callAI(prompt);
        }
        return callAIWithImages(prompt, publicUrls);
    }


}