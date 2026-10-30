package com.fernandoabf.spawnalerts.gametest

import com.cobblemon.mod.common.api.pokemon.PokemonProperties
import com.cobblemon.mod.common.pokemon.Pokemon
import com.fernandoabf.spawnalerts.MessageTemplate
import com.fernandoabf.spawnalerts.ScreenNotices
import com.fernandoabf.spawnalerts.SpawnAlerts
import com.fernandoabf.spawnalerts.SpawnAlertsMod
import com.fernandoabf.spawnalerts.config.SpawnAlertsConfig
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextColor
import net.minecraft.network.chat.contents.PlainTextContents
import net.minecraft.network.chat.contents.TranslatableContents
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.Vec3
import java.nio.file.Files

/**
 * Headless gametests with the real Cobblemon ("fabric-gametest" entrypoint), on real Pokémon made
 * from Cobblemon's species data. Must be a regular class with a public no-arg constructor.
 */
class SpawnAlertsGameTest : FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    fun rareSpawnsAreAnnouncedByDefault(helper: GameTestHelper) {
        val checks = Checks(helper)
        checks.check(!SpawnAlerts.shouldAnnounce(pokemon("pikachu"))) { "a plain Pikachu should not be announced" }
        checks.check(SpawnAlerts.shouldAnnounce(pokemon("pikachu").apply { shiny = true })) { "a shiny should be announced" }
        checks.check(SpawnAlerts.shouldAnnounce(pokemon("pikachu").apply { isAlpha = true })) { "an Alpha should be announced" }
        for (species in listOf("mewtwo", "mew", "nihilego", "greattusk")) {
            checks.check(SpawnAlerts.shouldAnnounce(pokemon(species))) { "$species (legendary/mythical/ultra beast/paradox) should be announced" }
        }
        checks.finish()
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    fun eachServerChoosesWhatIsAnnounced(helper: GameTestHelper) {
        val checks = Checks(helper)
        withConfig(SpawnAlertsConfig.Values(announceShiny = false)) {
            checks.check(!SpawnAlerts.shouldAnnounce(pokemon("pikachu").apply { shiny = true })) { "announceShiny false" }
        }
        withConfig(SpawnAlertsConfig.Values(announceLegendary = false)) {
            checks.check(!SpawnAlerts.shouldAnnounce(pokemon("mewtwo"))) { "announceLegendary false" }
            checks.check(SpawnAlerts.shouldAnnounce(pokemon("mew"))) { "mythical is its own option" }
        }
        withConfig(SpawnAlertsConfig.Values(announceMythical = false)) {
            checks.check(!SpawnAlerts.shouldAnnounce(pokemon("mew"))) { "announceMythical false" }
        }
        withConfig(SpawnAlertsConfig.Values(extraSpecies = listOf("Pikachu", "cobblemon:eevee", "not a valid id!"))) {
            checks.check(SpawnAlerts.shouldAnnounce(pokemon("pikachu"))) { "extraSpecies without a namespace" }
            checks.check(SpawnAlerts.shouldAnnounce(pokemon("eevee"))) { "extraSpecies with a namespace" }
            checks.check(!SpawnAlerts.shouldAnnounce(pokemon("rattata"))) { "a species not listed" }
        }
        withConfig(SpawnAlertsConfig.Values(ignoredSpecies = listOf("pikachu"), announceAll = true)) {
            checks.check(!SpawnAlerts.shouldAnnounce(pokemon("pikachu").apply { shiny = true; isAlpha = true })) { "ignoredSpecies wins over everything" }
            checks.check(SpawnAlerts.shouldAnnounce(pokemon("rattata"))) { "announceAll" }
        }
        withConfig(SpawnAlertsConfig.Values(enabled = false, announceAll = true)) {
            checks.check(!SpawnAlerts.shouldAnnounce(pokemon("mewtwo").apply { shiny = true })) { "enabled false" }
        }
        // Mega Showdown's Mega forms are aspects (mega, mega_x, mega_y, mega_z); it is not installed here.
        val mega = { pokemon("charizard").apply { isAlpha = true; shiny = true; forcedAspects = forcedAspects + "mega_x" } }
        checks.check(SpawnAlerts.isMega(mega()) && !SpawnAlerts.isMega(pokemon("charizard"))) { "a Mega form is told apart" }
        checks.check(!SpawnAlerts.shouldAnnounce(mega())) { "wild Megas are a surprise by default, even Alpha and shiny" }
        withConfig(SpawnAlertsConfig.Values(announceMega = true)) {
            checks.check(SpawnAlerts.shouldAnnounce(mega())) { "announceMega true announces them" }
        }
        checks.finish()
    }

