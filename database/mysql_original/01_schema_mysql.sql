-- ============================================================
-- PDTS — PUPOUS Document Tracking System
-- Database Schema | MySQL 8.0+
-- ============================================================

CREATE DATABASE IF NOT EXISTS pdts_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE pdts_db;

-- ============================================================
-- LOOKUP TABLES
-- ============================================================

CREATE TABLE educational_background_category (
    category_id          VARCHAR(10)  NOT NULL,
    category_name        VARCHAR(100) NOT NULL,
    category_code        VARCHAR(10)  NOT NULL UNIQUE,
    category_description TEXT,
    category_is_active   TINYINT(1)  NOT NULL DEFAULT 1,
    category_created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_ebc PRIMARY KEY (category_id)
) ENGINE=InnoDB;

CREATE TABLE role (
    role_id          INT         NOT NULL AUTO_INCREMENT,
    role_name        VARCHAR(50) NOT NULL UNIQUE,
    role_description TEXT,
    CONSTRAINT pk_role PRIMARY KEY (role_id)
) ENGINE=InnoDB;

CREATE TABLE permission (
    permission_id          INT          NOT NULL AUTO_INCREMENT,
    permission_name        VARCHAR(100) NOT NULL UNIQUE,
    permission_description TEXT,
    CONSTRAINT pk_permission PRIMARY KEY (permission_id)
) ENGINE=InnoDB;

CREATE TABLE role_permission (
    role_id       INT NOT NULL,
    permission_id INT NOT NULL,
    CONSTRAINT pk_role_permission PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_rp_role       FOREIGN KEY (role_id)       REFERENCES role(role_id),
    CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES permission(permission_id)
) ENGINE=InnoDB;

CREATE TABLE application_status (
    application_status_id    INT         NOT NULL AUTO_INCREMENT,
    application_status_name  VARCHAR(50) NOT NULL,
    application_status_color VARCHAR(10),
    CONSTRAINT pk_app_status PRIMARY KEY (application_status_id)
) ENGINE=InnoDB;

CREATE TABLE requirement_status (
    status_id                  INT         NOT NULL AUTO_INCREMENT,
    requirement_status_name    VARCHAR(50) NOT NULL,
    requirement_status_color   VARCHAR(10),
    requirement_status_desc    TEXT,
    is_final                   TINYINT(1)  NOT NULL DEFAULT 0,
    CONSTRAINT pk_req_status PRIMARY KEY (status_id)
) ENGINE=InnoDB;

CREATE TABLE requirement_type (
    type_id               INT          NOT NULL AUTO_INCREMENT,
    requirement_type_name VARCHAR(150) NOT NULL,
    type_is_active        TINYINT(1)  NOT NULL DEFAULT 1,
    CONSTRAINT pk_req_type PRIMARY KEY (type_id)
) ENGINE=InnoDB;

CREATE TABLE rejection_reason (
    rejection_reason_id          INT  NOT NULL AUTO_INCREMENT,
    rejection_reason_name        VARCHAR(100) NOT NULL,
    rejection_reason_description TEXT NOT NULL,
    rejection_reason_is_active   TINYINT(1) NOT NULL DEFAULT 1,
    CONSTRAINT pk_rejection_reason PRIMARY KEY (rejection_reason_id)
) ENGINE=InnoDB;

CREATE TABLE program (
    program_id   INT          NOT NULL AUTO_INCREMENT,
    program_name VARCHAR(200) NOT NULL,
    program_code VARCHAR(20),
    CONSTRAINT pk_program PRIMARY KEY (program_id)
) ENGINE=InnoDB;

CREATE TABLE campus (
    campus_id      INT          NOT NULL AUTO_INCREMENT,
    campus_name    VARCHAR(150) NOT NULL,
    campus_address TEXT,
    CONSTRAINT pk_campus PRIMARY KEY (campus_id)
) ENGINE=InnoDB;

