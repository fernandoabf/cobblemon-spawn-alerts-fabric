package com.fernandoabf.spawnalerts

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import java.util.UUID

/**
 * A phrase in the middle of one player's screen for a few seconds (a title: a mark in the category's colour, the phrase under it),
 * and a tremor: the camera's hurt tilt a few times, with no damage. The sense mode's near phrase goes there, so a player close to a
 * rare spawn feels it without reading the chat (1.4.0, the owner of Farbound: "tem que mostrar na tela... tipo sente um tremor... aí
 * dá um shake na tela"; until 1.3.0 it was a boss bar at the top). The server alone does it, the clients need nothing.
 */
object ScreenNotices {
    /** The ticks between two shakes of a tremor. */
    const val SHAKE_EVERY = 6

    private class Notice(val text: Component, val until: Long)

    private class Shake(val player: UUID, var left: Int, var next: Long)

    private val active = HashMap<UUID, Notice>()
    private val shakes = HashMap<UUID, Shake>()
    private var server: MinecraftServer? = null

    fun init() {
        ServerTickEvents.END_SERVER_TICK.register { server ->
            this.server = server
            tick(server.tickCount.toLong())
        }
    }

    /** Shows [text] in the middle of [player]'s screen, under a [mark] in [colour], for [ticks]; the one they had goes. */
    fun show(player: ServerPlayer, text: Component, colour: ChatFormatting, ticks: Int, mark: String = "✦") {
        val stay = ticks.coerceAtLeast(1)
        player.connection.send(ClientboundSetTitlesAnimationPacket(10, stay, 20))
        player.connection.send(ClientboundSetSubtitleTextPacket(text))
        player.connection.send(ClientboundSetTitleTextPacket(Component.literal(mark).withStyle(colour, ChatFormatting.BOLD)))
        active[player.uuid] = Notice(text, player.server.tickCount.toLong() + stay)
    }

    /** Shakes [player]'s screen [times] times, [SHAKE_EVERY] ticks apart: the hurt tilt, with no damage and no sound. */
    fun shake(player: ServerPlayer, times: Int) {
        if (times <= 0) return
        player.connection.send(ClientboundHurtAnimationPacket(player.id, 0f))
        if (times > 1) shakes[player.uuid] = Shake(player.uuid, times - 1, player.server.tickCount.toLong() + SHAKE_EVERY)
    }

    /** The phrase in the middle of [player]'s screen now, or null. */
    fun shown(player: ServerPlayer): Component? = active[player.uuid]?.text

    /** How many shakes of [player]'s tremor are still to come. */
    fun shaking(player: ServerPlayer): Int = shakes[player.uuid]?.left ?: 0

    /** Takes the phrases whose time is up away and gives the shakes that are due. Tests call it with a tick of theirs. */
    fun tick(now: Long) {
        active.entries.removeIf { now >= it.value.until }
        for (shake in shakes.values.filter { now >= it.next }) {
            val player = server?.playerList?.getPlayer(shake.player)
            if (player == null) {
                shakes.remove(shake.player)
                continue
            }
            player.connection.send(ClientboundHurtAnimationPacket(player.id, 0f))
            shake.left--
            if (shake.left <= 0) shakes.remove(shake.player) else shake.next = now + SHAKE_EVERY
        }
    }
}
