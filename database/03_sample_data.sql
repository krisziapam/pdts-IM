-- Optional sample data for dashboard and CRUD testing.
-- Run this after 01_schema.sql and 02_seed.sql while connected to pdts_db.

INSERT INTO applicant (
    applicant_first_name, applicant_middle_name, applicant_last_name,
    applicant_sex, applicant_civil_status, applicant_house_number_street,
    applicant_barangay, applicant_city_municipality, applicant_province,
    applicant_region, applicant_zip_code, applicant_birth_date,
    applicant_email_address, applicant_contact_number,
    educational_background_category_id, applicant_enrollment_status, user_id
) VALUES
('Juan', 'Santos', 'Dela Cruz', 1, 1, '123 Mabini Street', 'Barangay 1', 'Manila', 'Metro Manila', 'NCR', '1008', '2003-05-15', 'juan.delacruz@example.com', '09171234567', 'SHS-002', 'continuing', 1),
('Maria', 'Reyes', 'Santos', 2, 1, '45 Bonifacio Avenue', 'Barangay 2', 'Quezon City', 'Metro Manila', 'NCR', '1100', '2002-09-21', 'maria.santos@example.com', '09181234567', 'COL-004', 'continuing', 1)
ON CONFLICT (applicant_email_address) DO NOTHING;

INSERT INTO application (
    applicant_id, program_id, campus_id, application_status_id,
    application_date, application_semester, application_academic_year,
    application_reference_number
)
SELECT applicant_id, 1, 2, 1, CURRENT_DATE, 'First Semester', '2026-2027', 'APP-2026-' || LPAD(applicant_id::TEXT, 4, '0')
FROM applicant
WHERE applicant_email_address IN ('juan.delacruz@example.com', 'maria.santos@example.com')
ON CONFLICT (application_reference_number) DO NOTHING;

INSERT INTO requirement (
    application_id, requirement_type_id, requirement_status_id,
    requirement_tracking_no, requirement_file_name, requirement_image_path,
    requirement_uploaded_by_user_id
)
SELECT a.application_id, 1, 1, 'DOC-2026-' || LPAD(a.application_id::TEXT, 4, '0'), 'birth_certificate.pdf', 'uploads/birth_certificate.pdf', 1
FROM application a
WHERE a.application_reference_number LIKE 'APP-2026-%'
ON CONFLICT (requirement_tracking_no) DO NOTHING;
