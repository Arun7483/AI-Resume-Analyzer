# Resume Analyzer API

Java 21 / Spring Boot API with stateless JWT authentication, PostgreSQL persistence, Apache Tika document extraction, and Spring AI analysis/chat.

## Required environment

```bash
export DATABASE_URL='jdbc:postgresql://resume_app:change-me@localhost:5432/resume_analyzer'
export JWT_SECRET="$(openssl rand -base64 64)"
export RESUME_STORAGE_PATH='/var/lib/resume-analyzer/uploads'
export GROQ_API_KEY='...'
export GROQ_MODEL='llama-3.3-70b-versatile'
```

Run with `mvn spring-boot:run`. Flyway creates the PostgreSQL schema. Set `CORS_ALLOWED_ORIGIN` to the deployed frontend URL.

For the Aiven database, copy the revealed values from Aiven into Render as one secret named `DATABASE_URL`. Convert its Service URI from `postgres://` to `jdbc:postgresql://` and keep the SSL query parameter:

```text
jdbc:postgresql://avnadmin:YOUR_PASSWORD@pg-2ea8c8c-ruas58876-dde6.i.aivencloud.com:16477/defaultdb?sslmode=require
```

Do not enter the literal `CLICK_TO:REVEAL_PASSWORD`. Do not add separate `DATABASE_USERNAME` or `DATABASE_PASSWORD` variables; the application reads the credentials from this URL.

## Email verification (local Gmail setup)

The default profile logs a local verification link when SMTP is not configured. To deliver verification emails, enable the mail profile and use a Gmail app password:

```powershell
$env:MAIL_USERNAME = 'gptaru7483@gmail.com'
$env:MAIL_PASSWORD = 'your-16-character-google-app-password'
java -jar target\resume-analyzer-api-1.0.0.jar --spring.profiles.active=mail
```

Create the app password in the Google Account security settings after enabling 2-Step Verification. Do not use the normal Gmail password, and do not commit the app password to the repository.

## API

- `POST /api/v1/auth/register` — email, password, and fullName
- `POST /api/v1/auth/login` — email and password
- `POST /api/v1/resumes/upload` — multipart `file` and optional `jobDescription`
- `POST /api/v1/chat` — prompt and optional resumeId

All resume and chat endpoints require `Authorization: Bearer <token>`. Ownership is derived from the authenticated principal rather than request-provided user IDs.
