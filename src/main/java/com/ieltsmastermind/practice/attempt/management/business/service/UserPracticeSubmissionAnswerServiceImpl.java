package com.ieltsmastermind.practice.attempt.management.business.service;

import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.practice.attempt.management.business.interfaces.UserPracticeSubmissionAnswerService;
import com.ieltsmastermind.practice.attempt.management.domain.dto.*;
import com.ieltsmastermind.practice.attempt.management.domain.entity.*;
import com.ieltsmastermind.practice.attempt.management.domain.enums.Result;
import com.ieltsmastermind.practice.attempt.management.persistence.*;
import com.ieltsmastermind.practice.content.management.domain.entity.PracticeQuestion;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeQuestionType;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeTopicTag;
import com.ieltsmastermind.practice.content.management.persistence.PracticeQuestionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserPracticeSubmissionAnswerServiceImpl implements UserPracticeSubmissionAnswerService {

    private final UserPracticeSubmissionAnswerRepository answerRepository;
    private final UserPracticeSubmissionRepository submissionRepository;
    private final PracticeQuestionRepository practiceQuestionRepository;
    private final SubmissionQuestionTypeAccuracyRepository submissionQuestionTypeAccuracyRepository;
    private final SubmissionTopicTagAccuracyRepository submissionTopicTagAccuracyRepository;

    @Override
    @Transactional
    public UserPracticeSubmissionAnswerResponseDto create(UserPracticeSubmissionAnswerCreateRequestDto request) {

        UserPracticeSubmission submission = submissionRepository
                .findById(request.getUserPracticeSubmissionId())
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        UserPracticeSubmissionAnswer answer = new UserPracticeSubmissionAnswer();
        answer.setSubmission(submission);
        answer.setOrderIndex(request.getOrderIndex());
        answer.setAnswers(new ArrayList<>(request.getAnswers()));

        UserPracticeSubmissionAnswer saved = answerRepository.save(answer);

        UserPracticeSubmissionAnswerResponseDto responseDto = new UserPracticeSubmissionAnswerResponseDto();
        responseDto.setId(saved.getId());

        return responseDto;
    }

    @Override
    @Transactional
    public List<UserPracticeSubmissionAnswerResponseDto> createBulk(
            UserPracticeSubmissionAnswerBulkCreateRequestDto request
    ) {
        UserPracticeSubmission submission = submissionRepository
                .findById(request.getUserPracticeSubmissionId())
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        List<UserPracticeSubmissionAnswerCreateRequestDto> reqs = request.getAnswers();

        List<UserPracticeSubmissionAnswer> entities = new ArrayList<>(reqs.size());
        for (UserPracticeSubmissionAnswerCreateRequestDto r : reqs) {
            UserPracticeSubmissionAnswer answer = new UserPracticeSubmissionAnswer();
            answer.setSubmission(submission);
            answer.setOrderIndex(r.getOrderIndex());
            answer.setAnswers(new ArrayList<>(r.getAnswers()));
            entities.add(answer);
        }

        List<UserPracticeSubmissionAnswer> saved = answerRepository.saveAll(entities);

        setSubmissionResult(submission, saved);

        List<UserPracticeSubmissionAnswerResponseDto> response = new ArrayList<>(saved.size());
        for (UserPracticeSubmissionAnswer s : saved) {
            UserPracticeSubmissionAnswerResponseDto dto = new UserPracticeSubmissionAnswerResponseDto();
            dto.setId(s.getId());
            response.add(dto);
        }

        return response;
    }

    @Override
    public List<UserPracticeSubmissionAnswerResponseDto> getAllBySubmissionId(String submissionId, IncludeSpec includes) {
        List<UserPracticeSubmissionAnswer> answerRows =
                answerRepository.findAllBySubmission_IdOrderByOrderIndexAsc(submissionId);

        List<UserPracticeSubmissionAnswerResponseDto> result = new ArrayList<>();

        for (UserPracticeSubmissionAnswer row : answerRows) {
            UserPracticeSubmissionAnswerResponseDto dto = new UserPracticeSubmissionAnswerResponseDto();
            dto.setId(row.getId());
            applyIncludes(row, dto, includes);

            result.add(dto);
        }

        return result;
    }

    @Override
    public UserPracticeSubmissionAnswerResponseDto getById(String id, IncludeSpec includes) {

        UserPracticeSubmissionAnswer answer = answerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Submission answer not found"));

        UserPracticeSubmissionAnswerResponseDto dto = new UserPracticeSubmissionAnswerResponseDto();
        dto.setId(answer.getId());
        applyIncludes(answer, dto, includes);

        return dto;
    }

    private void applyIncludes(UserPracticeSubmissionAnswer answer,
                               UserPracticeSubmissionAnswerResponseDto dto,
                               IncludeSpec includes) {

        if (includes.has("submissionid")) dto.setSubmissionId(answer.getSubmission().getId());
        if (includes.has("orderindex")) dto.setOrderIndex(answer.getOrderIndex());
        if (includes.has("answers")) dto.setAnswers(new ArrayList<>(answer.getAnswers()));
        if (includes.has("result")) dto.setResult(answer.getResult());
    }

    private void setSubmissionResult(UserPracticeSubmission submission,
                                               List<UserPracticeSubmissionAnswer> savedAnswers) {

        String practiceContentId = submission.getPracticeContentId();

        List<PracticeQuestion> questions =
                practiceQuestionRepository.findByPracticeContent_IdOrderByOrderIndexAsc(practiceContentId);

        Map<Integer, UserPracticeSubmissionAnswer> answerByOrder = new HashMap<>();
        for (UserPracticeSubmissionAnswer answer : savedAnswers) {
            answerByOrder.put(answer.getOrderIndex(), answer);
        }

        int correct = 0;
        int wrong = 0;
        int skip = 0;

        Map<PracticeQuestionType, SubmissionQuestionTypeAccuracy> accuracyByType =
                new EnumMap<>(PracticeQuestionType.class);

        Map<PracticeTopicTag, SubmissionTopicTagAccuracy> accuracyByTopicTag =
                new EnumMap<>(PracticeTopicTag.class);

        for (PracticeQuestion question : questions) {
            UserPracticeSubmissionAnswer answer = answerByOrder.get(question.getOrderIndex());
            List<String> userAnswers = (answer != null) ? answer.getAnswers() : null;

            PracticeQuestionType questionType = question.getType();
            PracticeTopicTag topicTag = question.getTopicTag();

            SubmissionQuestionTypeAccuracy typeAccuracy = accuracyByType.computeIfAbsent(questionType, type -> {
                SubmissionQuestionTypeAccuracy a = new SubmissionQuestionTypeAccuracy();
                a.setSubmission(submission);
                a.setQuestionType(type);
                a.setExposureCount(0);
                a.setAnsweredQuestionCount(0);
                a.setCorrectQuestionCount(0);
                a.setWrongQuestionCount(0);
                a.setSkipQuestionCount(0);
                a.setCorrectAnswerPercentage(0.0);
                a.setSkipRate(0.0);
                a.setEffectiveAccuracy(0.0);
                return a;
            });

            SubmissionTopicTagAccuracy topicTagAccuracy = accuracyByTopicTag.computeIfAbsent(topicTag, tag -> {
                SubmissionTopicTagAccuracy a = new SubmissionTopicTagAccuracy();
                a.setSubmission(submission);
                a.setTopicTag(tag);
                a.setExposureCount(0);
                a.setAnsweredQuestionCount(0);
                a.setCorrectQuestionCount(0);
                a.setWrongQuestionCount(0);
                a.setSkipQuestionCount(0);
                a.setCorrectAnswerPercentage(0.0);
                a.setSkipRate(0.0);
                a.setEffectiveAccuracy(0.0);
                return a;
            });

            typeAccuracy.setExposureCount(typeAccuracy.getExposureCount() + 1);
            topicTagAccuracy.setExposureCount(topicTagAccuracy.getExposureCount() + 1);

            if (isAnswerEmpty(userAnswers)) {
                skip++;

                typeAccuracy.setSkipQuestionCount(typeAccuracy.getSkipQuestionCount() + 1);
                topicTagAccuracy.setSkipQuestionCount(topicTagAccuracy.getSkipQuestionCount() + 1);

                if (answer != null) {
                    answer.setResult(Result.SKIPPED);
                }
                continue;
            }

            typeAccuracy.setAnsweredQuestionCount(typeAccuracy.getAnsweredQuestionCount() + 1);
            topicTagAccuracy.setAnsweredQuestionCount(topicTagAccuracy.getAnsweredQuestionCount() + 1);

            if (isCorrect(userAnswers, question.getCorrectAnswers())) {
                correct++;

                typeAccuracy.setCorrectQuestionCount(typeAccuracy.getCorrectQuestionCount() + 1);
                topicTagAccuracy.setCorrectQuestionCount(topicTagAccuracy.getCorrectQuestionCount() + 1);

                answer.setResult(Result.CORRECT);
            } else {
                wrong++;

                typeAccuracy.setWrongQuestionCount(typeAccuracy.getWrongQuestionCount() + 1);
                topicTagAccuracy.setWrongQuestionCount(topicTagAccuracy.getWrongQuestionCount() + 1);

                answer.setResult(Result.WRONG);
            }
        }

        int totalQuestions = questions.size();
        int answeredQuestions = correct + wrong;

        double accuracyRate = totalQuestions == 0 ? 0.0 : (correct * 100.0) / totalQuestions;
        double skipRate = totalQuestions == 0 ? 0.0 : (skip * 100.0) / totalQuestions;
        double effectiveAccuracy = answeredQuestions == 0 ? 0.0 : (correct * 100.0) / answeredQuestions;

        double roundedAccuracyRate = roundToOneDecimal(accuracyRate);
        double roundedSkipRate = roundToOneDecimal(skipRate);
        double roundedEffectiveAccuracy = roundToOneDecimal(effectiveAccuracy);

        Double band = bandScoreFromPercentage(roundedAccuracyRate);

        submission.setCorrectAnswerCount(correct);
        submission.setWrongAnswerCount(wrong);
        submission.setSkipAnswerCount(skip);
        submission.setTotalQuestionCount(totalQuestions);
        submission.setAnsweredQuestionCount(answeredQuestions);

        submission.setAccuracyRate(roundedAccuracyRate);
        submission.setCorrectAnswerPercentage(roundedAccuracyRate);
        submission.setSkipRate(roundedSkipRate);
        submission.setEffectiveAccuracy(roundedEffectiveAccuracy);
        submission.setScore(band);

        for (SubmissionQuestionTypeAccuracy typeAccuracy : accuracyByType.values()) {
            int exposureCount = typeAccuracy.getExposureCount();
            int answeredCount = typeAccuracy.getAnsweredQuestionCount();
            int correctCount = typeAccuracy.getCorrectQuestionCount();
            int skipCount = typeAccuracy.getSkipQuestionCount();

            double typeCorrectAnswerPercentage = exposureCount == 0
                    ? 0.0
                    : (correctCount * 100.0) / exposureCount;

            double typeSkipRate = exposureCount == 0
                    ? 0.0
                    : (skipCount * 100.0) / exposureCount;

            double typeEffectiveAccuracy = answeredCount == 0
                    ? 0.0
                    : (correctCount * 100.0) / answeredCount;

            typeAccuracy.setCorrectAnswerPercentage(roundToOneDecimal(typeCorrectAnswerPercentage));
            typeAccuracy.setSkipRate(roundToOneDecimal(typeSkipRate));
            typeAccuracy.setEffectiveAccuracy(roundToOneDecimal(typeEffectiveAccuracy));
        }

        for (SubmissionTopicTagAccuracy topicTagAccuracy : accuracyByTopicTag.values()) {
            int exposureCount = topicTagAccuracy.getExposureCount();
            int answeredCount = topicTagAccuracy.getAnsweredQuestionCount();
            int correctCount = topicTagAccuracy.getCorrectQuestionCount();
            int skipCount = topicTagAccuracy.getSkipQuestionCount();

            double topicCorrectAnswerPercentage = exposureCount == 0
                    ? 0.0
                    : (correctCount * 100.0) / exposureCount;

            double topicSkipRate = exposureCount == 0
                    ? 0.0
                    : (skipCount * 100.0) / exposureCount;

            double topicEffectiveAccuracy = answeredCount == 0
                    ? 0.0
                    : (correctCount * 100.0) / answeredCount;

            topicTagAccuracy.setCorrectAnswerPercentage(roundToOneDecimal(topicCorrectAnswerPercentage));
            topicTagAccuracy.setSkipRate(roundToOneDecimal(topicSkipRate));
            topicTagAccuracy.setEffectiveAccuracy(roundToOneDecimal(topicEffectiveAccuracy));
        }

        submissionQuestionTypeAccuracyRepository.deleteBySubmission_Id(submission.getId());
        submissionTopicTagAccuracyRepository.deleteBySubmission_Id(submission.getId());

        submissionQuestionTypeAccuracyRepository.saveAll(accuracyByType.values());
        submissionTopicTagAccuracyRepository.saveAll(accuracyByTopicTag.values());
    }

    private double roundToOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private boolean isAnswerEmpty(List<String> answers) {
        if (answers == null || answers.isEmpty()) return true;
        return answers.stream().allMatch(a -> a == null || a.trim().isEmpty());
    }

    private boolean isCorrect(List<String> userAnswers, List<String> correctAnswers) {
        Set<String> userSet = normalizeToSet(userAnswers);
        Set<String> correctSet = normalizeToSet(correctAnswers);

        if (userSet.isEmpty() || correctSet.isEmpty()) return false;

        if (userSet.size() == 1) {
            return correctSet.contains(userSet.iterator().next());
        }

        return userSet.equals(correctSet);
    }

    private Set<String> normalizeToSet(List<String> values) {
        if (values == null) return Set.of();
        Set<String> out = new HashSet<>();
        for (String v : values) {
            String n = normalize(v);
            if (!n.isBlank()) out.add(n);
        }
        return out;
    }

    private String normalize(String s) {
        if (s == null) return "";
        String x = s.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        x = x.replaceAll("[\\p{Punct}]+$", "");
        return x;
    }

    private Double bandScoreFromCorrect(int correct) {
        if (correct >= 39) return 9.0;
        if (correct >= 37) return 8.5;
        if (correct >= 35) return 8.0;
        if (correct >= 32) return 7.5;
        if (correct >= 30) return 7.0;
        if (correct >= 26) return 6.5;
        if (correct >= 23) return 6.0;
        if (correct >= 18) return 5.5;
        if (correct >= 16) return 5.0;
        if (correct >= 13) return 4.5;
        if (correct >= 11) return 4.0;
        if (correct >= 8)  return 3.5;
        if (correct >= 6)  return 3.0;
        if (correct >= 4)  return 2.5;
        if (correct >= 2)  return 2.0;
        if (correct >= 1)  return 1.5;
        return 0.0;
    }

    private Double bandScoreFromPercentage(double percentage) {
        if (percentage >= 97.5) return 9.0;
        if (percentage >= 92.5) return 8.5;
        if (percentage >= 87.5) return 8.0;
        if (percentage >= 80.0) return 7.5;
        if (percentage >= 75.0) return 7.0;
        if (percentage >= 65.0) return 6.5;
        if (percentage >= 57.5) return 6.0;
        if (percentage >= 45.0) return 5.5;
        if (percentage >= 40.0) return 5.0;
        if (percentage >= 32.5) return 4.5;
        if (percentage >= 27.5) return 4.0;
        if (percentage >= 20.0) return 3.5;
        if (percentage >= 15.0) return 3.0;
        if (percentage >= 10.0) return 2.5;
        if (percentage >= 5.0) return 2.0;
        if (percentage >= 2.5) return 1.5;
        return 0.0;
    }
}
