-- ============================================================
-- PDTS — Seed Data | PostgreSQL
-- Run after 01_schema.sql
-- ============================================================

-- ============================================================
-- Curriculum types
-- ============================================================

INSERT INTO educational_background_category (
    category_id,
    category_name,
    category_code,
    category_description,
    category_is_active,
    category_created_at
) VALUES
('OLD-001', 'Old Curriculum', 'OLD', 'Pre-K12 traditional high school program.', 1, NOW()),
('SHS-002', 'Senior High School', 'SHS', 'K-12 SHS graduate (Grades 11-12).', 1, NOW()),
('ALS-003', 'Alternative Learning System', 'ALS', 'Non-formal ALS / A&E passers.', 1, NOW()),
('COL-004', 'College Undergraduate', 'COL', 'Tertiary-level degree program applicants.', 1, NOW()),
('TVT-005', 'TVET', 'TVET', 'Technical-Vocational Education and Training graduates.', 1, NOW())
ON CONFLICT (category_id) DO NOTHING;

-- ============================================================
-- Roles
-- ============================================================

INSERT INTO role (role_id, role_name, role_description) VALUES
(1, 'Admission Personnel', 'Can create and update applicant profiles and upload documents.'),
(2, 'Admin', 'Can change document statuses and manage rejection reasons.'),
(3, 'Head Admission', 'Full system access including user management and logs.')
ON CONFLICT (role_name) DO NOTHING;

-- ============================================================
-- Permissions
-- ============================================================

INSERT INTO permission (permission_id, permission_name, permission_description) VALUES
(1, 'UPLOAD_DOCUMENT', 'Upload scanned document images for applicants.'),
(2, 'REJECT_DOCUMENT', 'Reject a submitted document with a reason.'),
(3, 'RECEIVE_DOCUMENT', 'Mark a document as Verified/Received.'),
(4, 'VIEW_LOGS', 'View the system activity audit trail.'),
(5, 'MANAGE_USERS', 'Create, deactivate, and manage staff accounts.'),
(6, 'MANAGE_REASONS', 'Add, edit, or deactivate rejection reasons.'),
(7, 'REVOKE_TOKEN', 'Revoke or regenerate applicant access tokens.'),
(8, 'FILTER_SEARCH', 'Use the advanced filter and search panel.'),
(9, 'MANAGE_SETTINGS', 'Manage system configuration, campuses, programs, and master data.')
ON CONFLICT (permission_name) DO NOTHING;

-- ============================================================
-- Role-permission mappings
-- ============================================================

INSERT INTO role_permission (role_id, permission_id) VALUES
(1,1),(1,8),
(2,1),(2,2),(2,3),(2,5),(2,6),(2,7),(2,8),
(3,1),(3,2),(3,3),(3,4),(3,5),(3,6),(3,7),(3,8),(3,9)
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ============================================================
-- Application statuses
-- ============================================================

INSERT INTO application_status (
    application_status_id,
    application_status_name,
    application_status_color
) VALUES
(1, 'Pending', '#FFA500'),
(2, 'Under Review', '#2E75B6'),
(3, 'Approved', '#28A745'),
(4, 'Rejected', '#DC3545')
ON CONFLICT (application_status_name) DO NOTHING;

-- ============================================================
-- Requirement statuses
-- ============================================================

INSERT INTO requirement_status (
    status_id,
    requirement_status_name,
    requirement_status_color,
    requirement_status_desc,
    is_final
) VALUES
(1, 'Pending', '#FFA500', 'Document uploaded; awaiting Registrar initial action.', 0),
(2, 'Under Review', '#2E75B6', 'Registrar is actively reviewing the document.', 0),
(3, 'Verified/Received', '#28A745', 'Document fully verified and accepted into the official record.', 1),
(4, 'Rejected', '#DC3545', 'Document denied; rejection reason recorded and emailed.', 1),
(5, 'For Resubmission', '#C8A951', 'Flagged for corrected resubmission; guidance notes attached.', 0)
ON CONFLICT (requirement_status_name) DO NOTHING;

