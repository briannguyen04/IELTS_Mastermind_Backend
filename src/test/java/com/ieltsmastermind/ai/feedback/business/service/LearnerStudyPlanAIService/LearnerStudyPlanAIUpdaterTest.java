package com.ieltsmastermind.ai.feedback.business.service.LearnerStudyPlanAIService;

import com.ieltsmastermind.ai.feedback.domain.dto.StudyPlanAIResponse;
import com.ieltsmastermind.ai.feedback.domain.dto.TaskOutput;
import com.ieltsmastermind.ai.feedback.domain.entity.AIOutput;
import com.ieltsmastermind.practice.studyplan.management.domain.entity.LearnerStudyPlanStrengthBlock;
import com.ieltsmastermind.practice.studyplan.management.domain.entity.LearnerStudyPlanTask;
import com.ieltsmastermind.practice.studyplan.management.domain.entity.LearnerStudyPlanWeaknessBlock;
import com.ieltsmastermind.practice.studyplan.management.persistence.LearnerStudyPlanStrengthBlockRepository;
import com.ieltsmastermind.practice.studyplan.management.persistence.LearnerStudyPlanTaskRepository;
import com.ieltsmastermind.practice.studyplan.management.persistence.LearnerStudyPlanWeaknessBlockRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearnerStudyPlanAIUpdaterTest {

    @Mock
    private LearnerStudyPlanWeaknessBlockRepository weaknessRepository;

    @Mock
    private LearnerStudyPlanStrengthBlockRepository strengthRepository;

    @Mock
    private LearnerStudyPlanTaskRepository taskRepository;

    @InjectMocks
    private LearnerStudyPlanAIUpdater learnerStudyPlanAIUpdater;

    @Test
    void saveAIResult_whenOutputsMatchBlocksAndTasks_shouldUpdateWeaknessStrengthAndTaskFields() {
        String studyPlanId = "study-plan-1";

        LearnerStudyPlanWeaknessBlock weaknessBlock = weaknessBlock(
                "weakness-1",
                "Old weakness description.",
                "Old weakness explanation.",
                "Old weakness evidence.",
                "Old weakness recommendation."
        );
        LearnerStudyPlanStrengthBlock strengthBlock = strengthBlock(
                "strength-1",
                "Old strength description.",
                "Old strength explanation.",
                "Old strength evidence.",
                "Old strength recommendation."
        );
        LearnerStudyPlanTask task = task("task-1", "Old task description.");

        StudyPlanAIResponse response = response(
                List.of(aiOutput(
                        "weakness-1",
                        "New weakness description.",
                        "New weakness explanation.",
                        "New weakness evidence.",
                        "New weakness recommendation."
                )),
                List.of(aiOutput(
                        "strength-1",
                        "New strength description.",
                        "New strength explanation.",
                        "New strength evidence.",
                        "New strength recommendation."
                )),
                List.of(taskOutput("task-1", "New task description."))
        );

        when(weaknessRepository.findWeaknessBlocksByStudyPlanId(studyPlanId))
                .thenReturn(List.of(weaknessBlock));
        when(strengthRepository.findStrengthBlocksByStudyPlanId(studyPlanId))
                .thenReturn(List.of(strengthBlock));
        when(taskRepository.findTasksByStudyPlanId(studyPlanId))
                .thenReturn(List.of(task));

        learnerStudyPlanAIUpdater.saveAIResult(studyPlanId, response);

        assertThat(weaknessBlock.getDescription()).isEqualTo("New weakness description.");
        assertThat(weaknessBlock.getExplanation()).isEqualTo("New weakness explanation.");
        assertThat(weaknessBlock.getEvidence()).isEqualTo("New weakness evidence.");
        assertThat(weaknessBlock.getRecommendedNextAction()).isEqualTo("New weakness recommendation.");

        assertThat(strengthBlock.getDescription()).isEqualTo("New strength description.");
        assertThat(strengthBlock.getExplanation()).isEqualTo("New strength explanation.");
        assertThat(strengthBlock.getEvidence()).isEqualTo("New strength evidence.");
        assertThat(strengthBlock.getRecommendedNextAction()).isEqualTo("New strength recommendation.");

        assertThat(task.getDescription()).isEqualTo("New task description.");

        verify(weaknessRepository).findWeaknessBlocksByStudyPlanId(studyPlanId);
        verify(strengthRepository).findStrengthBlocksByStudyPlanId(studyPlanId);
        verify(taskRepository).findTasksByStudyPlanId(studyPlanId);
    }

    @Test
    void saveAIResult_whenSomeOutputsDoNotMatchExistingEntities_shouldUpdateOnlyMatchingEntitiesAndKeepOthersUnchanged() {
        String studyPlanId = "study-plan-1";

        LearnerStudyPlanWeaknessBlock matchingWeakness = weaknessBlock(
                "weakness-1",
                "Old matching weakness description.",
                "Old matching weakness explanation.",
                "Old matching weakness evidence.",
                "Old matching weakness recommendation."
        );
        LearnerStudyPlanWeaknessBlock unmatchedWeakness = weaknessBlock(
                "weakness-2",
                "Old unmatched weakness description.",
                "Old unmatched weakness explanation.",
                "Old unmatched weakness evidence.",
                "Old unmatched weakness recommendation."
        );

        LearnerStudyPlanStrengthBlock matchingStrength = strengthBlock(
                "strength-1",
                "Old matching strength description.",
                "Old matching strength explanation.",
                "Old matching strength evidence.",
                "Old matching strength recommendation."
        );
        LearnerStudyPlanStrengthBlock unmatchedStrength = strengthBlock(
                "strength-2",
                "Old unmatched strength description.",
                "Old unmatched strength explanation.",
                "Old unmatched strength evidence.",
                "Old unmatched strength recommendation."
        );

        LearnerStudyPlanTask matchingTask = task("task-1", "Old matching task description.");
        LearnerStudyPlanTask unmatchedTask = task("task-2", "Old unmatched task description.");

        StudyPlanAIResponse response = response(
                List.of(
                        aiOutput(
                                "weakness-1",
                                "New matching weakness description.",
                                "New matching weakness explanation.",
                                "New matching weakness evidence.",
                                "New matching weakness recommendation."
                        ),
                        aiOutput(
                                "unknown-weakness",
                                "Ignored weakness description.",
                                "Ignored weakness explanation.",
                                "Ignored weakness evidence.",
                                "Ignored weakness recommendation."
                        )
                ),
                List.of(
                        aiOutput(
                                "strength-1",
                                "New matching strength description.",
                                "New matching strength explanation.",
                                "New matching strength evidence.",
                                "New matching strength recommendation."
                        ),
                        aiOutput(
                                "unknown-strength",
                                "Ignored strength description.",
                                "Ignored strength explanation.",
                                "Ignored strength evidence.",
                                "Ignored strength recommendation."
                        )
                ),
                List.of(
                        taskOutput("task-1", "New matching task description."),
                        taskOutput("unknown-task", "Ignored task description.")
                )
        );

        when(weaknessRepository.findWeaknessBlocksByStudyPlanId(studyPlanId))
                .thenReturn(List.of(matchingWeakness, unmatchedWeakness));
        when(strengthRepository.findStrengthBlocksByStudyPlanId(studyPlanId))
                .thenReturn(List.of(matchingStrength, unmatchedStrength));
        when(taskRepository.findTasksByStudyPlanId(studyPlanId))
                .thenReturn(List.of(matchingTask, unmatchedTask));

        learnerStudyPlanAIUpdater.saveAIResult(studyPlanId, response);

        assertThat(matchingWeakness.getDescription()).isEqualTo("New matching weakness description.");
        assertThat(matchingWeakness.getExplanation()).isEqualTo("New matching weakness explanation.");
        assertThat(matchingWeakness.getEvidence()).isEqualTo("New matching weakness evidence.");
        assertThat(matchingWeakness.getRecommendedNextAction()).isEqualTo("New matching weakness recommendation.");

        assertThat(unmatchedWeakness.getDescription()).isEqualTo("Old unmatched weakness description.");
        assertThat(unmatchedWeakness.getExplanation()).isEqualTo("Old unmatched weakness explanation.");
        assertThat(unmatchedWeakness.getEvidence()).isEqualTo("Old unmatched weakness evidence.");
        assertThat(unmatchedWeakness.getRecommendedNextAction()).isEqualTo("Old unmatched weakness recommendation.");

        assertThat(matchingStrength.getDescription()).isEqualTo("New matching strength description.");
        assertThat(matchingStrength.getExplanation()).isEqualTo("New matching strength explanation.");
        assertThat(matchingStrength.getEvidence()).isEqualTo("New matching strength evidence.");
        assertThat(matchingStrength.getRecommendedNextAction()).isEqualTo("New matching strength recommendation.");

        assertThat(unmatchedStrength.getDescription()).isEqualTo("Old unmatched strength description.");
        assertThat(unmatchedStrength.getExplanation()).isEqualTo("Old unmatched strength explanation.");
        assertThat(unmatchedStrength.getEvidence()).isEqualTo("Old unmatched strength evidence.");
        assertThat(unmatchedStrength.getRecommendedNextAction()).isEqualTo("Old unmatched strength recommendation.");

        assertThat(matchingTask.getDescription()).isEqualTo("New matching task description.");
        assertThat(unmatchedTask.getDescription()).isEqualTo("Old unmatched task description.");
    }

    @Test
    void saveAIResult_whenOutputListsAreEmpty_shouldLeaveAllExistingEntitiesUnchanged() {
        String studyPlanId = "study-plan-1";

        LearnerStudyPlanWeaknessBlock weaknessBlock = weaknessBlock(
                "weakness-1",
                "Old weakness description.",
                "Old weakness explanation.",
                "Old weakness evidence.",
                "Old weakness recommendation."
        );
        LearnerStudyPlanStrengthBlock strengthBlock = strengthBlock(
                "strength-1",
                "Old strength description.",
                "Old strength explanation.",
                "Old strength evidence.",
                "Old strength recommendation."
        );
        LearnerStudyPlanTask task = task("task-1", "Old task description.");

        StudyPlanAIResponse response = response(List.of(), List.of(), List.of());

        when(weaknessRepository.findWeaknessBlocksByStudyPlanId(studyPlanId))
                .thenReturn(List.of(weaknessBlock));
        when(strengthRepository.findStrengthBlocksByStudyPlanId(studyPlanId))
                .thenReturn(List.of(strengthBlock));
        when(taskRepository.findTasksByStudyPlanId(studyPlanId))
                .thenReturn(List.of(task));

        learnerStudyPlanAIUpdater.saveAIResult(studyPlanId, response);

        assertThat(weaknessBlock.getDescription()).isEqualTo("Old weakness description.");
        assertThat(weaknessBlock.getExplanation()).isEqualTo("Old weakness explanation.");
        assertThat(weaknessBlock.getEvidence()).isEqualTo("Old weakness evidence.");
        assertThat(weaknessBlock.getRecommendedNextAction()).isEqualTo("Old weakness recommendation.");

        assertThat(strengthBlock.getDescription()).isEqualTo("Old strength description.");
        assertThat(strengthBlock.getExplanation()).isEqualTo("Old strength explanation.");
        assertThat(strengthBlock.getEvidence()).isEqualTo("Old strength evidence.");
        assertThat(strengthBlock.getRecommendedNextAction()).isEqualTo("Old strength recommendation.");

        assertThat(task.getDescription()).isEqualTo("Old task description.");

        verify(weaknessRepository).findWeaknessBlocksByStudyPlanId(studyPlanId);
        verify(strengthRepository).findStrengthBlocksByStudyPlanId(studyPlanId);
        verify(taskRepository).findTasksByStudyPlanId(studyPlanId);
    }

    @Test
    void saveAIResult_whenRepositoriesReturnEmptyLists_shouldNotThrowAndShouldStillLoadTasks() {
        String studyPlanId = "study-plan-1";
        StudyPlanAIResponse response = response(
                List.of(aiOutput(
                        "weakness-1",
                        "Weakness description.",
                        "Weakness explanation.",
                        "Weakness evidence.",
                        "Weakness recommendation."
                )),
                List.of(aiOutput(
                        "strength-1",
                        "Strength description.",
                        "Strength explanation.",
                        "Strength evidence.",
                        "Strength recommendation."
                )),
                List.of(taskOutput("task-1", "Task description."))
        );

        when(weaknessRepository.findWeaknessBlocksByStudyPlanId(studyPlanId))
                .thenReturn(List.of());
        when(strengthRepository.findStrengthBlocksByStudyPlanId(studyPlanId))
                .thenReturn(List.of());
        when(taskRepository.findTasksByStudyPlanId(studyPlanId))
                .thenReturn(List.of());

        learnerStudyPlanAIUpdater.saveAIResult(studyPlanId, response);

        verify(weaknessRepository).findWeaknessBlocksByStudyPlanId(studyPlanId);
        verify(strengthRepository).findStrengthBlocksByStudyPlanId(studyPlanId);
        verify(taskRepository).findTasksByStudyPlanId(studyPlanId);
    }

    @Test
    void saveAIResult_whenAIOutputFieldsAreNull_shouldUpdateMatchingBlockFieldsToNull() {
        String studyPlanId = "study-plan-1";

        LearnerStudyPlanWeaknessBlock weaknessBlock = weaknessBlock(
                "weakness-1",
                "Old weakness description.",
                "Old weakness explanation.",
                "Old weakness evidence.",
                "Old weakness recommendation."
        );
        LearnerStudyPlanStrengthBlock strengthBlock = strengthBlock(
                "strength-1",
                "Old strength description.",
                "Old strength explanation.",
                "Old strength evidence.",
                "Old strength recommendation."
        );
        LearnerStudyPlanTask task = task("task-1", "Old task description.");

        StudyPlanAIResponse response = response(
                List.of(aiOutput("weakness-1", null, null, null, null)),
                List.of(aiOutput("strength-1", null, null, null, null)),
                List.of(taskOutput("task-1", null))
        );

        when(weaknessRepository.findWeaknessBlocksByStudyPlanId(studyPlanId))
                .thenReturn(List.of(weaknessBlock));
        when(strengthRepository.findStrengthBlocksByStudyPlanId(studyPlanId))
                .thenReturn(List.of(strengthBlock));
        when(taskRepository.findTasksByStudyPlanId(studyPlanId))
                .thenReturn(List.of(task));

        learnerStudyPlanAIUpdater.saveAIResult(studyPlanId, response);

        assertThat(weaknessBlock.getDescription()).isNull();
        assertThat(weaknessBlock.getExplanation()).isNull();
        assertThat(weaknessBlock.getEvidence()).isNull();
        assertThat(weaknessBlock.getRecommendedNextAction()).isNull();

        assertThat(strengthBlock.getDescription()).isNull();
        assertThat(strengthBlock.getExplanation()).isNull();
        assertThat(strengthBlock.getEvidence()).isNull();
        assertThat(strengthBlock.getRecommendedNextAction()).isNull();

        assertThat(task.getDescription()).isNull();
    }

    @Test
    void saveAIResult_whenResponseIsNull_shouldThrowNullPointerExceptionAfterLoadingBlocksAndSkipTasksLookup() {
        String studyPlanId = "study-plan-1";

        when(weaknessRepository.findWeaknessBlocksByStudyPlanId(studyPlanId))
                .thenReturn(List.of());
        when(strengthRepository.findStrengthBlocksByStudyPlanId(studyPlanId))
                .thenReturn(List.of());

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> learnerStudyPlanAIUpdater.saveAIResult(studyPlanId, null)
        );

        assertThat(exception).isNotNull();

        verify(weaknessRepository).findWeaknessBlocksByStudyPlanId(studyPlanId);
        verify(strengthRepository).findStrengthBlocksByStudyPlanId(studyPlanId);
        verify(taskRepository, never()).findTasksByStudyPlanId(studyPlanId);
    }

    @Test
    void saveAIResult_whenWeaknessOutputListIsNull_shouldThrowNullPointerExceptionAndSkipTasksLookup() {
        String studyPlanId = "study-plan-1";
        StudyPlanAIResponse response = response(null, List.of(), List.of());

        when(weaknessRepository.findWeaknessBlocksByStudyPlanId(studyPlanId))
                .thenReturn(List.of());
        when(strengthRepository.findStrengthBlocksByStudyPlanId(studyPlanId))
                .thenReturn(List.of());

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> learnerStudyPlanAIUpdater.saveAIResult(studyPlanId, response)
        );

        assertThat(exception).isNotNull();

        verify(weaknessRepository).findWeaknessBlocksByStudyPlanId(studyPlanId);
        verify(strengthRepository).findStrengthBlocksByStudyPlanId(studyPlanId);
        verify(taskRepository, never()).findTasksByStudyPlanId(studyPlanId);
    }

    @Test
    void saveAIResult_whenStrengthOutputListIsNull_shouldThrowNullPointerExceptionAndSkipTasksLookup() {
        String studyPlanId = "study-plan-1";
        StudyPlanAIResponse response = response(List.of(), null, List.of());

        when(weaknessRepository.findWeaknessBlocksByStudyPlanId(studyPlanId))
                .thenReturn(List.of());
        when(strengthRepository.findStrengthBlocksByStudyPlanId(studyPlanId))
                .thenReturn(List.of());

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> learnerStudyPlanAIUpdater.saveAIResult(studyPlanId, response)
        );

        assertThat(exception).isNotNull();

        verify(weaknessRepository).findWeaknessBlocksByStudyPlanId(studyPlanId);
        verify(strengthRepository).findStrengthBlocksByStudyPlanId(studyPlanId);
        verify(taskRepository, never()).findTasksByStudyPlanId(studyPlanId);
    }

    @Test
    void saveAIResult_whenTaskOutputListIsNull_shouldThrowNullPointerExceptionAndSkipTasksLookup() {
        String studyPlanId = "study-plan-1";
        StudyPlanAIResponse response = response(List.of(), List.of(), null);

        when(weaknessRepository.findWeaknessBlocksByStudyPlanId(studyPlanId))
                .thenReturn(List.of());
        when(strengthRepository.findStrengthBlocksByStudyPlanId(studyPlanId))
                .thenReturn(List.of());

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> learnerStudyPlanAIUpdater.saveAIResult(studyPlanId, response)
        );

        assertThat(exception).isNotNull();

        verify(weaknessRepository).findWeaknessBlocksByStudyPlanId(studyPlanId);
        verify(strengthRepository).findStrengthBlocksByStudyPlanId(studyPlanId);
        verify(taskRepository, never()).findTasksByStudyPlanId(studyPlanId);
    }

    @Test
    void saveAIResult_whenDuplicateWeaknessOutputIdsExist_shouldThrowIllegalStateExceptionAndSkipTaskLookup() {
        String studyPlanId = "study-plan-1";
        StudyPlanAIResponse response = response(
                List.of(
                        aiOutput("weakness-1", "Description 1.", "Explanation 1.", "Evidence 1.", "Recommendation 1."),
                        aiOutput("weakness-1", "Description 2.", "Explanation 2.", "Evidence 2.", "Recommendation 2.")
                ),
                List.of(),
                List.of()
        );

        when(weaknessRepository.findWeaknessBlocksByStudyPlanId(studyPlanId))
                .thenReturn(List.of());
        when(strengthRepository.findStrengthBlocksByStudyPlanId(studyPlanId))
                .thenReturn(List.of());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> learnerStudyPlanAIUpdater.saveAIResult(studyPlanId, response)
        );

        assertThat(exception).isNotNull();

        verify(taskRepository, never()).findTasksByStudyPlanId(studyPlanId);
    }

    @Test
    void saveAIResult_whenDuplicateStrengthOutputIdsExist_shouldThrowIllegalStateExceptionAndSkipTaskLookup() {
        String studyPlanId = "study-plan-1";
        StudyPlanAIResponse response = response(
                List.of(),
                List.of(
                        aiOutput("strength-1", "Description 1.", "Explanation 1.", "Evidence 1.", "Recommendation 1."),
                        aiOutput("strength-1", "Description 2.", "Explanation 2.", "Evidence 2.", "Recommendation 2.")
                ),
                List.of()
        );

        when(weaknessRepository.findWeaknessBlocksByStudyPlanId(studyPlanId))
                .thenReturn(List.of());
        when(strengthRepository.findStrengthBlocksByStudyPlanId(studyPlanId))
                .thenReturn(List.of());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> learnerStudyPlanAIUpdater.saveAIResult(studyPlanId, response)
        );

        assertThat(exception).isNotNull();

        verify(taskRepository, never()).findTasksByStudyPlanId(studyPlanId);
    }

    @Test
    void saveAIResult_whenDuplicateTaskOutputIdsExist_shouldThrowIllegalStateExceptionAndSkipTaskUpdates() {
        String studyPlanId = "study-plan-1";
        LearnerStudyPlanTask task = task("task-1", "Old task description.");

        StudyPlanAIResponse response = response(
                List.of(),
                List.of(),
                List.of(
                        taskOutput("task-1", "Task description 1."),
                        taskOutput("task-1", "Task description 2.")
                )
        );

        when(weaknessRepository.findWeaknessBlocksByStudyPlanId(studyPlanId))
                .thenReturn(List.of());
        when(strengthRepository.findStrengthBlocksByStudyPlanId(studyPlanId))
                .thenReturn(List.of());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> learnerStudyPlanAIUpdater.saveAIResult(studyPlanId, response)
        );

        assertThat(exception).isNotNull();
        assertThat(task.getDescription()).isEqualTo("Old task description.");

        verify(taskRepository, never()).findTasksByStudyPlanId(studyPlanId);
    }

    private StudyPlanAIResponse response(List<AIOutput> weaknesses,
                                         List<AIOutput> strengths,
                                         List<TaskOutput> tasks) {
        StudyPlanAIResponse response = new StudyPlanAIResponse();

        response.setWeaknesses(weaknesses);
        response.setStrengths(strengths);
        response.setTasks(tasks);

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
        TaskOutput output = new TaskOutput();

        output.setId(id);
        output.setDescription(description);

        return output;
    }

    private LearnerStudyPlanWeaknessBlock weaknessBlock(String id,
                                                        String description,
                                                        String explanation,
                                                        String evidence,
                                                        String recommendedNextAction) {
        LearnerStudyPlanWeaknessBlock block = new LearnerStudyPlanWeaknessBlock();

        block.setId(id);
        block.setDescription(description);
        block.setExplanation(explanation);
        block.setEvidence(evidence);
        block.setRecommendedNextAction(recommendedNextAction);

        return block;
    }

    private LearnerStudyPlanStrengthBlock strengthBlock(String id,
                                                        String description,
                                                        String explanation,
                                                        String evidence,
                                                        String recommendedNextAction) {
        LearnerStudyPlanStrengthBlock block = new LearnerStudyPlanStrengthBlock();

        block.setId(id);
        block.setDescription(description);
        block.setExplanation(explanation);
        block.setEvidence(evidence);
        block.setRecommendedNextAction(recommendedNextAction);

        return block;
    }

    private LearnerStudyPlanTask task(String id, String description) {
        LearnerStudyPlanTask task = new LearnerStudyPlanTask();

        task.setId(id);
        task.setDescription(description);

        return task;
    }
}
