# Cahier des charges — Personal Relationship / People Graph App

## 1. Project overview

### Working concept
An open-source Android application that acts as a **private personal database of people and relationships**.

The app helps the user remember:

- who a person is;
- how they know that person;
- how people are related to each other;
- family relationships;
- important facts, notes, events, and documents related to a person;
- the path connecting two people.

The application should feel closer to a **private dossier + relationship graph** than to a conventional contact manager or genealogy application.

### Core idea
The same underlying relationship database can be viewed in multiple ways:

1. **Profile / dossier view** — detailed information about one person.
2. **Network graph view** — visual graph of people and their relationships.
3. **Family tree view** — traditional family-tree visualization centered on any selected person.
4. **Relationship path view** — explanation of how two people are connected.

---

## 2. Goals

The application should make it easy to:

- create people quickly;
- record relationships between people;
- understand complex family and social connections;
- attach notes, facts, photos, and documents;
- browse relationships visually;
- answer questions such as:
  - “Who is this person?”
  - “How do I know them?”
  - “How is this person related to me?”
  - “How are these two people connected?”
  - “Who are this person’s parents / children / siblings / partner?”
- keep all data private and preferably local to the device.

---

## 3. Product principles

### 3.1 Local-first
The application should work fully offline.

No account should be required.

The database should live locally on the device unless the user explicitly exports or backs it up.

### 3.2 Privacy-first
The app may contain sensitive personal information.

Therefore:

- no mandatory cloud synchronization;
- no analytics by default;
- no advertising;
- no automatic uploading of contacts, documents, or photos;
- optional biometric / PIN lock;
- optional encrypted backup.

### 3.3 Simple first, powerful later
Creating a person or relationship should be fast.

Advanced fields should never be mandatory.

The application should avoid becoming a heavy genealogy or CRM system.

### 3.4 One database, multiple views
Family trees, graphs, timelines, and relationship paths should all be generated from the same underlying entities and relationships.

A family tree is a **view of relationship data**, not a separate database.

---

## 4. Main entities

## 4.1 Person

Minimum fields:

- unique ID;
- first name / display name;
- optional last name;
- optional nickname;
- optional profile photo;
- notes.

Optional fields:

- birth date;
- death date;
- gender / pronouns;
- occupation;
- phone;
- email;
- address;
- place of birth;
- custom fields;
- tags;
- groups.

A person may have multiple names over time.

Examples:

- birth name;
- married name;
- nickname;
- previous surname;
- alias.

---

## 4.2 Relationship

A relationship connects two people.

Examples:

- parent of;
- child of;
- sibling of;
- spouse / partner of;
- ex-partner of;
- friend of;
- coworker of;
- neighbour of;
- knows;
- introduced by;
- teacher of;
- doctor of;
- custom relationship.

Possible fields:

- relationship type;
- source person;
- target person;
- start date;
- end date;
- notes;
- confidence / certainty;
- custom label.

### Reciprocal relationships
Some relationship types should automatically generate their inverse.

Examples:

- parent of ↔ child of;
- spouse of ↔ spouse of;
- sibling of ↔ sibling of;
- employer of ↔ employee of.

The user should not need to enter both sides manually.

---

## 4.3 Fact / information item

A person can have small pieces of structured or unstructured information attached to them.

Examples:

- “Lives in Lyon.”
- “Works at Renault.”
- “Moved to Paris in 2018.”
- “Likes photography.”
- “Met at university.”

Possible fields:

- text;
- type/category;
- date or date range;
- source;
- note;
- confidence;
- related people.

Facts should remain optional and lightweight.

---

## 4.4 Document

A document may be attached to one or more people.

Supported examples:

- photo;
- PDF;
- scan;
- screenshot;
- text note;
- URL/reference;
- certificate;
- letter;
- invitation.

Possible metadata:

- title;
- description;
- date;
- people involved;
- tags;
- source.

---

## 4.5 Event

An event represents something that happened and may involve one or more people.

Examples:

- birth;
- marriage;
- divorce;
- graduation;
- move;
- funeral;
- reunion;
- holiday;
- first meeting;
- job change;
- custom event.