-- ============================================================
-- Requirement types
-- ============================================================

INSERT INTO requirement_type (
    type_id,
    requirement_type_name,
    type_is_active
) VALUES
(1, 'PSA Birth Certificate', 1),
(2, 'Form 137 / Form 138', 1),
(3, 'Transcript of Records (TOR)', 1),
(4, 'Diploma (Certified Copy)', 1),
(5, '2x2 ID Pictures (4 pcs)', 1),
(6, 'X-Ray Result (within 6 months)', 1),
(7, 'Certificate of Good Moral Character', 1),
(8, 'NBI / Police Clearance', 1),
(9, 'Letter of Endorsement', 1),
(10, 'ALS Certificate of Rating', 1),
(11, 'TVET National Certificate (NC II/III)', 1),
(12, 'PSA Marriage Certificate', 1)
ON CONFLICT (requirement_type_name) DO NOTHING;

-- ============================================================
-- Rejection reasons
-- ============================================================

INSERT INTO rejection_reason (
    rejection_reason_name,
    rejection_reason_description,
    rejection_reason_is_active
) VALUES
('Document Blurry', 'The uploaded document image is too blurry to be legible. Please resubmit a clear, high-resolution scan.', 1),
('Expired Certificate', 'The submitted certificate or clearance has expired. Please provide a document issued within the last 6 months.', 1),
('Wrong Document Type', 'The uploaded file does not match the required document type. Please upload the correct document.', 1),
('Incomplete Document', 'The submitted document appears to be incomplete or is missing pages. Please resubmit the complete document.', 1),
('Photo Background Invalid', 'The ID photo background must be plain white. Colored or patterned backgrounds are not accepted.', 1),
('Unreadable File Format', 'The file format is not supported or is corrupted. Please resubmit as a clear JPEG or PDF.', 1),
('Not PSA-Authenticated', 'The submitted civil registry document is not PSA-authenticated. Please submit a PSA-authenticated copy.', 1),
('Old NSO Copy', 'The submitted document is an old NSO copy. Please submit the updated PSA-issued document.', 1),
('Blurred/Unrecognized Registry Number', 'The registry number is blurred, unreadable, or cannot be verified. Please submit a clearer copy.', 1),
('Name Mismatch', 'The name on the submitted document does not match the applicant record. Please submit a corrected document or supporting proof.', 1),
('Birth Date Mismatch', 'The birth date on the submitted document does not match the applicant record. Please submit the correct document.', 1)
ON CONFLICT (rejection_reason_name) DO NOTHING;

-- ============================================================
-- Programs
-- ============================================================

INSERT INTO program (
    program_name,
    program_code,
    program_is_active
) VALUES
('Bachelor of Science in Information Technology', 'BSIT', 1),
('Bachelor of Science in Business Administration', 'BSBA', 1),
('Bachelor of Science in Office Administration', 'BSOA', 1),
('Bachelor of Science in Tourism Management', 'BSTM', 1),
('Bachelor of Science in Entrepreneurship', 'BSENT', 1),
('Bachelor of Science in Criminology', 'BSCrim', 1),
('Bachelor of Science in Nursing', 'BSN', 1),
('Bachelor of Technology and Livelihood Education', 'BTLE', 1),
('NC II — Computer Hardware Servicing', 'NC2-CHS', 1),
('NC II — Bread and Pastry Production', 'NC2-BPP', 1)
ON CONFLICT (program_code) DO NOTHING;

-- ============================================================
-- Campuses
-- ============================================================

INSERT INTO campus (
    campus_name,
    campus_address,
    campus_is_active
) VALUES
('PUP Main Campus — Sta. Mesa, Manila', 'Anonas St., Sta. Mesa, Manila, 1008', 1),
('PUP Open University System', 'Anonas St., Sta. Mesa, Manila, 1008', 1),
('PUP Paranaque Campus', 'Dr. A. Santos Ave., Sucat, Paranaque City', 1),
('PUP Lopez, Quezon', 'Quezon Province Campus', 1),
('PUP San Juan Campus', 'San Juan, Metro Manila', 1)
ON CONFLICT (campus_name) DO NOTHING;