    /** By default the message names the Pokémon and the biome as translations, with the tags, and no coordinates. */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    fun theDefaultMessageNamesThePokemonAndTheBiomeOnly(helper: GameTestHelper) {
        val checks = Checks(helper)
        val shiny = pokemon("pikachu").apply { shiny = true; isAlpha = true }
        val message = SpawnAlerts.message(shiny, helper.level, BlockPos(120, 64, -340), biome("birch_forest"))
        val parts = message.flatten()

        val name = parts.firstOrNull { it.translationKey() == "cobblemon.species.pikachu.name" }
        val biome = parts.firstOrNull { it.translationKey() == "biome.minecraft.birch_forest" }
        checks.check(name != null) { "no Pikachu name in ${message.string}" }
        checks.check(biome != null) { "no biome name in ${message.string}" }
        checks.check(name?.style?.color == TextColor.fromLegacyFormat(ChatFormatting.YELLOW)) { "the name should be yellow (&e), got ${name?.style}" }
        checks.check(biome?.style?.color == TextColor.fromLegacyFormat(ChatFormatting.GREEN)) { "the biome should be green (&a), got ${biome?.style}" }

        val text = message.plainText()
        checks.check(text == "[Spawn] Um  ✦ Shiny Alfa apareceu em !") { "unexpected text around the names: '$text'" }
        checks.check(text.none { it.isDigit() } && '&' !in text) { "no coordinates or colour codes should show: '$text'" }

        val mewtwo = SpawnAlerts.message(pokemon("mewtwo"), helper.level, BlockPos.ZERO, biome("plains"))
        checks.check("Lendário" in mewtwo.plainText()) { "a legendary should get its tag" }
        checks.finish()
    }

    /** The server chooses how much of the place is told: biome, coordinates, nearest player. */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    fun eachServerChoosesHowMuchOfThePlaceIsTold(helper: GameTestHelper) = withPlayers(helper, listOf(Vec3(2.5, 2.0, 2.5))) { (near) ->
        val checks = Checks(helper)
        val shiny = pokemon("pikachu").apply { shiny = true }
        val spawn = helper.absolutePos(BlockPos(4, 2, 4))

        withConfig(SpawnAlertsConfig.Values(showCoordinates = true)) {
            val text = SpawnAlerts.message(shiny, helper.level, spawn, biome("plains")).plainText()
            checks.check("(x ${spawn.x}, y ${spawn.y}, z ${spawn.z})" in text) { "coordinates missing: '$text'" }
        }
        withConfig(SpawnAlertsConfig.Values(showBiome = false, showCoordinates = true)) {
            val message = SpawnAlerts.message(shiny, helper.level, spawn, biome("plains"))
            checks.check(message.flatten().none { it.translationKey()?.startsWith("biome.") == true }) { "showBiome false should drop the biome: ${message.string}" }
        }
        withConfig(SpawnAlertsConfig.Values(showNearestPlayer = true)) {
            val message = SpawnAlerts.message(shiny, helper.level, spawn, biome("plains"))
            checks.check(message.string.contains(near.gameProfile.name)) { "the nearest player should be named: ${message.string}" }
            checks.check("perto de" in message.plainText()) { "nearestPlayerFormat missing: '${message.plainText()}'" }
        }
        checks.finish()
    }

