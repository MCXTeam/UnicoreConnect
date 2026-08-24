package ru.unicorecms.unicoreconnect.adapters.bans.banmanager

import me.confuser.banmanager.common.api.BmAPI
import org.bukkit.Bukkit
import ru.unicorecms.unicoreconnect.common.platform.BanAdapter
import java.util.UUID

class BanManagerAdapter : BanAdapter {
    override val id = "banmanager"

    override fun available() = Bukkit.getPluginManager().getPlugin("BanManager") != null

    override fun banned(uuid: UUID) = BmAPI.isBanned(uuid)

    override fun ban(uuid: UUID, reason: String, actor: UUID?, expiresAt: Long?) {
        val player = BmAPI.getPlayer(uuid) ?: return
        val issuer = actor?.let { BmAPI.getPlayer(it) } ?: BmAPI.getConsole()

        if (expiresAt == null) {
            BmAPI.ban(player, issuer, reason, true)
        } else {
            BmAPI.ban(player, issuer, reason, true, expiresAt / 1000)
        }
    }

    override fun unban(uuid: UUID) {
        val current = BmAPI.getCurrentBan(uuid) ?: return

        BmAPI.unban(current, BmAPI.getConsole())
    }
}