CREATE TABLE deadline (
    deadline_id          INT      NOT NULL AUTO_INCREMENT,
    requirement_type_id  INT      NOT NULL,
    deadline_date        DATE     NOT NULL,
    deadline_description TEXT,
    CONSTRAINT pk_deadline PRIMARY KEY (deadline_id),
    CONSTRAINT fk_deadline_type FOREIGN KEY (requirement_type_id) REFERENCES requirement_type(type_id)
) ENGINE=InnoDB;

CREATE TABLE curriculum_requirement (
    category_id  VARCHAR(10) NOT NULL,
    type_id      INT         NOT NULL,
    is_mandatory TINYINT(1)  NOT NULL DEFAULT 1,
    CONSTRAINT pk_curriculum_req PRIMARY KEY (category_id, type_id),
    CONSTRAINT fk_cr_category FOREIGN KEY (category_id) REFERENCES educational_background_category(category_id),
    CONSTRAINT fk_cr_type     FOREIGN KEY (type_id)     REFERENCES requirement_type(type_id)
) ENGINE=InnoDB;

CREATE TABLE tracking_sequences (
    tracking_sequences_id            INT         NOT NULL AUTO_INCREMENT,
    tracking_sequences_entity_type   ENUM('student','document') NOT NULL UNIQUE,
    tracking_sequences_prefix        VARCHAR(5)  NOT NULL,
    tracking_sequences_last_sequence INT         NOT NULL DEFAULT 0,
    tracking_sequences_current_year  YEAR        NOT NULL,
    tracking_sequences_updated_at    DATETIME    ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_tracking PRIMARY KEY (tracking_sequences_id)
) ENGINE=InnoDB;

-- ============================================================
-- CORE ENTITIES
-- ============================================================

CREATE TABLE user (
    user_id            INT          NOT NULL AUTO_INCREMENT,
    user_last_name     VARCHAR(50)  NOT NULL,
    user_first_name    VARCHAR(50)  NOT NULL,
    user_middle_name   VARCHAR(50),
    user_suffix        VARCHAR(20),
    role_id            INT          NOT NULL,
    user_email_address VARCHAR(100) NOT NULL UNIQUE,
    user_password_hash VARCHAR(255) NOT NULL,
    user_is_active     TINYINT(1)  NOT NULL DEFAULT 1,
    user_created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_last_login    DATETIME,
    user_username      VARCHAR(50)  NOT NULL UNIQUE,
    CONSTRAINT pk_user   PRIMARY KEY (user_id),
    CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES role(role_id)
) ENGINE=InnoDB;

CREATE TABLE applicant (
    applicant_id                      INT          NOT NULL AUTO_INCREMENT,
    applicant_first_name              VARCHAR(50)  NOT NULL,
    applicant_middle_name             VARCHAR(50),
    applicant_last_name               VARCHAR(50)  NOT NULL,
    applicant_suffix                  VARCHAR(20),
    applicant_sex                     TINYINT(1)  NOT NULL,
    applicant_civil_status            TINYINT(1)  NOT NULL,
    applicant_house_number_street     VARCHAR(150),
    applicant_barangay                VARCHAR(100),
    applicant_city_municipality       VARCHAR(100),
    applicant_province                VARCHAR(100),
    applicant_region                  VARCHAR(100),
    applicant_zip_code                VARCHAR(10),
    applicant_birth_date              DATE         NOT NULL,
    applicant_email_address           VARCHAR(100) NOT NULL UNIQUE,
    applicant_contact_number          VARCHAR(20)  NOT NULL,
    educational_background_category_id VARCHAR(10) NOT NULL,
    applicant_enrollment_status       ENUM('on_leave','continuing') NOT NULL,
    applicant_is_protected            TINYINT(1)  NOT NULL DEFAULT 1,
    applicant_created_at              DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    applicant_updated_at              DATETIME    ON UPDATE CURRENT_TIMESTAMP,
    previous_education_id             INT,
    user_id                           INT          NOT NULL,
    CONSTRAINT pk_applicant PRIMARY KEY (applicant_id),
    CONSTRAINT fk_app_ebc  FOREIGN KEY (educational_background_category_id)
        REFERENCES educational_background_category(category_id),
    CONSTRAINT fk_app_user FOREIGN KEY (user_id) REFERENCES user(user_id),
    INDEX idx_applicant_last_name (applicant_last_name),
    INDEX idx_applicant_enrollment (applicant_enrollment_status)
) ENGINE=InnoDB;