    /** Who hears it: everyone by default, or only players within the radius. */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    fun eachServerChoosesWhoHearsIt(helper: GameTestHelper) =
        withPlayers(helper, listOf(Vec3(2.5, 2.0, 2.5), Vec3(2.5, 2.0, 2.5))) { (near, far) ->
            val checks = Checks(helper)
            far.setPos(far.position().add(500.0, 0.0, 0.0))
            val spawn = helper.absolutePos(BlockPos(4, 2, 4))
            val everyone = SpawnAlerts.recipients(helper.level, spawn)
            checks.check(near in everyone && far in everyone) { "by default everyone hears it" }
            withConfig(SpawnAlertsConfig.Values(radius = 64)) {
                val nearby = SpawnAlerts.recipients(helper.level, spawn)
                checks.check(near in nearby && far !in nearby) { "radius 64: near ${near in nearby}, far ${far in nearby}" }
            }
            withConfig(SpawnAlertsConfig.Values(sameDimensionOnly = true)) {
                checks.check(near in SpawnAlerts.recipients(helper.level, spawn)) { "a player in the same dimension hears it" }
            }
            checks.finish()
        }

    /**
     * The sense mode: a legendary and a Mega reach everyone, the near phrase within the radius and the far one
     * beyond; a shiny and an Alpha reach only the near players; a Pokémon in several categories uses the most
     * notable; no line names the Pokémon, the place or what makes it rare, while the log line names them all.
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    fun theSenseModeTellsWithoutNaming(helper: GameTestHelper) =
        withPlayers(helper, listOf(Vec3(2.5, 2.0, 2.5), Vec3(2.5, 2.0, 2.5))) { (near, far) ->
            val checks = Checks(helper)
            far.setPos(far.position().add(500.0, 0.0, 0.0))
            val spawn = helper.absolutePos(BlockPos(4, 2, 4))
            val phrases = SpawnAlertsConfig.DEFAULT_SENSE_PHRASES
            val words = listOf("Mewtwo", "Charizard", "Pikachu", "Rattata", "Lendário", "Mítico", "Shiny", "Alfa", "Mega", "Ultracriatura", "Paradoxo")
            withConfig(SpawnAlertsConfig.Values(mode = SpawnAlertsConfig.SENSE, announceMega = true)) {
                fun sensed(what: String, pokemon: Pokemon, category: String, everyone: Boolean) {
                    val lines = SpawnAlerts.sensed(pokemon, helper.level, spawn)
                    val nearText = lines[near]?.message?.plainText()
                    val farText = lines[far]?.message?.plainText()
                    checks.check(lines[near]?.near == true && nearText in phrases.getValue(category).near) { "$what: the near player should get a $category near phrase, got '$nearText'" }
                    if (everyone) {
                        checks.check(lines[far]?.near == false && farText in phrases.getValue(category).far) { "$what: the far player should get a $category far phrase, got '$farText'" }
                    } else {
                        checks.check(far !in lines) { "$what: the far player should get nothing, got '$farText'" }
                    }
                    for ((_, line) in lines) {
                        val text = line.message.string
                        checks.check(words.none { it in text } && text.none { it.isDigit() }) { "$what: a line names something: '$text'" }
                        checks.check(line.message.flatten().none { it.translationKey() != null }) { "$what: a line carries a name to translate: '$text'" }
                        checks.check(line.message.flatten().any { it.style.isItalic && it.style.color == TextColor.fromLegacyFormat(ChatFormatting.GRAY) }) { "$what: the line should be grey italic" }
                    }
                }
                sensed("a legendary", pokemon("mewtwo"), "legendary", everyone = true)
                sensed("a mythical", pokemon("mew"), "mythical", everyone = true)
                sensed("a Mega", pokemon("charizard").apply { forcedAspects = forcedAspects + "mega_x" }, "mega", everyone = true)
                sensed("an Ultra Beast", pokemon("nihilego"), "ultra_beast", everyone = true)
                sensed("a shiny", pokemon("pikachu").apply { shiny = true }, "shiny", everyone = false)
                sensed("an Alpha", pokemon("rattata").apply { isAlpha = true }, "alpha", everyone = false)
                sensed("a shiny Alpha legendary", pokemon("mewtwo").apply { shiny = true; isAlpha = true }, "legendary", everyone = true)
                sensed("a shiny Alpha", pokemon("rattata").apply { shiny = true; isAlpha = true }, "shiny", everyone = false)
                checks.check(SpawnAlerts.sensed(pokemon("rattata"), helper.level, spawn).isEmpty()) { "a plain Rattata should reach nobody" }

                val logged = SpawnAlerts.announce(pokemon("mewtwo"), helper.level, spawn, biome("plains"))
                checks.check(logged?.flatten()?.any { it.translationKey() == "cobblemon.species.mewtwo.name" } == true) { "the log line should name the Pokémon: ${logged?.string}" }
                checks.check(logged?.flatten()?.any { it.translationKey() == "biome.minecraft.plains" } == true) { "the log line should name the biome: ${logged?.string}" }
                checks.check("(x ${spawn.x}, y ${spawn.y}, z ${spawn.z})" in (logged?.plainText() ?: "")) { "the log line should give the block: ${logged?.plainText()}" }
            }
            // Each category's reach is the server's: a shiny can reach everyone, a legendary only the near players.
            val custom = SpawnAlertsConfig.Values(
                mode = SpawnAlertsConfig.SENSE,
                senseEveryone = listOf("shiny"),
                sensePhrases = SpawnAlertsConfig.DEFAULT_SENSE_PHRASES + ("shiny" to SpawnAlertsConfig.SensePhrases(near = listOf("perto"), far = listOf("longe"))),
            )
            withConfig(custom) {
                val shiny = SpawnAlerts.sensed(pokemon("pikachu").apply { shiny = true }, helper.level, spawn)
                checks.check(shiny[near]?.message?.plainText() == "perto" && shiny[far]?.message?.plainText() == "longe") { "a shiny for everyone: near '${shiny[near]?.message?.string}', far '${shiny[far]?.message?.string}'" }
                checks.check(far !in SpawnAlerts.sensed(pokemon("mewtwo"), helper.level, spawn)) { "a legendary only for the near players" }
            }
            withConfig(SpawnAlertsConfig.Values(mode = SpawnAlertsConfig.SENSE, senseRadius = 1000)) {
                checks.check(SpawnAlerts.sensed(pokemon("mewtwo"), helper.level, spawn)[far]?.near == true) { "within a radius of 1000 the far player is near" }
            }
            // The classic mode names the Pokémon, as before, and leaves the coordinates out.
            val classic = SpawnAlerts.announce(pokemon("mewtwo"), helper.level, spawn, biome("plains"))
            checks.check(classic?.flatten()?.any { it.translationKey() == "cobblemon.species.mewtwo.name" } == true && classic.plainText().none { it.isDigit() }) {
                "the classic message: ${classic?.string}"
            }
            checks.finish()
        }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    fun biomesWithoutATranslationFallBackToAReadableName(helper: GameTestHelper) {
        val checks = Checks(helper)
        val contents = SpawnAlerts.biomeName(ResourceLocation.fromNamespaceAndPath("terralith", "volcanic_peaks")).contents as? TranslatableContents
        checks.check(contents?.key == "biome.terralith.volcanic_peaks") { "key ${contents?.key}" }
        checks.check(contents?.fallback == "Volcanic Peaks") { "fallback ${contents?.fallback}" }
        checks.finish()
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    fun templatesUnderstandColourCodesAndPlaceholders(helper: GameTestHelper) {
        val checks = Checks(helper)
        val message = MessageTemplate.format("&cA &l{x}&r b {unknown} §9c", mapOf("x" to Component.literal("X")))
        val parts = message.flatten().filter { (it.contents as? PlainTextContents)?.text()?.isNotEmpty() == true }
        val texts = parts.map { (it.contents as PlainTextContents).text() }
        checks.check(texts == listOf("A ", "X", " b {unknown} ", "c")) { "parts $texts" }
        checks.check(parts.getOrNull(1)?.style?.let { it.color == TextColor.fromLegacyFormat(ChatFormatting.RED) && it.isBold } == true) { "the placeholder should be red and bold" }
        checks.check(parts.getOrNull(2)?.style?.let { it.color == null && !it.isBold } == true) { "&r should reset the style" }
        checks.check(parts.getOrNull(3)?.style?.color == TextColor.fromLegacyFormat(ChatFormatting.BLUE)) { "§ codes work too" }
        checks.finish()
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    fun onlyAnnouncedSpawnsSendAMessage(helper: GameTestHelper) {
        val checks = Checks(helper)
        val pos = helper.absolutePos(BlockPos(4, 2, 4))
        checks.check(SpawnAlerts.announce(pokemon("rattata"), helper.level, pos, biome("plains")) == null) { "a plain Rattata should send nothing" }
        checks.check(SpawnAlerts.announce(pokemon("rattata").apply { shiny = true }, helper.level, pos, biome("plains")) != null) { "a shiny Rattata should be announced" }
        checks.finish()
    }

    /** `/spawnalerts reload` reads the file again and `/spawnalerts test` runs, for operators. */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    fun theAdminCommandsWork(helper: GameTestHelper) {
        val checks = Checks(helper)
        val server = helper.level.server
        val file = FabricLoader.getInstance().configDir.resolve("${SpawnAlertsMod.MOD_ID}.json")
        val before = if (Files.isRegularFile(file)) Files.readString(file) else null
        val previous = SpawnAlertsConfig.values
        try {
            Files.writeString(file, """{ "showCoordinates": true, "radius": 32 }""")
            val source = server.createCommandSourceStack().withSuppressedOutput()
            checks.check(server.commands.dispatcher.execute("spawnalerts reload", source) == 1) { "reload failed" }
            checks.check(SpawnAlertsConfig.values.showCoordinates && SpawnAlertsConfig.values.radius == 32) { "reload did not read the file: ${SpawnAlertsConfig.values}" }
            checks.check(server.commands.dispatcher.execute("spawnalerts test", source) == 1) { "test failed" }
            SpawnAlertsConfig.values = SpawnAlertsConfig.Values(mode = SpawnAlertsConfig.SENSE)
            checks.check(server.commands.dispatcher.execute("spawnalerts test", source) == 1) { "test failed in the sense mode" }
        } finally {
            SpawnAlertsConfig.values = previous
            if (before != null) Files.writeString(file, before) else Files.deleteIfExists(file)
        }
        checks.finish()
    }

