# Data folder

Local JSON storage for the roleplay engine.

```
data/
├── characters.json       # list of all characters (id, name, worldId, imageUrl)
├── worlds.json           # list of all world ids
├── aurora/               # one folder per character
│   ├── aurora.json       # health, background, values, personality, presence
│   └── images/
│       └── aurora.png
├── laxus/
│   ├── laxus.json
│   └── images/
├── fantasy/              # one folder per world
│   └── fantasy.json
└── conversations/        # runtime story sessions (generated)
```

## Adding a character

1. Create folder `data/{id}/`
2. Add profile file `data/{id}/{id}.json` with health, background, values, etc.
3. Add portrait to `data/{id}/images/{id}.png`
4. Copy portrait to `frontend/public/characters/{id}.png` for the UI
5. Add an entry to `data/characters.json`

## Character profile fields (`{id}/{id}.json`)

| Field | Description |
|-------|-------------|
| `health` | `{ "current": 100, "max": 100 }` |
| `background` | Backstory |
| `values` | Core values |
| `personality` | Personality traits |
| `speakingStyle` | How they talk |
| `presence` | Default location, time, and situation |

## characters.json entry

```json
{
  "id": "aurora",
  "name": "Aurora",
  "worldId": "fantasy",
  "imageUrl": "/characters/aurora.png"
}
```
