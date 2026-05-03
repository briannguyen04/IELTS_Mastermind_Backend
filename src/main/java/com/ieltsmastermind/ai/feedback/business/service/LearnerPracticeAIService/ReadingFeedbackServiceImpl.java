package com.ieltsmastermind.ai.feedback.business.service.LearnerPracticeAIService;

import com.ieltsmastermind.ai.feedback.business.interfaces.ReadingFeedbackService;
import com.ieltsmastermind.ai.feedback.domain.dto.EvidenceDto;
import com.ieltsmastermind.ai.feedback.domain.dto.QuestionDto;
import com.ieltsmastermind.ai.feedback.domain.dto.ReadingFeedbackResponseDto;
import com.ieltsmastermind.ai.feedback.domain.entity.Evidence;
import com.ieltsmastermind.ai.feedback.domain.entity.QuestionFeedback;
import com.ieltsmastermind.ai.feedback.domain.entity.ReadingFeedback;
import com.ieltsmastermind.ai.feedback.persistence.ReadingFeedbackRepository;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeSubmission;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeSubmissionAnswer;
import com.ieltsmastermind.practice.attempt.management.domain.enums.Result;
import com.ieltsmastermind.practice.attempt.management.persistence.UserPracticeSubmissionAnswerRepository;
import com.ieltsmastermind.practice.attempt.management.persistence.UserPracticeSubmissionRepository;
import com.ieltsmastermind.practice.content.management.domain.entity.PracticeQuestion;
import com.ieltsmastermind.practice.content.management.domain.entity.ReadingPracticeContent;
import com.ieltsmastermind.practice.content.management.persistence.PracticeQuestionRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReadingFeedbackServiceImpl implements ReadingFeedbackService {

    @Autowired
    private AIFeedbackServiceImpl aiService;

    @Autowired
    private UserPracticeSubmissionRepository submissionRepository;

    @Autowired
    private PracticeQuestionRepository practiceQuestionRepository;

    @Autowired
    private UserPracticeSubmissionAnswerRepository submissionAnswerRepository;

    @Autowired
    private ReadingFeedbackRepository readingFeedbackRepository;

    @Override
    @Transactional
    public void createReadingFeedback(String submissionId) {

        if (readingFeedbackRepository.findBySubmissionId(submissionId).isPresent()) {
            return;
        }

        // 1. Get submission
        UserPracticeSubmission submission = submissionRepository.findWithContent(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        if (!(submission.getPracticeContent() instanceof ReadingPracticeContent content)) {
            throw new IllegalArgumentException("Invalid content type");
        }

        String contentId = submission.getPracticeContentId();

        // 2. Answers
        List<UserPracticeSubmissionAnswer> answers =
                submissionAnswerRepository.findBySubmissionId(submissionId);

        // 3. Questions
        List<PracticeQuestion> questions =
                practiceQuestionRepository.findByPracticeContent_IdOrderByOrderIndexAsc(contentId);

        Map<Integer, PracticeQuestion> questionMap = questions.stream()
                .collect(Collectors.toMap(
                        PracticeQuestion::getOrderIndex,
                        q -> q
                ));

        // 4. Filter wrong + skipped
        List<Map<String, Object>> filteredAnswers = answers.stream()
                .filter(a -> a.getResult() == Result.WRONG || a.getResult() == Result.SKIPPED)
                .map(a -> {
                    PracticeQuestion q = questionMap.get(a.getOrderIndex());

                    if (q == null) {
                        throw new RuntimeException("Question not found: " + a.getOrderIndex());
                    }

                    return Map.of(
                            "questionNumber", a.getOrderIndex(),
                            "userAnswer", a.getAnswers(),
                            "correctAnswer", q.getCorrectAnswers(),
                            "result", a.getResult().name(),
                            "questionType", q.getType().name()
                    );
                })
                .toList();

        // 5. All correct → skip AI
        if (filteredAnswers.isEmpty()) {
            ReadingFeedback feedback = new ReadingFeedback();
            feedback.setSubmissionId(submissionId);
            feedback.setSummary("Great job! All answers are correct.");
            readingFeedbackRepository.save(feedback);
            return;
        }

        // 6. Prompt
        String cleanInstruction = aiService.stripCustomTags(content.getInstructions());

        String prompt = buildReadingPrompt(
                content.getTitle(),
                cleanInstruction,
                content.getPassage(),
                filteredAnswers,
                answers,
                submission.getTimeSpentSeconds()
        );

        List<String> images = aiService.extractImages(content.getInstructions());
        List<String> publicUrls = aiService.toPublicUrls(images);

        String aiResponse;

        if (publicUrls.isEmpty()) {
            aiResponse = aiService.callAI(prompt);
        } else {
            aiResponse = aiService.callAIWithImages(prompt, publicUrls);
        }

        ReadingFeedbackResponseDto response =
                aiService.parseResponse(aiResponse, ReadingFeedbackResponseDto.class);

        saveReadingFeedback(submissionId, response);
    }

    // ================= SAVE =================

    private void saveReadingFeedback(String submissionId,
                                     ReadingFeedbackResponseDto response) {

        ReadingFeedback feedback = new ReadingFeedback();
        feedback.setSubmissionId(submissionId);
        feedback.setSummary(response.getSummary());

        List<QuestionFeedback> questionEntities = new ArrayList<>();

        for (QuestionDto q : response.getQuestions()) {

            QuestionFeedback qEntity = new QuestionFeedback();
            qEntity.setQuestionNumber(q.getQuestionNumber());
            qEntity.setReadingFeedback(feedback);

            if (q.getEvidence() != null) {
                Evidence ev = new Evidence();
                ev.setQuote(q.getEvidence().getQuote());
                ev.setReason(q.getEvidence().getReason());
                ev.setQuestionFeedback(qEntity);

                qEntity.setEvidence(ev);
            }

            questionEntities.add(qEntity);
        }

        feedback.setQuestions(questionEntities);

        readingFeedbackRepository.save(feedback);
    }


    @Override
    public ReadingFeedbackResponseDto getReadingFeedback(String submissionId) {


        List<UserPracticeSubmissionAnswer> answers = submissionAnswerRepository.findBySubmissionId(submissionId);

        Map<Integer, UserPracticeSubmissionAnswer> answerMap =
                answers.stream().collect(Collectors.toMap(
                        UserPracticeSubmissionAnswer::getOrderIndex,
                        a -> a
                ));

        UserPracticeSubmission submission = submissionRepository.findWithContent(submissionId)
                .orElseThrow();

        List<PracticeQuestion> questions =
                practiceQuestionRepository.findByPracticeContent_IdOrderByOrderIndexAsc(
                        submission.getPracticeContentId()
                );

        Map<Integer, PracticeQuestion> questionMap =
                questions.stream().collect(Collectors.toMap(
                        PracticeQuestion::getOrderIndex,
                        q -> q
                ));

        ReadingFeedback feedback = readingFeedbackRepository
                .findFullBySubmissionId(submissionId)
                .orElseThrow(() -> new RuntimeException("Feedback not found"));



        ReadingFeedbackResponseDto dto = new ReadingFeedbackResponseDto();
        dto.setSummary(feedback.getSummary());

        List<QuestionDto> questionDtos = new ArrayList<>();

        List<QuestionFeedback> sortedQuestions = feedback.getQuestions()
                .stream()
                .sorted(Comparator.comparing(QuestionFeedback::getQuestionNumber))
                .toList();

        for (QuestionFeedback q : sortedQuestions) {

            QuestionDto qDto = new QuestionDto();

            int qNum = q.getQuestionNumber();

            qDto.setQuestionNumber(qNum);


            UserPracticeSubmissionAnswer ans = answerMap.get(qNum);
            if (ans != null) {
                qDto.setResult(ans.getResult().name());
            }


            PracticeQuestion pq = questionMap.get(qNum);
            if (pq != null) {
                qDto.setCorrectAnswer(pq.getCorrectAnswers());
            }

            if (q.getEvidence() != null) {
                EvidenceDto evDto = new EvidenceDto();
                evDto.setQuote(q.getEvidence().getQuote());
                evDto.setReason(q.getEvidence().getReason());
                qDto.setEvidence(evDto);
            }

            questionDtos.add(qDto);
        }
        dto.setQuestions(questionDtos);

        return dto;
    }

    // ================= PROMPT =================

    private String buildReadingPrompt(String title,
                                      String instruction,
                                      String passage,
                                      List<Map<String, Object>> filteredAnswers,
                                      List<UserPracticeSubmissionAnswer> allAnswers,
                                      int timeSpentSeconds) {

        int total = allAnswers.size();

        long correct = allAnswers.stream().filter(a -> a.getResult() == Result.CORRECT).count();
        long wrong = allAnswers.stream().filter(a -> a.getResult() == Result.WRONG).count();
        long skipped = allAnswers.stream().filter(a -> a.getResult() == Result.SKIPPED).count();

        long accuracy = total == 0 ? 0 : Math.round(correct * 100.0 / total);

        return """
You are an IELTS Reading examiner.

You are given:
- title
- instruction (may contain images)
- passage
- list of questions with:
  questionType, result (WRONG/SKIP), userAnswer, list of correctAnswer, questionNumber

Student performance statistics:
- Total questions: %d
- Correct: %d
- Wrong: %d
- Skipped: %d
- Accuracy: %d
- Time spent (seconds): %d

Your tasks:
1. Write "summary" EXACTLY in this format:

"You completed this Reading test with a Band X.X equivalent. You answered %d out of %d questions correctly, with %d incorrect and %d skipped. Your overall accuracy was %d%%, and you finished the test in MM minutes SS seconds."

Rules for summary:
- Replace X.X with an estimated band score based on performance
- Convert time_spent_seconds into minutes and seconds (MM minutes SS seconds)
- DO NOT change any numbers (correct, total, wrong, skipped, accuracy)
- DO NOT add extra sentences
- MUST follow EXACT sentence structure above

2. For each question, output:
- questionNumber
- result (same as input)
- correctAnswer (same as input)
- evidence:
  - quote: exact quote from passage
  - reason: explain why this supports the correctAnswer

Rules:
- If an image is provided, you MUST use it to analyze spatial or visual information.
- You MUST return result, correctAnswer exactly as provided in input.
- correctAnswer MUST be an array of strings
- DO NOT change formatting, spelling, or content of correctAnswer.
- DO NOT generate new answers.
- Use only passage + provided correctAnswer.
- Evidence.quote must be exact passage text.
- Evidence.reason must clearly connect quote → correctAnswer.
- Return STRICT JSON ONLY.

Example:
{
  "summary": "You completed this Reading test with a Band 5.0 equivalent. You answered 18 out of 40 questions correctly, with 14 incorrect and 8 skipped. Your overall accuracy was 45%%, and you finished the test in 27 minutes 40 seconds.",
  "questions": [
    {
      "questionNumber": 12,
      "result": "WRONG",
      "correctAnswer": ["Truck"],
      "evidence": {
        "quote": "will be delivered by truck on Friday morning",
        "reason": "This clearly shows that the delivery will be by truck, so the correct answer is Truck."
      }
    }
  ]
}

Title:
%s

Instruction:
%s

Passage:
%s

Student Answers:
%s
""".formatted(
                total,
                correct,
                wrong,
                skipped,
                accuracy,
                timeSpentSeconds,

                correct,
                total,
                wrong,
                skipped,
                accuracy,

                title,
                aiService.normalizeInstruction(instruction),
                aiService.normalizeInstruction(passage),
                aiService.toJson(filteredAnswers)
        );
    }
}