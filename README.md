# Social Graph

A private map of the people in your life: who they are, how you know them, and
how they are connected to each other.

Everything lives on the phone. The app has **no `INTERNET` permission**, so it
cannot send your notes anywhere — that is checked by looking at the merged
manifest, not just promised here.

Licensed GPL-3.0-or-later; see `LICENSE` at the root of the repository.

## Building

```sh
git clone https://github.com/hamzatadlaoui/social-graph.git
cd social-graph
./gradlew :app:assembleDebug        # the app
./gradlew :app:testDebugUnitTest    # the rules, off-device
```

Kotlin, Jetpack Compose, Room and Material 3. AGP 9 brings its own Kotlin
(2.2.10), so the Compose compiler and KSP in `gradle/libs.versions.toml` are
pinned to match it — that is the one version relationship worth knowing about
before upgrading anything.

## How it is put together

| Package   | What lives there                                                        |
|-----------|-------------------------------------------------------------------------|
| `model/`  | `FuzzyDate`, `RelationshipType` — plain Kotlin, no Android               |
| `graph/`  | ego networks, family trees, shortest paths — also plain Kotlin           |
| `data/`   | Room entities, DAOs, the repository, the photo store                    |
| `ui/`     | Compose screens: people, dossier, graph, family, settings               |
| `ui/theme/` | the palette, the type scale and the shapes — the whole look           |
| `export/` | the backup format                                                       |

`model/` and `graph/` know nothing about Android or Room, which is why every
interesting rule is covered by ordinary JVM tests — 34 of them, run with
`./gradlew :app:testDebugUnitTest`.

### Relationships are stored both ways round

Recording "David is the parent of Alex" writes **two** rows sharing a `pairId`:
`PARENT_OF` from David, and `CHILD_OF` from Alex. Every question the app asks is
then a flat `WHERE fromId = ?`, and removing a tie is one delete by `pairId`.
`RelationshipEntity.inverse()` is the only place that knows this.

### Dates may be half known

`FuzzyDate` holds a year, optionally a month, optionally a day, and a flag for
"about". It is stored as one string: `1974-03-12`, `1974-03`, `1974`, `c.1974`,
or empty for unknown. The UI never asks for precision the user does not have.

## The look

Cold and clinical, after *Orwell: Keeping an Eye On You* — a blue-slate ground,
one cyan that carries everything selectable, amber held back for things wanting
attention, square corners, and condensed headings over a monospace body. Names
and half-known dates read as records rather than prose, which is the point.

`ui/theme/Color.kt` is the entire palette; no screen names a colour of its own.
Dynamic colour (Material You) is off by default in `SocialGraphTheme` — it would
replace the scheme wholesale on Android 12 and up — but remains available as a
parameter.

The two bundled font families are the only third-party assets in the repository;
see `THIRD-PARTY.md` for their provenance and licence.

## The backup format

`Settings → Export a backup` writes a zip:

```
social-graph-2026-09-01.zip
├── backup.json
└── photos/
    ├── 2b1e….jpg
    └── …
```

`backup.json` is plain, documented JSON so it outlives any one build of the app:

```json
{
  "version": 1,
  "people": [
    {
      "id": "…", "displayName": "Claire", "lastName": "Martin",
      "nickname": "Clo", "photo": "2b1e….jpg", "notes": "Met at university.",
      "birth": "c.1982", "death": "", "pronouns": "she/her",
      "isMe": false, "isFavourite": true,
      "createdAt": 1700000000000, "updatedAt": 1700000001000
    }
  ],
  "relationships": [
    {
      "id": "…", "pairId": "…", "fromId": "…", "toId": "…",
      "type": "PARTNER_OF", "customLabel": "",
      "start": "2004-06", "end": "", "notes": "", "certainty": "SURE"
    }
  ]
}
```

Both directions of each tie appear, sharing their `pairId`. Restoring matches on
`id`, so restoring the same file twice leaves one copy rather than two, and a row
that cannot be read is skipped rather than failing the whole restore.
