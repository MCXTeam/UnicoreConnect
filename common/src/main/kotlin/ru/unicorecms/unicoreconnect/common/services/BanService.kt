package ru.unicorecms.unicoreconnect.common.services

import ru.unicorecms.unicoreconnect.common.UnicoreCommon
import ru.unicorecms.unicoreconnect.common.types.Ban
import ru.unicorecms.unicoreconnect.common.types.BanRequest
import java.util.Date
import java.util.UUID

class BanService {
    private val config = UnicoreCommon.config
    private val baseUrl = "${config.apiUrl}/bans"

    fun find(uuid: UUID): Ban? {
        val result = UnicoreCommon.requester.get("$baseUrl/$uuid")

        if (!result.response.isSuccessful) return null

        return result.getOrThrow()
    }

    fun create(uuid: UUID, reason: String, actor: UUID?, expires: Date?) {
        UnicoreCommon.requester.post(baseUrl, BanRequest(uuid.toString(), reason, actor?.toString(), expires?.time))
    }

    fun delete(uuid: UUID) {
        UnicoreCommon.requester.delete("$baseUrl/$uuid")
    }
}
