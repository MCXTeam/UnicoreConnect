package ru.unicorecms.unicoreconnect.common.platform

import ru.unicorecms.unicoreconnect.common.UnicoreCommon
import java.util.UUID

class CommandPermissionAdapter(private val platform: UnicorePlatform) : PermissionAdapter {
    override val id = "commands"

    private val templates get() = UnicoreCommon.config.permissions.commands

    override fun available() = true

    override fun addGroup(uuid: UUID, group: String, expiresAt: Long?) {
        val template = if (expiresAt == null) templates.groupAdd else templates.groupAddTemp

        run(template, uuid, expiresAt, "group.ingame_id" to group)
    }

    override fun removeGroup(uuid: UUID, group: String) {
        run(templates.groupRemove, uuid, null, "group.ingame_id" to group)
    }

    override fun addPermission(uuid: UUID, permission: String, expiresAt: Long?) {
        val template = if (expiresAt == null) templates.permissionSet else templates.permissionSetTemp

        run(template, uuid, expiresAt, "permission.node" to permission)
    }

    override fun removePermission(uuid: UUID, permission: String) {
        run(templates.permissionUnset, uuid, null, "permission.node" to permission)
    }

    override fun groupsOf(uuid: UUID): Set<String> = emptySet()

    override fun has(uuid: UUID, permission: String): Boolean? = null

    override fun permissionsOf(uuid: UUID): Set<String> = emptySet()

    private fun run(template: String, uuid: UUID, expiresAt: Long?, vararg values: Pair<String, String>) {
        if (template.isBlank()) return

        val player = platform.player(uuid)
        val replacements = mutableMapOf(
            "user.uuid" to uuid.toString(),
            "user.username" to (player?.name ?: uuid.toString()),
            "period.duration" to duration(expiresAt),
        )

        values.forEach { replacements[it.first] = it.second }

        var command = template
        replacements.forEach { (key, value) -> command = command.replace("{$key}", value) }

        platform.scheduler.sync { platform.runCommand(command) }
    }

    private fun duration(expiresAt: Long?): String {
        if (expiresAt == null) return ""

        val seconds = (expiresAt - System.currentTimeMillis()) / 1000

        return if (seconds > 0) "${seconds}s" else "1s"
    }
}
