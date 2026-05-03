package com.ieltsmastermind.practice.attempt.management.persistence;

import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeWritingReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPracticeWritingReviewRepository extends JpaRepository<UserPracticeWritingReview, String> {

    List<UserPracticeWritingReview> findAllByWritingAnswer_IdOrderByCreatedAtAsc(
            String writingAnswerId
    );
    Optional<UserPracticeWritingReview> findByReviewedByUser_UserIdAndWritingAnswer_Id(
            String reviewedByUserId,
            String writingAnswerId
    );
}