    /** The sense mode's keys: a known mode, a radius of at least 1, known categories, and each category's phrases or the defaults. */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    fun theSenseModeIsReadFromTheConfig(helper: GameTestHelper) {
        val checks = Checks(helper)
        val file = Files.createTempFile("spawnalerts", ".json")
        val previous = SpawnAlertsConfig.values
        try {
            Files.writeString(file, """{ "mode": "SENSE", "senseRadius": 0, "senseEveryone": ["shiny", "Legendary", "nonsense"], "sensePhrases": { "shiny": { "near": ["um brilho"] } } }""")
            SpawnAlertsConfig.load(file)
            val loaded = SpawnAlertsConfig.values
            checks.check(loaded.sense && loaded.mode == SpawnAlertsConfig.SENSE) { "mode ${loaded.mode}" }
            checks.check(loaded.senseRadius == 1) { "a radius below 1 becomes 1, not ${loaded.senseRadius}" }
            checks.check(loaded.senseEveryone == listOf("shiny", "legendary")) { "senseEveryone ${loaded.senseEveryone}" }
            checks.check(loaded.sensePhrases["shiny"] == SpawnAlertsConfig.SensePhrases(near = listOf("um brilho"), far = emptyList())) { "shiny ${loaded.sensePhrases["shiny"]}" }
            checks.check(loaded.sensePhrases["legendary"] == SpawnAlertsConfig.DEFAULT_SENSE_PHRASES["legendary"]) { "a category the file leaves out keeps its phrases" }
            val written = Files.readString(file)
            for (key in listOf("\"mode\"", "\"senseRadius\"", "\"senseEveryone\"", "\"senseStyle\"", "\"sensePhrases\"", "\"alpha\"", "&7&o", "Você sente")) {
                checks.check(key in written) { "the rewritten config misses $key" }
            }
            Files.writeString(file, """{ "mode": "whisper" }""")
            SpawnAlertsConfig.load(file)
            checks.check(SpawnAlertsConfig.values.mode == SpawnAlertsConfig.CLASSIC) { "an unknown mode falls back to classic, not ${SpawnAlertsConfig.values.mode}" }
        } finally {
            SpawnAlertsConfig.values = previous
            Files.deleteIfExists(file)
        }
        checks.finish()
    }

