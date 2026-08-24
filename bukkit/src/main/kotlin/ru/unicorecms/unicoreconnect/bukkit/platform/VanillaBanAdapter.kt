package ru.unicorecms.unicoreconnect.bukkit.platform

import org.bukkit.Bukkit
import ru.unicorecms.unicoreconnect.common.platform.BanAdapter
import java.util.UUID

class VanillaBanAdapter : BanAdapter {
    override val id = "vanilla"

    override fun available() = true

    @Suppress("DEPRECATION")
    override fun banned(uuid: UUID): Boolean {
        val name = Bukkit.getOfflinePlayer(uuid).name ?: return false

        return Bukkit.getBanList(org.bukkit.BanList.Type.NAME).isBanned(name)
    }

    @Suppress("DEPRECATION")
    override fun ban(uuid: UUID, reason: String, actor: UUID?, expiresAt: Long?) {
        val name = Bukkit.getOfflinePlayer(uuid).name ?: return
        val source = actor?.let { Bukkit.getOfflinePlayer(it).name } ?: "UnicoreCMS"

        Bukkit.getBanList(org.bukkit.BanList.Type.NAME)
            .addBan(name, reason, expiresAt?.let { java.util.Date(it) }, source)
    }

    @Suppress("DEPRECATION")
    override fun unban(uuid: UUID) {
        val name = Bukkit.getOfflinePlayer(uuid).name ?: return

        Bukkit.getBanList(org.bukkit.BanList.Type.NAME).pardon(name)
    }
}
