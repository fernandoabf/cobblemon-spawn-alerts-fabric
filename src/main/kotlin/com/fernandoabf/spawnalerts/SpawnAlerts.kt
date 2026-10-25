package com.fernandoabf.spawnalerts

import com.cobblemon.mod.common.api.Priority
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.pokemon.labels.CobblemonPokemonLabels
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.pokemon.Pokemon
import com.fernandoabf.spawnalerts.config.SpawnAlertsConfig
import net.minecraft.Util
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.EntitySelector
import kotlin.random.Random

/**
 * Tells the players when a rare Pokémon spawns: which Pokémon and, as the server chooses, the biome,
 * the coordinates and the nearest player. The names go to the clients as translation keys
 * (Cobblemon's species, the game's biomes), so each player reads them in their own language and the
 * mod only has to be on the server. In the sense mode the players get a phrase that names nothing
 * instead ("Você sente uma presença poderosa por perto…"), the near one within the sense radius and,
 * for the categories that reach everyone, the far one beyond; the server log keeps the full line.
 */
object SpawnAlerts {
    fun init() {
        // Lowest priority on the raw spawn event: anything that cancels the spawn runs first.
        CobblemonEvents.ENTITY_SPAWN.subscribe(Priority.LOWEST) { event ->
            val entity = event.entity
            if (!event.isCanceled && entity is PokemonEntity) {
                val position = event.spawnablePosition
                announce(entity.pokemon, position.world, position.position, position.biomeName)
            }
        }
    }

    /** Whether a spawn of [pokemon] is announced: shiny, Alpha, legendary... as the config says. */
    fun shouldAnnounce(pokemon: Pokemon): Boolean {
        val config = SpawnAlertsConfig.values
        if (!config.enabled) return false
        val species = pokemon.species.resourceIdentifier
        if (species in config.ignoredSpeciesIds) return false
        if (!config.announceMega && isMega(pokemon)) return false
        return config.announceAll ||
            species in config.extraSpeciesIds ||
            (config.announceShiny && pokemon.shiny) ||
            (config.announceAlpha && pokemon.isAlpha) ||
            (config.announceLegendary && pokemon.hasLabels(CobblemonPokemonLabels.LEGENDARY)) ||
            (config.announceMythical && pokemon.hasLabels(CobblemonPokemonLabels.MYTHICAL)) ||
            (config.announceUltraBeast && pokemon.hasLabels(CobblemonPokemonLabels.ULTRA_BEAST)) ||
            (config.announceParadox && pokemon.hasLabels(CobblemonPokemonLabels.PARADOX))
    }

    /** In a Mega form: Mega Showdown's `mega_evolution` aspects (`mega`, `mega_x`, `mega_y`, `mega_z`). */
    fun isMega(pokemon: Pokemon): Boolean = pokemon.aspects.any { it == "mega" || it.startsWith("mega_") }

