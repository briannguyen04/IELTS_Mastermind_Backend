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

You must analyze each essay and generate structured feedback.

========================
STRICT OUTPUT FORMAT
========================

Return STRICT JSON ONLY:

{
  "answers": [
    {
      "orderIndex": 1,
      "feedbacks": [
        {
          "criterionName": "...",
          "feedbackType": "...",
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
TASK TYPE RULE (CRITICAL)
========================

Each essay includes a field "taskType":

- TASK_1 → you MUST use TASK_ACHIEVEMENT
- TASK_2 → you MUST use TASK_RESPONSE

CRITICAL:
- If taskType = TASK_1 → DO NOT use TASK_RESPONSE
- If taskType = TASK_2 → DO NOT use TASK_ACHIEVEMENT
- Use taskType as the ONLY source of truth. DO NOT guess.

========================
STRUCTURE RULES
========================

For EACH answer:

1. Generate feedback items BASED ON essay quality
- Good essay → up to 32 items
- Poor/very short essay → fewer items are allowed

2. ORDER (STRICT):

- First 8:
  TASK_ACHIEVEMENT (Task 1) OR TASK_RESPONSE (Task 2)

- Next 8:
  COHERENCE_AND_COHESION

- Next 8:
  LEXICAL_RESOURCE

- Last 8:
  GRAMMATICAL_RANGE_AND_ACCURACY

3. Each group of 8:
- First 4 → STRENGTH
- Last 4 → WEAKNESS

========================
LABEL SELECTION RULES
========================

You MUST select labels ONLY from the lists below.

IMPORTANT:
- If feedbackType = STRENGTH → use LEFT side of arrow
- If feedbackType = WEAKNESS → use RIGHT side of arrow

------------------------
TASK (Task 1 + Task 2)
------------------------

FULL_TASK_COVERAGE -> PARTIAL_TASK_COVERAGE
CLEAR_POSITION -> UNCLEAR_POSITION
CONSISTENT_POSITION -> INCONSISTENT_POSITION
RELEVANT_IDEAS -> IRRELEVANT_IDEAS
GOOD_IDEA_DEVELOPMENT -> INSUFFICIENT_IDEA_DEVELOPMENT
ADEQUATE_SUPPORT -> INSUFFICIENT_SUPPORT
APPROPRIATE_FORMAT -> INAPPROPRIATE_FORMAT
SUFFICIENT_LENGTH -> UNDER_LENGTH_RESPONSE

------------------------
TASK 1 ONLY
------------------------

CLEAR_OVERVIEW -> MISSING_OVERVIEW
GOOD_KEY_FEATURE_SELECTION -> MISSING_KEY_FEATURES
GOOD_DATA_SUPPORT -> INSUFFICIENT_DATA_SUPPORT

------------------------
TASK 2 ONLY
------------------------

CLEAR_CONCLUSION -> WEAK_CONCLUSION

------------------------
COHERENCE & COHESION
------------------------

LOGICAL_ORGANISATION -> WEAK_ORGANISATION
CLEAR_PROGRESSION -> UNCLEAR_PROGRESSION
EFFECTIVE_PARAGRAPHING -> WEAK_PARAGRAPHING
EFFECTIVE_COHESIVE_DEVICES -> MISUSED_COHESIVE_DEVICES
NATURAL_LINKER_USE -> OVERUSE_OF_LINKERS
GOOD_REFERENCE_USE -> UNCLEAR_REFERENCING
MINIMAL_REPETITION -> REPETITION
CLEAR_PARAGRAPH_FOCUS -> UNCLEAR_PARAGRAPH_FOCUS

------------------------
LEXICAL RESOURCE
------------------------

VARIED_VOCABULARY -> LIMITED_VOCABULARY
PRECISE_VOCABULARY -> IMPRECISE_VOCABULARY
APPROPRIATE_WORD_CHOICE -> INAPPROPRIATE_WORD_CHOICE
GOOD_COLLOCATION -> WEAK_COLLOCATION
APPROPRIATE_STYLE -> STYLE_INAPPROPRIATE
GOOD_SPELLING -> SPELLING_ERRORS
GOOD_WORD_FORMATION -> WORD_FORMATION_ERRORS
NATURAL_LANGUAGE_USE -> MEMORISED_LANGUAGE_OVERUSE

------------------------
GRAMMAR
------------------------

VARIED_SENTENCE_STRUCTURES -> LIMITED_GRAMMATICAL_RANGE
GOOD_COMPLEX_STRUCTURE_USE -> FAULTY_COMPLEX_SENTENCES
GRAMMAR_WELL_CONTROLLED -> GRAMMAR_ERRORS
GOOD_PUNCTUATION -> PUNCTUATION_ERRORS
WELL_FORMED_SENTENCES -> SENTENCE_FRAGMENT_OR_RUN_ON

========================
EVIDENCE RULES
========================

- evidenceSentences MUST be exact sentences from essay
- DO NOT paraphrase
- DO NOT invent

========================
WRITING RULES
========================

- description: short feedback
- explanation: why it is good or bad
- recommendedActionDescription: what to improve
- recommendedActionExplanation: how to improve

========================
STRICT CONSTRAINTS
========================

CRITERION NAME ENUM RULE (VERY IMPORTANT)

criterionName MUST be EXACTLY one of:

- TASK_RESPONSE
- TASK_ACHIEVEMENT
- COHERENCE_AND_COHESION
- LEXICAL_RESOURCE
- GRAMMATICAL_RANGE_AND_ACCURACY

Rules:
- If taskType = TASK_1, use TASK_ACHIEVEMENT for task-related feedback.
- If taskType = TASK_2, use TASK_RESPONSE for task-related feedback.
- For coherence feedback, use COHERENCE_AND_COHESION.
- For vocabulary feedback, use LEXICAL_RESOURCE.
- For grammar feedback, use GRAMMATICAL_RANGE_AND_ACCURACY.

ENUM STRICT MATCHING RULE (VERY IMPORTANT)

- All enum values MUST match EXACTLY the provided labels.
- DO NOT change formatting, spelling, or structure.
- DO NOT remove or add underscores.
- DO NOT use spaces instead of underscores.

    Example:
    CORRECT: UNDER_LENGTH_RESPONSE
    WRONG: UnderLengthResponse
    WRONG: UNDER LENGTH RESPONSE
    WRONG: under_length_response

- DO NOT change order
- SHOULD return up to 32 items if possible
- DO NOT force fake feedback if essay lacks content
- MUST follow structure exactly
- NO extra text outside JSON

========================
LOW QUALITY ESSAY RULE
========================

If the essay is extremely short, meaningless, or invalid (e.g. random text, "test", etc.):

- DO NOT force full feedback
- Generate ONLY relevant WEAKNESS feedback
- You MAY return fewer than 32 items
- Focus on major issues:
  - UNDER_LENGTH_RESPONSE
  - INSUFFICIENT_IDEA_DEVELOPMENT
  - UNCLEAR_POSITION
  - LIMITED_VOCABULARY
  - GRAMMAR_ERRORS

- Evidence sentences can reuse the original text if necessary

- NEVER fabricate complex strengths for invalid essays

========================
EXAMPLE
========================

{
  "answers": [
    {
      "orderIndex": 1,
      "feedbacks": [
        {
          "criterionName": "TASK_RESPONSE",
          "feedbackType": "STRENGTH",
          "label": "FULL_TASK_COVERAGE",
          "description": "The essay fully addresses all parts of the task.",
          "explanation": "All aspects of the question are covered with relevant responses.",
          "evidenceSentences": ["This essay discusses both advantages and disadvantages in detail."],
          "recommendedActionDescription": "Continue maintaining full coverage of all task parts.",
          "recommendedActionExplanation": "Ensure every future response answers all aspects clearly."
        },
        {
          "criterionName": "TASK_RESPONSE",
          "feedbackType": "STRENGTH",
          "label": "CLEAR_POSITION",
          "description": "The writer presents a clear position throughout the essay.",
          "explanation": "The opinion is easy to identify and consistently maintained.",
          "evidenceSentences": ["I strongly believe that this approach is beneficial."],
          "recommendedActionDescription": "Maintain a clear and direct position in all essays.",
          "recommendedActionExplanation": "State your opinion early and reinforce it in each paragraph."
        },
        {
          "criterionName": "TASK_RESPONSE",
          "feedbackType": "STRENGTH",
          "label": "RELEVANT_IDEAS",
          "description": "Ideas are relevant to the topic.",
          "explanation": "All arguments directly relate to the question.",
          "evidenceSentences": ["One key advantage is the improvement in productivity."],
          "recommendedActionDescription": "Keep focusing on relevant ideas.",
          "recommendedActionExplanation": "Avoid adding unrelated points."
        },
        {
          "criterionName": "TASK_RESPONSE",
          "feedbackType": "STRENGTH",
          "label": "ADEQUATE_SUPPORT",
          "description": "Ideas are supported with explanations.",
          "explanation": "Each point is explained clearly with supporting detail.",
          "evidenceSentences": ["For example, companies can reduce costs by automating tasks."],
          "recommendedActionDescription": "Continue providing supporting explanations.",
          "recommendedActionExplanation": "Always expand on your ideas with examples."
        },

        {
          "criterionName": "TASK_RESPONSE",
          "feedbackType": "WEAKNESS",
          "label": "INSUFFICIENT_IDEA_DEVELOPMENT",
          "description": "Some ideas are not fully developed.",
          "explanation": "Certain arguments lack depth and detail.",
          "evidenceSentences": ["Technology is important for society."],
          "recommendedActionDescription": "Develop ideas more thoroughly.",
          "recommendedActionExplanation": "Add explanations and examples to each main point."
        },
        {
          "criterionName": "TASK_RESPONSE",
          "feedbackType": "WEAKNESS",
          "label": "INSUFFICIENT_SUPPORT",
          "description": "Some points lack supporting evidence.",
          "explanation": "Statements are made without examples.",
          "evidenceSentences": ["This is a major issue nowadays."],
          "recommendedActionDescription": "Provide more supporting examples.",
          "recommendedActionExplanation": "Use real-life examples or explanations."
        },
        {
          "criterionName": "TASK_RESPONSE",
          "feedbackType": "WEAKNESS",
          "label": "UNCLEAR_POSITION",
          "description": "Position is not always clear.",
          "explanation": "The stance becomes unclear in some parts.",
          "evidenceSentences": ["Some people think this is good, while others disagree."],
          "recommendedActionDescription": "Clarify your position.",
          "recommendedActionExplanation": "Restate your opinion clearly in each paragraph."
        },
        {
          "criterionName": "TASK_RESPONSE",
          "feedbackType": "WEAKNESS",
          "label": "UNDER_LENGTH_RESPONSE",
          "description": "The response is shorter than required.",
          "explanation": "The essay does not meet the minimum word count.",
          "evidenceSentences": ["(Essay ends prematurely)"],
          "recommendedActionDescription": "Write a longer response.",
          "recommendedActionExplanation": "Aim for at least 250 words."
        },

        {
          "criterionName": "COHERENCE_AND_COHESION",
          "feedbackType": "STRENGTH",
          "label": "LOGICAL_ORGANISATION",
          "description": "The essay is logically organised.",
          "explanation": "Ideas are presented in a clear sequence.",
          "evidenceSentences": ["Firstly, ..., Secondly, ..., Finally, ..."],
          "recommendedActionDescription": "Maintain logical organisation.",
          "recommendedActionExplanation": "Keep structuring ideas clearly."
        }

        // ... (remaining items follow the same pattern)
        // You MUST continue this pattern until there are EXACTLY 32 items.
        // Distribution MUST be:
        // - First 8: TASK (Task Achievement or Task Response depending on taskType)
        // - Next 8: COHERENCE_AND_COHESION
        // - Next 8: LEXICAL_RESOURCE
        // - Last 8: GRAMMATICAL_RANGE_AND_ACCURACY
      ]
    }
  ]
}

========================
INPUT
========================
========================
INPUT EXPLANATION
========================

You will receive a list of student essays in JSON format.

Each essay has:

- orderIndex: the position of the essay (used to match output)
- taskType: either TASK_1 or TASK_2
- essayText: the student's full essay
- wordCount: number of words in the essay

You MUST:
- keep the same orderIndex in your response

Title:
%s

Instruction:
%s

Time spent (seconds):
%d

Student Essays:
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


