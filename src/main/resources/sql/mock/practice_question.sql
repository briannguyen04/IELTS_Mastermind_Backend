START TRANSACTION;

SET @practiceContentId = '8143c599-5862-4513-b2c0-2c1ecf0a7471';

SET @q2 = UUID();
SET @q3 = UUID();
SET @q4 = UUID();
SET @q5 = UUID();
SET @q6 = UUID();
SET @q7 = UUID();
SET @q8 = UUID();
SET @q9 = UUID();
SET @q10 = UUID();
SET @q11 = UUID();
SET @q12 = UUID();
SET @q13 = UUID();
SET @q14 = UUID();
SET @q15 = UUID();
SET @q16 = UUID();
SET @q17 = UUID();
SET @q18 = UUID();
SET @q19 = UUID();
SET @q20 = UUID();

INSERT INTO ielts_mastermind_db.practice_question (
  practice_question_id,
  order_index,
  type,
  topic_tag,
  practice_content_id
) VALUES
(@q2,  2,  'FORM_COMPLETION',   'TRAVEL_TOURISM_AND_TRANSPORT', @practiceContentId),
(@q3,  3,  'FORM_COMPLETION',   'TRAVEL_TOURISM_AND_TRANSPORT', @practiceContentId),
(@q4,  4,  'FORM_COMPLETION',   'TRAVEL_TOURISM_AND_TRANSPORT', @practiceContentId),
(@q5,  5,  'FORM_COMPLETION',   'TRAVEL_TOURISM_AND_TRANSPORT', @practiceContentId),
(@q6,  6,  'FORM_COMPLETION',   'TRAVEL_TOURISM_AND_TRANSPORT', @practiceContentId),

(@q7,  7,  'MULTIPLE_CHOICE',   'TRAVEL_TOURISM_AND_TRANSPORT', @practiceContentId),

(@q8,  8,  'MULTIPLE_CHOICE',   'TRAVEL_TOURISM_AND_TRANSPORT', @practiceContentId),
(@q9,  9,  'MULTIPLE_CHOICE',   'TRAVEL_TOURISM_AND_TRANSPORT', @practiceContentId),
(@q10, 10, 'MULTIPLE_CHOICE',   'TRAVEL_TOURISM_AND_TRANSPORT', @practiceContentId),

(@q11, 11, 'NOTE_COMPLETION',   'SOCIETY_SOCIAL_BEHAVIOR_AND_VALUES', @practiceContentId),
(@q12, 12, 'NOTE_COMPLETION',   'SOCIETY_SOCIAL_BEHAVIOR_AND_VALUES', @practiceContentId),
(@q13, 13, 'NOTE_COMPLETION',   'SOCIETY_SOCIAL_BEHAVIOR_AND_VALUES', @practiceContentId),
(@q14, 14, 'NOTE_COMPLETION',   'SOCIETY_SOCIAL_BEHAVIOR_AND_VALUES', @practiceContentId),
(@q15, 15, 'NOTE_COMPLETION',   'SOCIETY_SOCIAL_BEHAVIOR_AND_VALUES', @practiceContentId),

(@q16, 16, 'MULTIPLE_CHOICE',   'SOCIETY_SOCIAL_BEHAVIOR_AND_VALUES', @practiceContentId),
(@q17, 17, 'MULTIPLE_CHOICE',   'SOCIETY_SOCIAL_BEHAVIOR_AND_VALUES', @practiceContentId),
(@q18, 18, 'MULTIPLE_CHOICE',   'SOCIETY_SOCIAL_BEHAVIOR_AND_VALUES', @practiceContentId),
(@q19, 19, 'MULTIPLE_CHOICE',   'SOCIETY_SOCIAL_BEHAVIOR_AND_VALUES', @practiceContentId),
(@q20, 20, 'MULTIPLE_CHOICE',   'SOCIETY_SOCIAL_BEHAVIOR_AND_VALUES', @practiceContentId);

INSERT INTO ielts_mastermind_db.practice_question_answer (
  practice_question_id,
  answer_index,
  answer_value
) VALUES
-- Questions 2–6: gap filling answers
(@q2,  0, 'T'),
(@q3,  0, 'T'),
(@q4,  0, 'T'),
(@q5,  0, 'T'),
(@q6,  0, 'T'),

-- Question 7: single choice
(@q7,  0, 'A'),

-- Questions 8–10: pick 3, same answer set for all related questions
(@q8,  0, 'A'),
(@q8,  1, 'B'),
(@q8,  2, 'C'),

(@q9,  0, 'A'),
(@q9,  1, 'B'),
(@q9,  2, 'C'),

(@q10, 0, 'A'),
(@q10, 1, 'B'),
(@q10, 2, 'C'),

-- Questions 11–15: gap filling answers
(@q11, 0, 'T'),
(@q12, 0, 'T'),
(@q13, 0, 'T'),
(@q14, 0, 'T'),
(@q15, 0, 'T'),

-- Questions 16–20: pick 5, same answer set for all related questions
(@q16, 0, 'A'),
(@q16, 1, 'B'),
(@q16, 2, 'C'),
(@q16, 3, 'D'),
(@q16, 4, 'E'),

(@q17, 0, 'A'),
(@q17, 1, 'B'),
(@q17, 2, 'C'),
(@q17, 3, 'D'),
(@q17, 4, 'E'),

(@q18, 0, 'A'),
(@q18, 1, 'B'),
(@q18, 2, 'C'),
(@q18, 3, 'D'),
(@q18, 4, 'E'),

(@q19, 0, 'A'),
(@q19, 1, 'B'),
(@q19, 2, 'C'),
(@q19, 3, 'D'),
(@q19, 4, 'E'),

(@q20, 0, 'A'),
(@q20, 1, 'B'),
(@q20, 2, 'C'),
(@q20, 3, 'D'),
(@q20, 4, 'E');

COMMIT;