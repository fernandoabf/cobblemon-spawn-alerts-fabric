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

/**
 * Tells the players when a rare Pokémon spawns: which Pokémon and, as the server chooses, the biome,
 * the coordinates and the nearest player. The names go to the clients as translation keys
 * (Cobblemon's species, the game's biomes), so each player reads them in their own language and the
 * mod only has to be on the server.
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
        return config.announceAll ||
            species in config.extraSpeciesIds ||
            (config.announceShiny && pokemon.shiny) ||
            (config.announceAlpha && pokemon.isAlpha) ||
            (config.announceLegendary && pokemon.hasLabels(CobblemonPokemonLabels.LEGENDARY)) ||
            (config.announceMythical && pokemon.hasLabels(CobblemonPokemonLabels.MYTHICAL)) ||
            (config.announceUltraBeast && pokemon.hasLabels(CobblemonPokemonLabels.ULTRA_BEAST)) ||
            (config.announceParadox && pokemon.hasLabels(CobblemonPokemonLabels.PARADOX))
    }

    /**
     * Sends the message for a spawn of [pokemon] at [pos] in [biome] to the players the config picks
     * (the server log gets it too), if it is one the config announces. Returns the message, or null.
     */
    fun announce(pokemon: Pokemon, level: ServerLevel, pos: BlockPos, biome: ResourceLocation): Component? {
        if (!shouldAnnounce(pokemon)) return null
        val config = SpawnAlertsConfig.values
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

    /** The config's message with {pokemon}, {tags} and {location} (biome, coordinates, nearest player) filled in. */
    fun message(pokemon: Pokemon, level: ServerLevel, pos: BlockPos, biome: ResourceLocation): Component {
        val config = SpawnAlertsConfig.values
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
        val template = config.message.replace("{tags}", tags(pokemon)).replace("{location}", location)
        val values = mutableMapOf("pokemon" to pokemon.species.translatedName, "biome" to biomeName(biome))
        if (nearest != null) values["player"] = nearest.name
        return MessageTemplate.format(template, values)
    }

    /** The configured tags for what makes this Pokémon special (" ✦ Shiny", " Alfa", " Lendário"...). */
    fun tags(pokemon: Pokemon): String = buildString {
        val config = SpawnAlertsConfig.values
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
