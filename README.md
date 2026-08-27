<div align="center">

<img width="220" height="220" alt="WorldTaste" src="https://github.com/user-attachments/assets/89593566-830a-466a-b8f2-6cd2b2459d0b" />

# WorldTaste
### Food, farming, fishing, cooking and machines for Slimefun Legacy

WorldTaste brings a huge collection of food, crops, fishing content, butchering, cuisine, kitchen equipment and production machines to modern Slimefun servers.

[![Build](https://github.com/wickidcow/SF_WorldTaste/actions/workflows/build.yml/badge.svg)](https://github.com/wickidcow/SF_WorldTaste/actions/workflows/build.yml)
[![License](https://img.shields.io/github/license/wickidcow/SF_WorldTaste?label=license)](LICENSE)
[![Java](https://img.shields.io/badge/Bytecode-Java%2021-orange)](#requirements)
[![Paper](https://img.shields.io/badge/Paper-1.21.11-blue)](#requirements)
[![Slimefun](https://img.shields.io/badge/Slimefun-Legacy-brightgreen)](https://github.com/wickidcow/Slimefun-Legacy)
[![Language](https://img.shields.io/badge/Player%20language-English-brightgreen)](#english-first)

[Download](https://github.com/wickidcow/SF_WorldTaste/releases) ·
[Builds](https://github.com/wickidcow/SF_WorldTaste/actions) ·
[Slimefun Legacy](https://github.com/wickidcow/Slimefun-Legacy) ·
[InfinityExpansion2](https://github.com/wickidcow/SF_InfinityExpansion2) ·
[Report a Bug](https://github.com/wickidcow/SF_WorldTaste/issues)

Current compatibility line: **WorldTaste 1.0.11**

</div>

> [!IMPORTANT]
> **WorldTaste is an unofficial, independently maintained Slimefun addon fork.**
> This repository modernizes the original WorldTaste project for Slimefun Legacy, current Paper servers, English-language use and InfinityExpansion2 compatibility while preserving the original content identity wherever practical.
>
> It is not an official release of Slimefun, Slimefun Legacy, InfinityExpansion, InfinityExpansion2 or Mojang/Microsoft.

---

## ✨ What is WorldTaste?

WorldTaste is a large content addon focused on food and everyday-life systems that complement the technology-heavy side of Slimefun. It adds ingredients, crops, meals, fishing, meat processing, specialty cuisine, kitchen equipment, production machines and decorative content without requiring a client mod.

| Area | What it adds |
| --- | --- |
| **Food & cooking** | Baking, meat, seafood, soups, drinks, desserts, snacks, fermented foods and themed meals |
| **Crops & farming** | Seeds, crops, harvesting behavior and agricultural ingredients |
| **Fishing** | Rods, bait, weighted catches and fishing-related processing |
| **Butchering** | Mob-specific meats, ingredients and processing equipment |
| **Machines** | Food processors, cookers, workbenches, multiblocks and production systems |
| **Decoration** | Kitchen, shop and food-themed decorative items from the original content set |

The addon contains a very large data-driven content library. Existing Slimefun IDs and YAML keys are deliberately preserved where possible so modernization does not unnecessarily invalidate old recipes, stored items or existing worlds.

---

## 📦 Download and requirements

| Requirement | Supported setup |
| --- | --- |
| **Slimefun core** | Slimefun Legacy — primary build and compatibility target |
| **Minecraft / Paper API** | 1.21.11 |
| **Build JDK** | Java 25 |
| **WorldTaste bytecode** | Java 21 |
| **InfinityExpansion2** | Optional, supported through compatibility mapping |
| **InfinityExpansion** | Optional legacy compatibility |
| **JustEnoughGuide** | Optional integration |
| **Other integrations** | Gastronomicon, ExoticGarden, Cultivation and LogiTech where referenced by WorldTaste content |
| **Client mod** | Not required |
| **Resource pack** | Not required by WorldTaste |

WorldTaste follows the compatibility model used by Slimefun Legacy: CI runs on a modern JDK but deliberately emits **Java 21 bytecode** for the addon.

> [!WARNING]
> Back up your server before replacing an existing WorldTaste build. Install or update Slimefun addons with a **full server restart**. Do not use `/reload` or plugin hot-reload tools.

### Raw JAR downloads

GitHub Actions artifacts are ZIP archives by design, so this project does **not** use them as the primary downloadable build.

Development and version releases publish the compiled JAR directly through [GitHub Releases](https://github.com/wickidcow/SF_WorldTaste/releases):

```text
SF_WorldTaste1.0.11.jar
```

No ZIP extraction is required.

---

## 🚀 Installation

1. Stop the Minecraft server normally.
2. Create a backup of the server and Slimefun data.
3. Install a compatible build of [Slimefun Legacy](https://github.com/wickidcow/Slimefun-Legacy).
4. Download `SF_WorldTaste1.0.11.jar` from [GitHub Releases](https://github.com/wickidcow/SF_WorldTaste/releases).
5. Place the JAR in the server's `plugins/` directory.
6. Remove or archive any older WorldTaste JAR so only one copy can load.
7. Start the server and review the console for WorldTaste, Slimefun or optional-addon compatibility warnings.
8. Open the Slimefun Guide and test representative WorldTaste foods, crops, machines and recipes before deploying to a production server.

---

## 🔌 Compatibility

### Slimefun Legacy

Slimefun Legacy is the primary compatibility target for this fork. CI checks out the current `wickidcow/Slimefun-Legacy` source, builds it, then compiles WorldTaste against that freshly generated Slimefun JAR.

This catches API or linkage regressions that would be missed by compiling only against the older bundled development dependency.

### InfinityExpansion2

WorldTaste does **not** hard-depend on InfinityExpansion2. IE2 support is optional and is implemented without linking IE2 classes directly into the addon.

When InfinityExpansion2 is installed, legacy InfinityExpansion IDs used by WorldTaste recipes are resolved to their modern IE2 equivalents. The explicit compatibility table is checked against the canonical `LegacyIdMapper` in [SF_InfinityExpansion2](https://github.com/wickidcow/SF_InfinityExpansion2).

Examples include:

| Legacy InfinityExpansion ID | InfinityExpansion2 ID |
| --- | --- |
| `INFINITE_INGOT` | `IE_INFINITY_INGOT` |
| `INFINITE_MACHINE_CORE` | `IE_INFINITY_MACHINE_CORE` |
| `INFINITE_MACHINE_CIRCUIT` | `IE_INFINITY_MACHINE_CIRCUIT` |
| `END_ESSENCE` | `IE_ENDER_ESSENCE` |
| `INFINITY_FORGE` | `IE_INFINITY_WORKBENCH` |
| `BASIC_STRAINER` | `IE_STRAINER_1` |
| `INFINITY_STORAGE` | `IE_STORAGE_UNIT_6` |

Dynamic families are handled as well, including mob data cards and quarry oscillators.

The resolver tries the exact Slimefun ID first, then known IE1/IE2 mappings and compatibility fallbacks. This allows WorldTaste to remain usable across legacy and current InfinityExpansion installations without making IE2 a required dependency.

### Paper, Purpur and Folia

WorldTaste is built against the Paper 1.21.11 API and is intended primarily for the same modern Paper environment used by Slimefun Legacy.

Conventional Paper forks such as Purpur should generally behave the same as Paper. Folia compatibility depends on both Slimefun Legacy and every installed addon being safe for region-threaded execution; WorldTaste avoids obvious direct Bukkit scheduler and blocking-thread patterns, but Folia should still be treated as a staging/test target rather than assumed production support.

---

## 🌐 English-first

The original WorldTaste content was primarily Chinese and contains thousands of display strings. This fork provides an English-first presentation layer while preserving the stable content identifiers underneath it.

The modernization currently includes:

- English Slimefun Guide categories and group names
- English machine and menu controls
- English recipe and crafting interfaces
- English gameplay messages
- English loader, warning and diagnostic messages
- semantic English item names translated from the original display text where known
- curated English aliases for common historical WorldTaste IDs such as seafood, ingredients, drinks and meals
- no generic `Category Item` fallback for unresolved names
- suppression of unresolved Chinese-only lore instead of exposing mixed-language player text

The underlying Slimefun IDs and YAML keys are intentionally **not renamed simply for translation**. This avoids turning localization into a destructive item or world migration.

CI also scans Java runtime strings and rejects newly introduced CJK player/admin text outside the intentional translation lookup dictionary.

> [!NOTE]
> WorldTaste's enormous original content library means not every historic flavor-lore line has been manually rewritten word-for-word. Display names now prefer semantic English translations, while stable IDs remain unchanged for compatibility. Any still-untranslated edge cases can be translated without changing item identity.

---

## 🎨 Resource-pack behavior

WorldTaste does **not** ship or require a traditional Minecraft resource pack.

Its item presentation primarily uses:

- vanilla Minecraft materials;
- normal Slimefun item stacks; and
- custom player-head textures loaded from texture hashes, Base64 data or URLs.

For guide stability, WorldTaste category icons that were custom player heads are rendered with lightweight vanilla icons. This reduces the client-side texture burst when opening the WorldTaste guide while leaving the actual craftable WorldTaste items and their textures unchanged.

There is no bundled `pack.mcmeta` / `assets` resource-pack tree and WorldTaste does not require a separate CustomModelData pack for normal use.

---

## 🧪 Continuous verification

Every maintained build is checked for the compatibility points that matter most to this fork:

- English runtime-string validation
- current InfinityExpansion2 mapping synchronization
- current Slimefun Legacy source build
- WorldTaste compilation against that freshly built Slimefun Legacy JAR
- Java 21 class-file compatibility
- plugin metadata and IE2 soft-dependency validation
- direct raw-JAR discovery before release publishing

---

## ⚙️ Building from source

Clone the repository and run:

```bash
./gradlew clean build
```

To test against another Slimefun Legacy JAR:

```bash
./gradlew clean build -PslimefunJar=/path/to/Slimefun-Legacy.jar
```

Output:

```text
build/libs/SF_WorldTaste1.0.11.jar
```

---

## ❤️ Credits and project history

WorldTaste originates from the original content project created by **haiman233** and the community around its scripted/RSC implementation.

The standalone plugin version and later maintenance work built on that original project. Credit also belongs to **balugaq** for `RSCEditor`, **Eventually**, and the other original contributors who helped build the surrounding content ecosystem.

This fork is maintained by **wickidcow** as the modern Slimefun Legacy compatibility line. The goal is to preserve the original project's identity and content while making it practical to run on current English-language Slimefun servers.

---

## 🐛 Issues and testing

Please report reproducible problems through [GitHub Issues](https://github.com/wickidcow/SF_WorldTaste/issues).

For compatibility reports, include Minecraft/Paper version, Java version, Slimefun Legacy version, WorldTaste version, optional addon versions, relevant console output, and reproduction steps.
