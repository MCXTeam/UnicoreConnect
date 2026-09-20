package ru.unicorecms.unicoreconnect.common.modules

import ru.unicorecms.unicoreconnect.common.UnicoreCommon
import ru.unicorecms.unicoreconnect.common.events.EventDispatcher
import ru.unicorecms.unicoreconnect.common.events.SocketEvent
import ru.unicorecms.unicoreconnect.common.platform.Cancellable
import ru.unicorecms.unicoreconnect.common.platform.UnicorePlatform
import ru.unicorecms.unicoreconnect.common.types.QueuedCommand
import java.util.concurrent.atomic.AtomicBoolean

class CommandsModule(private val platform: UnicorePlatform) {
    private val logger = platform.logger
    private val running = AtomicBoolean(false)
    private var task: Cancellable? = null

    fun start() {
        EventDispatcher.on<SocketEvent.RUN_COMMANDS> { drain() }

        task = platform.scheduler.repeatSync(STARTUP_DELAY_TICKS, PERIOD_TICKS) { drain() }
    }

    fun stop() {
        task?.cancel()
        task = null
    }

    fun drain() {
        if (!running.compareAndSet(false, true)) return

        platform.scheduler.async {
            val pending = try {
                UnicoreCommon.commandsService.pending()
            } catch (error: Throwable) {
                logger.error("Очередь команд получить не удалось", error)
                running.set(false)
                return@async
            }

            if (pending.isEmpty()) {
                running.set(false)
                return@async
            }

            platform.scheduler.sync { execute(pending) }
        }
    }

    private fun execute(pending: Array<QueuedCommand>) {
        val done = arrayListOf<Int>()
        val failed = arrayListOf<Int>()

        for (item in pending) {
            try {
                platform.runCommand(item.command)
                done.add(item.id)
            } catch (error: Throwable) {
                logger.error("Команда с сайта не выполнена: ${item.command}", error)
                failed.add(item.id)
            }
        }

        platform.scheduler.async {
            try {
                UnicoreCommon.commandsService.ack(done, failed)
                logger.info("Команд с сайта выполнено: ${done.size}" + if (failed.isEmpty()) "" else ", с ошибкой: ${failed.size}")
                running.set(false)
                drain()
            } catch (error: Throwable) {
                logger.error("Отчёт о выполнении команд не ушёл, сайт вернёт их в очередь сам", error)
                running.set(false)
            }
        }
    }

    companion object {
        private const val STARTUP_DELAY_TICKS = 20L * 15
        private const val PERIOD_TICKS = 20L * 60
    }
}