Fields:

- title;
- date / approximate date;
- description;
- location;
- participants;
- attachments.

---

## 4.6 Group

Groups can help organize people without changing their relationships.

Examples:

- Dad’s family;
- Mum’s family;
- University;
- Work;
- Neighbours;
- Friends;
- Sports club.

A person may belong to multiple groups.

---

## 5. Core views

## 5.1 People list

Purpose:
Quick access to all known people.

Features:

- search;
- alphabetical browsing;
- profile thumbnails;
- filtering by group/tag;
- recently viewed;
- recently added;
- favorites.

---

## 5.2 Person profile / dossier

The central screen for one person.

Suggested sections:

### Header
- photo;
- name;
- nickname;
- age / birth date;
- quick relationship to the user.

Example:

> Sophie Martin  
> Your cousin’s wife

### Immediate relationships
Show:

- parents;
- siblings;
- partners;
- children;
- friends;
- coworkers;
- custom relationships.

### Facts
Small information cards.

### Timeline
Chronological events and facts.

### Documents
Photos, PDFs, scans, and notes associated with the person.

### Notes
Free-form personal notes.

### Actions
- edit;
- add relationship;
- add fact;
- add event;
- attach document;
- view in graph;
- view family tree;
- find relationship path.

---

## 5.3 Network graph view

A free-form graph representing people and relationships.

### Behaviour
- each person is a node;
- each relationship is an edge;
- tapping a node selects the person;
- selected node can be opened as a profile;
- graph can be re-centered on any person;
- zoom and pan;
- filter by relationship type;
- filter by group;
- choose relationship depth.

### Example

```text
Alice ── friend ── Bob
  │                 │
sister            coworker
  │                 │
Claire ────────── David
```

The graph is useful for understanding the broader social network.

---

## 5.4 Family tree view

A more traditional hierarchical family view.

The user can select **any person** as the central node.

Typical layout:

```text
       Robert ─── Anne
           │
     ┌─────┴─────┐
   David        Claire ─── Marc
     │                       │
   [ME]                    Sophie
```

### Rules

- parents appear above;
- partners appear beside a person;
- children appear below;
- siblings appear on the same generation;
- optionally display grandparents, grandchildren, cousins, and in-laws.

### Depth controls

Suggested options:

- Immediate family;
- 2 generations;
- Extended family;
- Custom depth.

### Re-centering

Tapping any person should allow:

> View family tree from this person

The tree then rebuilds around them.

---

## 5.5 Relationship path view

Allows selecting any two people and finding the shortest meaningful path between them.

Example:

```text
You
 ↓ child of
Marie
 ↓ sibling of
Paul
 ↓ spouse of
Sophie
```

Result:

> Sophie is connected to you through your mother’s brother.

Where possible, the app may translate known family paths into conventional relationship names:

- cousin;
- aunt;
- uncle;
- grandparent;
- niece;
- nephew;
- sibling-in-law;
- etc.

For complex paths, showing the explicit chain is sufficient.

---

## 5.6 Timeline view

A chronological view of events and facts.

Can exist at several levels:

- one person;
- one family/group;
- whole database.

Example:

```text
1982 — Claire born
2004 — Claire married Marc
2008 — Sophie born
2019 — Sophie moved to Nantes
2024 — You met Sophie
```

---

## 6. Smart relationship inference

The application may infer relationships from existing links.

Example:

If:

- Claire is the sister of David;
- David is the father of Alex;

then the app may infer:

- Claire is Alex’s aunt.

Important:

- inferred relationships must be clearly distinguishable from manually entered relationships;
- the app should not silently create permanent facts without user awareness;
- inference can initially be limited to common family relationships.

---

## 7. Dates and uncertainty

Personal information is often incomplete.

The app should support:

- exact dates;
- month + year;
- year only;
- approximate dates;
- unknown dates.

Examples:

- 12 March 1974;
- March 1974;
- 1974;
- circa 1974;
- unknown.

The UI should not force false precision.

---

## 8. Search

Global search should cover:

- people;
- nicknames;
- notes;
- facts;
- events;
- document titles;
- tags;
- groups.

Possible future natural-language searches:

- “people who live in Bordeaux”;
- “Dad’s cousins”;
- “people I met through Thomas”;
- “who works at Renault?”

These should be considered future features, not MVP requirements.

---

## 9. Import / export

### Export

The user should be able to export their database.

Possible formats:

- full application backup;
- JSON;
- CSV for people;
- GEDCOM for compatible family data;
- ZIP archive containing data + attachments.

### Import

Possible sources:

- previous app backup;
- Android contacts;
- GEDCOM;
- CSV.

Contacts import should be optional and selective.

---

## 10. Security

Recommended features:

- optional biometric lock;
- optional PIN lock;
- encrypted backup;
- clear local data deletion;
- no hidden network communication.

If cloud synchronization is ever added, it should remain optional.

---

## 11. MVP

The first usable version should remain deliberately small.

### Required MVP features

1. Create / edit / delete a person.
2. Add profile photo.
3. Add notes.
4. Create relationships between people.
5. Support common family relationship types.
6. Automatically handle reciprocal relationships.
7. People list with search.
8. Person profile / dossier.
9. Basic network graph.
10. Basic family tree centered on a selected person.
11. Re-center graph/tree on another person.
12. Local database.
13. Backup/export.

### Nice-to-have for MVP if easy

- groups;
- tags;
- birth dates;
- timeline;
- basic document/photo attachments.

---

## 12. Post-MVP features

Possible later additions:

- relationship path finding;
- automatic family relationship naming;
- events;
- richer timeline;
- PDF/document support;
- duplicate detection;
- merge profiles;
- custom relationship types;
- multiple names;
- places;
- organizations;
- pets;
- advanced graph filters;
- relationship inference;
- birthdays/reminders;
- “last seen” / “last contacted”;
- conversation reminders;
- contacts import;
- GEDCOM support;
- encrypted backups;
- optional synchronization;
- home-screen widgets.

---

## 13. Explicit non-goals

The application should initially avoid becoming:

- a social network;
- a messaging application;
- a surveillance tool;
- an OSINT scraper;
- a CRM;
- a full genealogy research suite;
- a cloud-only platform;
- an AI system that invents information about people.

The user enters and controls their own data.

---

## 14. Suggested navigation

A simple bottom navigation could contain:

### People
Main searchable list.

### Graph
Social/network relationship graph.

### Family
Family-tree view.

### Timeline
Events and chronological information.

Optionally:

### Settings
Backup, privacy, import/export, appearance.

Alternatively, Graph and Family could be different modes within a single **Relationships** tab.

---

## 15. Suggested Android architecture

Possible implementation:

- Kotlin;
- Jetpack Compose;
- Room database;
- Material 3;
- Navigation Compose;
- Android Photo Picker;
- Storage Access Framework for documents and backups.

Data model should be designed independently from the visual graph/tree implementation.

The graph and family tree are projections of the same relationship database.

---

## 16. Open-source goals

The application should:

- have a clear open-source license;
- have reproducible builds where practical;
- avoid proprietary dependencies where reasonable;
- work without Google services;
- be suitable for F-Droid distribution;
- document the local database / export format.

Possible licenses:

- GPL-3.0-or-later;
- AGPL-3.0-or-later;
- Apache-2.0;
- MIT.

GPL-3.0-or-later is a reasonable default for a FOSS Android application if the goal is to keep derivative versions open.

---

## 17. UX direction

The application should feel:

- personal;
- calm;
- visual;
- fast;
- private;
- slightly “dossier-like” without feeling sinister;
- easier than genealogy software;
- richer than a contact manager.

The most important design principle:

> Adding a person or relationship should take seconds, not minutes.

Advanced information should be optional and progressively discoverable.

---

## 18. Product identity

Possible positioning:

> **A private map of the people in your life.**

Alternative:

> **Remember who people are, how you know them, and how they are connected.**

The defining feature is not merely storing contacts.

It is the ability to represent the same people as:

- personal profiles;
- a social graph;
- a family tree;
- a timeline;
- relationship paths.