    /** Missing keys get their defaults, and the file is rewritten with every key and readable & codes. */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    fun configFillsDefaultsAndKeepsColourCodesReadable(helper: GameTestHelper) {
        val checks = Checks(helper)
        val file = Files.createTempFile("spawnalerts", ".json")
        val previous = SpawnAlertsConfig.values
        try {
            Files.writeString(file, """{ "announceAlpha": false, "extraSpecies": ["dratini"], "radius": -5, "message": "&b{pokemon} em {biome}" }""")
            SpawnAlertsConfig.load(file)
            val loaded = SpawnAlertsConfig.values
            checks.check(loaded == SpawnAlertsConfig.Values(announceAlpha = false, extraSpecies = listOf("dratini"), message = "&b{pokemon} em {biome}")) { "loaded $loaded" }
            checks.check(loaded.extraSpeciesIds == setOf(ResourceLocation.fromNamespaceAndPath("cobblemon", "dratini"))) { "ids ${loaded.extraSpeciesIds}" }
            val written = Files.readString(file)
            for (key in listOf("enabled", "announceMythical", "ignoredSpecies", "showCoordinates", "showNearestPlayer", "radius", "actionBar", "coordinatesFormat", "message", "_comments")) {
                checks.check("\"$key\"" in written) { "the rewritten config misses $key" }
            }
            checks.check("&b{pokemon}" in written && "✦ Shiny" in written) { "colour codes and symbols should stay readable:\n$written" }
        } finally {
            SpawnAlertsConfig.values = previous
            Files.deleteIfExists(file)
        }
        checks.finish()
    }

