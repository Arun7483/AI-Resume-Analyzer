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

### Run locally from IntelliJ IDEA

The repository includes `src/main/resources/application-local.properties` for IntelliJ local development. Open **Run > Edit Configurations**, select the backend application, set **Active profiles** to `local`, and add your database and optional AI values as environment variables:

```text
SPRING_PROFILES_ACTIVE=local
DATABASE_URL=jdbc:postgresql://localhost:5432/resume_analyzer
DB_USERNAME=postgres
DB_PASSWORD=YOUR_POSTGRES_PASSWORD
JWT_SECRET=YOUR_LOCAL_LONG_RANDOM_SECRET
CORS_ALLOWED_ORIGIN=http://localhost:4200
BACKEND_URL=http://localhost:8080
FRONTEND_URL=http://localhost:4200
GROQ_API_KEY=YOUR_GROQ_KEY
GROQ_MODEL=llama-3.3-70b-versatile
JSEARCH_API_KEY=YOUR_RAPIDAPI_JSEARCH_KEY
```

For the hosted Aiven database, replace only the database values with:

```text
DATABASE_URL=jdbc:postgresql://pg-2ea8c8c-ruas58876-dde6.i.aivencloud.com:16477/defaultdb?sslmode=require
DB_USERNAME=avnadmin
DB_PASSWORD=YOUR_AIVEN_PASSWORD
```

Alternatively, start it from PowerShell after setting the same variables:

```powershell
cd backend
$env:DATABASE_URL = 'jdbc:postgresql://pg-2ea8c8c-ruas58876-dde6.i.aivencloud.com:16477/defaultdb?sslmode=require'
$env:DB_USERNAME = 'avnadmin'
$env:DB_PASSWORD = 'YOUR_AIVEN_PASSWORD'
$env:JWT_SECRET = 'YOUR_LOCAL_LONG_RANDOM_SECRET'
$env:CORS_ALLOWED_ORIGIN = 'http://localhost:4200'
$env:BACKEND_URL = 'http://localhost:8080'
$env:FRONTEND_URL = 'http://localhost:4200'
mvn spring-boot:run
```

The local JWT fallback is only for development. Never use it in Render; keep a strong `JWT_SECRET` configured there.

### Live job listings

The public feeds are best-effort and may return fewer than 1,000 records or be blocked by a hosting provider. For a large set of real listings, create a RapidAPI JSearch subscription and add `JSEARCH_API_KEY` to the IntelliJ or Render environment. The backend queries multiple resume-derived role searches, deduplicates application URLs, and ranks the combined results against the uploaded resume. Groq is used for resume analysis/chat; it is not a live job database.

For the Aiven database, configure these three Render variables. Convert its Service URI from `postgres://` to a JDBC URL without credentials and keep the SSL query parameter:

```text
DATABASE_URL=jdbc:postgresql://pg-2ea8c8c-ruas58876-dde6.i.aivencloud.com:16477/defaultdb?sslmode=require
DB_USERNAME=avnadmin
DB_PASSWORD=YOUR_AIVEN_PASSWORD
```

Enter the actual values in Render. Do not enter `${DATABASE_USERNAME}`, `${DATABASE_PASSWORD}`, or the literal `CLICK_TO:REVEAL_PASSWORD`.

Normal email/password signup is enabled and signs users in immediately; it does not require email verification. Google sign-in also creates verified accounts. The verification endpoints remain available for existing accounts and backwards compatibility.

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
- `GET /api/v1/resumes/job-matches` — active job listings ranked against the latest uploaded resume
- `POST /api/v1/chat` — prompt and optional resumeId

All resume, job-match, and chat endpoints require `Authorization: Bearer <token>`. Ownership is derived from the authenticated principal rather than request-provided user IDs. Job applications open on the original listing site through each returned `applyUrl`.
