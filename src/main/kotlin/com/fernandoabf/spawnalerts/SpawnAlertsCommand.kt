package com.fernandoabf.spawnalerts

import com.cobblemon.mod.common.api.pokemon.PokemonProperties
import com.fernandoabf.spawnalerts.config.SpawnAlertsConfig
import com.mojang.brigadier.CommandDispatcher
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

/**
 * `/spawnalerts reload` reads `config/spawnalerts.json` again; `/spawnalerts test` shows the message
 * of a shiny Pikachu spawning where you stand, only to you, and in the sense mode what the players would
 * get instead: the near phrase of the shiny Pikachu and of a legendary, and the legendary's far phrase.
 * Both need operator permission (level 2).
 */
object SpawnAlertsCommand {
    fun init() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ -> register(dispatcher) }
    }

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("spawnalerts")
                .requires { it.hasPermission(2) }
                .then(Commands.literal("reload").executes { context ->
                    SpawnAlertsConfig.load()
                    context.source.sendSuccess({ Component.literal("Spawn Alerts: config/spawnalerts.json recarregada.") }, true)
                    1
                })
                .then(Commands.literal("test").executes { context ->
                    val source = context.source
                    val level = source.level
                    val pos = BlockPos.containing(source.position)
                    val biome = level.getBiome(pos).unwrapKey().map { it.location() }
                        .orElse(ResourceLocation.withDefaultNamespace("plains"))
                    val sample = PokemonProperties.parse("pikachu").create().apply { shiny = true }
                    source.sendSystemMessage(SpawnAlerts.message(sample, level, pos, biome))
                    val config = SpawnAlertsConfig.values
                    if (config.sense) {
                        fun phrase(category: String, near: Boolean): Component {
                            val phrases = config.sensePhrases[category]
                            val text = (if (near) phrases?.near else phrases?.far)?.randomOrNull() ?: return Component.literal("  (nenhuma frase)")
                            return Component.literal("  ").append(MessageTemplate.format(config.senseStyle + text, emptyMap()))
                        }
                        source.sendSystemMessage(Component.literal("Modo sense: o que os jogadores recebem no lugar disso:"))
                        source.sendSystemMessage(phrase("shiny", near = true))
                        source.sendSystemMessage(phrase("legendary", near = true))
                        if ("legendary" in config.senseEveryone) source.sendSystemMessage(phrase("legendary", near = false))
                    }
                    1
                })
        )
    }
}
