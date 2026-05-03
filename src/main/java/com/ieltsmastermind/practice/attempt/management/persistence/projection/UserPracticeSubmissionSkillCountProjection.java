package com.ieltsmastermind.practice.attempt.management.persistence.projection;

import com.ieltsmastermind.practice.content.management.domain.enums.PracticeContentSkill;

public interface UserPracticeSubmissionSkillCountProjection {
    PracticeContentSkill getSkill();
    Long getSubmissionCount();
}
