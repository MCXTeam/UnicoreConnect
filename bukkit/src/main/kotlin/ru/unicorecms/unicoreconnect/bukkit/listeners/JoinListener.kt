package ru.unicorecms.unicoreconnect.bukkit.listeners

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import ru.unicorecms.unicoreconnect.common.modules.BansModule
import ru.unicorecms.unicoreconnect.common.modules.DonateModule

class JoinListener(private val donate: DonateModule?, private val bans: BansModule?) : Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    fun onJoin(event: PlayerJoinEvent) {
        val uuid = event.player.uniqueId

        donate?.handleJoin(uuid)
        bans?.handleJoin(uuid)
    }
}
