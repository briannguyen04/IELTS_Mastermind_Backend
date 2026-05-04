USE ielts_mastermind_db;

SET @practice_content_id = 'cafa853c-1323-410a-a407-c1ef23b72c56';

START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS tmp_practice_questions;

DELETE pqa
FROM practice_question_answer pqa
JOIN practice_question pq
    ON pqa.practice_question_id = pq.practice_question_id
WHERE pq.practice_content_id = @practice_content_id;

DELETE FROM practice_question
WHERE practice_content_id = @practice_content_id;

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
    ELT(
        FLOOR(1 + RAND() * 12),
        'MULTIPLE_CHOICE',
        'MATCHING',
        'PLAN_LABELLING',
        'MAP_LABELLING',
        'DIAGRAM_LABELLING',
        'FORM_COMPLETION',
        'NOTE_COMPLETION',
        'TABLE_COMPLETION',
        'FLOW_CHART_COMPLETION',
        'SUMMARY_COMPLETION',
        'SENTENCE_COMPLETION',
        'SHORT_ANSWER_QUESTIONS'
    ) AS type,
    ELT(
        FLOOR(1 + RAND() * 22),
        'EDUCATION_AND_LEARNING',
        'WORK_JOBS_AND_CAREERS',
        'TECHNOLOGY_INTERNET_AND_AI',
        'HEALTH_HEALTHCARE_AND_LIFESTYLE',
        'ENVIRONMENT_CLIMATE_AND_SUSTAINABILITY',
        'GOVERNMENT_LAW_AND_PUBLIC_POLICY',
        'SOCIETY_SOCIAL_BEHAVIOR_AND_VALUES',
        'FAMILY_CHILDREN_AND_AGEING',
        'MEDIA_ADVERTISING_AND_COMMUNICATION',
        'CULTURE_ART_TRADITIONS_AND_LANGUAGE',
        'TRAVEL_TOURISM_AND_TRANSPORT',
        'HOUSING_CITIES_AND_URBAN_RURAL_LIFE',
        'SCIENCE_RESEARCH_AND_INNOVATION',
        'BUSINESS_ECONOMY_AND_CONSUMER_BEHAVIOR',
        'FOOD_AGRICULTURE_AND_FARMING',
        'SPORT_LEISURE_AND_HOBBIES',
        'HISTORY_ARCHAEOLOGY_AND_HERITAGE',
        'ENERGY_NATURAL_RESOURCES_AND_INFRASTRUCTURE',
        'CRIME_SAFETY_AND_SECURITY',
        'GLOBALISATION_MIGRATION_AND_INTERNATIONAL_DEVELOPMENT',
        'POPULATION_AND_DEMOGRAPHICS',
        'ANIMALS_AND_WILDLIFE'
    ) AS topic_tag,
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

DROP TEMPORARY TABLE tmp_practice_questions;

COMMIT;