    /**
     * Sends the message for a spawn of [pokemon] at [pos] in [biome] to the players the config picks
     * (the server log gets it too), if it is one the config announces. Returns the message, or null.
     */
    fun announce(pokemon: Pokemon, level: ServerLevel, pos: BlockPos, biome: ResourceLocation): Component? {
        if (!shouldAnnounce(pokemon)) return null
        val config = SpawnAlertsConfig.values
        if (config.sense) {
            // The staff reads in the log what the players only sense: the Pokémon, the biome and the block.
            val full = message(pokemon, level, pos, biome, config.copy(showBiome = true, showCoordinates = true))
            level.server.sendSystemMessage(full)
            for ((player, line) in sensed(pokemon, level, pos)) {
                player.sendSystemMessage(line.message, config.actionBar)
                if (config.playSound && line.near) {
                    player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 0.8f)
                }
            }
            return full
        }
        val message = message(pokemon, level, pos, biome)
        level.server.sendSystemMessage(message)
        for (player in recipients(level, pos)) {
            player.sendSystemMessage(message, config.actionBar)
            if (config.playSound) {
                player.playNotifySound(SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.7f, 1.4f)
            }
        }
        return message
    }

    /** The players who hear about a spawn at [pos] in [level]: everyone, the same dimension, or a radius. */
    fun recipients(level: ServerLevel, pos: BlockPos): List<ServerPlayer> {
        val config = SpawnAlertsConfig.values
        val radiusSqr = config.radius.toDouble() * config.radius
        return level.server.playerList.players.filter { player ->
            when {
                config.radius > 0 -> player.level() == level && player.blockPosition().distSqr(pos) <= radiusSqr
                config.sameDimensionOnly -> player.level() == level
                else -> true
            }
        }
    }

    /** A sensed spawn as one player gets it: the phrase, and whether they were near enough for the near one. */
    data class SenseLine(val message: Component, val near: Boolean)

    /** What a spawn of [pokemon] is for the sense mode: the first category it is in that the config announces, or null. */
    fun senseCategory(pokemon: Pokemon): String? {
        val config = SpawnAlertsConfig.values
        return when {
            config.announceLegendary && pokemon.hasLabels(CobblemonPokemonLabels.LEGENDARY) -> "legendary"
            config.announceMythical && pokemon.hasLabels(CobblemonPokemonLabels.MYTHICAL) -> "mythical"
            config.announceMega && isMega(pokemon) -> "mega"
            config.announceUltraBeast && pokemon.hasLabels(CobblemonPokemonLabels.ULTRA_BEAST) -> "ultra_beast"
            config.announceParadox && pokemon.hasLabels(CobblemonPokemonLabels.PARADOX) -> "paradox"
            config.announceShiny && pokemon.shiny -> "shiny"
            config.announceAlpha && pokemon.isAlpha -> "alpha"
            else -> null
        }
    }

    /**
     * The sense mode's phrase for each player who gets one, for a spawn of [pokemon] at [pos]: one near phrase
     * picked for the players within the sense radius (same dimension) and, when the category reaches everyone,
     * one far phrase for the rest. Nothing for a Pokémon in no category, or a category with no phrases.
     */
    fun sensed(pokemon: Pokemon, level: ServerLevel, pos: BlockPos, random: Random = Random.Default): Map<ServerPlayer, SenseLine> {
        val config = SpawnAlertsConfig.values
        val category = senseCategory(pokemon) ?: return emptyMap()
        val phrases = config.sensePhrases[category] ?: return emptyMap()
        val near = phrases.near.randomOrNull(random)?.let { SenseLine(MessageTemplate.format(config.senseStyle + it, emptyMap()), true) }
        val far = phrases.far.randomOrNull(random)?.takeIf { category in config.senseEveryone }
            ?.let { SenseLine(MessageTemplate.format(config.senseStyle + it, emptyMap()), false) }
        val radiusSqr = config.senseRadius.toDouble() * config.senseRadius
        val out = LinkedHashMap<ServerPlayer, SenseLine>()
        for (player in level.server.playerList.players) {
            val close = player.level() == level && player.blockPosition().distSqr(pos) <= radiusSqr
            (if (close) near else far)?.let { out[player] = it }
        }
        return out
    }

    /** The config's message with {pokemon}, {tags} and {location} (biome, coordinates, nearest player) filled in. */
    fun message(pokemon: Pokemon, level: ServerLevel, pos: BlockPos, biome: ResourceLocation, config: SpawnAlertsConfig.Values = SpawnAlertsConfig.values): Component {
        val nearest = if (config.showNearestPlayer) {
            level.getNearestPlayer(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, -1.0, EntitySelector.NO_SPECTATORS)
        } else {
            null
        }
        val location = buildString {
            if (config.showBiome) append(config.biomeFormat)
            if (config.showCoordinates) {
                append(config.coordinatesFormat.replace("{x}", "${pos.x}").replace("{y}", "${pos.y}").replace("{z}", "${pos.z}"))
            }
            if (nearest != null) append(config.nearestPlayerFormat)
        }
        val template = config.message.replace("{tags}", tags(pokemon, config)).replace("{location}", location)
        val values = mutableMapOf("pokemon" to pokemon.species.translatedName, "biome" to biomeName(biome))
        if (nearest != null) values["player"] = nearest.name
        return MessageTemplate.format(template, values)
    }

    /** The configured tags for what makes this Pokémon special (" ✦ Shiny", " Alfa", " Lendário"...). */
    fun tags(pokemon: Pokemon, config: SpawnAlertsConfig.Values = SpawnAlertsConfig.values): String = buildString {
        if (pokemon.shiny) append(config.shinyTag)
        if (pokemon.isAlpha) append(config.alphaTag)
        when {
            pokemon.hasLabels(CobblemonPokemonLabels.LEGENDARY) -> append(config.legendaryTag)
            pokemon.hasLabels(CobblemonPokemonLabels.MYTHICAL) -> append(config.mythicalTag)
            pokemon.hasLabels(CobblemonPokemonLabels.ULTRA_BEAST) -> append(config.ultraBeastTag)
            pokemon.hasLabels(CobblemonPokemonLabels.PARADOX) -> append(config.paradoxTag)
        }
    }

    /**
     * The biome's name in the player's language. A biome whose name the client does not know (one
     * from a data pack, say) shows its id made readable: `terralith:volcanic_peaks` -> "Volcanic Peaks".
     */
    fun biomeName(biome: ResourceLocation): Component {
        val readable = biome.path.substringAfterLast('/').split('_').joinToString(" ") { word ->
            word.replaceFirstChar { it.titlecase() }
        }
        return Component.translatableWithFallback(Util.makeDescriptionId("biome", biome), readable)
    }
}
