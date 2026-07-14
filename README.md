# 0721ACGN

ACGN WebGIS graduation project, organized for GitHub upload.

## Structure

- `frontend/` - Vue 3 + Vite frontend.
- `backend/` - Spring Boot backend and database scripts.
- `backend/scripts/` - Python data synchronization scripts.

## Local setup

Frontend:

```bash
cd frontend
npm install
npm run dev
```

Backend:

```bash
cd backend
copy src\main\resources\application.example.properties src\main\resources\application.properties
mvnw.cmd spring-boot:run
```

Fill `backend/src/main/resources/application.properties` with your local database, OSS, and agent credentials before running the backend.
