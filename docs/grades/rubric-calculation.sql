-- Handoff provisório para AUTH-10-DB/DB-010.
-- Não aplicar no banco compartilhado sem revisão do time de dados.

CREATE TABLE academic_pi.rubric (
    id uuid PRIMARY KEY,
    code varchar(80) NOT NULL UNIQUE,
    name varchar(255) NOT NULL,
    created_by_id uuid,
    created_at timestamptz NOT NULL
);

CREATE TABLE academic_pi.rubric_version (
    id uuid PRIMARY KEY,
    rubric_id uuid NOT NULL REFERENCES academic_pi.rubric(id),
    version_number integer NOT NULL CHECK (version_number > 0),
    minimum_score numeric(10,4) NOT NULL,
    maximum_score numeric(10,4) NOT NULL,
    rounding_scale integer NOT NULL CHECK (rounding_scale BETWEEN 0 AND 8),
    rounding_mode varchar(32) NOT NULL,
    formula varchar(32) NOT NULL,
    created_by_id uuid,
    created_at timestamptz NOT NULL,
    CONSTRAINT uk_rubric_version_number UNIQUE (rubric_id, version_number),
    CONSTRAINT ck_rubric_version_limits CHECK (minimum_score < maximum_score)
);

CREATE TABLE academic_pi.rubric_criterion (
    id uuid PRIMARY KEY,
    rubric_version_id uuid NOT NULL REFERENCES academic_pi.rubric_version(id),
    code varchar(80) NOT NULL,
    name varchar(255) NOT NULL,
    weight numeric(10,8) NOT NULL CHECK (weight > 0),
    minimum_score numeric(10,4) NOT NULL,
    maximum_score numeric(10,4) NOT NULL,
    position integer NOT NULL CHECK (position >= 0),
    CONSTRAINT uk_rubric_criterion_code UNIQUE (rubric_version_id, code),
    CONSTRAINT ck_rubric_criterion_limits CHECK (minimum_score < maximum_score)
);

CREATE TABLE academic_pi.examination_board_evaluator (
    board_id uuid NOT NULL REFERENCES academic_pi.examination_board(id),
    evaluator_id uuid NOT NULL REFERENCES auth.user_account(id),
    PRIMARY KEY (board_id, evaluator_id)
);

ALTER TABLE academic_pi.evaluation
    ADD COLUMN rubric_version_id uuid REFERENCES academic_pi.rubric_version(id);

CREATE TABLE academic_pi.evaluation_criterion_score (
    id uuid PRIMARY KEY,
    evaluation_id uuid NOT NULL REFERENCES academic_pi.evaluation(id),
    criterion_id uuid NOT NULL REFERENCES academic_pi.rubric_criterion(id),
    score numeric(10,4) NOT NULL,
    CONSTRAINT uk_evaluation_criterion_score UNIQUE (evaluation_id, criterion_id)
);

CREATE UNIQUE INDEX uk_evaluation_board_evaluator
    ON academic_pi.evaluation (board_id, evaluator_id);