-- Prevent deletion of applicant records
DELIMITER $$
CREATE TRIGGER trg_protect_applicant
BEFORE DELETE ON applicant
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'Applicant records are permanently protected and cannot be deleted.';
END$$
DELIMITER ;

CREATE TABLE previous_education (
    previous_education_id             INT          NOT NULL AUTO_INCREMENT,
    applicant_id                      INT          NOT NULL,
    educational_background_category_id VARCHAR(10) NOT NULL,
    mode_of_learning                  ENUM('Online','Face-to-face','Modular','Blended') NOT NULL,
    last_school_name                  VARCHAR(200) NOT NULL,
    school_address                    TEXT,
    year_graduated                    YEAR,
    track                             VARCHAR(100),
    strand                            VARCHAR(100),
    exam_center                       VARCHAR(200),
    year_passed                       YEAR,
    units_earned                      DECIMAL(5,1),
    last_course                       VARCHAR(150),
    CONSTRAINT pk_prev_ed PRIMARY KEY (previous_education_id),
    CONSTRAINT fk_pe_applicant FOREIGN KEY (applicant_id) REFERENCES applicant(applicant_id),
    CONSTRAINT fk_pe_ebc FOREIGN KEY (educational_background_category_id)
        REFERENCES educational_background_category(category_id)
) ENGINE=InnoDB;

CREATE TABLE applicant_emergency_contact (
    contact_id     INT          NOT NULL AUTO_INCREMENT,
    applicant_id   INT          NOT NULL,
    contact_name   VARCHAR(100) NOT NULL,
    relationship   VARCHAR(50)  NOT NULL,
    contact_number VARCHAR(20),
    contact_address TEXT,
    CONSTRAINT pk_ec PRIMARY KEY (contact_id),
    CONSTRAINT fk_ec_applicant FOREIGN KEY (applicant_id) REFERENCES applicant(applicant_id)
) ENGINE=InnoDB;

CREATE TABLE application (
    application_id               INT          NOT NULL AUTO_INCREMENT,
    applicant_id                 INT          NOT NULL,
    program_id                   INT          NOT NULL,
    campus_id                    INT          NOT NULL,
    application_status_id        INT          NOT NULL,
    deadline_id                  INT,
    application_date             DATE         NOT NULL,
    application_semester         VARCHAR(20)  NOT NULL,
    application_academic_year    VARCHAR(20)  NOT NULL,
    application_reference_number VARCHAR(30)  NOT NULL UNIQUE,
    application_last_notified_date DATETIME,
    CONSTRAINT pk_application  PRIMARY KEY (application_id),
    CONSTRAINT fk_appl_applicant FOREIGN KEY (applicant_id)          REFERENCES applicant(applicant_id),
    CONSTRAINT fk_appl_program   FOREIGN KEY (program_id)            REFERENCES program(program_id),
    CONSTRAINT fk_appl_campus    FOREIGN KEY (campus_id)             REFERENCES campus(campus_id),
    CONSTRAINT fk_appl_status    FOREIGN KEY (application_status_id) REFERENCES application_status(application_status_id),
    CONSTRAINT fk_appl_deadline  FOREIGN KEY (deadline_id)           REFERENCES deadline(deadline_id)
) ENGINE=InnoDB;

