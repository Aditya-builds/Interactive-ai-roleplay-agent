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
5. Output Directory: `dist/frontend/browser`
6. Install Command: `npm ci`

**Environment variable (Vercel):**

```text
NG_APP_API_URL=https://interactive-ai-roleplay-backend.onrender.com
```

No trailing slash. The build script writes this into `environment.prod.ts` automatically.

Local dev uses relative `/api` URLs with the dev proxy — no env var needed.

### Backend (Render)

Use the included `render.yaml` blueprint, or create a **Web Service** manually with **Docker**:

| Setting | Value |
|---------|-------|
| Language | Docker |
| Root Directory | *(leave empty — repo root)* |
| Dockerfile Path | `backend/Dockerfile` |
| Docker Context | `.` (repository root) |

The Dockerfile builds Quarkus from `backend/` and copies the repo `data/` folder into the image at `/data` so stories, personas, and characters load correctly.

**Do not** set Root Directory to `backend` only — the build context must include the sibling `data/` directory.

**Environment variables (Render):**

```text
OPENAI_API_KEY=your_key
CORS_ORIGINS=https://your-app.vercel.app
ROLEPLAY_DATA_DIR=/data
```

`ROLEPLAY_DATA_DIR=/data` is already set in the Dockerfile and `render.yaml`; you only need to override it if you change the image layout.

For DeepSeek instead of OpenAI:

```text
LLM_API_KEY=your_key
LLM_BASE_URL=https://api.deepseek.com/v1
LLM_MODEL=deepseek-chat
```

Never put API keys in the Angular frontend.

**Verify after deploy:**

```text
https://interactive-ai-roleplay-backend.onrender.com/api/stories
```

You should get JSON (e.g. Ashbitten). If that works, set `NG_APP_API_URL` on Vercel to the same backend URL.

#### Alternative: native Java build (no Docker)

If Render offers Java 21 natively for your account:

| Setting | Value |
|---------|-------|
| Root Directory | `backend` |
| Build Command | `mvn package -DskipTests` |
| Start Command | `java -jar target/quarkus-app/quarkus-run.jar` |
| `ROLEPLAY_DATA_DIR` | `../data` |

Docker is recommended because it bundles `data/` reliably.

### Connect frontend → backend

After Render deploys, copy the backend URL into Vercel's `NG_APP_API_URL` and redeploy the frontend.

## Usage

1. Open http://localhost:4200
2. Select a character (e.g. Aurora)
3. Type an action or dialogue and press Send
4. The character responds via the configured LLM; the conversation is saved to `data/conversations/`
5. Click **Generate Scene** in the chat header to create a scene image from the current moment (uses your API key from the settings panel)

### Scene image generation (V1 + optional V2)

Images are generated on demand — not on every chat turn.

**V1 (default):** Quarkus plans the scene from current state + recent messages, builds a structured prompt, and calls **gpt-image-2**.

**V2 (optional):** A separate Python **visual agent** (LangGraph) acts as a visual director — selecting relevant characters, interaction poses, and a compact prompt. Quarkus still owns all story state and image storage. If the agent is off or unreachable, V1 is used automatically.

Enable V2 locally:

```powershell
# Terminal 1 — visual agent
cd visual-agent
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8090

# Terminal 2 — backend (add to backend/.env or application.properties)
# roleplay.visual.director.enabled=true
# roleplay.visual.director.base-url=http://localhost:8090
cd backend
mvn quarkus:dev
```

See `data/docs/JSON_MODEL.md` §13 and `visual-agent/README.md` for the full visual architecture.

## Project structure

```
visual comic genrator/
├── backend/          Quarkus REST API + roleplay engine + scene image orchestration
├── frontend/         Angular chat UI (Generate Scene button, scene image cards)
├── visual-agent/     Optional LangGraph visual director (Python / FastAPI)
└── data/             JSON storage (see data/README.md)
    ├── characters.json       list of all characters
    ├── characters/
    │   ├── aurora.json       character profile + visualIdentity
    │   └── references/       canonical images for scene generation
    ├── generated-images/     runtime scene outputs (gitignored)
    ├── worlds.json           list of all worlds
    └── conversations/        runtime story sessions
```

## API

- `GET  /api/characters`
- `GET  /api/characters/{id}`
- `POST /api/conversations` — body: `{ "characterId": "aurora" }`
- `GET  /api/conversations/{id}`
- `POST /api/conversations/{id}/messages` — body: `{ "content": "..." }`
- `POST /api/conversations/{id}/scene-images` — generate scene image (header: `X-LLM-Api-Key`)
- `GET  /api/scene-images/{id}` — scene image metadata
- `GET  /api/scene-images/{id}/content` — scene image bytes
- `GET  /api/visuals/references/{characterId}` — canonical character reference image
