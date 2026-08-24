# OwnDeck

Full-stack invoice and warranty manager built with:
- Frontend: React + Vite + Tailwind
- Backend: Spring Boot + JWT + JPA
- OCR: PDFBox + Tess4J
- AI: OpenAI (optional, with safe fallback)

This repository is now standardized on a single stack:
- `frontend/`: React application
- `backend/`: Spring Boot API

Legacy Express backend files and duplicate generated artifacts have been removed so the Spring Boot backend remains the only supported server implementation. The AI complaint and invoice-extraction flow stays in the Spring services.

## Features

1. Authentication
- Signup/login with JWT
- User-level product isolation

2. Invoice Management
- Upload PDF/image invoices
- Secure local file storage
- Save extracted product data in DB

3. OCR + AI Extraction
- Extract invoice text from PDF/images
- Parse and store:
  - product name
  - title/description
  - quantity
  - purchase date
  - warranty duration or expiry date
- Auto-calculate expiry date when missing

4. Product Dashboard
- Status badges: Active, Expiring Soon, Expired
- Filters and search
- Expiry metrics

5. Warranty Alert Popup
- On dashboard load, shows products expiring in:
  - 30 days
  - 1 day

6. AI Complaint Generator
- Per-product complaint section
- Input issue description
- Output subject + professional email body

## API Endpoints

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
- `GET /api/health`

## Database Model

### users
- `id`
- `name`
- `email`
- `password_hash`
- `phone`

### product_invoices
- `id`
- `user_id`
- `product_name`
- `title_description`
- `quantity`
- `purchase_date`
- `expiry_date`
- `warranty_duration_months`
- `invoice_url`
- `status`
- `ocr_text`

## Setup

### Prerequisites
- Java 17+
- Maven 3.9+
- Node.js 20+

### 1) Install dependencies
```bash
npm install
npm run install:all
```

This installs:
- root tooling used to run both apps together
- frontend dependencies for the React app

### 2) Configure environment

Backend values are read from `backend/src/main/resources/application.yml` with env overrides.

Important env vars:
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `APP_JWT_SECRET`
- `APP_PUBLIC_BASE_URL`
- `OPENAI_API_KEY` (optional)

Frontend `.env`:
```env
VITE_API_BASE_URL=http://localhost:8081/api
```

### 3) Run
```bash
npm run dev
```

- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8081`
- Health: `http://localhost:8081/api/health`

## Project Notes

- The backend no longer requires Node.js packages inside `backend/`.
- Generated files such as `backend/uploads`, `backend/target`, `frontend/dist`, logs, and extracted macOS archive metadata are intentionally ignored.
