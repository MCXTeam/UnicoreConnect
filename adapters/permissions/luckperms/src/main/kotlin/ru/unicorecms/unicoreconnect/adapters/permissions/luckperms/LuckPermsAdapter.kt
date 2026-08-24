package ru.unicorecms.unicoreconnect.adapters.permissions.luckperms

import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import net.luckperms.api.context.DefaultContextKeys
import net.luckperms.api.model.user.User
import net.luckperms.api.node.NodeType
import net.luckperms.api.node.types.InheritanceNode
import net.luckperms.api.node.types.PermissionNode
import ru.unicorecms.unicoreconnect.common.platform.PermissionAdapter
import java.util.UUID

class LuckPermsAdapter : PermissionAdapter {
    override val id = "luckperms"

    private val provider: LuckPerms? by lazy {
        try {
            LuckPermsProvider.get()
        } catch (error: Throwable) {
            null
        }
    }

    private val global get() = provider?.serverName == "global"

    override fun available() = provider != null

    override fun addGroup(uuid: UUID, group: String, expiresAt: Long?) = edit(uuid) { api, user ->
        val node = InheritanceNode.builder(group)

        if (!global) node.withContext(DefaultContextKeys.SERVER_KEY, api.serverName)
        if (expiresAt != null) node.expiry(expiresAt / 1000)

        user.data().add(node.build())
    }

    override fun removeGroup(uuid: UUID, group: String) = edit(uuid) { api, user ->
        user.getNodes(NodeType.INHERITANCE)
            .filter { it.groupName == group && (global || it.contexts.contains(DefaultContextKeys.SERVER_KEY, api.serverName)) }
            .forEach { user.data().remove(it) }
    }

    override fun addPermission(uuid: UUID, permission: String, expiresAt: Long?) = edit(uuid) { api, user ->
        val node = PermissionNode.builder(permission)

        if (!global) node.withContext(DefaultContextKeys.SERVER_KEY, api.serverName)
        if (expiresAt != null) node.expiry(expiresAt / 1000)

        user.data().add(node.build())
    }

    override fun removePermission(uuid: UUID, permission: String) = edit(uuid) { api, user ->
        user.getNodes(NodeType.PERMISSION)
            .filter { it.permission == permission && (global || it.contexts.contains(DefaultContextKeys.SERVER_KEY, api.serverName)) }
            .forEach { user.data().remove(it) }
    }

    override fun groupsOf(uuid: UUID): Set<String> {
        val api = provider ?: return emptySet()
        val user = api.userManager.loadUser(uuid).join() ?: return emptySet()

        return user.getInheritedGroups(user.queryOptions)
            .filter { global || it.queryOptions.context().contains(DefaultContextKeys.SERVER_KEY, api.serverName) }
            .map { it.name }
            .toSet()
    }

    override fun has(uuid: UUID, permission: String): Boolean? {
        val api = provider ?: return null
        val user = api.userManager.getUser(uuid) ?: api.userManager.loadUser(uuid).join() ?: return null

        return user.cachedData.permissionData.checkPermission(permission).asBoolean()
    }

    override fun permissionsOf(uuid: UUID): Set<String> {
        val api = provider ?: return emptySet()
        val user = api.userManager.loadUser(uuid).join() ?: return emptySet()

        return user.getNodes(NodeType.PERMISSION)
            .filter { global || it.contexts.contains(DefaultContextKeys.SERVER_KEY, api.serverName) }
            .map { it.permission }
            .toSet()
    }

    private fun edit(uuid: UUID, block: (LuckPerms, User) -> Unit) {
        val api = provider ?: return
        val user = api.userManager.loadUser(uuid).join() ?: return

        block(api, user)
        api.userManager.saveUser(user)
    }
}
