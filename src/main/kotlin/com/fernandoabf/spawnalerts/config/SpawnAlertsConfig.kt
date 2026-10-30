package com.fernandoabf.spawnalerts.config

import com.fernandoabf.spawnalerts.SpawnAlerts
import com.fernandoabf.spawnalerts.SpawnAlertsMod
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.resources.ResourceLocation
import java.nio.file.Files
import java.nio.file.Path

/**
 * Server-side options in `config/spawnalerts.json`: what is announced, how much of the place is told
 * and who hears it, or, in the sense mode, the phrases that tell nothing. Missing keys get their default
 * and the file is rewritten with every option, so new options show up after an update;
 * `/spawnalerts reload` reads it again. The texts are in Brazilian Portuguese by default: the server
 * writes them, so they cannot follow each player's language (the Pokémon and biome names do, see
 * [SpawnAlerts]).
 */
object SpawnAlertsConfig {
    /** The message naming the Pokémon (the default). */
    const val CLASSIC = "classic"

    /** The players' sense: a phrase that names nothing, never the Pokémon, the place or what makes it rare. */
    const val SENSE = "sense"

    /** What a spawn is, for the sense mode, from the most notable: a Pokémon in several uses the first. */
    val SENSE_CATEGORIES = listOf("legendary", "mythical", "mega", "ultra_beast", "paradox", "shiny", "alpha")

    /** A category's phrases: [near] for the players within the sense radius, [far] for everyone else. */
    data class SensePhrases(val near: List<String>, val far: List<String> = emptyList())

    private val POWERFUL = SensePhrases(
        near = listOf("Você sente uma presença poderosa por perto…", "O ar ficou pesado de repente…", "Algo antigo despertou não muito longe daqui…"),
        far = listOf("Uma presença poderosa despertou em algum lugar do mundo…", "O mundo inteiro estremeceu por um instante…", "Algo antigo despertou em algum lugar…"),
    )
    private val OTHERWORLDLY = SensePhrases(
        near = listOf("O espaço parece se dobrar por perto…", "Você sente algo que não pertence a este mundo…"),
        far = listOf("O espaço se dobrou em algum lugar do mundo…", "Algo que não pertence a este mundo apareceu em algum lugar…"),
    )

    /** The phrases the sense mode starts with, in Brazilian Portuguese. */
    val DEFAULT_SENSE_PHRASES: Map<String, SensePhrases> = mapOf(
        "legendary" to POWERFUL,
        "mythical" to POWERFUL,
        "mega" to SensePhrases(near = listOf("Uma energia estranha pulsa por perto…"), far = listOf("Uma energia estranha pulsou em algum lugar do mundo…")),
        "ultra_beast" to OTHERWORLDLY,
        "paradox" to OTHERWORLDLY,
        "shiny" to SensePhrases(near = listOf("Algo brilhou de relance por perto…", "Você teve a impressão de ver um brilho diferente…")),
        "alpha" to SensePhrases(near = listOf("Você sente que algo grande está por perto…", "O chão treme com passos pesados por perto…")),
    )

