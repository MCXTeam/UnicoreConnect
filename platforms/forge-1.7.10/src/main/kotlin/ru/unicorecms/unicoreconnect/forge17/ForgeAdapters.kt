package ru.unicorecms.unicoreconnect.forge17

import com.mojang.authlib.GameProfile
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.server.MinecraftServer
import net.minecraft.server.management.UserListBansEntry
import ru.unicorecms.unicoreconnect.adapters.permissions.forgeessentials.ForgeEssentialsAdapter
import ru.unicorecms.unicoreconnect.common.platform.BanAdapter
import java.util.Date
import java.util.UUID

object ForgePermissions {
    val forgeEssentials = ForgeEssentialsAdapter()

    fun check(handle: EntityPlayerMP, permission: String): Boolean {
        if (handle.canCommandSenderUseCommand(2, "")) return true

        return forgeEssentials.has(handle.uniqueID, permission) ?: false
    }
}

class VanillaBanAdapter(private val serverProvider: () -> MinecraftServer?) : BanAdapter {
    override val id = "vanilla"

    override fun available() = serverProvider() != null

    override fun banned(uuid: UUID): Boolean {
        val server = serverProvider() ?: return false
        val profile = profile(server, uuid) ?: return false

        return server.configurationManager.func_152608_h().func_152702_a(profile)
    }

    override fun ban(uuid: UUID, reason: String, actor: UUID?, expiresAt: Long?) {
        val server = serverProvider() ?: return
        val profile = profile(server, uuid) ?: return
        val source = actor?.let { profile(server, it)?.name } ?: SOURCE
        val entry = UserListBansEntry(profile, Date(), source, expiresAt?.let { Date(it) }, reason)

        server.configurationManager.func_152608_h().func_152687_a(entry)
        online(server, uuid)?.playerNetServerHandler?.kickPlayerFromServer(reason)
    }

    override fun unban(uuid: UUID) {
        val server = serverProvider() ?: return
        val profile = profile(server, uuid) ?: return

        server.configurationManager.func_152608_h().func_152684_c(profile)
    }

    private fun profile(server: MinecraftServer, uuid: UUID): GameProfile? {
        online(server, uuid)?.let { return it.gameProfile }

        return server.func_152358_ax().func_152652_a(uuid)
    }

    @Suppress("UNCHECKED_CAST")
    private fun online(server: MinecraftServer, uuid: UUID): EntityPlayerMP? =
        (server.configurationManager.playerEntityList as? List<EntityPlayerMP>)?.firstOrNull { it.uniqueID == uuid }

    companion object {
        private const val SOURCE = "UnicoreCMS"
    }
}
