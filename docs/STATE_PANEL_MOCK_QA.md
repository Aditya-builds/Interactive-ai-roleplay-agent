# State Panel Mock QA

Verifies that scene, character, and relationship state returned by the backend
updates correctly — the same data the Angular state panel displays.

## Run backend mock QA

```powershell
cd backend
mvn test -Dtest=StatePanelMockQaTest
```

## Scenarios

### 1. Caring message (matches live UI flow)

**User:** `you look tired are you alright ?`

**Mock LLM proposes:**
- trust +2, affection +1
- emotion → grateful
- status → exhausted
- situation → "Aurora is opening up about her fatigue."

**Asserts:**
- `SendMessageResponse.characterState`, `.scene`, `.relationships` all updated
- Persisted conversation JSON matches

### 2. REST API round-trip

**User:** `I spar with Aurora and land a heavy hit.`

**Mock LLM proposes:**
- location → training_ground
- health 100 → 85
- emotion → irritated, status → injured
- respect +2

**Asserts:**
- `POST /api/conversations/{id}/messages` response JSON
- `GET /api/conversations/{id}` persisted state

## Run frontend unit test

```powershell
cd frontend
npm test -- --include=**/chat.component.spec.ts --browsers=ChromeHeadless --watch=false
```

## Live UI note

The state panel only changes when the **LLM proposes stateChanges** that pass validation.
If GPT narrates injury or warmth but proposes no changes, values stay the same.

After each send, the UI reloads the full conversation from `GET /api/conversations/{id}`
so the panel always reflects saved backend state.

Restart the Quarkus dev server after backend changes so `characterState` is included
in `SendMessageResponse`.
