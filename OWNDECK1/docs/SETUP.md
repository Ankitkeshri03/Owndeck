# OwnDeck Setup Guide (React + Spring Boot)

The project has been consolidated to a single supported architecture:
- `frontend/` runs the React UI
- `backend/` runs the Spring Boot API
- AI/OCR behavior remains inside the Spring Boot services

## 1. Requirements
- Java 17+
- Maven 3.9+
- Node.js 20+

## 2. Install Dependencies
From project root:

```bash
npm install
npm run install:all
```

This installs the root developer tooling and the React frontend dependencies. The backend is built with Maven only.

## 3. Backend Configuration

Spring Boot config is in:
- `backend/src/main/resources/application.yml`

Use environment variables to override defaults.

Recommended production values:

```env
SERVER_PORT=8081
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/owndeck?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=your_password
SPRING_DATASOURCE_DRIVER_CLASS_NAME=com.mysql.cj.jdbc.Driver
APP_JWT_SECRET=replace_with_long_secure_random_secret
APP_JWT_EXPIRATION_MS=604800000
APP_STORAGE_LOCATION=uploads
APP_PUBLIC_BASE_URL=http://localhost:8081
APP_ALLOWED_ORIGINS=http://localhost:5173
OPENAI_API_KEY=optional
OPENAI_MODEL=gpt-4o-mini
OPENAI_BASE_URL=https://api.openai.com/v1
OCR_TESSDATA_PATH=.
```

Notes:
- Default DB is in-memory H2 for quick local startup.
- Set MySQL env vars to use MySQL in production.

## 4. Frontend Configuration

Create `frontend/.env`:

```env
VITE_API_BASE_URL=http://localhost:8081/api
```

## 5. Run Both Apps

From project root:

```bash
npm run dev
```

## 6. Useful Commands

```bash
npm run dev:backend
npm run dev:frontend
npm run test:backend
npm run build:backend
npm run build
```

## 7. API Quick Reference
- `POST /api/auth/signup`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `GET /api/products`
- `GET /api/products/alerts`
- `POST /api/products`
- `POST /api/products/upload`
- `GET /api/products/{id}`
- `PUT /api/products/{id}`
- `DELETE /api/products/{id}`
- `GET /api/products/{id}/invoice`
- `POST /api/ai/products/{id}/complaint`
- `POST /api/ocr/extract`

## 8. Cleanup Notes

Legacy Express backend files, duplicate generated folders, and extracted archive metadata are intentionally removed from this version of the project. Keep the following generated paths out of source control:
- `backend/uploads`
- `backend/target`
- `frontend/dist`
- `logs`
- `__MACOSX`
