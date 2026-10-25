# Spawn Alerts

Tells the players when a rare **Cobblemon** Pokémon spawns: which Pokémon and where, as precisely
as each server chooses. By default **only the biome, never the coordinates**. For **Cobblemon 1.8.2**
on **Fabric 1.21.1**.

> [Spawn] Um **Pikachu ✦ Shiny** apareceu em **Floresta de Bétulas**!

**Em português:** avisa no chat quando aparece um Pokémon raro (shiny, Alfa, lendário, mítico,
ultracriatura, paradoxo): qual Pokémon e em qual bioma. Cada servidor escolhe na config o que é
anunciado, se mostra coordenadas e o jogador mais perto, e quem recebe (todos, a mesma dimensão ou um
raio). Ou o **modo sentido**: no lugar do nome, uma frase que não entrega nada ("Você sente uma presença
poderosa por perto…"), para quem está perto e, nos mais raros, uma de longe para todo o servidor. O mod
roda no servidor: os jogadores não precisam instalar. O que muda em cada versão:
[`CHANGELOG.md`](CHANGELOG.md).

## How it works

- Every natural Pokémon spawn goes through Cobblemon's spawn event. When the Pokémon is one the
  config announces (by default: **shiny, Alpha, legendary, mythical, Ultra Beast and paradox**), the
  players get a chat message and a bell. The server log gets the message too.
- By default the message only names the Pokémon and the biome; the server can add the coordinates
  and the nearest player, or drop the biome. The names go to the players as translations from
  Cobblemon and the game, so each player reads them in their own language; a biome with no
  translation (from a data pack, for one) shows its id made readable (`volcanic_peaks` →
  "Volcanic Peaks").
- Or, in the **sense mode**, the players never learn which Pokémon: they get a phrase picked at random
  that names nothing (see below), and the server log keeps the full line for the staff.
- Spawns another mod cancels are not announced.
- `/spawnalerts reload` reads the config again without a restart, and `/spawnalerts test` shows you
  the message of a shiny Pikachu spawning where you stand (operators only).

## Installing

Download the jar from the
[latest release](https://github.com/fernandoabf/cobblemon-spawn-alerts-fabric/releases/latest)
and put it in `mods/`:

- **Server:** only on the server. Players without the mod can join.
- **Single player:** in your game's `mods/`.

It needs Fabric API and Cobblemon 1.8.x (Cobblemon already bundles fabric-language-kotlin).

## Configuration

`config/spawnalerts.json` (created on the first start; `/spawnalerts reload` after editing it). The
texts are in Brazilian Portuguese by default: the server writes them, so change them there for
another language.

**What is announced**

| Option | Default | What it does |
| --- | --- | --- |
| `enabled` | `true` | Announce rare spawns at all. |
| `announceShiny`, `announceAlpha` | `true` | Shiny / Alpha Pokémon. |
| `announceLegendary`, `announceMythical` | `true` | Legendary / mythical Pokémon. |
| `announceUltraBeast`, `announceParadox` | `true` | Ultra Beasts / paradox Pokémon. |
| `extraSpecies` | `[]` | Species always announced, e.g. `["dratini", "cobblemon:riolu"]`. |
| `ignoredSpecies` | `[]` | Species never announced, not even shiny or Alpha. |
| `announceAll` | `false` | Every spawn (floods the chat: Cobblemon spawns dozens a minute). |
| `announceMega` | `false` | Wild Mega Pokémon (a data pack's spawns in a Mega Showdown Mega form), even Alpha or shiny ones; off keeps them a surprise. |

**How much of the place is told** (the parts go into `{location}`)

| Option | Default | Text | Shows |
| --- | --- | --- | --- |
| `showBiome` | `true` | `biomeFormat`: ` em &a{biome}` | the biome |
| `showCoordinates` | `false` | `coordinatesFormat`: ` &7(x {x}, y {y}, z {z})` | the exact block |
| `showNearestPlayer` | `false` | `nearestPlayerFormat`: ` &fperto de &b{player}` | the nearest player's name |

**Who hears it, and how**

| Option | Default | What it does |
| --- | --- | --- |
| `sameDimensionOnly` | `false` | Only players in the dimension of the spawn. |
| `radius` | `0` | Only players within this many blocks of the spawn (0: everyone). |
| `actionBar` | `false` | On the action bar above the hotbar instead of the chat. |
| `playSound` | `true` | A bell with the message. |

**Texts**

| Option | Default |
| --- | --- |
| `message` | `&6[Spawn] &fUm &e{pokemon}{tags}&f apareceu{location}&f!` |
| `shinyTag`, `alphaTag`, `legendaryTag`, `mythicalTag`, `ultraBeastTag`, `paradoxTag` (what `{tags}` shows) | ` ✦ Shiny`, ` Alfa`, ` Lendário`, ` Mítico`, ` Ultracriatura`, ` Paradoxo` |

`{pokemon}` and `{biome}` can also be used directly in `message`; colours use `&` codes (`&6` gold,
`&e` yellow, `&a` green, `&f` white, `&7` grey...).

**The sense mode** (`"mode": "sense"`; the default is `"classic"`, the message above)

No message to the players names the Pokémon, the biome, the coordinates or the nearest player, and no tag
says shiny, Alpha, legendary or the like: a spawn is something the players *sense*. Each spawn the config
announces falls into its most notable category (`legendary`, `mythical`, `mega`, `ultra_beast`,
`paradox`, `shiny`, `alpha`, in that order); one of the category's near phrases goes to the players
within `senseRadius` (same dimension) and, for the categories in `senseEveryone`, one of its far phrases
to everyone else:

> *Você sente uma presença poderosa por perto…* (a legendary, near)
> *Uma presença poderosa despertou em algum lugar do mundo…* (the same legendary, for everyone else)
> *Algo brilhou de relance por perto…* (a shiny, only near)

What is announced still follows the options above (`extraSpecies` and `announceAll` only count in the
classic mode). The server log gets the full classic line with the biome and the coordinates, so the staff
still knows. With `playSound`, the near players hear a soft chime.

| Option | Default | What it does |
| --- | --- | --- |
| `mode` | `classic` | `sense` turns the sense mode on. |
| `senseRadius` | `64` | How near (blocks, same dimension) a player gets the near phrase. |
| `senseEveryone` | `["legendary", "mythical", "mega", "ultra_beast", "paradox"]` | The categories everyone hears of (near phrase within the radius, far phrase beyond); the others reach only the near players. |
| `senseStyle` | `&7&o` | The `&` codes every phrase starts with (grey italic). |
| `sensePhrases` | Brazilian Portuguese phrases | Per category, `near` and `far` lists; one phrase is picked at random per spawn. |

## Versions

| Component | Version |
| --- | --- |
| Minecraft | 1.21.1 (official Mojang mappings) |
| Fabric Loader | 0.19.5 (mod requires >= 0.16.10) |
| Fabric API | 0.116.17+1.21.1 |
| Cobblemon | 1.8.2+1.21.1 |
| Gradle / Loom / Kotlin | 8.14.3 / 1.13.6 / 2.2.20 |
| Java | 21 |

## Building

You need a **JDK 21**.

```bash
./gradlew build                  # -> build/libs/spawnalerts-<version>.jar
./gradlew runGametest            # headless gametest server with the real Cobblemon; exits by itself
./gradlew runProductionGametest  # the same tests on a production Fabric server (remapped jars)
```

The first configuration downloads the official Cobblemon 1.8.2 jar from Modrinth into `run-libs/`
(git-ignored, SHA-1 checked). The mod compiles against it (Cobblemon's spawn event) but never ships
it.

Releases are published by `.github/workflows/release.yml`: a push with a `mod_version` that has no
release yet builds the jar, runs `build runGametest runProductionGametest` and publishes it as
release `v<mod_version>`, with the version's section of `CHANGELOG.md` as notes.

## Credits and licenses

- Code and icon (drawn by [`tools/make_icon.py`](tools/make_icon.py)): **fernandoabf** — all rights reserved, see
  [`LICENSE`](LICENSE).
- [Cobblemon](https://cobblemon.com) is developed by the Cobblemon team. Pokémon is © Nintendo /
  Game Freak / The Pokémon Company. This project is not affiliated with any of them.
