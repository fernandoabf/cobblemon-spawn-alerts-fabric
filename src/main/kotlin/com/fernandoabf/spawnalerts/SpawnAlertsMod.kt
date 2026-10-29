package com.fernandoabf.spawnalerts

import com.fernandoabf.spawnalerts.config.SpawnAlertsConfig
import net.fabricmc.api.ModInitializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** Fabric entrypoint (fabric.mod.json -> "main", adapter "kotlin"); the work is in [SpawnAlerts]. */
object SpawnAlertsMod : ModInitializer {
    const val MOD_ID = "spawnalerts"

    @JvmField
    val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

    override fun onInitialize() {
        SpawnAlertsConfig.load()
        SpawnAlerts.init()
        ScreenNotices.init()
        SpawnAlertsCommand.init()
        LOGGER.info("Spawn Alerts initialized (enabled: {}, announce all: {}).", SpawnAlertsConfig.values.enabled, SpawnAlertsConfig.values.announceAll)
    }
}
