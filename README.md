# PDTS — PUPOUS Document Tracking System

**Polytechnic University of the Philippines — Open University System**
Office of the University Registrar | AY 2025–2026 | BSITOUMN 2-3

Online Website link: https://pdts-im.onrender.com/login

---

## Tech Stack

| Layer      | Technology                          |
|------------|-------------------------------------|
| Frontend   | HTML5 · CSS3 · Vanilla JavaScript   |
| Backend    | Java 17 · Spring Boot 3.x           |
| Database   | MySQL 8.0 · MySQL Workbench         |
| Email      | JavaMail API / SMTP                 |
| Security   | Spring Security · BCrypt            |
| Templates  | Thymeleaf                           |

---

## Prerequisites

Before running PDTS, install the following:

- **Java JDK 17** (LTS) — [https://adoptium.net](https://adoptium.net)
- **Maven 3.8+** — [https://maven.apache.org/download.cgi](https://maven.apache.org/download.cgi)
- **MySQL 8.0+** — [https://dev.mysql.com/downloads/mysql](https://dev.mysql.com/downloads/mysql)
- **MySQL Workbench** — [https://dev.mysql.com/downloads/workbench](https://dev.mysql.com/downloads/workbench)
- **VS Code** — with the extension pack listed in `.vscode/extensions.json`

---

## Step 1 — Clone / Open in VS Code

```bash
# If cloning from a repo
git clone <your-repo-url>
cd pdts-project

# Or open the folder directly in VS Code
code .
```

VS Code will detect the Maven project and prompt you to install recommended extensions.
Accept all recommended extensions from `.vscode/extensions.json`.

---

## Step 2 — Set Up the Database

1. Open **MySQL Workbench** and connect to your local MySQL server.

2. Run the schema script:
   ```
   File → Open SQL Script → database/01_schema.sql → Execute (⚡)
   ```

3. Run the seed data script:
   ```
   File → Open SQL Script → database/02_seed.sql → Execute (⚡)
   ```

4. Verify the `pdts_db` database has been created with all tables.

---

## Step 3 — Configure the Application

Open `src/main/resources/application.properties` and update:

```properties
# Your MySQL credentials
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD

# Your SMTP email (for automated notifications)
spring.mail.username=your_email@gmail.com
spring.mail.password=your_app_password
```

> **Gmail users:** Generate an App Password at
> [https://myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords)
> (requires 2FA enabled). Use the generated 16-character password, not your Gmail password.

---

## Step 4 — Create the Default Admin Account

After running the seed scripts, the admin record has a placeholder password hash.
To set a real bcrypt-hashed password, run this Java snippet once or use the
Spring Boot Devtools console:

```java
// Temporary: generate a bcrypt hash
String hash = new BCryptPasswordEncoder(12).encode("Admin@2025");
System.out.println(hash);
// Copy the output into MySQL Workbench:
// UPDATE pdts_db.user SET user_password_hash = '<paste_hash>' WHERE user_username = 'admin001';
```

---

## Step 5 — Run the Application

### Option A — VS Code Spring Boot Dashboard (Recommended)

1. Click the **Spring Boot Dashboard** icon in the Activity Bar (left sidebar).
2. Click the ▶ play button next to `pdts`.
3. The app will start on `http://localhost:8080`.

### Option B — Maven Terminal

```bash
./mvnw spring-boot:run
# Windows:
mvnw.cmd spring-boot:run
```

### Option C — Build & Run JAR

```bash
./mvnw clean package -DskipTests
java -jar target/pdts-1.0.0.jar
```

---

## Step 6 — Access the System

| URL                              | Description                               |
|----------------------------------|-------------------------------------------|
| `http://localhost:8080/login`    | Staff login (Employee ID + Password)      |
| `http://localhost:8080/dashboard`| Main dashboard (requires login)           |
| `http://localhost:8080/`         | Landing page with token search            |

**Default login credentials:**
- Username: `admin001`
- Password: `Admin@2025` *(after updating the hash in Step 4)*

---

## Project Structure

```
pdts-project/
├── .vscode/
│   ├── settings.json          VS Code Java/Spring settings
│   └── extensions.json        Recommended extensions
├── database/
│   ├── 01_schema.sql          Full MySQL schema (all tables, triggers, view)
│   └── 02_seed.sql            Seed data (curriculum types, statuses, admin)
├── src/main/java/com/pdts/
│   ├── PdtsApplication.java   Spring Boot entry point
│   ├── config/
│   │   └── SecurityConfig.java    Spring Security + BCrypt
│   ├── controller/
│   │   ├── AuthController.java
│   │   ├── DashboardController.java
│   │   ├── ApplicantController.java
│   │   ├── RequirementController.java
│   │   ├── RejectionReasonController.java
│   │   ├── PublicPortalController.java
│   │   └── PortalStatusController.java
│   ├── model/
│   │   ├── Applicant.java
│   │   ├── Application.java
│   │   ├── ApplicantAccessToken.java
│   │   ├── Requirement.java
│   │   ├── RequirementStatus.java
│   │   ├── RequirementType.java
│   │   ├── RejectionReason.java
│   │   ├── Role.java
│   │   └── User.java
│   ├── repository/
│   │   ├── ApplicantRepository.java
│   │   ├── ApplicationRepository.java
│   │   ├── ApplicantAccessTokenRepository.java
│   │   ├── RequirementRepository.java
│   │   ├── RejectionReasonRepository.java
│   │   └── UserRepository.java
│   └── service/
│       ├── ApplicantService.java
│       ├── EmailService.java
│       ├── RequirementService.java
│       ├── TokenService.java
│       └── TrackingNumberService.java
├── src/main/resources/
│   ├── static/
│   │   ├── assets/logo.png    PUP seal (add manually)
│   │   ├── css/
│   │   │   ├── login.css
│   │   │   ├── dashboard.css
│   │   │   ├── applicant.css
│   │   │   └── portal.css
│   │   └── js/
│   │       ├── login.js
│   │       ├── dashboard.js
│   │       └── portal.js
│   ├── templates/
│   │   ├── login.html
│   │   ├── dashboard.html
│   │   ├── applicant-detail.html
│   │   └── portal-status.html
│   └── application.properties
└── pom.xml
```

---

## Adding the PUP Logo

Place the PUP seal image at:
```
src/main/resources/static/assets/logo.png
```
The provided images (`9573.png` / `9576.jpg`) can be renamed and placed there.
Recommended: use the transparent seal PNG, resized to 200×200px.

---

## Common Issues

| Problem                          | Fix                                                            |
|----------------------------------|----------------------------------------------------------------|
| `Access denied for user 'root'`  | Update `spring.datasource.password` in `application.properties`|
| Port 8080 already in use         | Change `server.port=8081` in `application.properties`         |
| Email not sending                | Verify Gmail App Password; check SMTP settings                 |
| `Table 'pdts_db.xxx' doesn't exist` | Re-run `01_schema.sql` in MySQL Workbench                  |
| Java version error               | Run `java -version`; must be 17+                              |
| Maven not found                  | Use `./mvnw` (included wrapper) instead of `mvn`              |

---

## SQL Query Reference

Ten demonstration queries are embedded in `PDTS_Merged_Final.docx` Section 11.
They are also ready to run directly in MySQL Workbench against `pdts_db`.

| # | Title                                 | Difficulty |
|---|---------------------------------------|------------|
| 1 | List All Applicants with Curriculum   | Simple     |
| 2 | Count Documents Per Status            | Simple     |
| 3 | Active Rejection Reasons              | Simple     |
| 4 | Full Document Status Report           | Moderate   |
| 5 | System Activity Audit Log (30 days)   | Moderate   |
| 6 | Applicants with Incomplete Submissions| Moderate   |
| 7 | Document Timeline by Campus/Semester  | Moderate   |
| 8 | Staff Processing Efficiency Ranking   | Difficult  |
| 9 | Brute-Force Detection via Token Log   | Difficult  |
|10 | Applicant Document Completion Report  | Difficult  |

---

*PDTS — BSITOUMN 2-3 | Polytechnic University of the Philippines — Open University System*
