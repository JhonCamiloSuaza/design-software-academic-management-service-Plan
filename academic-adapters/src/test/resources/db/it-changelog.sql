CREATE SCHEMA IF NOT EXISTS academic_management;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS academic_management.tech_line (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    code VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by UUID NOT NULL,
    CONSTRAINT pk_tech_line PRIMARY KEY (id)
);

ALTER TABLE academic_management.tech_line
    ADD CONSTRAINT uq_tech_line_code UNIQUE (code);

CREATE TABLE IF NOT EXISTS academic_management.tech_network (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    tech_line_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    code VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by UUID NOT NULL,
    CONSTRAINT pk_tech_network PRIMARY KEY (id),
    CONSTRAINT fk_tech_network_tech_line FOREIGN KEY (tech_line_id)
        REFERENCES academic_management.tech_line (id) ON DELETE RESTRICT
);

ALTER TABLE academic_management.tech_network
    ADD CONSTRAINT uq_tech_network_tech_line_id_code UNIQUE (tech_line_id, code);

CREATE TABLE IF NOT EXISTS academic_management.knowledge_network (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    tech_network_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    code VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by UUID NOT NULL,
    CONSTRAINT pk_knowledge_network PRIMARY KEY (id),
    CONSTRAINT fk_knowledge_network_tech_network FOREIGN KEY (tech_network_id)
        REFERENCES academic_management.tech_network (id) ON DELETE RESTRICT
);

ALTER TABLE academic_management.knowledge_network
    ADD CONSTRAINT uq_knowledge_network_tech_network_id_code UNIQUE (tech_network_id, code);

-- HU-ACAD-04 uses the real training_program shape from the Liquibase DDL.
CREATE TABLE IF NOT EXISTS academic_management.training_program (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    knowledge_network_id UUID NOT NULL,
    program_code VARCHAR(20) NOT NULL,
    name VARCHAR(200) NOT NULL,
    training_level VARCHAR(20) NOT NULL,
    total_hours INT NOT NULL,
    version INT NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    row_version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by UUID NOT NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,
    CONSTRAINT pk_training_program PRIMARY KEY (id),
    CONSTRAINT uq_training_program_program_code UNIQUE (program_code),
    CONSTRAINT fk_training_program_knowledge_network FOREIGN KEY (knowledge_network_id)
        REFERENCES academic_management.knowledge_network (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_training_program_training_level
        CHECK (training_level IN ('AUXILIARY', 'OPERATOR', 'TECHNICIAN', 'TECHNOLOGIST')),
    CONSTRAINT ck_training_program_total_hours CHECK (total_hours >= 0)
);

-- HU-ACAD-04 proves the frozen program_version snapshot.
-- HU-ACAD-05 also uses POST /api/v1/fichas to enforce the active-program invariant.
CREATE TABLE IF NOT EXISTS academic_management.enrollment_ficha (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    ficha_number VARCHAR(20) NOT NULL,
    program_id UUID NOT NULL,
    program_version INT NOT NULL,
    training_center_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'INDUCTION',
    start_date DATE NOT NULL,
    expected_end_date DATE,
    actual_end_date DATE,
    training_shift VARCHAR(20) NOT NULL,
    training_modality VARCHAR(20) NOT NULL,
    max_capacity INT NOT NULL,
    row_version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by UUID NOT NULL,
    CONSTRAINT pk_enrollment_ficha PRIMARY KEY (id),
    CONSTRAINT uq_enrollment_ficha_ficha_number UNIQUE (ficha_number),
    CONSTRAINT fk_enrollment_ficha_training_program FOREIGN KEY (program_id)
        REFERENCES academic_management.training_program (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
);

ALTER TABLE academic_management.enrollment_ficha
    ADD CONSTRAINT ck_enrollment_ficha_training_shift
    CHECK (training_shift IN ('DAY', 'NIGHT', 'MIXED'));

ALTER TABLE academic_management.enrollment_ficha
    ADD CONSTRAINT ck_enrollment_ficha_training_modality
    CHECK (training_modality IN ('IN_PERSON', 'VIRTUAL', 'HYBRID'));

ALTER TABLE academic_management.enrollment_ficha
    ADD CONSTRAINT ck_enrollment_ficha_max_capacity
    CHECK (max_capacity > 0);

ALTER TABLE academic_management.enrollment_ficha
    ADD CONSTRAINT ck_enrollment_ficha_status
    CHECK (status IN ('INDUCTION', 'EXECUTION', 'PRODUCTIVE_STAGE', 'COMPLETED', 'CANCELLED'));

CREATE TABLE IF NOT EXISTS academic_management.outbox (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    topic VARCHAR(120) NOT NULL,
    routing_key VARCHAR(120) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    retry_count INT NOT NULL DEFAULT 0,
    published_at TIMESTAMPTZ,
    dead_lettered_at TIMESTAMPTZ,
    last_error TEXT,
    CONSTRAINT pk_academic_outbox PRIMARY KEY (id),
    CONSTRAINT uq_academic_outbox_event_id UNIQUE (event_id),
    CONSTRAINT ck_academic_outbox_retry_count CHECK (retry_count >= 0)
);

CREATE INDEX IF NOT EXISTS ix_academic_outbox_pending
    ON academic_management.outbox (next_attempt_at, created_at)
    WHERE published_at IS NULL AND dead_lettered_at IS NULL;

-- HU-ACAD-06 uses the real competency and learning_outcome shapes from Liquibase.
CREATE TABLE IF NOT EXISTS academic_management.competency (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    program_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    sena_code VARCHAR(20) NOT NULL,
    hours INT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    row_version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by UUID NOT NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,
    CONSTRAINT pk_competency PRIMARY KEY (id),
    CONSTRAINT uq_competency_program_id_sena_code UNIQUE (program_id, sena_code),
    CONSTRAINT fk_competency_training_program FOREIGN KEY (program_id)
        REFERENCES academic_management.training_program (id)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT ck_competency_hours CHECK (hours >= 0)
);

CREATE TABLE IF NOT EXISTS academic_management.learning_outcome (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    competency_id UUID NOT NULL,
    code VARCHAR(20) NOT NULL,
    description TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    row_version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by UUID NOT NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,
    CONSTRAINT pk_learning_outcome PRIMARY KEY (id),
    CONSTRAINT uq_learning_outcome_competency_id_code UNIQUE (competency_id, code),
    CONSTRAINT fk_learning_outcome_competency FOREIGN KEY (competency_id)
        REFERENCES academic_management.competency (id)
        ON DELETE CASCADE ON UPDATE RESTRICT
);