    /** The shipped metadata keeps the license and the author, and its icon exists. */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    fun metadataKeepsLicenseAuthorAndIcon(helper: GameTestHelper) {
        val checks = Checks(helper)
        val mod = FabricLoader.getInstance().getModContainer(SpawnAlertsMod.MOD_ID).orElseThrow()
        val meta = mod.metadata
        checks.check("All-Rights-Reserved" in meta.license) { "the license should be All-Rights-Reserved (${meta.license})" }
        checks.check(meta.authors.any { it.name == "fernandoabf" }) { "authors: ${meta.authors.map { it.name }}" }
        checks.check(meta.version.friendlyString.matches(Regex("""\d+\.\d+\.\d+\+1\.21\.1"""))) { "unexpected version ${meta.version.friendlyString}" }
        val icon = meta.getIconPath(128).flatMap { mod.findPath(it) }
        checks.check(icon.isPresent && Files.isRegularFile(icon.get())) { "icon ${meta.getIconPath(128)} not found" }
        checks.finish()
    }

    /** A plain [species]: never shiny (1 in 8192 otherwise) nor Alpha, so each test sets what it checks. */
    private fun pokemon(species: String): Pokemon = PokemonProperties.parse(species).create().apply {
        shiny = false
        isAlpha = false
    }

