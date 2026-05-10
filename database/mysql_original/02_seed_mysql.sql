-- ============================================================
-- PDTS — Seed Data
-- Run after 01_schema.sql
-- ============================================================

USE pdts_db;

-- Curriculum types
INSERT INTO educational_background_category VALUES
('OLD-001', 'Old Curriculum',          'OLD',  'Pre-K12 traditional high school program.',              1, NOW()),
('SHS-002', 'Senior High School',      'SHS',  'K-12 SHS graduate (Grades 11-12).',                    1, NOW()),
('ALS-003', 'Alternative Learning System','ALS','Non-formal ALS / A&E passers.',                        1, NOW()),
('COL-004', 'College Undergraduate',   'COL',  'Tertiary-level degree program applicants.',             1, NOW()),
('TVT-005', 'TVET',                    'TVET', 'Technical-Vocational Education and Training graduates.', 1, NOW());

-- Roles
INSERT INTO role (role_name, role_description) VALUES
('Admission Personnel', 'Can create and update applicant profiles and upload documents.'),
('Admin',               'Can change document statuses and manage rejection reasons.'),
('Head Admission',      'Full system access including user management and logs.');

-- Permissions
INSERT INTO permission (permission_name, permission_description) VALUES
('UPLOAD_DOCUMENT',   'Upload scanned document images for applicants.'),
('REJECT_DOCUMENT',   'Reject a submitted document with a reason.'),
('RECEIVE_DOCUMENT',  'Mark a document as Verified/Received.'),
('VIEW_LOGS',         'View the system activity audit trail.'),
('MANAGE_USERS',      'Create, deactivate, and manage staff accounts.'),
('MANAGE_REASONS',    'Add, edit, or deactivate rejection reasons.'),
('REVOKE_TOKEN',      'Revoke or regenerate applicant access tokens.'),
('FILTER_SEARCH',     'Use the advanced filter and search panel.');

-- Role-permission mappings
-- Admission Personnel: upload, search
INSERT INTO role_permission VALUES (1,1),(1,8);
-- Admin: upload, reject, receive, search, view logs, manage reasons, revoke token
INSERT INTO role_permission VALUES (2,1),(2,2),(2,3),(2,5),(2,6),(2,7),(2,8);
-- Head Admission: all permissions
INSERT INTO role_permission VALUES (3,1),(3,2),(3,3),(3,4),(3,5),(3,6),(3,7),(3,8);

-- Application statuses
INSERT INTO application_status (application_status_name, application_status_color) VALUES
('Pending',       '#FFA500'),
('Under Review',  '#2E75B6'),
('Approved',      '#28A745'),
('Rejected',      '#DC3545');

-- Document processing statuses
INSERT INTO requirement_status (requirement_status_name, requirement_status_color, requirement_status_desc, is_final) VALUES
('Pending',          '#FFA500', 'Document uploaded; awaiting Registrar initial action.',          0),
('Under Review',     '#2E75B6', 'Registrar is actively reviewing the document.',                  0),
('Verified/Received','#28A745', 'Document fully verified and accepted into the official record.', 1),
('Rejected',         '#DC3545', 'Document denied; rejection reason recorded and emailed.',        1),
('For Resubmission', '#C8A951', 'Flagged for corrected resubmission; guidance notes attached.',   0);

-- Document types
INSERT INTO requirement_type (requirement_type_name) VALUES
('PSA Birth Certificate'),
('Form 137 / Form 138'),
('Transcript of Records (TOR)'),
('Diploma (Certified Copy)'),
('2x2 ID Pictures (4 pcs)'),
('X-Ray Result (within 6 months)'),
('Certificate of Good Moral Character'),
('NBI / Police Clearance'),
('Letter of Endorsement'),
('ALS Certificate of Rating'),
('TVET National Certificate (NC II/III)'),
('PSA Marriage Certificate');

-- Rejection reasons
INSERT INTO rejection_reason (rejection_reason_name, rejection_reason_description) VALUES
('Document Blurry',         'The uploaded document image is too blurry to be legible. Please resubmit a clear, high-resolution scan.'),
('Expired Certificate',     'The submitted certificate or clearance has expired. Please provide a document issued within the last 6 months.'),
('Wrong Document Type',     'The uploaded file does not match the required document type. Please upload the correct document.'),
('Incomplete Document',     'The submitted document appears to be incomplete or is missing pages. Please resubmit the complete document.'),
('Photo Background Invalid','The ID photo background must be plain white. Colored or patterned backgrounds are not accepted.'),
('Unreadable File Format',  'The file format is not supported or is corrupted. Please resubmit as a clear JPEG or PDF.');

-- Programs
INSERT INTO program (program_name, program_code) VALUES
('Bachelor of Science in Information Technology',   'BSIT'),
('Bachelor of Science in Business Administration',  'BSBA'),
('Bachelor of Science in Criminology',              'BSCrim'),
('Bachelor of Science in Nursing',                  'BSN'),
('Bachelor of Technology and Livelihood Education', 'BTLE'),
('NC II — Computer Hardware Servicing',             'NC2-CHS'),
('NC II — Bread and Pastry Production',             'NC2-BPP');

-- Campuses
INSERT INTO campus (campus_name, campus_address) VALUES
('PUP Main Campus — Sta. Mesa, Manila',         'Anonas St., Sta. Mesa, Manila, 1008'),
('PUP Open University System',                  'Anonas St., Sta. Mesa, Manila, 1008'),
('PUP Paranaque Campus',                        'Dr. A. Santos Ave., Sucat, Paranaque City'),
('PUP Lopez, Quezon',                           'Quezon Province Campus'),
('PUP San Juan Campus',                         'San Juan, Metro Manila');

-- Tracking sequences
INSERT INTO tracking_sequences (tracking_sequences_entity_type, tracking_sequences_prefix, tracking_sequences_last_sequence, tracking_sequences_current_year) VALUES
('student',  'STU', 0, YEAR(NOW())),
('document', 'DOC', 0, YEAR(NOW()));

-- Curriculum requirements (mandatory docs per curriculum)
-- Old Curriculum
INSERT INTO curriculum_requirement VALUES ('OLD-001',1,1),('OLD-001',2,1),('OLD-001',5,1),('OLD-001',6,1),('OLD-001',7,1),('OLD-001',8,1);
-- SHS
INSERT INTO curriculum_requirement VALUES ('SHS-002',1,1),('SHS-002',2,1),('SHS-002',4,1),('SHS-002',5,1),('SHS-002',6,1),('SHS-002',7,1),('SHS-002',8,1);
-- ALS
INSERT INTO curriculum_requirement VALUES ('ALS-003',1,1),('ALS-003',5,1),('ALS-003',10,1),('ALS-003',8,1);
-- College
INSERT INTO curriculum_requirement VALUES ('COL-004',1,1),('COL-004',3,1),('COL-004',4,1),('COL-004',5,1),('COL-004',6,1),('COL-004',7,1),('COL-004',8,1),('COL-004',9,1);
-- TVET
INSERT INTO curriculum_requirement VALUES ('TVT-005',1,1),('TVT-005',5,1),('TVT-005',11,1),('TVT-005',8,1),('TVT-005',7,1);

-- Default admin account (password: Admin@2025 — bcrypt hashed)
-- Generate a proper bcrypt hash in production; this is a placeholder
INSERT INTO users (user_last_name, user_first_name, role_id, user_email_address, user_password_hash, user_username) VALUES
('Administrator', 'System', 3, 'admin@pup.edu.ph', '$2a$10$placeholder_bcrypt_hash_replace_me_in_production_123456', 'admin001');
