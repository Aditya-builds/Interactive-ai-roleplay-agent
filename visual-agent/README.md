# Visual Agent (V2)

LangGraph-based visual reasoning service for the roleplay visual generation pipeline.

## Responsibilities

- Decide whether a moment is visually important
- Select relevant characters and context
- Plan multi-character spatial interaction
- Compile a compact prompt for `gpt-image-2`

This service does **not** mutate authoritative story state or persist data.

## Run locally

```bash
cd visual-agent
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8090 --reload
```

## API

- `GET /health`
- `POST /visual/plan`

Enable from Quarkus with:

```properties
roleplay.visual.director.enabled=true
roleplay.visual.director.base-url=http://localhost:8090
```

If the director is unavailable, the backend falls back to V1 `VisualScenePlannerService`.

## Optional LangSmith

```bash
set LANGSMITH_TRACING=true
set LANGSMITH_PROJECT=visual-agent
```

## Tests

```bash
pytest
```
