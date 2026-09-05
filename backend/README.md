# Resume Analyzer API

Java 21 / Spring Boot API with stateless JWT authentication, PostgreSQL persistence, Apache Tika document extraction, and Spring AI analysis/chat.

## Required environment

```bash
export DATABASE_URL='jdbc:postgresql://localhost:5432/resume_analyzer'
export DB_USERNAME='avnadmin'
export DB_PASSWORD='change-me'
export JWT_SECRET="$(openssl rand -base64 64)"
export RESUME_STORAGE_PATH='/var/lib/resume-analyzer/uploads'
export GROQ_API_KEY='...'
export GROQ_MODEL='llama-3.3-70b-versatile'
export RESEND_API_KEY='re_...'
export GOOGLE_CLIENT_ID='your-google-web-client-id.apps.googleusercontent.com'
export BACKEND_URL='http://localhost:8080'
export FRONTEND_URL='http://localhost:4200'
export MAIL_HOST='smtp.gmail.com'
export MAIL_PORT='465'
export MAIL_USERNAME='your-gmail-address'
export MAIL_PASSWORD='your-gmail-app-password'
export MAIL_SSL_ENABLE='true'
export MAIL_STARTTLS_ENABLE='false'
```

Run with `mvn spring-boot:run`. Flyway creates the PostgreSQL schema. Set `CORS_ALLOWED_ORIGIN` to the deployed frontend URL.

For the Aiven database, configure these three Render variables. Convert its Service URI from `postgres://` to a JDBC URL without credentials and keep the SSL query parameter:

```text
DATABASE_URL=jdbc:postgresql://pg-2ea8c8c-ruas58876-dde6.i.aivencloud.com:16477/defaultdb?sslmode=require
DB_USERNAME=avnadmin
DB_PASSWORD=YOUR_AIVEN_PASSWORD
```

Enter the actual values in Render. Do not enter `${DATABASE_USERNAME}`, `${DATABASE_PASSWORD}`, or the literal `CLICK_TO:REVEAL_PASSWORD`.

Render runs the `prod,mail` profiles so signup verification emails are sent. If `RESEND_API_KEY` is configured, verification uses Resend's HTTPS API and does not depend on Render allowing SMTP connections. Set `MAIL_FROM` to a sender verified in Resend. SMTP remains available as a fallback when `RESEND_API_KEY` is empty.

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
