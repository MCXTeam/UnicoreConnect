package ru.unicorecms.unicoreconnect.common.services

import ru.unicorecms.unicoreconnect.common.UnicoreCommon
import ru.unicorecms.unicoreconnect.common.types.CommandsAck
import ru.unicorecms.unicoreconnect.common.types.QueuedCommand
import java.io.IOException

class CommandsService {
    private var config = UnicoreCommon.config
    private var baseUrl = "${config.apiUrl}/rcon/${config.server}/commands"

    fun pending(): Array<QueuedCommand> {
        return UnicoreCommon.requester.get(baseUrl).getOrThrow()
    }

    fun ack(done: List<Int>, failed: List<Int>) {
        val response = UnicoreCommon.requester.post("$baseUrl/ack", CommandsAck(done, failed)).response

        if (!response.isSuccessful) throw IOException(response.toString())
    }
}
