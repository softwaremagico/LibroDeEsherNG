# Libro de Esher 2

[![GNU GPL 3.0 License](https://img.shields.io/badge/license-GNU_GPL_3.0-brightgreen.svg)](https://github.com/softwaremagico/librodeesher/blob/master/license/gnugpl/license.txt)
[![Powered by](https://img.shields.io/badge/powered%20by%20java-orange.svg?logo=OpenJDK&logoColor=white)]()

Libro de Esher is a Java library for the **Rolemaster** character system. It provides a fully
data-driven rules engine covering every rulebook/supplement of the original desktop application,
migrated from legacy tab-separated text files into bilingual (Spanish/English) XML, plus the
character-creation model built on top of it.

## Feature Overview

### Rule catalog and validation

- Every rule concept (categories, skills, perks, weapons, trainings, professions, magic spell lists,
  races, cultures) is loaded from XML through Jackson, instead of the legacy custom text-file parsers.
- A single, cohesive `RulesCatalog` entry point resolves any of these by id without callers needing
  to know which factory/file owns each concept.
- A rule-reference resolver validates that every cross-reference between rule elements (e.g. a race
  granting bonuses to a category, a training requiring a skill) actually points to an id that exists
  among the currently enabled modules, instead of failing silently or at random.
- Ids are always English and derived consistently from the original Spanish names; enums are
  serialized by their plain English constant name. The original Spanish text is kept side by side
  with its English translation on every user-facing name, never lost.

### Character domain coverage

- Character foundations: race, culture, profession selection (by id, resolved through the rule
  catalog).
- The ten characteristics, with their full development rules: initial/temporal/potential value
  rolls, per-rank bonus tables, and age-related decline.
- Level-by-level development bookkeeping: category and skill ranks, characteristic upgrade rolls,
  spell list acquisition costs, favourite skills and specializations.
- A generic decision-resolution layer for every "choose one of {a; b; ...}" grant found throughout
  the rule data (a training's category/skill choices, characteristic upgrades, and more), validating
  every player choice against what was actually offered.
- Background points spent on extra category/skill ranks, characteristic rolls, and history languages.
- Flat category/skill bonuses and characteristic preferences granted by a character's profession.

### Modular rule data

- XML-based modular content loading system: every rulebook/supplement lives under its own folder in
  `modules/`, and can be enabled or disabled independently.
- Built-in support for the full Rolemaster catalog, including:
  - Character Law and Core rules
  - Spell Law (Channeling, Essence, Mentalism)
  - Arms Law &amp; Claw Law (Firearms, The Armory)
  - Companions (Channeling, Essence, Mentalism, Skill, Treasure)
  - Martial Arts Companion
  - Fire &amp; Ice: The Elemental Companion
  - Races and Cultures (including Underground Races)
  - Shadow World setting material
  - Pulp Adventures
  - House rules / non-official content

### Migration tooling

- One-shot command-line migration tools convert every legacy text file format into the
  corresponding XML, reproducing the original parsing rules (including documented quirks and data
  typos found along the way) while never leaving untranslated or unresolved text behind.
- Every migrated data type is covered by tests asserting both the migration logic (against small,
  representative fixtures) and the resulting real data (against the actual generated XML).

## Scope

This repository contains the core rules library and character-creation model. It is **not** a
standalone end-user application.

Two additional modules are scaffolded for future work:

- `librodeesher-random`: random character generation.
- `librodeesher-pdf`: printable character sheet export.

## Architecture

- Multi-module Maven build (`librodeesher-rules`, `librodeesher-random`, `librodeesher-pdf`).
- Library-first: no UI dependency, designed for integration into external apps/services, and safe to
  run on Android (Jackson XML instead of JAXB/DOM/SAX, no reflection-heavy frameworks).
- Rule data lives outside the compiled jar's `src/main/resources`, under a sibling `modules/` folder,
  one directory per rulebook/supplement, so it can be edited without recompiling.

## Notes

- Rolemaster is a trademark owned by Iron Crown Enterprises / Aurigas Aldebaron.