-- ============================================================
-- System settings
-- ============================================================

INSERT INTO system_setting (
    setting_key,
    setting_value,
    setting_label,
    setting_type,
    setting_options,
    setting_is_active,
    setting_updated_at
) VALUES
('academic_year', '2025-2026', 'Academic Year', 'text', NULL, 1, NOW()),
('current_semester', 'First Semester', 'Current Semester', 'select', 'First Semester,Second Semester,Summer', 1, NOW()),
('max_photo_upload_kb', '300', 'Max Photo Upload Size KB', 'number', NULL, 1, NOW()),
('email_reminder_day', 'Monday', 'Email Reminder Day', 'select', 'Monday,Tuesday,Wednesday,Thursday,Friday,Saturday,Sunday', 1, NOW()),
('portal_status', 'OPEN', 'Applicant Portal Status', 'select', 'OPEN,CLOSED', 1, NOW()),
('registrar_email', 'admin@pup.edu.ph', 'Registrar Email Address', 'email', NULL, 1, NOW()),
('school_year_label', 'AY 2025-2026', 'School Year Display Label', 'text', NULL, 1, NOW())
ON CONFLICT (setting_key) DO UPDATE SET
    setting_value = EXCLUDED.setting_value,
    setting_label = EXCLUDED.setting_label,
    setting_type = EXCLUDED.setting_type,
    setting_options = EXCLUDED.setting_options,
    setting_is_active = EXCLUDED.setting_is_active,
    setting_updated_at = NOW();

-- ============================================================
-- Tracking sequences
-- ============================================================

INSERT INTO tracking_sequences (
    tracking_sequences_entity_type,
    tracking_sequences_prefix,
    tracking_sequences_last_sequence,
    tracking_sequences_current_year
) VALUES
('student', 'STU', 0, EXTRACT(YEAR FROM CURRENT_DATE)::INT),
('document', 'DOC', 0, EXTRACT(YEAR FROM CURRENT_DATE)::INT)
ON CONFLICT (tracking_sequences_entity_type) DO NOTHING;

-- ============================================================
-- Curriculum requirements
-- ============================================================

INSERT INTO curriculum_requirement (category_id, type_id, is_mandatory) VALUES
('OLD-001',1,1),('OLD-001',2,1),('OLD-001',5,1),('OLD-001',6,1),('OLD-001',7,1),('OLD-001',8,1),
('SHS-002',1,1),('SHS-002',2,1),('SHS-002',4,1),('SHS-002',5,1),('SHS-002',6,1),('SHS-002',7,1),('SHS-002',8,1),
('ALS-003',1,1),('ALS-003',5,1),('ALS-003',10,1),('ALS-003',8,1),
('COL-004',1,1),('COL-004',3,1),('COL-004',4,1),('COL-004',5,1),('COL-004',6,1),('COL-004',7,1),('COL-004',8,1),('COL-004',9,1),
('TVT-005',1,1),('TVT-005',5,1),('TVT-005',11,1),('TVT-005',8,1),('TVT-005',7,1)
ON CONFLICT (category_id, type_id) DO NOTHING;

-- ============================================================
-- Default master admin account
-- Username: admin001
-- Password: Admin@2025
-- ============================================================

INSERT INTO app_user (
    user_last_name,
    user_first_name,
    user_middle_name,
    role_id,
    user_email_address,
    user_password_hash,
    user_username,
    user_is_active
) VALUES (
    'Administrator',
    'System',
    NULL,
    3,
    'admin@pup.edu.ph',
    '{noop}Admin@2025',
    'admin001',
    1
)
ON CONFLICT (user_username) DO NOTHING;
