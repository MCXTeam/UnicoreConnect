package ru.unicorecms.unicoreconnect.bukkit.listeners

import me.confuser.banmanager.bukkit.api.events.PlayerBannedEvent
import me.confuser.banmanager.bukkit.api.events.PlayerUnbanEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import ru.unicorecms.unicoreconnect.common.modules.BansModule
import java.util.UUID

class BanManagerListener(private val bans: BansModule) : Listener {
    @EventHandler
    fun onBan(event: PlayerBannedEvent) {
        val ban = event.ban
        val actor = ban.actor?.uuid

        bans.onServerBan(ban.player.uuid, ban.reason, actor, ban.expires.takeIf { it > 0 }?.let { java.util.Date(it * 1000) })
    }

    @EventHandler
    fun onUnban(event: PlayerUnbanEvent) {
        bans.onServerUnban(event.ban.player.uuid as UUID)
    }
}
