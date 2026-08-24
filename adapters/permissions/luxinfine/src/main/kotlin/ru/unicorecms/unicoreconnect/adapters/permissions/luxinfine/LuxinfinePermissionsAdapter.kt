package ru.unicorecms.unicoreconnect.adapters.permissions.luxinfine

import ml.luxinfine.helper.integrations.DefaultIntegrations
import ml.luxinfine.helper.integrations.Permissions
import ru.unicorecms.unicoreconnect.common.platform.PermissionAdapter
import java.time.Instant
import java.util.UUID

class LuxinfinePermissionsAdapter : PermissionAdapter {
    override val id = ID

    private val api: Permissions? by lazy {
        try {
            DefaultIntegrations.permissions()?.takeIf { !it.isDummy }
        } catch (error: Throwable) {
            null
        }
    }

    override fun available(): Boolean {
        val features = api?.supportedFeatures ?: return false

        return features.contains(Permissions.Features.GROUPS) || features.contains(Permissions.Features.PERM_PERMISSIONS)
    }

    override fun addGroup(uuid: UUID, group: String, expiresAt: Long?) {
        val api = api ?: return
        val player = player(api, uuid) ?: return

        createGroup(api, group)

        if (expiresAt != null && player.addGroup(group, Instant.ofEpochMilli(expiresAt)).isSuccess) return

        player.addGroup(group)
    }

    override fun removeGroup(uuid: UUID, group: String) {
        player(uuid)?.removeGroup(group)
    }

    override fun addPermission(uuid: UUID, permission: String, expiresAt: Long?) {
        val player = player(uuid) ?: return

        if (expiresAt != null && player.setTempPermissionValue(permission, Instant.ofEpochMilli(expiresAt), true).isSuccess) return

        player.setPermissionValue(permission, true)
    }

    override fun removePermission(uuid: UUID, permission: String) {
        player(uuid)?.removePermission(permission)
    }

    override fun groupsOf(uuid: UUID): Set<String> = api?.getPlayerGroups(uuid).orEmpty()

    override fun permissionsOf(uuid: UUID): Set<String> {
        val permissions = player(uuid)?.permissions?.getOrNull() ?: return emptySet()

        return permissions.filter { it.isAllowPermission }.map { it.name }.toSet()
    }

    override fun has(uuid: UUID, permission: String): Boolean? = api?.hasPermission(uuid, permission)

    private fun player(uuid: UUID): Permissions.PlayerData? {
        val api = api ?: return null

        return player(api, uuid)
    }

    private fun player(api: Permissions, uuid: UUID): Permissions.PlayerData? = api.getPlayerData(uuid).getOrNull()

    private fun createGroup(api: Permissions, group: String) {
        if (api.getGroup(group).isSuccess) return
        if (!api.supportedFeatures.contains(Permissions.Features.GROUP_CREATE)) return

        api.createGroup(group)
    }

    companion object {
        const val ID = "luxinfine"
    }
}
