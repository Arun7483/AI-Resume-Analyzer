# Resume Analyzer API

Java 21 / Spring Boot API with stateless JWT authentication, MySQL persistence, Apache Tika document extraction, and Spring AI analysis/chat.

## Required environment

```bash
export DB_URL='jdbc:mysql://localhost:3306/resume_analyzer?createDatabaseIfNotExist=true&serverTimezone=UTC'
export DB_USERNAME='resume_app'
export DB_PASSWORD='change-me'
export JWT_SECRET="$(openssl rand -base64 64)"
export RESUME_STORAGE_PATH='/var/lib/resume-analyzer/uploads'
export OPENAI_API_KEY='...'
```

Run with `./mvnw spring-boot:run` when using a Maven wrapper, or `mvn spring-boot:run`. Flyway creates the schema. The only allowed browser origin is the Angular UI at `http://localhost:50344`; override `app.cors.allowed-origin` for another deployment.

## API

- `POST /api/v1/auth/register` — email, password, and fullName
- `POST /api/v1/auth/login` — email and password
- `POST /api/v1/resumes/upload` — multipart `file` and optional `jobDescription`
- `POST /api/v1/chat` — prompt and optional resumeId

All resume and chat endpoints require `Authorization: Bearer <token>`. Ownership is derived from the authenticated principal rather than request-provided user IDs.
