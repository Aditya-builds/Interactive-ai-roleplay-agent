# Data directory

This folder holds all JSON for the roleplay engine.

## Canonical model (target)

See **[docs/JSON_MODEL.md](docs/JSON_MODEL.md)** for the full architecture.

```
data/
├── docs/JSON_MODEL.md       ← design spec (read this first)
├── characters.json          ← index
├── worlds.json              ← index
├── characters/              ← DEFINITIONS: who exists
├── worlds/                  ← DEFINITIONS: world + location catalog
└── conversations/           ← RUNTIME: one file per story session
    └── conversation-001.json  ← reference example (20 messages)
```

## Three layers

| Layer | Location | Example |
|-------|----------|---------|
| Definitions | `characters/`, `worlds/` | Aurora's personality, guild_hall description |
| World state | inside `conversations/` | Aurora HP 72, current scene, relationships |
| History | inside `conversations/` | events, memories, messages |

## Reference conversation

**[conversations/conversation-001.json](conversations/conversation-001.json)** is a complete 10-exchange story showing:

- Scene progression: guild_hall → forest → forest_clearing → guild_hall
- Character state evolution: Aurora 100→72 HP, injured, determined
- Relationship changes: Aurora↔User trust 42→58
- Multi-character dialogue: Aurora and Laxus with `characterId` on assistant messages
- Events vs memories distinction
- Location ids validated against `worlds/fantasy_world.json`

## Legacy layout (still used by running Java code)

Until Java is migrated, the backend still reads:

- `aurora/aurora.json`, `laxus/laxus.json`
- `fantasy/fantasy.json`
- `worldId: "fantasy"` (not `fantasy_world`)

Do not delete legacy files until migration is complete.