CREATE TABLE archived_record (
    archived_record_id            BIGINT       NOT NULL AUTO_INCREMENT,
    application_id                INT          NOT NULL,
    applicant_id                  INT          NOT NULL,
    archived_record_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    archived_record_reason        TEXT,
    requirement_type_id           INT,
    applicant_emergency_contact_id INT,
    archived_record_type          ENUM('File','Educational Background','Personal Info') NOT NULL,
    archived_record_source        VARCHAR(100),
    archived_record_data_snapshot JSON,
    archived_record_by_user_id    INT          NOT NULL,
    CONSTRAINT pk_archived PRIMARY KEY (archived_record_id),
    CONSTRAINT fk_ar_application FOREIGN KEY (application_id) REFERENCES application(application_id),
    CONSTRAINT fk_ar_applicant   FOREIGN KEY (applicant_id)   REFERENCES applicant(applicant_id),
    CONSTRAINT fk_ar_user        FOREIGN KEY (archived_record_by_user_id) REFERENCES user(user_id)
) ENGINE=InnoDB;

CREATE TABLE requirement (
    requirement_id                    INT          NOT NULL AUTO_INCREMENT,
    application_id                    INT          NOT NULL,
    requirement_type_id               INT          NOT NULL,
    requirement_status_id             INT          NOT NULL DEFAULT 1,
    requirement_tracking_no           VARCHAR(30)  NOT NULL UNIQUE,
    requirement_file_name             VARCHAR(255) NOT NULL,
    requirement_image_path            TEXT         NOT NULL,
    requirement_upload_date           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    requirement_uploaded_by_user_id   INT          NOT NULL,
    requirement_date_received         DATETIME,
    requirement_processed_by_user_id  INT,
    requirement_processed_at          DATETIME,
    rejection_reason_id               INT,
    rejection_reason_rejected_by_user_id INT,
    rejection_reason_rejected_at      DATETIME,
    requirement_remarks               TEXT,
    requirement_is_email_sent         TINYINT(1)  NOT NULL DEFAULT 0,
    requirement_has_archive_match     TINYINT(1)  NOT NULL DEFAULT 0,
    archive_id                        BIGINT,
    CONSTRAINT pk_requirement  PRIMARY KEY (requirement_id),
    CONSTRAINT fk_req_application FOREIGN KEY (application_id)           REFERENCES application(application_id),
    CONSTRAINT fk_req_type        FOREIGN KEY (requirement_type_id)       REFERENCES requirement_type(type_id),
    CONSTRAINT fk_req_status      FOREIGN KEY (requirement_status_id)     REFERENCES requirement_status(status_id),
    CONSTRAINT fk_req_uploader    FOREIGN KEY (requirement_uploaded_by_user_id) REFERENCES user(user_id),
    CONSTRAINT fk_req_processor   FOREIGN KEY (requirement_processed_by_user_id) REFERENCES user(user_id),
    CONSTRAINT fk_req_rejection   FOREIGN KEY (rejection_reason_id)       REFERENCES rejection_reason(rejection_reason_id),
    CONSTRAINT fk_req_archive     FOREIGN KEY (archive_id)                REFERENCES archived_record(archived_record_id),
    INDEX idx_req_status (requirement_status_id)
) ENGINE=InnoDB;

CREATE TABLE user_activity_log (
    user_activity_log_id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_activity_log_user_id     INT          NOT NULL,
    user_activity_log_action_type VARCHAR(100) NOT NULL,
    user_activity_log_entity_type VARCHAR(50)  NOT NULL,
    archived_record_id            BIGINT,
    user_activity_log_description TEXT,
    user_activity_log_old_value   TEXT,
    user_activity_log_new_value   TEXT,
    user_activity_log_ip_address  VARCHAR(45),
    user_activity_log_performed_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_ual  PRIMARY KEY (user_activity_log_id),
    CONSTRAINT fk_ual_user FOREIGN KEY (user_activity_log_user_id) REFERENCES user(user_id),
    INDEX idx_ual_performed_at (user_activity_log_performed_at)
) ENGINE=InnoDB;

-- ============================================================
-- TOKEN TABLES
-- ============================================================

