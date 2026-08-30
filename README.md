# Interactive AI Roleplay Engine

Local-first text roleplay application with an Angular frontend and Quarkus backend.

## Prerequisites

- Java 21+
- Maven 3.9+
- Node.js 20+
- OpenAI API key

## Setup

### 1. Configure OpenAI API key

Copy the example env file and add your key:

```powershell
cd backend
copy .env.example .env
```

Edit `backend/.env`:

```env
OPENAI_API_KEY=your-openai-api-key-here
```

Quarkus loads `backend/.env` automatically in dev mode. The key is mapped to `roleplay.llm.openai.api-key` via `${OPENAI_API_KEY}` in `application.properties`.

The default model is **gpt-4** (configurable in `backend/src/main/resources/application.properties`).

### 2. Start the backend

```powershell
cd backend
mvn quarkus:dev
```

Backend runs at http://localhost:8080

### 3. Start the frontend

```powershell
cd frontend
npm start
```

Frontend runs at http://localhost:4200 and proxies `/api` to the backend.

## Deployment

This repo deploys as **two separate services** — do not deploy the whole monorepo as one Vercel project.

```text
frontend/  →  Vercel   (Angular static site)
backend/   →  Render   (Quarkus API)
```

### Frontend (Vercel)

1. Import `Aditya-builds/Interactive-ai-roleplay-agent` on Vercel
2. Set **Root Directory** to `frontend`
3. Framework: **Angular**
4. Build Command: `npm run build`
5. Output Directory: `dist/frontend`
6. Install Command: `npm ci`

**Environment variable (Vercel):**

```text
NG_APP_API_URL=https://your-backend.onrender.com
```

No trailing slash. The build script writes this into `environment.prod.ts` automatically.

Local dev uses relative `/api` URLs with the dev proxy — no env var needed.

### Backend (Render)

Use the included `render.yaml` blueprint, or create a **Web Service** manually:

| Setting | Value |
|---------|-------|
| Root Directory | `backend` |
| Build Command | `mvn package -DskipTests` |
| Start Command | `java -jar target/quarkus-app/quarkus-run.jar` |

**Environment variables (Render):**

```text
OPENAI_API_KEY=your_key
CORS_ORIGINS=https://your-app.vercel.app
ROLEPLAY_DATA_DIR=../data
```

For DeepSeek instead of OpenAI:

```text
LLM_API_KEY=your_key
LLM_BASE_URL=https://api.deepseek.com/v1
LLM_MODEL=deepseek-chat
```

Never put API keys in the Angular frontend.

### Connect frontend → backend

After Render deploys, copy the backend URL into Vercel's `NG_APP_API_URL` and redeploy the frontend.

## Usage

1. Open http://localhost:4200
2. Select a character (e.g. Aurora)
3. Type an action or dialogue and press Send
4. The character responds via GPT-4; the conversation is saved to `data/conversations/`

## Project structure

```
visual comic genrator/
├── backend/          Quarkus REST API + roleplay engine
├── frontend/         Angular chat UI
└── data/             JSON storage (see data/README.md)
    ├── characters.json   list of all characters
    ├── aurora/           character folder (profile + images)
    ├── worlds.json       list of all worlds
    └── conversations/    runtime story sessions
```

## API

- `GET  /api/characters`
- `GET  /api/characters/{id}`
- `POST /api/conversations` — body: `{ "characterId": "aurora" }`
- `GET  /api/conversations/{id}`
- `POST /api/conversations/{id}/messages` — body: `{ "content": "..." }`
