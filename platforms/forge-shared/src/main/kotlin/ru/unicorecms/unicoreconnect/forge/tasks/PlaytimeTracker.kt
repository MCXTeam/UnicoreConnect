package ru.unicorecms.unicoreconnect.forge.tasks

import ru.unicorecms.unicoreconnect.common.UnicoreCommon
import ru.unicorecms.unicoreconnect.common.platform.Cancellable
import ru.unicorecms.unicoreconnect.forge.ForgePlatformImpl
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PlaytimeTracker(private val platform: ForgePlatformImpl) {
    private val lastActivity = ConcurrentHashMap<UUID, Long>()
    private var task: Cancellable? = null

    fun start() {
        task = platform.forgeScheduler.repeatSync(TICKS_PER_MINUTE, TICKS_PER_MINUTE) { tick() }
    }

    fun stop() {
        task?.cancel()
        task = null
        lastActivity.clear()
    }

    fun onJoin(uuid: UUID) {
        lastActivity[uuid] = System.currentTimeMillis()
    }

    fun onQuit(uuid: UUID) {
        lastActivity.remove(uuid)
    }

    private fun tick() {
        val active = platform.onlinePlayers().filter { player -> !afk(player.uuid) }.map { it.uuid }

        if (active.isEmpty()) return

        platform.scheduler.async {
            try {
                UnicoreCommon.playtimeService.update(active)
            } catch (error: Throwable) {
                platform.logger.error("Время игроков на сайт не отправлено", error)
            }
        }
    }

    private fun afk(uuid: UUID): Boolean {
        val player = platform.player(uuid) ?: return true
        val moved = moved(uuid)

        if (moved) lastActivity[uuid] = System.currentTimeMillis()

        val last = lastActivity[uuid] ?: System.currentTimeMillis()

        return System.currentTimeMillis() - last > AFK_TIMEOUT_MS
    }

    private fun moved(uuid: UUID): Boolean {
        val handle = (platform.player(uuid) as? ru.unicorecms.unicoreconnect.forge.ForgePlayer)?.handle() ?: return false
        val block = handle.blockPosition()
        val position = Triple(block.x, block.y, block.z)
        val previous = positions.put(uuid, position)

        return previous != position
    }

    private val positions = ConcurrentHashMap<UUID, Triple<Int, Int, Int>>()

    companion object {
        private const val TICKS_PER_MINUTE = 20L * 60
        private const val AFK_TIMEOUT_MS = 5 * 60 * 1000L
    }
}
