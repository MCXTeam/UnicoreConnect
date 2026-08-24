package ru.unicorecms.unicoreconnect.common.modules

import ru.unicorecms.unicoreconnect.common.UnicoreCommon
import ru.unicorecms.unicoreconnect.common.platform.UnicorePlatform
import java.util.Date
import java.util.UUID

class BansModule(private val platform: UnicorePlatform) {
    private val logger = platform.logger

    fun handleJoin(uuid: UUID) {
        val bans = platform.bans ?: return

        if (!bans.available()) return

        try {
            val siteBan = UnicoreCommon.banService.find(uuid)
            val serverBanned = bans.banned(uuid)

            if (siteBan == null && serverBanned) {
                bans.unban(uuid)
                return
            }

            if (siteBan != null && !serverBanned) {
                val actor = siteBan.actor?.uuid?.takeIf { it.isNotBlank() }?.let { UUID.fromString(it) }

                bans.ban(uuid, siteBan.reason, actor, siteBan.expires?.time)
            }
        } catch (error: Throwable) {
            logger.error("Синхронизация банов игрока $uuid не удалась", error)
        }
    }

    fun onServerBan(uuid: UUID, reason: String, actor: UUID?, expires: Date?) {
        platform.scheduler.async {
            try {
                UnicoreCommon.banService.create(uuid, reason, actor, expires)
            } catch (error: Throwable) {
                logger.error("Бан игрока $uuid на сайт не отправлен", error)
            }
        }
    }

    fun onServerUnban(uuid: UUID) {
        platform.scheduler.async {
            try {
                UnicoreCommon.banService.delete(uuid)
            } catch (error: Throwable) {
                logger.error("Разбан игрока $uuid на сайт не отправлен", error)
            }
        }
    }
}
