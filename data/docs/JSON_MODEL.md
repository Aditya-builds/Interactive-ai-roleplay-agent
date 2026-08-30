# Roleplay Engine — JSON Data Model

This document defines the **canonical JSON architecture** for the roleplay engine.
Java classes are a projection of this model — not the other way around.

## Three layers of data

Every piece of JSON belongs to exactly one layer:

| Layer | Question | Mutability | Storage |
|-------|----------|------------|---------|
| **Definitions** | What exists? | Static (authoring) | `characters/`, `worlds/` |
| **World state** | What is happening right now? | Mutable per turn | Inside `conversations/` |
| **History** | What has happened? | Append-only (trimmed) | Inside `conversations/` |

```
ROLEPLAY WORLD
      │
      ├─ DEFINITIONS          (never modified by gameplay)
      │    ├─ characters/
      │    └─ worlds/
      │
      └─ RUNTIME STORY        (one file per play session)
           └─ conversations/{id}.json
                ├─ currentScene        ← world state
                ├─ characterStates      ← world state
                ├─ relationships        ← world state
                ├─ events               ← history
                ├─ memories             ← history
                └─ messages             ← history
```

## Directory layout

```
data/
├── docs/
│   └── JSON_MODEL.md          ← this file
├── characters.json            ← index of character ids
├── worlds.json                ← index of world ids
├── characters/
│   ├── aurora.json            ← definition
│   └── laxus.json
├── worlds/
│   └── fantasy_world.json     ← definition + location catalog
└── conversations/
    └── conversation-001.json  ← full runtime example
```

Legacy layout (`aurora/aurora.json`, `fantasy/fantasy.json`) remains until Java is migrated.

---

## 1. Character definition (`characters/{id}.json`)

**Layer: Definition.** Describes who the character is. Never modified during play.

```jsonc
{
  "id": "aurora",
  "name": "Aurora",
  "worldId": "fantasy_world",
  "type": "main_character",       // main_character | npc | antagonist
  "imageUrl": "/characters/aurora.png",

  "profile": {
    "personality": ["calm", "intelligent"],
    "background": "...",
    "speakingStyle": "...",
    "values": ["respects strength"]
  },

  "appearance": {
    "hair": "silver",
    "eyes": "violet",
    "build": "athletic"
  },

  "abilities": [],                // future: skills, magic, etc.

  "defaultState": {
    "health": { "current": 100, "max": 100 },
    "locationId": "guild_hall",   // must exist in world.locations
    "status": "healthy",
    "emotion": "calm"
  },

  "defaultRelationships": [
    {
      "characterB": "user",
      "metrics": { "trust": 42, "respect": 67, "affection": 12, "familiarity": 54, "suspicion": 8 },
      "status": "developing",
      "summary": "Aurora respects the user's abilities but does not fully trust them yet."
    }
  ],

  "seedMemories": [
    {
      "subject": "aurora",
      "participants": ["aurora", "user"],
      "content": "The user protected Aurora during an earlier forest mission.",
      "importance": 0.85,
      "emotionalImpact": "positive",
      "tags": ["mission", "trust"]
    }
  ]
}
```

### Rules

- `defaultState.locationId` **must** reference a location in the character's world.
- `defaultState` is copied into conversation `characterStates` at creation time only.
- Definition files are never written back by the backend during gameplay.

---

## 2. World definition (`worlds/{id}.json`)

**Layer: Definition.** The world bible + **location catalog**.

```jsonc
{
  "id": "fantasy_world",
  "name": "Fantasy World",
  "description": "...",
  "rules": ["Magic exists", "Guilds hold political power"],

  "locations": [
    {
      "id": "guild_hall",
      "name": "Guild Hall",
      "description": "A large, lively guild building in the city center."
    },
    {
      "id": "forest",
      "name": "Whispering Forest",
      "description": "A dangerous forest on the guild's eastern border."
    }
  ]
}
```

### Location validation (V1)

State changes that set a location must use an `id` from this catalog:

```
LLM proposes locationId = "dark_forest_123"
         │
         ▼
   Exists in world.locations?
      ┌──┴──┐
     YES    NO → reject
      │
    apply
```

Dynamic location creation is deferred to a later version.

---

## 3. Runtime: current scene

**Layer: World state.** Lives inside the conversation file.

This is **not** the location definition — it is what's happening at that location *right now*.

```jsonc
{
  "locationId": "guild_hall",       // → worlds/fantasy_world.json locations[]
  "time": "evening",
  "charactersPresent": ["aurora", "user", "laxus"],
  "currentSituation": "The guild is unusually quiet after the forest sortie.",
  "currentConflict": null,
  "environment": {
    "weather": "clear",
    "lighting": "dim"
  }
}
```

---

## 4. Runtime: character state

**Layer: World state.** Per-character condition for this story session.

```jsonc
{
  "characterId": "aurora",
  "health": { "current": 72, "max": 100 },
  "locationId": "guild_hall",
  "status": "injured",
  "emotion": "determined"
}
```

Stored as a map in the conversation:

```jsonc
"characterStates": {
  "aurora": { ... },
  "laxus":  { ... }
}
```

Two stories can have different Aurora HP without touching `characters/aurora.json`.

---

