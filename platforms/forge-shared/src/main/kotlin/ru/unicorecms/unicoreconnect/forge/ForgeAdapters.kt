package ru.unicorecms.unicoreconnect.forge

import com.mojang.authlib.GameProfile
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.players.UserBanListEntry
import ru.unicorecms.unicoreconnect.common.platform.BanAdapter
import java.util.Date
import java.util.UUID

object ForgePermissions {
    private val luckPerms: Any? by lazy {
        try {
            Class.forName("net.luckperms.api.LuckPermsProvider")
                .getMethod("get")
                .invoke(null)
        } catch (error: Throwable) {
            null
        }
    }

    fun available() = luckPerms != null

    fun check(player: ServerPlayer, permission: String): Boolean {
        if (player.hasPermissions(2)) return true

        val api = luckPerms ?: return false

        return try {
            val userManager = api.javaClass.getMethod("getUserManager").invoke(api)
            val user = userManager.javaClass.getMethod("getUser", UUID::class.java).invoke(userManager, player.uuid) ?: return false
            val cached = user.javaClass.getMethod("getCachedData").invoke(user)
            val permissionData = cached.javaClass.getMethod("getPermissionData").invoke(cached)
            val result = permissionData.javaClass.getMethod("checkPermission", String::class.java).invoke(permissionData, permission)

            result.javaClass.getMethod("asBoolean").invoke(result) as Boolean
        } catch (error: Throwable) {
            false
        }
    }
}

class VanillaBanAdapter(private val serverProvider: () -> MinecraftServer?) : BanAdapter {
    override val id = "vanilla"

    override fun available() = serverProvider() != null

    override fun banned(uuid: UUID): Boolean {
        val server = serverProvider() ?: return false
        val profile = profile(server, uuid) ?: return false

        return server.playerList.bans.isBanned(profile)
    }

    override fun ban(uuid: UUID, reason: String, actor: UUID?, expiresAt: Long?) {
        val server = serverProvider() ?: return
        val profile = profile(server, uuid) ?: return
        val source = actor?.let { profile(server, it)?.name } ?: SOURCE
        val entry = UserBanListEntry(profile, Date(), source, expiresAt?.let { Date(it) }, reason)

        server.playerList.bans.add(entry)
        server.playerList.getPlayer(uuid)?.connection?.disconnect(net.minecraft.network.chat.Component.literal(reason))
    }

    override fun unban(uuid: UUID) {
        val server = serverProvider() ?: return
        val profile = profile(server, uuid) ?: return

        server.playerList.bans.remove(profile)
    }

    private fun profile(server: MinecraftServer, uuid: UUID): GameProfile? {
        server.playerList.getPlayer(uuid)?.let { return it.gameProfile }

        return server.profileCache?.get(uuid)?.orElse(null)
    }

    companion object {
        private const val SOURCE = "UnicoreCMS"
    }
}