    data class Values(
        val enabled: Boolean = true,

        // What is announced.
        val announceShiny: Boolean = true,
        val announceAlpha: Boolean = true,
        val announceLegendary: Boolean = true,
        val announceMythical: Boolean = true,
        val announceUltraBeast: Boolean = true,
        val announceParadox: Boolean = true,
        /** Species always announced, as ids (`pikachu` or `cobblemon:pikachu`). */
        val extraSpecies: List<String> = emptyList(),
        /** Species never announced, even shiny or Alpha. */
        val ignoredSpecies: List<String> = emptyList(),
        /** Every Pokémon spawn: floods the chat, Cobblemon spawns dozens a minute. */
        val announceAll: Boolean = false,
        /**
         * Wild Mega Pokémon (a data pack's spawns in a Mega form), even when they are Alphas or shiny.
         * Off: they stay a surprise for whoever finds them.
         */
        val announceMega: Boolean = false,

        // How much of the place is told: each part goes into {location}.
        val showBiome: Boolean = true,
        val showCoordinates: Boolean = false,
        val showNearestPlayer: Boolean = false,

        // Who hears it, and how.
        /** Only players in the dimension of the spawn. */
        val sameDimensionOnly: Boolean = false,
        /** Only players within this many blocks of the spawn (same dimension); 0 for no limit. */
        val radius: Int = 0,
        /** On the action bar above the hotbar instead of the chat. */
        val actionBar: Boolean = false,
        val playSound: Boolean = true,

        // Texts, with & colour codes.
        val message: String = "&6[Spawn] &fUm &e{pokemon}{tags}&f apareceu{location}&f!",
        val biomeFormat: String = " em &a{biome}",
        val coordinatesFormat: String = " &7(x {x}, y {y}, z {z})",
        val nearestPlayerFormat: String = " &fperto de &b{player}",
        val shinyTag: String = " ✦ Shiny",
        val alphaTag: String = " Alfa",
        val legendaryTag: String = " Lendário",
        val mythicalTag: String = " Mítico",
        val ultraBeastTag: String = " Ultracriatura",
        val paradoxTag: String = " Paradoxo",

        // The players' sense, instead of the message above.
        /** [CLASSIC]: the message naming the Pokémon; [SENSE]: a phrase that names nothing (the server log keeps the full line). */
        val mode: String = CLASSIC,
        /** In the sense mode, how near (same dimension) a player has to be to get a category's near phrase. */
        val senseRadius: Int = 64,
        /** In the sense mode, the near phrase also in the middle of the screen (a title only that player sees), for [nearScreenSeconds]. */
        val nearScreen: Boolean = true,
        val nearScreenSeconds: Int = 6,
        /** In the sense mode, the near phrase also shakes that player's screen (a tremor: the hurt tilt [shakeTimes] times, no damage). */
        val shake: Boolean = true,
        val shakeTimes: Int = 3,
        /** In the sense mode, the categories every player hears of: the near phrase within [senseRadius], the far one beyond. The others reach only the near players. */
        val senseEveryone: List<String> = listOf("legendary", "mythical", "mega", "ultra_beast", "paradox"),
        /** The style of every phrase, with & codes. */
        val senseStyle: String = "&7&o",
        /** Each category's phrases, one picked at random per spawn for the near players and one for the far ones. */
        val sensePhrases: Map<String, SensePhrases> = DEFAULT_SENSE_PHRASES,
    ) {
        /** Whether the sense mode is on. */
        val sense: Boolean get() = mode == SENSE

        /** [extraSpecies] as resource ids. */
        val extraSpeciesIds: Set<ResourceLocation> = speciesIds(extraSpecies)

        /** [ignoredSpecies] as resource ids. */
        val ignoredSpeciesIds: Set<ResourceLocation> = speciesIds(ignoredSpecies)
    }

    /** Current values. Replaced on load; tests may set it directly. */
    @Volatile
    @JvmStatic
    var values: Values = Values()

    private val COMMENTS = linkedMapOf(
        "enabled" to "Announce rare Pokémon spawns. /spawnalerts reload reads this file again; /spawnalerts test shows the message to you.",
        "announceShiny" to "What is announced: shiny, Alpha (they lead herds), legendary, mythical, Ultra Beast and paradox Pokémon.",
        "extraSpecies" to "Species always announced, e.g. [\"dratini\", \"cobblemon:riolu\"].",
        "ignoredSpecies" to "Species never announced, not even shiny or Alpha.",
        "announceAll" to "Announce every spawn. Floods the chat: Cobblemon spawns dozens of Pokémon a minute.",
        "announceMega" to "Announce wild Mega Pokémon (a data pack's spawns in a Mega form, with Mega Showdown), even Alphas or shiny ones. false keeps them a surprise for whoever finds them.",
        "showBiome" to "What {location} tells: the biome (biomeFormat), the exact coordinates (coordinatesFormat) and the name of the nearest player (nearestPlayerFormat).",
        "sameDimensionOnly" to "Only players in the dimension of the spawn get the message.",
        "radius" to "Only players within this many blocks of the spawn get the message (0: everyone).",
        "actionBar" to "Show the message on the action bar, above the hotbar, instead of the chat.",
        "playSound" to "Play a bell with the message.",
        "message" to "The message. {pokemon} and {biome} show in each player's language; {tags} is the tags below; {location} is the parts turned on above. Colours with & codes (&6 gold, &e yellow, &a green, &f white, &7 grey...).",
        "coordinatesFormat" to "{x}, {y} and {z} are the block where the Pokémon spawned.",
        "shinyTag" to "Tag added after the name of a shiny Pokémon (the same for the other tags).",
        "mode" to "\"classic\": the message above, naming the Pokémon. \"sense\": the players' sense instead, a phrase picked from sensePhrases that never names the Pokémon, the biome, the coordinates or what makes it rare (shiny, Alpha, legendary...); the server log keeps the full message, with the coordinates. What is announced still follows the options above (extraSpecies and announceAll only count in the classic mode).",
        "senseRadius" to "Sense mode: players within this many blocks of the spawn (same dimension) get the category's near phrase.",
        "nearScreen" to "Sense mode: the near phrase also shows in the middle of the screen (a title only that player sees, under a mark in the category's colour), with the sound, besides the chat.",
        "nearScreenSeconds" to "Sense mode: how long the near phrase stays in the middle of the screen.",
        "shake" to "Sense mode: the near phrase also shakes the player's screen, a tremor (the camera's hurt tilt, no damage; a player with Damage Tilt at 0 does not see it).",
        "shakeTimes" to "Sense mode: how many shakes the tremor has, a third of a second apart.",
        "senseEveryone" to "Sense mode: the categories every player hears of, the near phrase within senseRadius and the far one beyond (legendary, mythical, mega, ultra_beast, paradox, shiny, alpha). The others reach only the players within senseRadius.",
        "senseStyle" to "Sense mode: the & codes every phrase starts with (&7&o: grey italic).",
        "sensePhrases" to "Sense mode: per category, near and far phrases, one picked at random for each spawn. A Pokémon in several categories uses the first of: legendary, mythical, mega, ultra_beast, paradox, shiny, alpha.",
    )

