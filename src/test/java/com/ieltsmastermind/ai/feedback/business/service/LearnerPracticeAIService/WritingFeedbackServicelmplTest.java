package com.ieltsmastermind.ai.feedback.business.service.LearnerPracticeAIService;

import com.ieltsmastermind.ai.feedback.domain.dto.WritingAnswerFeedbackDto;
import com.ieltsmastermind.ai.feedback.domain.dto.WritingFeedbackItemDto;
import com.ieltsmastermind.ai.feedback.domain.dto.WritingFeedbackResponseDto;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeSubmission;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeWritingAnswer;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeWritingCriterionFeedback;
import com.ieltsmastermind.practice.attempt.management.domain.enums.FeedbackAuthorType;
import com.ieltsmastermind.practice.attempt.management.domain.enums.WritingCriterionName;
import com.ieltsmastermind.practice.attempt.management.domain.enums.WritingFeedbackLabel;
import com.ieltsmastermind.practice.attempt.management.domain.enums.WritingFeedbackType;
import com.ieltsmastermind.practice.attempt.management.persistence.UserPracticeSubmissionRepository;
import com.ieltsmastermind.practice.attempt.management.persistence.UserPracticeWritingAnswerRepository;
import com.ieltsmastermind.practice.attempt.management.persistence.UserPracticeWritingCriterionFeedbackRepository;
import com.ieltsmastermind.practice.content.management.domain.entity.PracticeContent;
import com.ieltsmastermind.practice.content.management.domain.entity.ReadingPracticeContent;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeTaskType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WritingFeedbackServicelmplTest {

    @Mock
    private AIFeedbackServiceImpl aiService;

    @Mock
    private UserPracticeSubmissionRepository submissionRepository;

    @Mock
    private UserPracticeWritingAnswerRepository userPracticeWritingAnswerRepository;

    @Mock
    private UserPracticeWritingCriterionFeedbackRepository userPracticeWritingCriterionFeedbackRepository;

    @InjectMocks
    private WritingFeedbackServicelmpl writingFeedbackService;

    @Test
    void createWritingFeedback_whenWritingAnswersExistAndAIResponseIsValid_shouldBuildPromptCallAIAndSaveFeedback() {
        String submissionId = "submission-1";
        UserPracticeSubmission submission = writingSubmission(submissionId, practiceContent(PracticeTaskType.TASK_2));
        UserPracticeWritingAnswer answer = writingAnswer("answer-1", submission, 1, "I strongly agree with this view.", 7);

        WritingFeedbackResponseDto aiResponse = writingResponse(
                answerFeedback(
                        1,
                        feedbackItem(
                                "TASK_RESPONSE",
                                "STRENGTH",
                                "clear position",
                                "Clear position.",
                                "The opinion is consistent.",
                                List.of("I strongly agree with this view."),
                                "Keep the position clear.",
                                "State your opinion directly."
                        )
                )
        );

        when(userPracticeWritingAnswerRepository.findAllBySubmission_IdOrderByOrderIndexAsc(submissionId))
                .thenReturn(List.of(answer));
        when(submissionRepository.findWithContent(submissionId)).thenReturn(Optional.of(submission));
        when(aiService.stripCustomTags("[f]Instruction[/f] [img src=\"/uploads/task.png\"]"))
                .thenReturn("Clean instruction [img src=\"/uploads/task.png\"]");
        when(aiService.extractImages("[f]Instruction[/f] [img src=\"/uploads/task.png\"]"))
                .thenReturn(List.of("/uploads/task.png"));
        when(aiService.toPublicUrls(List.of("/uploads/task.png")))
                .thenReturn(List.of("https://backend.example.com/images/task.png"));
        when(aiService.toJson(anyList())).thenReturn("essays-json");
        when(aiService.normalizeInstruction("Clean instruction [img src=\"/uploads/task.png\"]"))
                .thenReturn("Normalized instruction");
        when(aiService.callAIUnified(anyString(), anyList())).thenReturn("raw-ai-response");
        when(aiService.parseWritingResponse("raw-ai-response")).thenReturn(aiResponse);

        writingFeedbackService.createWritingFeedback(submissionId);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<String>> publicUrlsCaptor = ArgumentCaptor.forClass(List.class);
        verify(aiService).callAIUnified(promptCaptor.capture(), publicUrlsCaptor.capture());

        String prompt = promptCaptor.getValue();

        assertThat(prompt).contains("You are an IELTS Writing examiner.");
        assertThat(prompt).contains("Title:\nWriting Task 2");
        assertThat(prompt).contains("Instruction:\nNormalized instruction");
        assertThat(prompt).contains("Time spent (seconds):\n600");
        assertThat(prompt).contains("Student Essays:\nessays-json");
        assertThat(publicUrlsCaptor.getValue()).containsExactly("https://backend.example.com/images/task.png");

        ArgumentCaptor<UserPracticeWritingCriterionFeedback> feedbackCaptor =
                ArgumentCaptor.forClass(UserPracticeWritingCriterionFeedback.class);
        verify(userPracticeWritingCriterionFeedbackRepository).save(feedbackCaptor.capture());

        UserPracticeWritingCriterionFeedback savedFeedback = feedbackCaptor.getValue();

        assertThat(answer.getCriterionFeedbacks()).isEmpty();
        assertThat(savedFeedback.getAuthorType()).isEqualTo(FeedbackAuthorType.AI);
        assertThat(savedFeedback.getCriterionName()).isEqualTo(WritingCriterionName.TASK_RESPONSE);
        assertThat(savedFeedback.getFeedbackType()).isEqualTo(WritingFeedbackType.STRENGTH);
        assertThat(savedFeedback.getLabel()).isEqualTo(WritingFeedbackLabel.CLEAR_POSITION);
        assertThat(savedFeedback.getDescription()).isEqualTo("Clear position.");
        assertThat(savedFeedback.getExplanation()).isEqualTo("The opinion is consistent.");
        assertThat(savedFeedback.getEvidenceSentences()).containsExactly("I strongly agree with this view.");
        assertThat(savedFeedback.getRecommendedActionDescription()).isEqualTo("Keep the position clear.");
        assertThat(savedFeedback.getRecommendedActionExplanation()).isEqualTo("State your opinion directly.");
        assertThat(savedFeedback.getWritingAnswer()).isSameAs(answer);

        verify(submissionRepository).findWithContent(submissionId);
    }

    @Test
    void createWritingFeedback_whenAllAnswersAlreadyHaveFeedback_shouldSkipAIAndSave() {
        String submissionId = "submission-1";
        UserPracticeSubmission submission = writingSubmission(submissionId, practiceContent(PracticeTaskType.TASK_2));
        UserPracticeWritingAnswer answer1 = writingAnswer("answer-1", submission, 1, "Essay one.", 2);
        UserPracticeWritingAnswer answer2 = writingAnswer("answer-2", submission, 2, "Essay two.", 2);
        answer1.setCriterionFeedbacks(new ArrayList<>(List.of(existingFeedback(WritingCriterionName.TASK_RESPONSE))));
        answer2.setCriterionFeedbacks(new ArrayList<>(List.of(existingFeedback(WritingCriterionName.COHERENCE_AND_COHESION))));

        when(userPracticeWritingAnswerRepository.findAllBySubmission_IdOrderByOrderIndexAsc(submissionId))
                .thenReturn(List.of(answer1, answer2));

        writingFeedbackService.createWritingFeedback(submissionId);

        verify(submissionRepository, never()).findWithContent(anyString());
        verify(aiService, never()).callAIUnified(anyString(), anyList());
        verify(userPracticeWritingCriterionFeedbackRepository, never())
                .save(any(UserPracticeWritingCriterionFeedback.class));
    }

    @Test
    void createWritingFeedback_whenNoWritingAnswersExist_shouldThrowRuntimeExceptionAndSkipSubmissionLookup() {
        when(userPracticeWritingAnswerRepository.findAllBySubmission_IdOrderByOrderIndexAsc("submission-1"))
                .thenReturn(List.of());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> writingFeedbackService.createWritingFeedback("submission-1")
        );

        assertThat(exception.getMessage()).isEqualTo("No writing answers found");

        verify(submissionRepository, never()).findWithContent(anyString());
        verify(aiService, never()).callAIUnified(anyString(), anyList());
        verify(userPracticeWritingCriterionFeedbackRepository, never())
                .save(any(UserPracticeWritingCriterionFeedback.class));
    }

    @Test
    void createWritingFeedback_whenSubmissionDoesNotExist_shouldThrowRuntimeExceptionAndSkipAI() {
        String submissionId = "submission-1";
        UserPracticeSubmission submission = writingSubmission(submissionId, practiceContent(PracticeTaskType.TASK_2));
        UserPracticeWritingAnswer answer = writingAnswer("answer-1", submission, 1, "Essay.", 1);

        when(userPracticeWritingAnswerRepository.findAllBySubmission_IdOrderByOrderIndexAsc(submissionId))
                .thenReturn(List.of(answer));
        when(submissionRepository.findWithContent(submissionId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> writingFeedbackService.createWritingFeedback(submissionId)
        );

        assertThat(exception.getMessage()).isEqualTo("Submission not found");

        verify(aiService, never()).callAIUnified(anyString(), anyList());
        verify(userPracticeWritingCriterionFeedbackRepository, never())
                .save(any(UserPracticeWritingCriterionFeedback.class));
    }

    @Test
    void createWritingFeedback_whenContentIsReadingPracticeContent_shouldThrowIllegalArgumentExceptionAndSkipAI() {
        String submissionId = "submission-1";
        ReadingPracticeContent content = new ReadingPracticeContent();
        content.setTitle("Reading Practice");
        content.setInstructions("Reading instruction");
        content.setTask(PracticeTaskType.TASK_1);

        UserPracticeSubmission submission = writingSubmission(submissionId, content);
        UserPracticeWritingAnswer answer = writingAnswer("answer-1", submission, 1, "Essay.", 1);

        when(userPracticeWritingAnswerRepository.findAllBySubmission_IdOrderByOrderIndexAsc(submissionId))
                .thenReturn(List.of(answer));
        when(submissionRepository.findWithContent(submissionId)).thenReturn(Optional.of(submission));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> writingFeedbackService.createWritingFeedback(submissionId)
        );

        assertThat(exception.getMessage()).isEqualTo("Invalid content type for writing");

        verify(aiService, never()).callAIUnified(anyString(), anyList());
        verify(userPracticeWritingCriterionFeedbackRepository, never())
                .save(any(UserPracticeWritingCriterionFeedback.class));
    }

    @Test
    void createWritingFeedback_whenAIFailsThreeTimes_shouldThrowRuntimeExceptionAndSkipSave() {
        String submissionId = "submission-1";
        UserPracticeSubmission submission = writingSubmission(submissionId, practiceContent(PracticeTaskType.TASK_2));
        UserPracticeWritingAnswer answer = writingAnswer("answer-1", submission, 1, "Essay.", 1);

        when(userPracticeWritingAnswerRepository.findAllBySubmission_IdOrderByOrderIndexAsc(submissionId))
                .thenReturn(List.of(answer));
        when(submissionRepository.findWithContent(submissionId)).thenReturn(Optional.of(submission));
        when(aiService.stripCustomTags(anyString())).thenReturn("Clean instruction");
        when(aiService.extractImages(anyString())).thenReturn(List.of());
        when(aiService.toPublicUrls(List.of())).thenReturn(List.of());
        when(aiService.toJson(anyList())).thenReturn("essays-json");
        when(aiService.normalizeInstruction("Clean instruction")).thenReturn("Normalized instruction");
        when(aiService.callAIUnified(anyString(), anyList())).thenReturn("raw-ai-response");
        when(aiService.parseWritingResponse("raw-ai-response"))
                .thenThrow(new RuntimeException("parse failed"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> writingFeedbackService.createWritingFeedback(submissionId)
        );

        assertThat(exception.getMessage()).isEqualTo("AI failed after retries");
        assertThat(exception.getCause()).hasMessage("parse failed");

        verify(aiService, org.mockito.Mockito.times(3)).callAIUnified(anyString(), anyList());
        verify(aiService, org.mockito.Mockito.times(3)).parseWritingResponse("raw-ai-response");
        verify(userPracticeWritingCriterionFeedbackRepository, never())
                .save(any(UserPracticeWritingCriterionFeedback.class));
    }

    @Test
    void createWritingFeedback_whenAIFirstAttemptFailsAndSecondAttemptSucceeds_shouldRetryWithStricterPromptAndSave() {
        String submissionId = "submission-1";
        UserPracticeSubmission submission = writingSubmission(submissionId, practiceContent(PracticeTaskType.TASK_2));
        UserPracticeWritingAnswer answer = writingAnswer("answer-1", submission, 1, "Essay.", 1);
        WritingFeedbackResponseDto aiResponse = writingResponse(
                answerFeedback(
                        1,
                        feedbackItem(
                                "TASK_RESPONSE",
                                "WEAKNESS",
                                "unclear position",
                                "Position is unclear.",
                                "The essay does not state a clear opinion.",
                                List.of("Essay."),
                                "State your opinion clearly.",
                                "Put your opinion in the introduction."
                        )
                )
        );

        when(userPracticeWritingAnswerRepository.findAllBySubmission_IdOrderByOrderIndexAsc(submissionId))
                .thenReturn(List.of(answer));
        when(submissionRepository.findWithContent(submissionId)).thenReturn(Optional.of(submission));
        when(aiService.stripCustomTags(anyString())).thenReturn("Clean instruction");
        when(aiService.extractImages(anyString())).thenReturn(List.of());
        when(aiService.toPublicUrls(List.of())).thenReturn(List.of());
        when(aiService.toJson(anyList())).thenReturn("essays-json");
        when(aiService.normalizeInstruction("Clean instruction")).thenReturn("Normalized instruction");
        when(aiService.callAIUnified(anyString(), anyList()))
                .thenReturn("bad-response")
                .thenReturn("good-response");
        when(aiService.parseWritingResponse("bad-response"))
                .thenThrow(new RuntimeException("parse failed"));
        when(aiService.parseWritingResponse("good-response")).thenReturn(aiResponse);

        writingFeedbackService.createWritingFeedback(submissionId);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiService, org.mockito.Mockito.times(2)).callAIUnified(promptCaptor.capture(), anyList());

        assertThat(promptCaptor.getAllValues().get(0))
                .doesNotContain("CRITICAL: RETURN VALID JSON ONLY. NO TEXT.");
        assertThat(promptCaptor.getAllValues().get(1))
                .contains("CRITICAL: RETURN VALID JSON ONLY. NO TEXT.");

        ArgumentCaptor<UserPracticeWritingCriterionFeedback> feedbackCaptor =
                ArgumentCaptor.forClass(UserPracticeWritingCriterionFeedback.class);
        verify(userPracticeWritingCriterionFeedbackRepository).save(feedbackCaptor.capture());

        assertThat(feedbackCaptor.getValue().getFeedbackType()).isEqualTo(WritingFeedbackType.WEAKNESS);
        assertThat(feedbackCaptor.getValue().getLabel()).isEqualTo(WritingFeedbackLabel.UNCLEAR_POSITION);
    }

    @Test
    void createWritingFeedback_whenAIResponseHasUnknownOrderIndex_shouldThrowRuntimeExceptionAndSkipSave() {
        String submissionId = "submission-1";
        UserPracticeSubmission submission = writingSubmission(submissionId, practiceContent(PracticeTaskType.TASK_2));
        UserPracticeWritingAnswer answer = writingAnswer("answer-1", submission, 1, "Essay.", 1);
        WritingFeedbackResponseDto aiResponse = writingResponse(
                answerFeedback(
                        2,
                        feedbackItem(
                                "TASK_RESPONSE",
                                "STRENGTH",
                                "CLEAR_POSITION",
                                "Clear.",
                                "Clear.",
                                List.of("Essay."),
                                "Keep it.",
                                "Keep it."
                        )
                )
        );

        when(userPracticeWritingAnswerRepository.findAllBySubmission_IdOrderByOrderIndexAsc(submissionId))
                .thenReturn(List.of(answer));
        when(submissionRepository.findWithContent(submissionId)).thenReturn(Optional.of(submission));
        when(aiService.stripCustomTags(anyString())).thenReturn("Clean instruction");
        when(aiService.extractImages(anyString())).thenReturn(List.of());
        when(aiService.toPublicUrls(List.of())).thenReturn(List.of());
        when(aiService.toJson(anyList())).thenReturn("essays-json");
        when(aiService.normalizeInstruction("Clean instruction")).thenReturn("Normalized instruction");
        when(aiService.callAIUnified(anyString(), anyList())).thenReturn("raw-ai-response");
        when(aiService.parseWritingResponse("raw-ai-response")).thenReturn(aiResponse);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> writingFeedbackService.createWritingFeedback(submissionId)
        );

        assertThat(exception.getMessage()).isEqualTo("Answer not found for orderIndex=2");

        verify(userPracticeWritingCriterionFeedbackRepository, never())
                .save(any(UserPracticeWritingCriterionFeedback.class));
    }

    @Test
    void createWritingFeedback_whenAIResponseHasEmptyFeedbackList_shouldRetryThenThrowRuntimeExceptionAndSkipSave() {
        String submissionId = "submission-1";
        UserPracticeSubmission submission = writingSubmission(submissionId, practiceContent(PracticeTaskType.TASK_2));
        UserPracticeWritingAnswer answer = writingAnswer("answer-1", submission, 1, "Essay.", 1);
        WritingFeedbackResponseDto aiResponse = writingResponse(answerFeedback(1));

        when(userPracticeWritingAnswerRepository.findAllBySubmission_IdOrderByOrderIndexAsc(submissionId))
                .thenReturn(List.of(answer));
        when(submissionRepository.findWithContent(submissionId)).thenReturn(Optional.of(submission));
        when(aiService.stripCustomTags(anyString())).thenReturn("Clean instruction");
        when(aiService.extractImages(anyString())).thenReturn(List.of());
        when(aiService.toPublicUrls(List.of())).thenReturn(List.of());
        when(aiService.toJson(anyList())).thenReturn("essays-json");
        when(aiService.normalizeInstruction("Clean instruction")).thenReturn("Normalized instruction");
        when(aiService.callAIUnified(anyString(), anyList())).thenReturn("raw-ai-response");
        when(aiService.parseWritingResponse("raw-ai-response")).thenReturn(aiResponse);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> writingFeedbackService.createWritingFeedback(submissionId)
        );

        assertThat(exception.getMessage()).isEqualTo("AI failed after retries");
        assertThat(exception.getCause()).hasMessage("Empty feedback list");

        verify(aiService, org.mockito.Mockito.times(3)).callAIUnified(anyString(), anyList());
        verify(aiService, org.mockito.Mockito.times(3)).parseWritingResponse("raw-ai-response");
        verify(userPracticeWritingCriterionFeedbackRepository, never())
                .save(any(UserPracticeWritingCriterionFeedback.class));
    }

    @Test
    void createWritingFeedback_whenAIResponseHasInvalidEnum_shouldThrowRuntimeExceptionAndSkipSavingInvalidItem() {
        String submissionId = "submission-1";
        UserPracticeSubmission submission = writingSubmission(submissionId, practiceContent(PracticeTaskType.TASK_2));
        UserPracticeWritingAnswer answer = writingAnswer("answer-1", submission, 1, "Essay.", 1);
        WritingFeedbackResponseDto aiResponse = writingResponse(
                answerFeedback(
                        1,
                        feedbackItem(
                                "BAD_CRITERION",
                                "STRENGTH",
                                "CLEAR_POSITION",
                                "Clear.",
                                "Clear.",
                                List.of("Essay."),
                                "Keep it.",
                                "Keep it."
                        )
                )
        );

        when(userPracticeWritingAnswerRepository.findAllBySubmission_IdOrderByOrderIndexAsc(submissionId))
                .thenReturn(List.of(answer));
        when(submissionRepository.findWithContent(submissionId)).thenReturn(Optional.of(submission));
        when(aiService.stripCustomTags(anyString())).thenReturn("Clean instruction");
        when(aiService.extractImages(anyString())).thenReturn(List.of());
        when(aiService.toPublicUrls(List.of())).thenReturn(List.of());
        when(aiService.toJson(anyList())).thenReturn("essays-json");
        when(aiService.normalizeInstruction("Clean instruction")).thenReturn("Normalized instruction");
        when(aiService.callAIUnified(anyString(), anyList())).thenReturn("raw-ai-response");
        when(aiService.parseWritingResponse("raw-ai-response")).thenReturn(aiResponse);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> writingFeedbackService.createWritingFeedback(submissionId)
        );

        assertThat(exception.getMessage()).contains("Invalid enum from AI: criterionName=BAD_CRITERION");

        verify(userPracticeWritingCriterionFeedbackRepository, never())
                .save(any(UserPracticeWritingCriterionFeedback.class));
    }

    @Test
    void getWritingFeedback_whenFeedbackExists_shouldReturnSortedFeedbackDtos() {
        String submissionId = "submission-1";
        UserPracticeSubmission submission = writingSubmission(submissionId, practiceContent(PracticeTaskType.TASK_2));
        UserPracticeWritingAnswer answer = writingAnswer("answer-1", submission, 1, "Essay.", 1);

        UserPracticeWritingCriterionFeedback grammarWeakness =
                existingFeedback(WritingCriterionName.GRAMMATICAL_RANGE_AND_ACCURACY);
        grammarWeakness.setFeedbackType(WritingFeedbackType.WEAKNESS);
        grammarWeakness.setLabel(WritingFeedbackLabel.GRAMMAR_ERRORS);

        UserPracticeWritingCriterionFeedback taskWeakness =
                existingFeedback(WritingCriterionName.TASK_RESPONSE);
        taskWeakness.setFeedbackType(WritingFeedbackType.WEAKNESS);
        taskWeakness.setLabel(WritingFeedbackLabel.UNCLEAR_POSITION);

        UserPracticeWritingCriterionFeedback taskStrength =
                existingFeedback(WritingCriterionName.TASK_RESPONSE);
        taskStrength.setFeedbackType(WritingFeedbackType.STRENGTH);
        taskStrength.setLabel(WritingFeedbackLabel.CLEAR_POSITION);

        UserPracticeWritingCriterionFeedback coherenceStrength =
                existingFeedback(WritingCriterionName.COHERENCE_AND_COHESION);
        coherenceStrength.setFeedbackType(WritingFeedbackType.STRENGTH);
        coherenceStrength.setLabel(WritingFeedbackLabel.LOGICAL_ORGANISATION);

        answer.setCriterionFeedbacks(new ArrayList<>(List.of(
                grammarWeakness,
                taskWeakness,
                taskStrength,
                coherenceStrength
        )));

        when(userPracticeWritingAnswerRepository.findAllBySubmission_IdOrderByOrderIndexAsc(submissionId))
                .thenReturn(List.of(answer));

        WritingFeedbackResponseDto result = writingFeedbackService.getWritingFeedback(submissionId);

        assertThat(result).isNotNull();
        assertThat(result.getAnswers()).hasSize(1);
        assertThat(result.getAnswers().get(0).getOrderIndex()).isEqualTo(1);
        assertThat(result.getAnswers().get(0).getFeedbacks()).hasSize(4);

        List<WritingFeedbackItemDto> feedbacks = result.getAnswers().get(0).getFeedbacks();

        assertThat(feedbacks.get(0).getCriterionName()).isEqualTo("TASK_RESPONSE");
        assertThat(feedbacks.get(0).getFeedbackType()).isEqualTo("STRENGTH");
        assertThat(feedbacks.get(0).getLabel()).isEqualTo("CLEAR_POSITION");

        assertThat(feedbacks.get(1).getCriterionName()).isEqualTo("TASK_RESPONSE");
        assertThat(feedbacks.get(1).getFeedbackType()).isEqualTo("WEAKNESS");
        assertThat(feedbacks.get(1).getLabel()).isEqualTo("UNCLEAR_POSITION");

        assertThat(feedbacks.get(2).getCriterionName()).isEqualTo("COHERENCE_AND_COHESION");
        assertThat(feedbacks.get(2).getFeedbackType()).isEqualTo("STRENGTH");
        assertThat(feedbacks.get(2).getLabel()).isEqualTo("LOGICAL_ORGANISATION");

        assertThat(feedbacks.get(3).getCriterionName()).isEqualTo("GRAMMATICAL_RANGE_AND_ACCURACY");
        assertThat(feedbacks.get(3).getFeedbackType()).isEqualTo("WEAKNESS");
        assertThat(feedbacks.get(3).getLabel()).isEqualTo("GRAMMAR_ERRORS");

        assertThat(feedbacks.get(0).getDescription()).isEqualTo("Description.");
        assertThat(feedbacks.get(0).getExplanation()).isEqualTo("Explanation.");
        assertThat(feedbacks.get(0).getEvidenceSentences()).containsExactly("Evidence sentence.");
        assertThat(feedbacks.get(0).getRecommendedActionDescription()).isEqualTo("Action.");
        assertThat(feedbacks.get(0).getRecommendedActionExplanation()).isEqualTo("Action explanation.");

        verify(userPracticeWritingAnswerRepository).findAllBySubmission_IdOrderByOrderIndexAsc(submissionId);
    }

    @Test
    void getWritingFeedback_whenNoWritingAnswersExist_shouldThrowRuntimeException() {
        when(userPracticeWritingAnswerRepository.findAllBySubmission_IdOrderByOrderIndexAsc("submission-1"))
                .thenReturn(List.of());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> writingFeedbackService.getWritingFeedback("submission-1")
        );

        assertThat(exception.getMessage()).isEqualTo("No writing answers found");
    }

    @Test
    void getWritingFeedback_whenAnswerHasNoFeedback_shouldThrowRuntimeException() {
        String submissionId = "submission-1";
        UserPracticeSubmission submission = writingSubmission(submissionId, practiceContent(PracticeTaskType.TASK_2));
        UserPracticeWritingAnswer answer = writingAnswer("answer-1", submission, 3, "Essay.", 1);
        answer.setCriterionFeedbacks(new ArrayList<>());

        when(userPracticeWritingAnswerRepository.findAllBySubmission_IdOrderByOrderIndexAsc(submissionId))
                .thenReturn(List.of(answer));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> writingFeedbackService.getWritingFeedback(submissionId)
        );

        assertThat(exception.getMessage()).isEqualTo("Feedback not generated for answer orderIndex=3");
    }

    @Test
    void privateBuildWritingPrompt_whenCalled_shouldIncludeTitleNormalizedInstructionTimeAndEssaysJson() {
        UserPracticeSubmission submission = writingSubmission("submission-1", practiceContent(PracticeTaskType.TASK_1));
        UserPracticeWritingAnswer answer = writingAnswer("answer-1", submission, 1, "Task 1 essay.", 3);

        when(aiService.toJson(anyList())).thenReturn("essays-json");
        when(aiService.normalizeInstruction("Clean instruction")).thenReturn("Normalized instruction");

        String result = invokePrivateMethod(
                "buildWritingPrompt",
                new Class<?>[]{String.class, String.class, List.class, int.class},
                "Writing Task 1",
                "Clean instruction",
                List.of(answer),
                300
        );

        assertThat(result).contains("Writing Task 1");
        assertThat(result).contains("Normalized instruction");
        assertThat(result).contains("300");
        assertThat(result).contains("essays-json");
        assertThat(result).contains("TASK_1");
        assertThat(result).contains("TASK_ACHIEVEMENT");
        assertThat(result).contains("Return STRICT JSON ONLY");
    }

    @Test
    void privateNormalizeEnum_whenInputIsNullOrMessy_shouldReturnExpectedEnumText() {
        String nullResult = invokePrivateMethod(
                "normalizeEnum",
                new Class<?>[]{String.class},
                new Object[]{null}
        );
        String spaceResult = invokePrivateMethod(
                "normalizeEnum",
                new Class<?>[]{String.class},
                "clear position"
        );
        String hyphenResult = invokePrivateMethod(
                "normalizeEnum",
                new Class<?>[]{String.class},
                "under-length response"
        );
        String doubleUnderscoreResult = invokePrivateMethod(
                "normalizeEnum",
                new Class<?>[]{String.class},
                "CLEAR__POSITION"
        );

        assertThat(nullResult).isNull();
        assertThat(spaceResult).isEqualTo("CLEAR_POSITION");
        assertThat(hyphenResult).isEqualTo("UNDER_LENGTH_RESPONSE");
        assertThat(doubleUnderscoreResult).isEqualTo("CLEAR_POSITION");
    }

    @Test
    void privateGetCriterionOrder_whenEachCriterionIsProvided_shouldReturnExpectedOrder() {
        assertThat((Integer) invokePrivateMethod(
                "getCriterionOrder",
                new Class<?>[]{WritingCriterionName.class},
                WritingCriterionName.TASK_RESPONSE
        )).isZero();
        assertThat((Integer) invokePrivateMethod(
                "getCriterionOrder",
                new Class<?>[]{WritingCriterionName.class},
                WritingCriterionName.TASK_ACHIEVEMENT
        )).isZero();
        assertThat((Integer) invokePrivateMethod(
                "getCriterionOrder",
                new Class<?>[]{WritingCriterionName.class},
                WritingCriterionName.COHERENCE_AND_COHESION
        )).isEqualTo(1);
        assertThat((Integer) invokePrivateMethod(
                "getCriterionOrder",
                new Class<?>[]{WritingCriterionName.class},
                WritingCriterionName.LEXICAL_RESOURCE
        )).isEqualTo(2);
        assertThat((Integer) invokePrivateMethod(
                "getCriterionOrder",
                new Class<?>[]{WritingCriterionName.class},
                WritingCriterionName.GRAMMATICAL_RANGE_AND_ACCURACY
        )).isEqualTo(3);
    }

    @Test
    void privateValidateAIResponse_whenResponseIsValid_shouldNotThrowException() {
        WritingFeedbackResponseDto response = writingResponse(
                answerFeedback(
                        1,
                        feedbackItem(
                                "TASK_RESPONSE",
                                "STRENGTH",
                                "CLEAR_POSITION",
                                "Clear.",
                                "Clear.",
                                List.of("Essay."),
                                "Keep it.",
                                "Keep it."
                        )
                )
        );

        assertDoesNotThrow(() -> invokePrivateMethod(
                "validateAIResponse",
                new Class<?>[]{WritingFeedbackResponseDto.class},
                response
        ));
    }

    @Test
    void privateValidateAIResponse_whenResponseIsNull_shouldThrowRuntimeException() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> invokePrivateMethod(
                        "validateAIResponse",
                        new Class<?>[]{WritingFeedbackResponseDto.class},
                        new Object[]{null}
                )
        );

        assertThat(exception.getMessage()).isEqualTo("Invalid AI response: null");
    }

    @Test
    void privateValidateAIResponse_whenAnswersAreNull_shouldThrowRuntimeException() {
        WritingFeedbackResponseDto response = new WritingFeedbackResponseDto();
        response.setAnswers(null);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> invokePrivateMethod(
                        "validateAIResponse",
                        new Class<?>[]{WritingFeedbackResponseDto.class},
                        response
                )
        );

        assertThat(exception.getMessage()).isEqualTo("Invalid AI response: null");
    }

    @Test
    void privateValidateAIResponse_whenOrderIndexIsNull_shouldThrowRuntimeException() {
        WritingAnswerFeedbackDto answerDto = answerFeedback(null, validFeedbackItem());
        WritingFeedbackResponseDto response = writingResponse(answerDto);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> invokePrivateMethod(
                        "validateAIResponse",
                        new Class<?>[]{WritingFeedbackResponseDto.class},
                        response
                )
        );

        assertThat(exception.getMessage()).isEqualTo("Missing orderIndex");
    }

    @Test
    void privateValidateAIResponse_whenFeedbackListIsEmpty_shouldThrowRuntimeException() {
        WritingFeedbackResponseDto response = writingResponse(answerFeedback(1));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> invokePrivateMethod(
                        "validateAIResponse",
                        new Class<?>[]{WritingFeedbackResponseDto.class},
                        response
                )
        );

        assertThat(exception.getMessage()).isEqualTo("Empty feedback list");
    }

    @Test
    void privateValidateAIResponse_whenFeedbackListHasMoreThanThirtyTwoItems_shouldThrowRuntimeException() {
        List<WritingFeedbackItemDto> items = new ArrayList<>();

        for (int index = 0; index < 33; index++) {
            items.add(validFeedbackItem());
        }

        WritingAnswerFeedbackDto answerDto = new WritingAnswerFeedbackDto();
        answerDto.setOrderIndex(1);
        answerDto.setFeedbacks(items);

        WritingFeedbackResponseDto response = writingResponse(answerDto);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> invokePrivateMethod(
                        "validateAIResponse",
                        new Class<?>[]{WritingFeedbackResponseDto.class},
                        response
                )
        );

        assertThat(exception.getMessage()).isEqualTo("Too many feedback items");
    }

    @Test
    void privateValidateAIResponse_whenItemHasMissingEnum_shouldThrowRuntimeException() {
        WritingFeedbackItemDto item = validFeedbackItem();
        item.setLabel(null);

        WritingFeedbackResponseDto response = writingResponse(answerFeedback(1, item));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> invokePrivateMethod(
                        "validateAIResponse",
                        new Class<?>[]{WritingFeedbackResponseDto.class},
                        response
                )
        );

        assertThat(exception.getMessage()).isEqualTo("Invalid item: missing enum");
    }

    private UserPracticeSubmission writingSubmission(String id, PracticeContent content) {
        UserPracticeSubmission submission = new UserPracticeSubmission();

        submission.setId(id);
        submission.setPracticeContentId("content-1");
        submission.setPracticeContent(content);
        submission.setTimeSpentSeconds(600);

        return submission;
    }

    private PracticeContent practiceContent(PracticeTaskType taskType) {
        PracticeContent content = new PracticeContent();

        content.setTitle(taskType == PracticeTaskType.TASK_1 ? "Writing Task 1" : "Writing Task 2");
        content.setInstructions("[f]Instruction[/f] [img src=\"/uploads/task.png\"]");
        content.setTask(taskType);

        return content;
    }

    private UserPracticeWritingAnswer writingAnswer(String id,
                                                    UserPracticeSubmission submission,
                                                    Integer orderIndex,
                                                    String essayText,
                                                    Integer wordCount) {
        UserPracticeWritingAnswer answer = new UserPracticeWritingAnswer();

        answer.setId(id);
        answer.setSubmission(submission);
        answer.setOrderIndex(orderIndex);
        answer.setEssayText(essayText);
        answer.setWordCount(wordCount);
        answer.setCriterionFeedbacks(new ArrayList<>());

        return answer;
    }

    private UserPracticeWritingCriterionFeedback existingFeedback(WritingCriterionName criterionName) {
        UserPracticeWritingCriterionFeedback feedback = new UserPracticeWritingCriterionFeedback();

        feedback.setId("feedback-" + criterionName.name());
        feedback.setAuthorType(FeedbackAuthorType.AI);
        feedback.setCriterionName(criterionName);
        feedback.setFeedbackType(WritingFeedbackType.STRENGTH);
        feedback.setLabel(WritingFeedbackLabel.CLEAR_POSITION);
        feedback.setDescription("Description.");
        feedback.setExplanation("Explanation.");
        feedback.setEvidenceSentences(List.of("Evidence sentence."));
        feedback.setRecommendedActionDescription("Action.");
        feedback.setRecommendedActionExplanation("Action explanation.");

        return feedback;
    }

    private WritingFeedbackResponseDto writingResponse(WritingAnswerFeedbackDto... answers) {
        WritingFeedbackResponseDto response = new WritingFeedbackResponseDto();

        response.setAnswers(List.of(answers));

        return response;
    }

    private WritingAnswerFeedbackDto answerFeedback(Integer orderIndex,
                                                    WritingFeedbackItemDto... feedbackItems) {
        WritingAnswerFeedbackDto answerDto = new WritingAnswerFeedbackDto();

        answerDto.setOrderIndex(orderIndex);
        answerDto.setFeedbacks(new ArrayList<>(List.of(feedbackItems)));

        return answerDto;
    }

    private WritingFeedbackItemDto validFeedbackItem() {
        return feedbackItem(
                "TASK_RESPONSE",
                "STRENGTH",
                "CLEAR_POSITION",
                "Clear.",
                "Clear.",
                List.of("Essay."),
                "Keep it.",
                "Keep it."
        );
    }

    private WritingFeedbackItemDto feedbackItem(String criterionName,
                                                String feedbackType,
                                                String label,
                                                String description,
                                                String explanation,
                                                List<String> evidenceSentences,
                                                String recommendedActionDescription,
                                                String recommendedActionExplanation) {
        WritingFeedbackItemDto item = new WritingFeedbackItemDto();

        item.setCriterionName(criterionName);
        item.setFeedbackType(feedbackType);
        item.setLabel(label);
        item.setDescription(description);
        item.setExplanation(explanation);
        item.setEvidenceSentences(evidenceSentences);
        item.setRecommendedActionDescription(recommendedActionDescription);
        item.setRecommendedActionExplanation(recommendedActionExplanation);

        return item;
    }

    @SuppressWarnings("unchecked")
    private <T> T invokePrivateMethod(String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = WritingFeedbackServicelmpl.class.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return (T) method.invoke(writingFeedbackService, args);
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
