[README_PDTS_merged.md](https://github.com/user-attachments/files/27648677/README_PDTS_merged.md)
# PDTS — PUPOUS Document Tracking System

**Polytechnic University of the Philippines — Open University System**  
Office of the University Registrar | AY 2025–2026 | BSITOUMN 2-3

**Live site:** <https://pdts-im.onrender.com/login>

---

## Overview

PDTS, or the **PUPOUS Document Tracking System**, is a Spring Boot web application for managing applicant records, admission documents, document status updates, applicant tracking tokens, staff users, audit logs, and student-facing status lookup.

This README consolidates the previous project notes from:

- `README.md`
- `README_COMPLETE_SYSTEM.md`
- `README_POSTGRESQL.md`

The current project version is configured primarily for **PostgreSQL**, with legacy MySQL files preserved for reference.

---

## Main Features

- **Staff login and role-based access** using Spring Security.
- **Dashboard summaries** for applicants, applications, documents, and recent activity.
- **Applicant management** for creating, viewing, editing, and searching applicant records.
- **Application record creation** during applicant registration.
- **Document/requirement tracking** with upload, review, receive, reject, and resubmission workflows.
- **Tracking number generation** for student and document records.
- **Public status portal** where applicants can check their document status using a reference number and token.
- **Email notifications** for document status updates, applicant tokens, and manual email notices.
- **Staff user management** with activate/deactivate controls.
- **Rejection reason management** for standardizing document review feedback.
- **Activity log viewer** for audit trail monitoring.
- **Reports and tracking lookup pages** for administrative use.
- **Render-ready deployment** through the included `Dockerfile`.
- **Scheduled Render ping workflow** through `.github/workflows/daily-ping.yml`.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.5.14 |
| Web/UI | Thymeleaf, HTML5, CSS3, Vanilla JavaScript |
| Security | Spring Security, BCrypt, form login |
| Database | PostgreSQL 13+ |
| Persistence | Spring Data JPA, Hibernate, JdbcTemplate |
| Email | Resend HTTP API for the main PostgreSQL/Render setup |
| Build Tool | Maven |
| Deployment | Docker, Render |

---

## Project Structure

```text
pdts-IM-main/
├── .github/
│   └── workflows/
│       └── daily-ping.yml              # Scheduled ping for Render deployment
├── Dockerfile                          # Docker build for Render/container deployment
├── README.md                           # Main project documentation
├── database/
│   ├── 01_schema.sql                   # PostgreSQL schema
│   ├── 02_seed.sql                     # PostgreSQL seed data
│   ├── 03_sample_data.sql              # Optional test/sample records
│   └── mysql_original/                 # Original MySQL scripts kept for reference
├── pdts_db/
│   └── pdts_db.sqlproj                 # Database project file
├── pom.xml                             # Maven project configuration
└── src/
    └── main/
        ├── java/com/pdts/
        │   ├── PdtsApplication.java    # Spring Boot entry point
        │   ├── config/                 # Security and application properties classes
        │   ├── controller/             # MVC and REST controllers
        │   ├── model/                  # JPA entity models
        │   ├── repository/             # Spring Data repositories
        │   └── service/                # Business logic services
        └── resources/
            ├── application.properties  # Main PostgreSQL/Render configuration
            ├── application-mysql.properties
            ├── static/                 # CSS, JavaScript, images, icons
            └── templates/              # Thymeleaf pages
```

---

## Prerequisites

Install these before running the project locally:

- **Java JDK 21**
- **Maven 3.9+**
- **PostgreSQL 13+**
- **pgAdmin** or **psql** for running SQL scripts
- **VS Code**, IntelliJ IDEA, NetBeans, or another Java IDE
- **Git** for cloning and version control

> Note: This repository does not currently include Maven wrapper files such as `mvnw` or `mvnw.cmd`, so Maven must be installed on your computer before using `mvn` commands.

---

## Local Setup

### 1. Clone the repository

```bash
git clone <your-repository-url>
cd pdts-IM-main
```

If you downloaded the ZIP from GitHub, extract it and open the extracted folder in your IDE.

---

### 2. Create the PostgreSQL database

Open **pgAdmin** or **psql**, then create the database:

```sql
CREATE DATABASE pdts_db;
```

Connect to the new `pdts_db` database before running the project scripts.

---

### 3. Run the database scripts

Run the SQL files in this order:

```text
database/01_schema.sql
database/02_seed.sql
database/03_sample_data.sql   optional, for testing dashboard data
```

The first two scripts are required. The third script is optional and only adds sample applicant/application/document records for local testing.

---

### 4. Configure environment variables

The main configuration file is:

```text
src/main/resources/application.properties
```

It expects the following environment variables:

| Variable | Required | Example |
|---|---:|---|
| `DATABASE_URL` | Yes | `jdbc:postgresql://localhost:5432/pdts_db` |
| `DATABASE_USERNAME` | Yes | `postgres` |
| `DATABASE_PASSWORD` | Yes | `your_postgres_password` |
| `RESEND_API_KEY` | Required for email | `re_xxxxxxxxxxxxx` |
| `RESEND_FROM_EMAIL` | Optional | `PDTS Registrar <onboarding@resend.dev>` |
| `PORT` | Optional | `8080` locally or `10000` on Render |

For local development, you may set the variables through your terminal.

**Windows PowerShell:**

```powershell
$env:DATABASE_URL="jdbc:postgresql://localhost:5432/pdts_db"
$env:DATABASE_USERNAME="postgres"
$env:DATABASE_PASSWORD="your_postgres_password"
$env:RESEND_API_KEY="your_resend_api_key"
$env:PORT="8080"
```

**macOS/Linux:**

```bash
export DATABASE_URL="jdbc:postgresql://localhost:5432/pdts_db"
export DATABASE_USERNAME="postgres"
export DATABASE_PASSWORD="your_postgres_password"
export RESEND_API_KEY="your_resend_api_key"
export PORT="8080"
```

For a quick local test without email, you can still run the app without a valid Resend key, but email-sending features will fail until `RESEND_API_KEY` is configured.

---

### 5. Run the application

From the project root folder, run:

```bash
mvn spring-boot:run
```

Then open:

```text
http://localhost:8080/login
```

If you set a different `PORT`, use that port instead.

---

## Default Login

After running `database/02_seed.sql`, use the seeded staff account:

```text
Username: admin001
Password: Admin@2025
```

The seeded account is assigned to the **Head Admission** role.

---

## Important URLs

| URL | Purpose | Access |
|---|---|---|
| `/login` | Staff login page | Public |
| `/dashboard` | Main staff dashboard | Staff only |
| `/applicants` | Applicant list and management | Staff only |
| `/requirements` | Document/requirement tracking | Staff only |
| `/users` | Staff user management | Staff only |
| `/logs` | Activity log viewer | Staff only |
| `/email-notifications` | Manual email notification page | Staff only |
| `/tracking-lookup` | Tracking lookup utility | Staff only |
| `/reports` | Reports page | Staff only |
| `/` | Public applicant tracking/status lookup landing page | Public |
| `/api/portal/verify` | Public token/reference verification endpoint | Public |
| `/api/portal/status` | Public document status endpoint | Public |

---

## User Roles Seeded by the Database

The seed script creates these roles:

| Role | General Purpose |
|---|---|
| Admission Personnel | Can create/update applicant records and upload documents. |
| Admin | Can manage document review actions, users, rejection reasons, and applicant tokens. |
| Head Admission | Full system access, including user management and activity logs. |

---

## Database Notes

The PostgreSQL schema uses `app_user` instead of `user` because `user` can conflict with PostgreSQL reserved/special keywords.

Main database files:

```text
database/01_schema.sql        PostgreSQL schema
database/02_seed.sql          Required lookup data and default admin account
database/03_sample_data.sql   Optional sample applicant/application/document data
database/mysql_original/      Original MySQL files kept for reference
```

The schema includes:

- Applicant records
- Previous education records
- Emergency contacts
- Applications
- Requirement/document records
- Requirement statuses and types
- Rejection reasons
- Staff users, roles, and permissions
- Tracking sequences
- Applicant access tokens
- Token access logs
- User activity logs
- Public portal status view

---

## Email Configuration

The main PostgreSQL/Render version uses **Resend** through these properties:

```properties
resend.api-key=${RESEND_API_KEY}
resend.from-email=${RESEND_FROM_EMAIL:PDTS Registrar <onboarding@resend.dev>}
```

Email is used for:

- Sending applicant access tokens
- Sending document status updates
- Sending rejection/resubmission notices
- Sending manual staff-triggered notifications

Do not commit real API keys, email passwords, database URLs, or database passwords to GitHub. Use environment variables, Render environment settings, or GitHub Secrets.

---

## Running with Docker

The included `Dockerfile` builds the Spring Boot app using Maven and runs it with Java 21.

Build the image:

```bash
docker build -t pdts .
```

Run the container:

```bash
docker run --rm -p 10000:10000 \
  -e PORT=10000 \
  -e DATABASE_URL="jdbc:postgresql://host.docker.internal:5432/pdts_db" \
  -e DATABASE_USERNAME="postgres" \
  -e DATABASE_PASSWORD="your_postgres_password" \
  -e RESEND_API_KEY="your_resend_api_key" \
  pdts
```

Then open:

```text
http://localhost:10000/login
```

---

## Render Deployment Notes

This project is ready for Render/container deployment through the included `Dockerfile`.

Required Render environment variables:

```text
DATABASE_URL=jdbc:postgresql://<host>:<port>/<database>
DATABASE_USERNAME=<database_username>
DATABASE_PASSWORD=<database_password>
RESEND_API_KEY=<resend_api_key>
RESEND_FROM_EMAIL=PDTS Registrar <your_verified_sender>
PORT=10000
```

The app is configured to listen on:

```properties
server.port=${PORT:10000}
server.address=0.0.0.0
```

The GitHub Actions workflow in `.github/workflows/daily-ping.yml` currently pings:

```text
https://pdts-im.onrender.com/login
```

If your Render URL changes, update that workflow file before pushing to GitHub.

---

## MySQL / XAMPP Notes

The current main application setup is PostgreSQL.

Original MySQL scripts are still available in:

```text
database/mysql_original/
```

A legacy MySQL configuration file is also available:

```text
src/main/resources/application-mysql.properties
```

If you want to switch back to MySQL or XAMPP/MariaDB, you must also restore/add the MySQL JDBC dependency in `pom.xml`, because the current Maven configuration uses the PostgreSQL driver.

---

## Common Issues and Fixes

| Problem | Likely Cause | Fix |
|---|---|---|
| `mvn` is not recognized | Maven is not installed or not in PATH | Install Maven 3.9+ and restart the terminal. |
| `DATABASE_URL` error | Environment variable is missing | Set `DATABASE_URL=jdbc:postgresql://localhost:5432/pdts_db`. |
| `password authentication failed` | Wrong PostgreSQL password | Check `DATABASE_USERNAME` and `DATABASE_PASSWORD`. |
| `relation does not exist` | SQL scripts were not run | Run `01_schema.sql`, then `02_seed.sql`. |
| Login fails for `admin001` | Seed data not loaded or account inactive | Re-run/check `02_seed.sql` and confirm the `app_user` row exists. |
| Port already in use | Another app is using the same port | Set `PORT=8081` or stop the other process. |
| Email does not send | Missing Resend configuration | Set `RESEND_API_KEY` and a valid `RESEND_FROM_EMAIL`. |
| Uploaded files are missing after redeploy | Render free/container disk is ephemeral | Use persistent storage or external object storage for production uploads. |

---

## Before Pushing to GitHub

Recommended checklist:

- Keep only one main README: `README.md`.
- Do not commit API keys, passwords, `.env` files, or private database credentials.
- Confirm the live Render URL is correct.
- Confirm `database/01_schema.sql` and `database/02_seed.sql` are the current PostgreSQL scripts.
- Update the screenshots section if you add screenshots later.
- Add a license file if the project needs one.

---

## Academic Use Notice

This project was developed as a school/demo implementation for the Polytechnic University of the Philippines Open University System. Review and harden security, storage, logging, and deployment settings before using it in a real production environment.
