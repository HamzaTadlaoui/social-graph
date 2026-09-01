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
| `ui/`     | Compose screens: people, dossier, graph, family, files, settings         |
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

## Files, and who is in them

A fourth tab holds documents: photographs, scanned letters, certificates,
recordings - any file at all. Each one is copied into the app's own storage
rather than pointed at, for the same reason photos are: a content URI's
permission can be withdrawn, and a document that vanishes from a dossier is
worse than the copy costing space.

Anyone can be tagged in a file. On a photograph you drag a box round a face and
give it a name, as many times as there are people in it; the region is stored as
fractions of the image rather than pixels, so it still means the same thing after
the picture is re-encoded at another size. Anything that is not an image is
tagged as a whole, which is the only sensible thing to say about a PDF. Either
way the tag reads from both ends: the file lists who is in it, and a person's
page grows an **Appears in** section.

No box is final. Tap one to open it again: drag inside it to move it, drag a
corner to resize it, and the change is written back as soon as you let go.
`CropBox` in `model/` holds that arithmetic - flipping a box dragged past its own
corner, stopping it at the edges of the picture, working out where a fitted image
actually landed inside the view - and being plain Kotlin, all of it is covered by
ordinary JVM tests rather than by poking at a phone.

A rectangle under two per cent of the picture each way is refused, because a stray
tap on a photograph should not silently become a tag. Deleting a file, or a
person, takes their tags with it.

## The look

The network is drawn as a board of faces: each node is the person's photograph,
framed in cyan, amber for whoever the view is centred on, with their initials
standing in where there is no photograph yet. `rememberPortraits` decodes each
file once at the size a node is actually drawn, so a large network does not pull
camera-resolution bitmaps into memory to fill a screen of small squares.

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
├── photos/
│   ├── 2b1e….jpg
│   └── …
└── documents/
    ├── a73f….png
    └── …
```

Documents are included rather than merely listed: a backup that restores to a set
of entries with no files behind them is not a backup. `backup.json` is plain,
documented JSON so it outlives any one build of the app:

```json
{
  "version": 2,
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

Version 2 added `documents` and `documentTags`. A version 1 file simply has
neither and still restores, which is why every reader is written to find nothing
rather than to fail.
