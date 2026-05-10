# PDTS PostgreSQL Setup Guide

This project has been adjusted to run with PostgreSQL instead of XAMPP MySQL/MariaDB.

## Recommended tools

- VS Code or NetBeans for Java/Spring Boot coding
- PostgreSQL for the database
- pgAdmin or psql for running SQL scripts
- JDK 21
- Maven

## What was changed

1. `pom.xml`
   - Replaced MySQL connector with PostgreSQL JDBC driver.
   - Fixed the misplaced `spring-boot-configuration-processor` dependency.

2. `src/main/resources/application.properties`
   - Changed database URL to PostgreSQL.
   - Changed Hibernate dialect to PostgreSQL.
   - Added missing `pdts.*` settings required by the app.

3. `src/main/java/com/pdts/model/User.java`
   - Changed the table from `user` to `app_user` because `user` can cause issues in PostgreSQL.

4. `database/01_schema.sql`
   - Converted MySQL syntax to PostgreSQL syntax.

5. `database/02_seed.sql`
   - Converted seed data to PostgreSQL.
   - Fixed the incorrect `users` table name from the original file.
   - Added a working BCrypt password hash for the default admin account.

Original MySQL scripts were kept in:

```text
/database/mysql_original/
```

## Setup steps

### 1. Create the database

Open pgAdmin or psql and create the database:

```sql
CREATE DATABASE pdts_db;
```

Then connect to the `pdts_db` database.

### 2. Run the schema script

Run:

```text
database/01_schema.sql
```

### 3. Run the seed script

Run:

```text
database/02_seed.sql
```

### 4. Update your PostgreSQL password

Open:

```text
src/main/resources/application.properties
```

Change this line:

```properties
spring.datasource.password=YOUR_POSTGRES_PASSWORD
```

Use your actual PostgreSQL password.

### 5. Run the project

From the project folder, run:

```bash
mvn spring-boot:run
```

Then open:

```text
http://localhost:8080/login
```

## Default login

```text
Username: admin001
Password: Admin@2025
```

## If you want to use XAMPP MySQL instead

Use the original MySQL scripts in:

```text
database/mysql_original/
```

Then restore the MySQL configuration from:

```text
src/main/resources/application-mysql.properties
```
