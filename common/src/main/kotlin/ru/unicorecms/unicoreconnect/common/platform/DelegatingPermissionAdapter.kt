package ru.unicorecms.unicoreconnect.common.platform

import java.util.UUID

class DelegatingPermissionAdapter(private val supplier: () -> PermissionAdapter?) : PermissionAdapter {
    override val id get() = supplier()?.id ?: "none"

    override fun available() = supplier()?.available() ?: false

    override fun addGroup(uuid: UUID, group: String, expiresAt: Long?) {
        supplier()?.addGroup(uuid, group, expiresAt)
    }

    override fun removeGroup(uuid: UUID, group: String) {
        supplier()?.removeGroup(uuid, group)
    }

    override fun addPermission(uuid: UUID, permission: String, expiresAt: Long?) {
        supplier()?.addPermission(uuid, permission, expiresAt)
    }

    override fun removePermission(uuid: UUID, permission: String) {
        supplier()?.removePermission(uuid, permission)
    }

    override fun groupsOf(uuid: UUID) = supplier()?.groupsOf(uuid) ?: emptySet()

    override fun permissionsOf(uuid: UUID) = supplier()?.permissionsOf(uuid) ?: emptySet()

    override fun has(uuid: UUID, permission: String) = supplier()?.has(uuid, permission)
}
