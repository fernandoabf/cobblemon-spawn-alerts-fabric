package com.fernandoabf.spawnalerts

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerBossEvent
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.BossEvent
import java.util.UUID

/**
 * A phrase at the top of one player's screen for a few seconds: a boss bar only they see, emptying as the time runs out, taken away
 * when it is up (or when a new one comes). The sense mode's near phrase goes there too, so a player close to a rare spawn notices it
 * without reading the chat; the server alone does it, the clients need nothing.
 */
object ScreenNotices {
    private class Notice(val bar: ServerBossEvent, val player: UUID, val from: Long, val until: Long)

    private val active = HashMap<UUID, Notice>()

    fun init() {
        ServerTickEvents.END_SERVER_TICK.register { server -> tick(server.tickCount.toLong()) }
    }

    /** Shows [text] at the top of [player]'s screen, in [colour], for [ticks]; the one they had goes. */
    fun show(player: ServerPlayer, text: Component, colour: BossEvent.BossBarColor, ticks: Int) {
        active.remove(player.uuid)?.bar?.removeAllPlayers()
        val bar = ServerBossEvent(text, colour, BossEvent.BossBarOverlay.PROGRESS).apply { setProgress(1f) }
        bar.addPlayer(player)
        val now = player.server.tickCount.toLong()
        active[player.uuid] = Notice(bar, player.uuid, now, now + ticks.coerceAtLeast(1))
    }

    /** The phrase at the top of [player]'s screen now, or null. */
    fun shown(player: ServerPlayer): Component? = active[player.uuid]?.bar?.name

    /** Empties each bar with its time, every 10 ticks, and takes the ones whose time is up away. Tests call it with a tick of theirs. */
    fun tick(now: Long) {
        val done = active.values.filter { now >= it.until }
        for (notice in done) {
            notice.bar.removeAllPlayers()
            active.remove(notice.player)
        }
        if (now % 10 == 0L) {
            for (notice in active.values) notice.bar.progress = ((notice.until - now).toFloat() / (notice.until - notice.from)).coerceIn(0f, 1f)
        }
    }
}
