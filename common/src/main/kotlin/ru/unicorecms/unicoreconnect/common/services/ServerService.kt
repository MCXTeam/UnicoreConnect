package ru.unicorecms.unicoreconnect.common.services

import ru.unicorecms.unicoreconnect.common.UnicoreCommon
import ru.unicorecms.unicoreconnect.common.platform.UnicoreLogger
import ru.unicorecms.unicoreconnect.common.types.Server

class ServerService {
    private val config = UnicoreCommon.config

    fun check(logger: UnicoreLogger): Server? {
        return try {
            UnicoreCommon.requester.get("${config.apiUrl}/servers/${config.server}").getOrThrow()
        } catch (error: Exception) {
            logger.warn(error.message ?: error.toString())
            null
        }
    }
}
