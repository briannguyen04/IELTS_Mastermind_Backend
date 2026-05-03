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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearnerStudyPlanAIUpdater {
    private final LearnerStudyPlanWeaknessBlockRepository weaknessRepository;
    private final LearnerStudyPlanStrengthBlockRepository strengthRepository;
    private final LearnerStudyPlanTaskRepository taskRepository;

    @Transactional
    public void saveAIResult(String studyPlanId, StudyPlanAIResponse res) {
        List<LearnerStudyPlanWeaknessBlock> weaknesses =
                weaknessRepository.findWeaknessBlocksByStudyPlanId(studyPlanId);

        List<LearnerStudyPlanStrengthBlock> strengths =
                strengthRepository.findStrengthBlocksByStudyPlanId(studyPlanId);

        Map<String, AIOutput> wMap = res.getWeaknesses().stream()
                .collect(Collectors.toMap(AIOutput::getId, Function.identity()));

        Map<String, AIOutput> sMap = res.getStrengths().stream()
                .collect(Collectors.toMap(AIOutput::getId, Function.identity()));

        Map<String, TaskOutput> tMap = res.getTasks().stream()
                .collect(Collectors.toMap(TaskOutput::getId, Function.identity()));

        updateWeaknessBlocks(weaknesses, wMap);
        updateStrengthBlocks(strengths, sMap);
        updateTasks(studyPlanId, tMap);
    }

    private void updateWeaknessBlocks(
            List<LearnerStudyPlanWeaknessBlock> weaknesses,
            Map<String, AIOutput> outputs
    ) {


        for (var w : weaknesses) {

            AIOutput out = outputs.get(w.getId());

            if (out == null) {
                continue;
            }




            // UPDATE
            w.setDescription(out.getDescription());
            w.setExplanation(out.getExplanation());
            w.setEvidence(out.getEvidence());
            w.setRecommendedNextAction(out.getRecommendation());


        }


    }

    private void updateStrengthBlocks(
            List<LearnerStudyPlanStrengthBlock> strengths,
            Map<String, AIOutput> outputs
    ) {
        for (var s : strengths) {
            AIOutput out = outputs.get(s.getId());
            if (out == null) continue;

            s.setDescription(out.getDescription());
            s.setExplanation(out.getExplanation());
            s.setEvidence(out.getEvidence());
            s.setRecommendedNextAction(out.getRecommendation());
        }
    }

    private void updateTasks(
            String studyPlanId,
            Map<String, TaskOutput> outputs
    ) {
        List<LearnerStudyPlanTask> tasks =
                taskRepository.findTasksByStudyPlanId(studyPlanId);

        for (var t : tasks) {
            TaskOutput out = outputs.get(t.getId());
            if (out == null) continue;

            t.setDescription(out.getDescription());
        }
    }
}