package ru.unicorecms.unicoreconnect.adapters.bans.luxinfine

import ml.luxinfine.helper.integrations.DefaultIntegrations
import ml.luxinfine.helper.integrations.Punishments
import ru.unicorecms.unicoreconnect.common.platform.BanAdapter
import java.util.UUID

class LuxinfineBanAdapter : BanAdapter {
    override val id = ID

    private val api: Punishments? by lazy {
        try {
            DefaultIntegrations.punishments()?.takeIf { !it.isDummy }
        } catch (error: Throwable) {
            null
        }
    }

    override fun available(): Boolean {
        val types = api?.supportedPunishTypes ?: return false

        return types.contains(Punishments.PunishType.BAN)
    }

    override fun banned(uuid: UUID) = api?.isBanned(uuid) ?: false

    override fun ban(uuid: UUID, reason: String, actor: UUID?, expiresAt: Long?) {
        api?.ban(uuid, seconds(expiresAt), actor, reason)
    }

    override fun unban(uuid: UUID) {
        api?.unban(uuid)
    }

    private fun seconds(expiresAt: Long?): Long {
        if (expiresAt == null) return PERMANENT

        val left = (expiresAt - System.currentTimeMillis()) / 1000

        return if (left > 0) left else 1
    }

    companion object {
        const val ID = "luxinfine"

        private const val PERMANENT = 0L
    }
}
