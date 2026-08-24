package ru.unicorecms.unicoreconnect.forge112

import com.mojang.authlib.GameProfile
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.server.MinecraftServer
import net.minecraft.server.management.UserListBansEntry
import net.minecraft.util.text.TextComponentString
import net.minecraftforge.server.permission.DefaultPermissionLevel
import net.minecraftforge.server.permission.PermissionAPI
import ru.unicorecms.unicoreconnect.adapters.permissions.forgeessentials.ForgeEssentialsAdapter
import ru.unicorecms.unicoreconnect.common.Permissions
import ru.unicorecms.unicoreconnect.common.platform.BanAdapter
import java.util.Date
import java.util.UUID

object ForgePermissions {
    val forgeEssentials = ForgeEssentialsAdapter()

    fun register() {
        Permissions.everyone.forEach { node -> register(node, DefaultPermissionLevel.ALL) }
        Permissions.operators.forEach { node -> register(node, DefaultPermissionLevel.OP) }
    }

    private fun register(node: String, level: DefaultPermissionLevel) {
        runCatching { PermissionAPI.registerNode(node, level, node) }
    }

    fun check(handle: EntityPlayerMP, permission: String): Boolean {
        if (handle.canUseCommand(2, "")) return true

        return try {
            PermissionAPI.hasPermission(handle, permission)
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

        return server.playerList.bannedPlayers.isBanned(profile)
    }

    override fun ban(uuid: UUID, reason: String, actor: UUID?, expiresAt: Long?) {
        val server = serverProvider() ?: return
        val profile = profile(server, uuid) ?: return
        val source = actor?.let { profile(server, it)?.name } ?: SOURCE
        val entry = UserListBansEntry(profile, Date(), source, expiresAt?.let { Date(it) }, reason)

        server.playerList.bannedPlayers.addEntry(entry)
        server.playerList.getPlayerByUUID(uuid)?.connection?.disconnect(TextComponentString(reason))
    }

    override fun unban(uuid: UUID) {
        val server = serverProvider() ?: return
        val profile = profile(server, uuid) ?: return

        server.playerList.bannedPlayers.removeEntry(profile)
    }

    private fun profile(server: MinecraftServer, uuid: UUID): GameProfile? {
        server.playerList.getPlayerByUUID(uuid)?.let { return it.gameProfile }

        return server.playerProfileCache?.getProfileByUUID(uuid)
    }

    companion object {
        private const val SOURCE = "UnicoreCMS"
    }
}