    private fun biome(path: String) = ResourceLocation.withDefaultNamespace(path)

    private fun Component.flatten(): List<Component> = listOf(this) + siblings.flatMap { it.flatten() }

    private fun Component.translationKey(): String? = (contents as? TranslatableContents)?.key

    /** The literal text of the message, without the translated names. */
    private fun Component.plainText(): String = flatten().mapNotNull { (it.contents as? PlainTextContents)?.text() }.joinToString("")

    /**
     * Runs [test] with mock players in the server's player list, standing at [positions] (relative to
     * the test), and logs them out afterwards. makeMockServerPlayerInLevel is deprecated, but it is the
     * only helper that joins the player list.
     */
    @Suppress("DEPRECATION")
    /** The near phrase stays in the middle of the near player's screen for its seconds, with a tremor; the far player gets neither. */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    fun theNearPhraseShowsAtTheTopOfTheScreen(helper: GameTestHelper) =
        withPlayers(helper, listOf(Vec3(2.5, 2.0, 2.5), Vec3(2.5, 2.0, 2.5))) { (near, far) ->
            val checks = Checks(helper)
            far.setPos(far.position().add(500.0, 0.0, 0.0))
            val spawn = helper.absolutePos(BlockPos(4, 2, 4))
            withConfig(SpawnAlertsConfig.Values(mode = SpawnAlertsConfig.SENSE, nearScreenSeconds = 2)) {
                SpawnAlerts.announce(pokemon("mewtwo"), helper.level, spawn, biome("plains"))
                val shown = ScreenNotices.shown(near)?.plainText()
                checks.check(shown in SpawnAlertsConfig.DEFAULT_SENSE_PHRASES.getValue("legendary").near) { "the near player should see a legendary near phrase at the top, got '$shown'" }
                checks.check(ScreenNotices.shown(far) == null) { "the far player should see nothing at the top" }
                checks.check(SpawnAlerts.colour("legendary") == net.minecraft.ChatFormatting.GOLD && SpawnAlerts.colour("shiny") == net.minecraft.ChatFormatting.YELLOW) { "the colours" }
                checks.check(ScreenNotices.shaking(near) == 2 && ScreenNotices.shaking(far) == 0) { "a tremor of 3 shakes for the near player (one now, 2 to come): ${ScreenNotices.shaking(near)}" }
                val now = helper.level.server.tickCount.toLong()
                for (t in now + 1..now + 20) ScreenNotices.tick(t)
                checks.check(ScreenNotices.shown(near) != null) { "one second in, the phrase is still there" }
                checks.check(ScreenNotices.shaking(near) == 0) { "the tremor's shakes are given in that second" }
                ScreenNotices.tick(now + 41)
                checks.check(ScreenNotices.shown(near) == null) { "after two seconds it goes" }
            }
            withConfig(SpawnAlertsConfig.Values(mode = SpawnAlertsConfig.SENSE, nearScreen = false)) {
                SpawnAlerts.announce(pokemon("mewtwo"), helper.level, spawn, biome("plains"))
                checks.check(ScreenNotices.shown(near) == null) { "with nearScreen off, only the chat" }
            }
            checks.finish()
        }

    private fun withPlayers(helper: GameTestHelper, positions: List<Vec3>, test: (List<ServerPlayer>) -> Unit) {
        val players = positions.map { pos -> helper.makeMockServerPlayerInLevel().also { it.setPos(helper.absoluteVec(pos)) } }
        try {
            test(players)
        } finally {
            players.forEach { helper.level.server.playerList.remove(it) }
        }
    }

    private inline fun withConfig(values: SpawnAlertsConfig.Values, action: () -> Unit) {
        val previous = SpawnAlertsConfig.values
        SpawnAlertsConfig.values = values
        try {
            action()
        } finally {
            SpawnAlertsConfig.values = previous
        }
    }
}
