USE ielts_mastermind_db;

SET @practice_content_id = '9816ec8b-cbab-4954-9ec6-f93dc06807de';

START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS tmp_old_practice_questions;
DROP TEMPORARY TABLE IF EXISTS tmp_practice_questions;

CREATE TEMPORARY TABLE tmp_old_practice_questions AS
SELECT practice_question_id
FROM practice_question
WHERE practice_content_id = @practice_content_id;

DELETE FROM practice_question_answer
WHERE practice_question_id IN (
    SELECT practice_question_id
    FROM tmp_old_practice_questions
);

DELETE FROM practice_question
WHERE practice_question_id IN (
    SELECT practice_question_id
    FROM tmp_old_practice_questions
);

CREATE TEMPORARY TABLE tmp_practice_questions AS
WITH RECURSIVE seq(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1
    FROM seq
    WHERE n < 40
)
SELECT
    UUID() AS practice_question_id,
    n AS order_index,
    CASE
        WHEN n BETWEEN 1 AND 6 THEN 'FORM_COMPLETION'
        WHEN n BETWEEN 7 AND 10 THEN 'MULTIPLE_CHOICE'
        WHEN n BETWEEN 11 AND 15 THEN 'NOTE_COMPLETION'
        WHEN n BETWEEN 16 AND 20 THEN 'SENTENCE_COMPLETION'
        WHEN n BETWEEN 21 AND 25 THEN 'FORM_COMPLETION'
        WHEN n BETWEEN 26 AND 30 THEN 'MULTIPLE_CHOICE'
        WHEN n BETWEEN 31 AND 35 THEN 'NOTE_COMPLETION'
        WHEN n BETWEEN 36 AND 40 THEN 'MULTIPLE_CHOICE'
    END AS type,
    'TRAVEL_TOURISM_AND_TRANSPORT' AS topic_tag,
    @practice_content_id AS practice_content_id
FROM seq;

INSERT INTO practice_question (
    practice_question_id,
    order_index,
    type,
    topic_tag,
    practice_content_id
)
SELECT
    practice_question_id,
    order_index,
    type,
    topic_tag,
    practice_content_id
FROM tmp_practice_questions;

INSERT INTO practice_question_answer (
    practice_question_id,
    answer_index,
    answer_value
)
SELECT
    practice_question_id,
    0,
    'A'
FROM tmp_practice_questions;

UPDATE practice_content
SET question_count = 40
WHERE practice_content_id = @practice_content_id;

DROP TEMPORARY TABLE tmp_old_practice_questions;
DROP TEMPORARY TABLE tmp_practice_questions;

COMMIT;