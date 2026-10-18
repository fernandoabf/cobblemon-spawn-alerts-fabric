package com.fernandoabf.spawnalerts.gametest

import net.minecraft.gametest.framework.GameTestHelper

/** Collects every failed check of a test, so one run reports all of them instead of the first. */
class Checks(private val helper: GameTestHelper) {
    private val errors = mutableListOf<String>()

    fun check(condition: Boolean, message: () -> String) {
        if (!condition) errors += message()
    }

    /** Fails the test with every collected error, or marks it as passed. */
    fun finish() {
        if (errors.isEmpty()) {
            helper.succeed()
        } else {
            helper.fail("${errors.size} check(s) failed:\n - " + errors.joinToString("\n - "))
        }
    }
}
