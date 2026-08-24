package ru.unicorecms.unicoreconnect.common

import io.socket.client.IO
import io.socket.client.Socket
import ru.unicorecms.unicoreconnect.common.events.EventDispatcher
import ru.unicorecms.unicoreconnect.common.events.SocketEvent
import ru.unicorecms.unicoreconnect.common.platform.UnicoreLogger
import ru.unicorecms.unicoreconnect.common.types.User
import ru.unicorecms.unicoreconnect.common.types.UserDonate
import ru.unicorecms.unicoreconnect.common.types.UserPermission
import java.util.Collections.singletonList
import java.util.Collections.singletonMap

class SocketClient(private val logger: UnicoreLogger) {
    private val config = UnicoreCommon.config
    private var socket: Socket? = null
    private var connectedOnce = false

    fun reconnectHandler() {
        val current = socket ?: return

        if (!current.connected()) {
            try {
                current.connect()
            } catch (error: Throwable) {
                logger.warn("Не удалось переподключиться к UnicoreCMS: ${error.message}")
            }
        }
    }

    fun connect() {
        try {
            socket = IO.socket(config.apiUrl, IO.Options().apply {
                extraHeaders = singletonMap("authorization", singletonList("Api-Key ${config.apiKey}"))
                timestampRequests = true
                secure = true
                forceNew = true
                reconnection = false
            })
        } catch (error: Throwable) {
            logger.error("Адрес UnicoreCMS указан неверно, вебсокет не запущен", error)
            return
        }

        socket?.on("me") { args -> handle("me") { onMe(args) } }
        socket?.on("give_group") { args -> handle("give_group") { onGiveGroup(args) } }
        socket?.on("take_group") { args -> handle("take_group") { onTakeGroup(args) } }
        socket?.on("give_permission") { args -> handle("give_permission") { onGivePermission(args) } }
        socket?.on("take_permission") { args -> handle("take_permission") { onTakePermission(args) } }

        socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
            logger.warn("UnicoreCMS недоступен: ${args.firstOrNull()}")
        }

        socket?.on(Socket.EVENT_DISCONNECT) {
            logger.info("Соединение с UnicoreCMS потеряно, переподключаемся")
        }

        try {
            socket?.connect()
        } catch (error: Throwable) {
            logger.error("Не удалось открыть вебсокет к UnicoreCMS", error)
        }
    }

    fun close() {
        socket?.off()
        socket?.disconnect()
        socket = null
    }

    private fun handle(event: String, block: () -> Unit) {
        try {
            block()
        } catch (error: Throwable) {
            logger.error("Событие '$event' обработать не удалось", error)
        }
    }

    private fun onMe(args: Array<out Any>) {
        val payload = args.firstOrNull() ?: run {
            logger.warn("Ключ API отклонён сайтом")
            return
        }

        val data = UnicoreCommon.gson.fromJson(payload.toString(), User::class.java)

        if (!data.perms.contains(CONNECT_PERMISSION)) {
            logger.warn("У ключа API нет права '$CONNECT_PERMISSION'")
            return
        }

        if (!connectedOnce) {
            connectedOnce = true
            logger.info("Соединение с UnicoreCMS установлено")
        }
    }

    private fun onGiveGroup(args: Array<out Any>) {
        val payload = parse<UserDonate>(args) ?: return

        if (payload.server.id != config.server) return

        EventDispatcher.post(SocketEvent.GIVE_GROUP(payload)) { logger.error("Ошибка обработчика give_group", it) }
    }

    private fun onTakeGroup(args: Array<out Any>) {
        val payload = parse<UserDonate>(args) ?: return

        if (payload.server.id != config.server) return

        EventDispatcher.post(SocketEvent.TAKE_GROUP(payload)) { logger.error("Ошибка обработчика take_group", it) }
    }

    private fun onGivePermission(args: Array<out Any>) {
        val payload = parse<UserPermission>(args) ?: return

        if (payload.server.id != config.server) return

        EventDispatcher.post(SocketEvent.GIVE_PERMISSION(payload)) { logger.error("Ошибка обработчика give_permission", it) }
    }

    private fun onTakePermission(args: Array<out Any>) {
        val payload = parse<UserPermission>(args) ?: return

        if (payload.server.id != config.server) return

        EventDispatcher.post(SocketEvent.TAKE_PERMISSION(payload)) { logger.error("Ошибка обработчика take_permission", it) }
    }

    private inline fun <reified T> parse(args: Array<out Any>): T? {
        val payload = args.firstOrNull() ?: return null

        return UnicoreCommon.gson.fromJson(payload.toString(), T::class.java)
    }

    companion object {
        const val CONNECT_PERMISSION = "kernel.unicore.connect"
    }
}