## 5. Runtime: relationships

**Layer: World state** (metrics) + **History** (summary evolves).

Each relationship is an **independent edge** between two characters:

```jsonc
{
  "id": "rel-aurora-user",
  "characterA": "aurora",
  "characterB": "user",
  "metrics": {
    "trust": 55,
    "respect": 74,
    "affection": 18,
    "familiarity": 62,
    "suspicion": 5
  },
  "status": "developing",
  "summary": "After the forest battle, Aurora trusts the user more — they held the line when she was exposed."
}
```

Supports N×N relationships: Aurora↔User, Aurora↔Laxus, Laxus↔User, etc.

### State change mapping

```jsonc
{
  "type": "RELATIONSHIP",
  "targetId": "rel-aurora-user",   // relationship id
  "field": "trust",
  "operation": "INCREASE",
  "value": "3"
}
```

Alternative (also valid for LLM prompts):

```jsonc
{
  "type": "RELATIONSHIP",
  "targetId": "user",              // characterB — resolved against characterA = focal character
  "field": "trust",
  "operation": "INCREASE",
  "value": "3"
}
```

---

## 6. History: events

**Layer: History.** Raw record of what happened.

```jsonc
{
  "id": "event-102",
  "type": "COMBAT",               // COMBAT | TRAVEL | RELATIONSHIP | DISCOVERY | DIALOGUE
  "description": "Aurora and the user defeated an ice mage ambush in the forest.",
  "participants": ["aurora", "user", "ice_mage"],
  "locationId": "forest",
  "importance": 0.9,
  "timestamp": "2026-08-30T18:22:00Z"
}
```

> **Event** = "What happened?"

---

## 7. History: memories

**Layer: History.** What is **worth remembering** for future turns.

```jsonc
{
  "id": "memory-031",
  "subject": "aurora",
  "participants": ["aurora", "user"],
  "content": "The user stepped between Aurora and the ice mage's blast when she was exposed.",
  "importance": 0.92,
  "emotionalImpact": "positive",
  "tags": ["combat", "trust", "protection"],
  "source": "llm",
  "createdAt": "2026-08-30T18:22:30Z"
}
```

> **Memory** = "What is worth remembering?"

An event can spawn zero or more memories. They serve different retrieval roles in prompts.

---

## 8. History: messages

**Layer: History.** The conversation transcript.

```jsonc
{ "id": "msg-001", "role": "user",      "content": "...", "timestamp": "..." }
{ "id": "msg-002", "role": "assistant",  "characterId": "aurora", "content": "...", "timestamp": "..." }
```

Assistant messages **must** include `characterId` so multi-character dialogue works later.

---

## 9. Conversation container

One file = one story timeline.

```jsonc
{
  "id": "conversation-001",
  "worldId": "fantasy_world",
  "focalCharacterId": "aurora",     // primary POV character (V1); becomes one of activeCharacters in V2
  "player": { "id": "user", "name": "Aditya" },
  "activeCharacters": ["aurora"],
  "createdAt": "...",
  "updatedAt": "...",

  "currentScene": { ... },
  "characterStates": { ... },
  "relationships": [ ... ],
  "events": [ ... ],
  "memories": [ ... ],
  "messages": [ ... ]
}
```

### Instantiation flow

```
characters/aurora.json
        │
        │  POST /conversations { characterId: "aurora" }
        ▼
Copy defaultState     → characterStates.aurora
Copy defaultRelationships → relationships[]
Copy seedMemories     → memories[]
Init currentScene     → from defaultState.locationId + world
        │
        ▼
conversations/{uuid}.json
```

---

## 10. Turn pipeline (unchanged principle)

```
User message
     ↓
LLM → structured JSON
     ├── response        (narrative)
     ├── stateChanges[]  (proposals)
     ├── events[]        (proposals)
     └── memories[]      (proposals)
     ↓
Backend validates against definitions (locations, fields, deltas)
     ↓
Apply to conversation runtime state
     ↓
Save conversations/{id}.json
     ↓
Next turn prompt reads: situation + events + memories + relationships + messages
```

---

## 11. Prompt context assembly (target)

Each turn, the LLM receives:

```
CURRENT SITUATION     ← currentScene + characterStates
RECENT EVENTS         ← last N events (newest first)
IMPORTANT MEMORIES    ← top N by importance
RELATIONSHIPS         ← all active edges with summaries
RECENT CONVERSATION   ← last N messages
USER MESSAGE          ← this turn's input
```

---

## 12. Migration notes (current Java → this model)

| Current | Target |
|---------|--------|
| `data/aurora/aurora.json` | `data/characters/aurora.json` |
| `data/fantasy/fantasy.json` | `data/worlds/fantasy_world.json` + locations[] |
| `Conversation.characterId` | `focalCharacterId` + `activeCharacters[]` |
| `Conversation.scene.location` | `currentScene.locationId` (validated) |
| `Relationship.targetId` | `relationships[].characterA/B` + `id` |
| `StoryMemoryEntry` without subject | `memories[].subject` |
| `Message` without characterId | `messages[].characterId` on assistant |
| `CharacterRuntimeState.location` | `characterStates[id].locationId` |

See `conversations/conversation-001.json` for a complete reference instance.
