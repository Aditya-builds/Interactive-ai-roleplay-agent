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
