package com.ieltsmastermind.ai.feedback.business.service.LearnerPracticeAIService;

import com.ieltsmastermind.ai.feedback.business.interfaces.WritingFeedbackService;
import com.ieltsmastermind.ai.feedback.domain.dto.*;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeSubmission;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeWritingAnswer;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeWritingCriterionFeedback;
import com.ieltsmastermind.practice.attempt.management.domain.enums.*;
import com.ieltsmastermind.practice.attempt.management.persistence.UserPracticeSubmissionRepository;
import com.ieltsmastermind.practice.attempt.management.persistence.UserPracticeWritingAnswerRepository;
import com.ieltsmastermind.practice.attempt.management.persistence.UserPracticeWritingCriterionFeedbackRepository;
import com.ieltsmastermind.practice.content.management.domain.entity.ListeningPracticeContent;
import com.ieltsmastermind.practice.content.management.domain.entity.PracticeContent;
import com.ieltsmastermind.practice.content.management.domain.entity.ReadingPracticeContent;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class WritingFeedbackServicelmpl implements WritingFeedbackService {
    @Autowired
    private AIFeedbackServiceImpl aiService;

    @Autowired
    private UserPracticeSubmissionRepository submissionRepository;

    @Autowired
    private UserPracticeWritingAnswerRepository userPracticeWritingAnswerRepository;

    @Autowired
    private UserPracticeWritingCriterionFeedbackRepository userPracticeWritingCriterionFeedbackRepository;

    private static final Logger log = LoggerFactory.getLogger(WritingFeedbackServicelmpl.class);

    @Override
    @Transactional
    public void createWritingFeedback(String submissionId) {

        // 1. Lấy tất cả answers
        List<UserPracticeWritingAnswer> answers =
                userPracticeWritingAnswerRepository
                        .findAllBySubmission_IdOrderByOrderIndexAsc(submissionId);

        if (answers.isEmpty()) {
            throw new RuntimeException("No writing answers found");
        }

        // 2. Check đã có feedback chưa
        boolean alreadyHasFeedback = answers.stream().allMatch(answer ->
                answer.getCriterionFeedbacks() != null &&
                        !answer.getCriterionFeedbacks().isEmpty()
        );

        // 3. Nếu đã đủ → skip
        if (alreadyHasFeedback) {
            return;
        }


        // 5. Get submission
        UserPracticeSubmission submission = submissionRepository.findWithContent(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        PracticeContent content = submission.getPracticeContent();

        if (content instanceof ReadingPracticeContent ||
                content instanceof ListeningPracticeContent) {
            throw new IllegalArgumentException("Invalid content type for writing");
        }

        String contentId = submission.getPracticeContentId();


        // 6. Prompt
        String cleanInstruction = aiService.stripCustomTags(content.getInstructions());

        String prompt = buildWritingPrompt(
                content.getTitle(),
                cleanInstruction,
                answers,
                submission.getTimeSpentSeconds()
        );

        List<String> images = aiService.extractImages(content.getInstructions());
        List<String> publicUrls = aiService.toPublicUrls(images);


        WritingFeedbackResponseDto response = callAIWithRetry(prompt, publicUrls);

        saveWritingFeedback(submissionId, response);

    }

    // ================= SAVE =================

    private void saveWritingFeedback(String submissionId,
                                     WritingFeedbackResponseDto response) {

        // 1. Lấy answers từ DB
        List<UserPracticeWritingAnswer> answers =
                userPracticeWritingAnswerRepository
                        .findAllBySubmission_IdOrderByOrderIndexAsc(submissionId);

        if (answers.isEmpty()) {
            throw new RuntimeException("No writing answers found");
        }

        // 2. Map orderIndex → answer
        Map<Integer, UserPracticeWritingAnswer> answerMap =
                answers.stream()
                        .collect(Collectors.toMap(
                                UserPracticeWritingAnswer::getOrderIndex,
                                a -> a
                        ));
        // 3. Loop từng answer DTO từ AI
        for (WritingAnswerFeedbackDto answerDto : response.getAnswers()) {

            UserPracticeWritingAnswer answer =
                    answerMap.get(answerDto.getOrderIndex());

            if (answer == null) {
                throw new RuntimeException(
                        "Answer not found for orderIndex=" + answerDto.getOrderIndex()
                );
            }

            List<WritingFeedbackItemDto> items = answerDto.getFeedbacks();

            if (items == null || items.isEmpty()) {
                throw new RuntimeException("AI returned empty feedback");
            }

            // clear old (orphanRemoval)
            answer.getCriterionFeedbacks().clear();

            // 4. Map từng item → entity
            for (WritingFeedbackItemDto item : items) {

                UserPracticeWritingCriterionFeedback entity =
                        new UserPracticeWritingCriterionFeedback();

                entity.setAuthorType(FeedbackAuthorType.AI);

                try {
                    entity.setCriterionName(
                            WritingCriterionName.valueOf(item.getCriterionName())
                    );

                    entity.setFeedbackType(
                            WritingFeedbackType.valueOf(item.getFeedbackType())
                    );

                    entity.setLabel(
                            WritingFeedbackLabel.valueOf(normalizeEnum(item.getLabel()))
                    );

                } catch (Exception e) {
                    throw new RuntimeException(
                            "Invalid enum from AI: criterionName=" + item.getCriterionName()
                                    + ", feedbackType=" + item.getFeedbackType()
                                    + ", label=" + item.getLabel(),
                            e
                    );
                }

                entity.setDescription(item.getDescription());
                entity.setExplanation(item.getExplanation());
                entity.setEvidenceSentences(item.getEvidenceSentences());

                entity.setRecommendedActionDescription(item.getRecommendedActionDescription());
                entity.setRecommendedActionExplanation(item.getRecommendedActionExplanation());

                entity.setWritingAnswer(answer);
                userPracticeWritingCriterionFeedbackRepository.save(entity);
            }

        }
    }




    @Override
    @Transactional
    public WritingFeedbackResponseDto getWritingFeedback(String submissionId) {

        // 1. Lấy answers
        List<UserPracticeWritingAnswer> answers =
                userPracticeWritingAnswerRepository
                        .findAllBySubmission_IdOrderByOrderIndexAsc(submissionId);

        if (answers.isEmpty()) {
            throw new RuntimeException("No writing answers found");
        }

        List<WritingAnswerFeedbackDto> answerDtos = new ArrayList<>();

        // 2. Loop từng answer
        for (UserPracticeWritingAnswer answer : answers) {

            List<UserPracticeWritingCriterionFeedback> feedbacks = answer.getCriterionFeedbacks();

            if (feedbacks.isEmpty()) {
                throw new RuntimeException(
                        "Feedback not generated for answer orderIndex=" + answer.getOrderIndex()
                );
            }

            // sort lại cho chắc (32 item đúng thứ tự)
            List<UserPracticeWritingCriterionFeedback> sortedFeedbacks =
                    feedbacks.stream()
                            .sorted(Comparator
                                    .comparing((UserPracticeWritingCriterionFeedback fb) ->
                                            getCriterionOrder(fb.getCriterionName()))
                                    .thenComparing(fb ->
                                            fb.getFeedbackType() == WritingFeedbackType.STRENGTH ? 0 : 1)
                            )
                            .toList();

            List<WritingFeedbackItemDto> feedbackDtos = new ArrayList<>();

            for (UserPracticeWritingCriterionFeedback fb : sortedFeedbacks) {

                WritingFeedbackItemDto dto = new WritingFeedbackItemDto();

                dto.setCriterionName(fb.getCriterionName().name());
                dto.setFeedbackType(fb.getFeedbackType().name());
                dto.setLabel(fb.getLabel().name());

                dto.setDescription(fb.getDescription());
                dto.setExplanation(fb.getExplanation());
                dto.setEvidenceSentences(fb.getEvidenceSentences());

                dto.setRecommendedActionDescription(fb.getRecommendedActionDescription());
                dto.setRecommendedActionExplanation(fb.getRecommendedActionExplanation());

                feedbackDtos.add(dto);
            }

            WritingAnswerFeedbackDto answerDto = new WritingAnswerFeedbackDto();
            answerDto.setOrderIndex(answer.getOrderIndex());
            answerDto.setFeedbacks(feedbackDtos);

            answerDtos.add(answerDto);
        }

        // 3. wrap response
        WritingFeedbackResponseDto response = new WritingFeedbackResponseDto();
        response.setAnswers(answerDtos);

        return response;
    }

    // ================= PROMPT =================

    private String buildWritingPrompt(String title,
                                      String instruction,
                                      List<UserPracticeWritingAnswer> answers,
                                      int timeSpentSeconds
    ) {


        String essaysJson = aiService.toJson(
                answers.stream().map(a -> Map.of(
                        "orderIndex", a.getOrderIndex(),
                        "taskType", a.getSubmission()
                                .getPracticeContent()
                                .getTask()
                                .name(),
                        "essayText", a.getEssayText(),
                        "wordCount", a.getWordCount()
                )).toList()
        );


        return """
You are an IELTS Writing examiner.

Return STRICT JSON ONLY (no extra text):

{
  "answers": [
    {
      "orderIndex": 1,
      "feedbacks": [
        {
          "criterionName": "...",
          "feedbackType": "STRENGTH|WEAKNESS",
          "label": "...",
          "description": "...",
          "explanation": "...",
          "evidenceSentences": ["..."],
          "recommendedActionDescription": "...",
          "recommendedActionExplanation": "..."
        }
      ]
    }
  ]
}

========================
CRITICAL RULES
========================

1. TASK TYPE:
- TASK_1 → use TASK_ACHIEVEMENT only
- TASK_2 → use TASK_RESPONSE only
- NEVER mix

2. CRITERION ENUM (EXACT MATCH):
- TASK_RESPONSE
- TASK_ACHIEVEMENT
- COHERENCE_AND_COHESION
- LEXICAL_RESOURCE
- GRAMMATICAL_RANGE_AND_ACCURACY

3. STRUCTURE (STRICT ORDER):
Per answer → up to 32 feedbacks:

- First 8: TASK (TASK_1 → TASK_ACHIEVEMENT, TASK_2 → TASK_RESPONSE)
- Next 8: COHERENCE_AND_COHESION
- Next 8: LEXICAL_RESOURCE
- Last 8: GRAMMATICAL_RANGE_AND_ACCURACY

Each group:
- First 4 → STRENGTH
- Last 4 → WEAKNESS

4. LABEL RULE:
- MUST use EXACT labels below
- STRENGTH → left side
- WEAKNESS → right side

TASK:
FULL_TASK_COVERAGE|PARTIAL_TASK_COVERAGE
CLEAR_POSITION|UNCLEAR_POSITION
CONSISTENT_POSITION|INCONSISTENT_POSITION
RELEVANT_IDEAS|IRRELEVANT_IDEAS
GOOD_IDEA_DEVELOPMENT|INSUFFICIENT_IDEA_DEVELOPMENT
ADEQUATE_SUPPORT|INSUFFICIENT_SUPPORT
APPROPRIATE_FORMAT|INAPPROPRIATE_FORMAT
SUFFICIENT_LENGTH|UNDER_LENGTH_RESPONSE

TASK 1 ONLY:
CLEAR_OVERVIEW|MISSING_OVERVIEW
GOOD_KEY_FEATURE_SELECTION|MISSING_KEY_FEATURES
GOOD_DATA_SUPPORT|INSUFFICIENT_DATA_SUPPORT

TASK 2 ONLY:
CLEAR_CONCLUSION|WEAK_CONCLUSION

COHERENCE:
LOGICAL_ORGANISATION|WEAK_ORGANISATION
CLEAR_PROGRESSION|UNCLEAR_PROGRESSION
EFFECTIVE_PARAGRAPHING|WEAK_PARAGRAPHING
EFFECTIVE_COHESIVE_DEVICES|MISUSED_COHESIVE_DEVICES
NATURAL_LINKER_USE|OVERUSE_OF_LINKERS
GOOD_REFERENCE_USE|UNCLEAR_REFERENCING
MINIMAL_REPETITION|REPETITION
CLEAR_PARAGRAPH_FOCUS|UNCLEAR_PARAGRAPH_FOCUS

LEXICAL:
VARIED_VOCABULARY|LIMITED_VOCABULARY
PRECISE_VOCABULARY|IMPRECISE_VOCABULARY
APPROPRIATE_WORD_CHOICE|INAPPROPRIATE_WORD_CHOICE
GOOD_COLLOCATION|WEAK_COLLOCATION
APPROPRIATE_STYLE|STYLE_INAPPROPRIATE
GOOD_SPELLING|SPELLING_ERRORS
GOOD_WORD_FORMATION|WORD_FORMATION_ERRORS
NATURAL_LANGUAGE_USE|MEMORISED_LANGUAGE_OVERUSE

GRAMMAR:
VARIED_SENTENCE_STRUCTURES|LIMITED_GRAMMATICAL_RANGE
GOOD_COMPLEX_STRUCTURE_USE|FAULTY_COMPLEX_SENTENCES
GRAMMAR_WELL_CONTROLLED|GRAMMAR_ERRORS
GOOD_PUNCTUATION|PUNCTUATION_ERRORS
WELL_FORMED_SENTENCES|SENTENCE_FRAGMENT_OR_RUN_ON

5. EVIDENCE:
- MUST be exact sentences from essay
- NO paraphrase

6. LOW QUALITY ESSAY:
- May return fewer items
- Focus on major weaknesses only
- DO NOT fabricate strengths

7. GENERAL:
- Keep same orderIndex
- Do NOT change ids/labels
- No hallucination
- No extra text outside JSON

========================
INPUT
========================

Title: %s
Instruction: %s
TimeSpent: %d

Essays:
%s
""".formatted(
                title,
                aiService.normalizeInstruction(instruction),
                timeSpentSeconds,
                essaysJson
        );
    }


    private int getCriterionOrder(WritingCriterionName name) {
        return switch (name) {
            case TASK_RESPONSE, TASK_ACHIEVEMENT -> 0;
            case COHERENCE_AND_COHESION -> 1;
            case LEXICAL_RESOURCE -> 2;
            case GRAMMATICAL_RANGE_AND_ACCURACY -> 3;
        };
    }

    private String normalizeEnum(String value) {
        if (value == null) return null;

        return value
                .trim()
                .toUpperCase()
                .replace(" ", "_")
                .replace("-", "_")
                .replace("__", "_");
    }

    private WritingFeedbackResponseDto callAIWithRetry(String prompt, List<String> publicUrls) {

        int maxAttempts = 3;
        int attempt = 0;
        String retryPrompt = prompt;
        while (attempt < maxAttempts) {
            try {
                attempt++;
                System.out.println("========== PROMPT ==========");
                System.out.println(retryPrompt);
                System.out.println("Prompt length = " + retryPrompt.length());
                System.out.println("Estimated tokens = " + (retryPrompt.length() / 4));
                System.out.println("============================");
                String rawResponse = aiService.callAIUnified(retryPrompt, publicUrls);

                log.info("AI attempt {} success", attempt);
                log.debug("AI raw response: {}", rawResponse);

                WritingFeedbackResponseDto parsed =
                        aiService.parseWritingResponse(rawResponse);

                validateAIResponse(parsed);

                return parsed;

            } catch (Exception e) {
                log.warn("AI failed attempt {}: {}", attempt, e.getMessage());

                if (attempt == maxAttempts) {
                    throw new RuntimeException("AI failed after retries", e);
                }

                // 👇 tăng độ ép format ở retry
                retryPrompt += "\n\nCRITICAL: RETURN VALID JSON ONLY. NO TEXT.";
            }
        }

        throw new RuntimeException("Unexpected AI error");
    }

    private void validateAIResponse(WritingFeedbackResponseDto response) {

        if (response == null || response.getAnswers() == null) {
            throw new RuntimeException("Invalid AI response: null");
        }

        for (WritingAnswerFeedbackDto answer : response.getAnswers()) {

            if (answer.getOrderIndex() == null) {
                throw new RuntimeException("Missing orderIndex");
            }

            List<WritingFeedbackItemDto> items = answer.getFeedbacks();

            if (items == null || items.isEmpty()) {
                throw new RuntimeException("Empty feedback list");
            }
            if (items.size() > 32) {
                throw new RuntimeException("Too many feedback items");
            }

            for (WritingFeedbackItemDto item : items) {
                if (item.getCriterionName() == null ||
                        item.getFeedbackType() == null ||
                        item.getLabel() == null) {
                    throw new RuntimeException("Invalid item: missing enum");
                }
            }
        }
    }

}