CREATE TABLE applicant_access_token (
    token_id                  BIGINT      NOT NULL AUTO_INCREMENT,
    application_id            INT         NOT NULL UNIQUE,
    applicant_id              INT         NOT NULL,
    token_hash                VARCHAR(64) NOT NULL,
    token_prefix              VARCHAR(8)  NOT NULL,
    token_issued_at           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    token_expires_at          DATETIME    NOT NULL,
    token_is_revoked          TINYINT(1)  NOT NULL DEFAULT 0,
    token_last_used_at        DATETIME,
    token_use_count           INT         NOT NULL DEFAULT 0,
    token_email_sent          TINYINT(1)  NOT NULL DEFAULT 0,
    token_issued_by_user_id   INT         NOT NULL,
    token_revoked_by_user_id  INT,
    token_revoked_at          DATETIME,
    CONSTRAINT pk_aat          PRIMARY KEY (token_id),
    CONSTRAINT uq_aat_app      UNIQUE (application_id),
    CONSTRAINT fk_aat_app      FOREIGN KEY (application_id) REFERENCES application(application_id),
    CONSTRAINT fk_aat_applicant FOREIGN KEY (applicant_id)  REFERENCES applicant(applicant_id),
    CONSTRAINT fk_aat_issued   FOREIGN KEY (token_issued_by_user_id) REFERENCES user(user_id),
    CONSTRAINT fk_aat_revoked  FOREIGN KEY (token_revoked_by_user_id) REFERENCES user(user_id)
) ENGINE=InnoDB;

CREATE TABLE token_access_log (
    access_log_id            BIGINT      NOT NULL AUTO_INCREMENT,
    token_id                 BIGINT,
    application_reference_no VARCHAR(30) NOT NULL,
    access_log_ip_address    VARCHAR(45) NOT NULL,
    access_log_user_agent    VARCHAR(500),
    access_log_result        ENUM('SUCCESS','INVALID_TOKEN','EXPIRED_TOKEN','REVOKED_TOKEN','RATE_LIMITED','NOT_FOUND') NOT NULL,
    access_log_performed_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_tal  PRIMARY KEY (access_log_id),
    CONSTRAINT fk_tal_token FOREIGN KEY (token_id) REFERENCES applicant_access_token(token_id),
    INDEX idx_tal_token_time (token_id, access_log_performed_at),
    INDEX idx_tal_ip (access_log_ip_address)
) ENGINE=InnoDB;

-- ============================================================
-- PUBLIC PORTAL VIEW
-- ============================================================

CREATE OR REPLACE VIEW vw_student_status AS
SELECT
    a.application_reference_number,
    CONCAT(ap.applicant_first_name, ' ',
           COALESCE(ap.applicant_middle_name, ''), ' ',
           ap.applicant_last_name)          AS applicant_full_name,
    p.program_name,
    c.campus_name,
    a.application_semester,
    a.application_academic_year,
    ast.application_status_name,
    r.requirement_tracking_no,
    rt.requirement_type_name,
    rs.requirement_status_name,
    rs.requirement_status_color,
    rr.rejection_reason_description,
    r.requirement_upload_date,
    r.requirement_processed_at,
    CASE WHEN rs.status_id = 5 THEN r.requirement_remarks ELSE NULL END AS resubmission_notes
FROM application a
JOIN applicant         ap  ON ap.applicant_id         = a.applicant_id
JOIN program           p   ON p.program_id             = a.program_id
JOIN campus            c   ON c.campus_id              = a.campus_id
JOIN application_status ast ON ast.application_status_id = a.application_status_id
JOIN requirement       r   ON r.application_id         = a.application_id
JOIN requirement_type  rt  ON rt.type_id               = r.requirement_type_id
JOIN requirement_status rs ON rs.status_id             = r.requirement_status_id
LEFT JOIN rejection_reason rr ON rr.rejection_reason_id = r.rejection_reason_id;

-- Public portal DB user (run as root)
-- CREATE USER IF NOT EXISTS 'pdts_portal'@'localhost' IDENTIFIED BY 'portal_readonly_2025';
-- GRANT SELECT ON pdts_db.vw_student_status TO 'pdts_portal'@'localhost';