    fun load(path: Path = FabricLoader.getInstance().configDir.resolve("${SpawnAlertsMod.MOD_ID}.json")) {
        val json = try {
            if (Files.isRegularFile(path)) JsonParser.parseString(Files.readString(path)).asJsonObject else JsonObject()
        } catch (e: Exception) {
            SpawnAlertsMod.LOGGER.error("Could not read {}, using the defaults: {}", path, e.toString())
            JsonObject()
        }
        val d = Values()
        values = Values(
            enabled = json.bool("enabled", d.enabled),
            announceShiny = json.bool("announceShiny", d.announceShiny),
            announceAlpha = json.bool("announceAlpha", d.announceAlpha),
            announceLegendary = json.bool("announceLegendary", d.announceLegendary),
            announceMythical = json.bool("announceMythical", d.announceMythical),
            announceUltraBeast = json.bool("announceUltraBeast", d.announceUltraBeast),
            announceParadox = json.bool("announceParadox", d.announceParadox),
            extraSpecies = json.strings("extraSpecies", d.extraSpecies),
            ignoredSpecies = json.strings("ignoredSpecies", d.ignoredSpecies),
            announceAll = json.bool("announceAll", d.announceAll),
            announceMega = json.bool("announceMega", d.announceMega),
            showBiome = json.bool("showBiome", d.showBiome),
            showCoordinates = json.bool("showCoordinates", d.showCoordinates),
            showNearestPlayer = json.bool("showNearestPlayer", d.showNearestPlayer),
            sameDimensionOnly = json.bool("sameDimensionOnly", d.sameDimensionOnly),
            radius = json.int("radius", d.radius).coerceIn(0, 1_000_000),
            actionBar = json.bool("actionBar", d.actionBar),
            playSound = json.bool("playSound", d.playSound),
            message = json.string("message", d.message),
            biomeFormat = json.string("biomeFormat", d.biomeFormat),
            coordinatesFormat = json.string("coordinatesFormat", d.coordinatesFormat),
            nearestPlayerFormat = json.string("nearestPlayerFormat", d.nearestPlayerFormat),
            shinyTag = json.string("shinyTag", d.shinyTag),
            alphaTag = json.string("alphaTag", d.alphaTag),
            legendaryTag = json.string("legendaryTag", d.legendaryTag),
            mythicalTag = json.string("mythicalTag", d.mythicalTag),
            ultraBeastTag = json.string("ultraBeastTag", d.ultraBeastTag),
            paradoxTag = json.string("paradoxTag", d.paradoxTag),
            mode = json.string("mode", d.mode).lowercase().takeIf { it == CLASSIC || it == SENSE } ?: d.mode,
            senseRadius = json.int("senseRadius", d.senseRadius).coerceIn(1, 1_000_000),
            nearScreen = json.bool("nearScreen", d.nearScreen),
            nearScreenSeconds = json.int("nearScreenSeconds", d.nearScreenSeconds).coerceIn(1, 60),
            shake = json.bool("shake", d.shake),
            shakeTimes = json.int("shakeTimes", d.shakeTimes).coerceIn(1, 10),
            senseEveryone = json.strings("senseEveryone", d.senseEveryone).map { it.lowercase() }.filter { it in SENSE_CATEGORIES }.distinct(),
            senseStyle = json.string("senseStyle", d.senseStyle),
            sensePhrases = json.phrases("sensePhrases", d.sensePhrases),
        )
        save(path)
    }

