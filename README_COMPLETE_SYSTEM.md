# PDTS Complete Dashboard + Basic CRUD Version

This version adds working pages for:

- Dashboard summaries from PostgreSQL
- Applicant list, create, view, edit
- Application creation during applicant registration
- Requirement/document list, upload, status update
- Staff user list, create, activate/deactivate
- Activity log viewing page

## Run order

1. Create database in pgAdmin:

```sql
CREATE DATABASE pdts_db;
```

2. Run scripts in this order:

```text
database/01_schema.sql
database/02_seed.sql
database/03_sample_data.sql   optional, for testing dashboard data
```

3. Check `src/main/resources/application.properties`.

Default PostgreSQL settings in this package:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/pdts_db
spring.datasource.username=postgres
spring.datasource.password=root
```

4. Run:

```bash
mvn clean spring-boot:run
```

5. Open:

```text
http://localhost:8080/login
```

Default staff login:

```text
Employee ID: admin001
Password: Admin@2025
```

## Notes

- This is a school/demo implementation. It uses `{noop}Admin@2025` for the seeded admin account so the local login is easier to test.
- CRUD uses normal Spring MVC pages and `JdbcTemplate` for reliability with the existing PostgreSQL schema.
- Applicant deletion is not implemented because the schema protects applicant records from deletion.
