package ru.unicorecms.unicoreconnect.common.modules

import ru.unicorecms.unicoreconnect.common.UnicoreCommon
import ru.unicorecms.unicoreconnect.common.config.MessagesConfig
import ru.unicorecms.unicoreconnect.common.events.EventDispatcher
import ru.unicorecms.unicoreconnect.common.events.SocketEvent
import ru.unicorecms.unicoreconnect.common.platform.PermissionAdapter
import ru.unicorecms.unicoreconnect.common.platform.UnicorePlatform
import ru.unicorecms.unicoreconnect.common.services.donate.DonateGroupService
import ru.unicorecms.unicoreconnect.common.services.donate.DonatePermissionService
import ru.unicorecms.unicoreconnect.common.types.DonatePermission
import ru.unicorecms.unicoreconnect.common.types.UserDonate
import ru.unicorecms.unicoreconnect.common.types.UserPermission
import java.util.UUID

class DonateModule(private val platform: UnicorePlatform) {
    private val logger = platform.logger
    private val adapter: PermissionAdapter? get() = platform.permissions

    fun start() {
        val permissions = adapter

        if (permissions == null || !permissions.available()) {
            logger.warn("Модуль донат-групп выключен: система прав не найдена")
            return
        }

        platform.scheduler.async {
            try {
                val groups = UnicoreCommon.donateGroupService.load()
                val nodes = UnicoreCommon.donatePermissionService.load()

                logger.info("Загружено донат-групп: ${groups.size}, донат-прав: ${nodes.size}")
            } catch (error: Throwable) {
                logger.error("Списки донат-групп и прав получить не удалось", error)
            }
        }

        EventDispatcher.on<SocketEvent.GIVE_GROUP> { platform.scheduler.async { giveGroup(it.payload) } }
        EventDispatcher.on<SocketEvent.TAKE_GROUP> { platform.scheduler.async { takeGroup(it.payload) } }
        EventDispatcher.on<SocketEvent.GIVE_PERMISSION> { platform.scheduler.async { givePermission(it.payload) } }
        EventDispatcher.on<SocketEvent.TAKE_PERMISSION> { platform.scheduler.async { takePermission(it.payload) } }
    }

    fun handleJoin(uuid: UUID) {
        val permissions = adapter ?: return

        if (!permissions.available()) return

        platform.scheduler.async {
            try {
                syncGroups(uuid, permissions)
                syncPermissions(uuid, permissions)
            } catch (error: Throwable) {
                logger.error("Синхронизация донат-групп игрока $uuid не удалась", error)
            }
        }
    }

    private fun syncGroups(uuid: UUID, permissions: PermissionAdapter) {
        val expected = UnicoreCommon.donateGroupService.userGroups(uuid)
        val actual = permissions.groupsOf(uuid)

        expected.forEach { donate ->
            if (!actual.contains(donate.group.ingame_id)) giveGroup(donate)
        }

        DonateGroupService.groups.forEach { group ->
            val stillGranted = expected.any { it.group.ingame_id == group.ingame_id }

            if (!stillGranted && actual.contains(group.ingame_id)) permissions.removeGroup(uuid, group.ingame_id)
        }
    }

    private fun syncPermissions(uuid: UUID, permissions: PermissionAdapter) {
        val expected = UnicoreCommon.donatePermissionService.userPermissions(uuid)
        val actual = permissions.permissionsOf(uuid)

        expected.forEach { granted ->
            val nodes = granted.permission.perms.orEmpty()

            if (nodes.isNotEmpty() && !nodes.all { actual.contains(it) }) givePermission(granted)
        }

        DonatePermissionService.permissions.forEach { permission ->
            val nodes = permission.perms.orEmpty()

            if (nodes.isEmpty()) return@forEach

            val stillGranted = expected.any { it.permission.id == permission.id }

            if (!stillGranted && nodes.any { actual.contains(it) }) removeNodes(uuid, permission, permissions)
        }
    }

    private fun giveGroup(payload: UserDonate) {
        val permissions = adapter ?: return
        val uuid = uuidOf(payload.user.uuid) ?: return

        permissions.addGroup(uuid, payload.group.ingame_id, payload.expired?.time)
        notify(payload.user.username, MessagesConfig.KEY_GIVE_GROUP, payload.group.name)
    }

    private fun takeGroup(payload: UserDonate) {
        val permissions = adapter ?: return
        val uuid = uuidOf(payload.user.uuid) ?: return

        permissions.removeGroup(uuid, payload.group.ingame_id)
        notify(payload.user.username, MessagesConfig.KEY_TAKE_GROUP, payload.group.name)
    }

    private fun givePermission(payload: UserPermission) {
        val permissions = adapter ?: return
        val uuid = uuidOf(payload.user.uuid) ?: return

        payload.permission.perms.orEmpty().forEach { permissions.addPermission(uuid, it, payload.expired?.time) }
        notify(payload.user.username, MessagesConfig.KEY_GIVE_PERMISSION, payload.permission.name)
    }

    private fun takePermission(payload: UserPermission) {
        val permissions = adapter ?: return
        val uuid = uuidOf(payload.user.uuid) ?: return

        removeNodes(uuid, payload.permission, permissions)
        notify(payload.user.username, MessagesConfig.KEY_TAKE_PERMISSION, payload.permission.name)
    }

    private fun removeNodes(uuid: UUID, permission: DonatePermission, permissions: PermissionAdapter) {
        permission.perms.orEmpty().forEach { permissions.removePermission(uuid, it) }
    }

    private fun notify(username: String, key: String, value: String) {
        val message = UnicoreCommon.messages?.invoke(key, value) ?: return

        platform.scheduler.sync { platform.player(username)?.sendMessage(message) }
    }

    private fun uuidOf(value: String): UUID? {
        return try {
            UUID.fromString(value)
        } catch (error: IllegalArgumentException) {
            logger.warn("Некорректный uuid игрока: '$value'")
            null
        }
    }
}
