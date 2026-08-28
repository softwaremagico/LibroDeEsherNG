# Libro de Esher NG

[![GNU GPL 3.0 License](https://img.shields.io/badge/license-GNU_GPL_3.0-brightgreen.svg)](https://github.com/softwaremagico/librodeesher/blob/master/license/gnugpl/license.txt)
[![Powered by](https://img.shields.io/badge/powered%20by%20java-orange.svg?logo=OpenJDK&logoColor=white)]()

> His heartbeat pounded in his ears as he ran through the long corridors. "Pixie, no...
> sylph, no... gnome, no..." his lips mouthed in a low voice as he turned the pages of that old
> book he had found long ago and now held in his hands. He knew these corridors like the back of
> his hand, which let him find what he needed while fleeing toward the heart of his lair, or
> perhaps toward some place far from here. He had to hurry, since they must be very close by now.
> The echo carried to him the laughter and excitement of those intruders in search of riches.
> "Goblin, no... orc, maybe, though I need something bigger...". He couldn't understand how such
> a disparate band of races — elves, humans, dwarves — could join together to sack his home. They
> had come in unannounced, and his poorly paid minions had died quickly. Perhaps he should have
> prepared his defenses better instead of spending these last few days on his personal pleasures.
> Adventurers are known for their unpredictable whims. They had a thirst for blood this evening
> and no other, and of course, they never give much warning. It was too late for regrets now, and
> his only chance was to summon something big or terrifying enough to at least give the intruders
> some entertainment and delay them by an hour or so. Perhaps the evening would be over before the
> intruders reached him, and by night he'd have time enough to better fortify whatever chambers
> remained intact, move his savings somewhere safer, and find more select minions; or perhaps, at
> least, this defeat wouldn't be a total disgrace, and he could die with what little honor he had
> left. "Centaur, no...". Nothing he found pleased him. "Laan, no...". A scream in the distance
> told him the last of his minions had fallen. "Fairy, no...", he had already gone through half
> the book's pages and still had nothing to his liking. "Gratar, no..." his fingers kept turning
> until, finally, they stopped on one page: "War troll". His lips repeated it again, "War troll",
> and again, and again, as if trying to make the idea real through the sound of his own voice
> alone. Meanwhile, his fingers drummed on the book and his steps grew slower. His mind began to
> see a way out. "War troll", yes, that could work — "troll" sounded big enough, and "war" was as
> terrifying as what he was looking for. Together, these two words had the flavor of the solution
> he needed. "War Troll" he said again, each time louder, as if savoring the words. He stopped
> running and turned toward where the sounds of looting and death were coming from. If he had
> enough time to summon one or two big enough to block this corridor, maybe he had a chance, and
> the evening wouldn't be a disaster after all...

## About Libro de Esher

Libro de Esher is an application that makes it easy to create character sheets for your players,
or to create non-player characters in just a few seconds. It also lets each user quickly customize
and add their own races and professions, giving all the flexibility needed for any kind of game.

Libro de Esher was originally developed by Jorge Hortelano Otero as a free-software desktop
application, distributed under the GNU license, hoping it would spare game masters the tedious
work of building a PC or help them out with their NPCs. This repository, **Libro de Esher NG**, is
a full modernization of the [original application](https://github.com/softwaremagico/LibroDeEsher):
the same Rolemaster rules engine and character-creation model, rebuilt from scratch as a library.

Highlights of the modernization:

- **XML rule data instead of legacy text templates.** Every rulebook/supplement used to be a set of
  custom tab-separated `.txt` files with their own ad-hoc parsing quirks. All of it has been
  migrated into structured, bilingual (Spanish/English) XML, read through Jackson instead of
  hand-rolled parsers, with ids and enums consistently in English and every cross-reference between
  rule elements validated instead of failing silently.
- **A modernized, tested codebase.** The domain model, rule catalog, migration tools and
  character-creation logic are new code, covered by unit tests down to the migration fixtures and
  the real generated data, replacing the original application's Swing UI-coupled, untested classes.
- **Library-first architecture.** No AWT/Swing/JPA dependencies: the rules engine is a plain,
  Android-safe Java library, ready to be consumed by different front-ends instead of being tied to
  one desktop UI.

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

- Original application icons from the Gartoon Gnome Icon Theme, under the GNU license.
- The original desktop application used the iText library for PDF character sheet generation.
- Original Pulp Adventures sheet templates created by Daniel Cordellat.
- Rolemaster is a trademark owned by Iron Crown Enterprises / Aurigas Aldebaron.