    private fun save(path: Path) {
        val v = values
        val out = JsonObject()
        out.add("_comments", JsonObject().apply { COMMENTS.forEach { (key, text) -> addProperty(key, text) } })
        out.addProperty("enabled", v.enabled)
        out.addProperty("announceShiny", v.announceShiny)
        out.addProperty("announceAlpha", v.announceAlpha)
        out.addProperty("announceLegendary", v.announceLegendary)
        out.addProperty("announceMythical", v.announceMythical)
        out.addProperty("announceUltraBeast", v.announceUltraBeast)
        out.addProperty("announceParadox", v.announceParadox)
        out.add("extraSpecies", JsonArray().apply { v.extraSpecies.forEach { add(it) } })
        out.add("ignoredSpecies", JsonArray().apply { v.ignoredSpecies.forEach { add(it) } })
        out.addProperty("announceAll", v.announceAll)
        out.addProperty("announceMega", v.announceMega)
        out.addProperty("showBiome", v.showBiome)
        out.addProperty("showCoordinates", v.showCoordinates)
        out.addProperty("showNearestPlayer", v.showNearestPlayer)
        out.addProperty("sameDimensionOnly", v.sameDimensionOnly)
        out.addProperty("radius", v.radius)
        out.addProperty("actionBar", v.actionBar)
        out.addProperty("playSound", v.playSound)
        out.addProperty("message", v.message)
        out.addProperty("biomeFormat", v.biomeFormat)
        out.addProperty("coordinatesFormat", v.coordinatesFormat)
        out.addProperty("nearestPlayerFormat", v.nearestPlayerFormat)
        out.addProperty("shinyTag", v.shinyTag)
        out.addProperty("alphaTag", v.alphaTag)
        out.addProperty("legendaryTag", v.legendaryTag)
        out.addProperty("mythicalTag", v.mythicalTag)
        out.addProperty("ultraBeastTag", v.ultraBeastTag)
        out.addProperty("paradoxTag", v.paradoxTag)
        out.addProperty("mode", v.mode)
        out.addProperty("senseRadius", v.senseRadius)
        out.addProperty("nearScreen", v.nearScreen)
        out.addProperty("nearScreenSeconds", v.nearScreenSeconds)
        out.addProperty("shake", v.shake)
        out.addProperty("shakeTimes", v.shakeTimes)
        out.add("senseEveryone", JsonArray().apply { v.senseEveryone.forEach { add(it) } })
        out.addProperty("senseStyle", v.senseStyle)
        out.add("sensePhrases", JsonObject().apply {
            for (category in SENSE_CATEGORIES) {
                val phrases = v.sensePhrases[category] ?: continue
                add(category, JsonObject().apply {
                    add("near", JsonArray().apply { phrases.near.forEach { add(it) } })
                    add("far", JsonArray().apply { phrases.far.forEach { add(it) } })
                })
            }
        })
        try {
            Files.createDirectories(path.parent)
            // No HTML escaping: the & colour codes stay readable in the file.
            Files.writeString(path, GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(out) + "\n")
        } catch (e: Exception) {
            SpawnAlertsMod.LOGGER.error("Could not write {}: {}", path, e.toString())
        }
    }

    /** Species ids, `cobblemon:` when no namespace is given; invalid ids are skipped. */
    private fun speciesIds(ids: List<String>): Set<ResourceLocation> = ids.mapNotNull { id ->
        ResourceLocation.tryParse(if (':' in id) id.lowercase() else "cobblemon:${id.lowercase()}")
    }.toSet()

    private fun JsonObject.bool(key: String, default: Boolean): Boolean =
        runCatching { get(key)?.asBoolean }.getOrNull() ?: default

    private fun JsonObject.int(key: String, default: Int): Int =
        runCatching { get(key)?.asInt }.getOrNull() ?: default

    private fun JsonObject.string(key: String, default: String): String =
        runCatching { get(key)?.takeIf { it.isJsonPrimitive }?.asString }.getOrNull() ?: default

    private fun JsonObject.strings(key: String, default: List<String>): List<String> =
        runCatching { get(key)?.asJsonArray?.map { it.asString } }.getOrNull() ?: default

    /** Every category's phrases: the file's lists where it has them, [default]'s for the rest. */
    private fun JsonObject.phrases(key: String, default: Map<String, SensePhrases>): Map<String, SensePhrases> {
        val categories = runCatching { get(key)?.asJsonObject }.getOrNull()
        return SENSE_CATEGORIES.associateWith { category ->
            val fallback = default[category] ?: SensePhrases(emptyList())
            val entry = runCatching { categories?.get(category)?.asJsonObject }.getOrNull() ?: return@associateWith fallback
            SensePhrases(entry.strings("near", fallback.near), entry.strings("far", fallback.far))
        }
    }
}
