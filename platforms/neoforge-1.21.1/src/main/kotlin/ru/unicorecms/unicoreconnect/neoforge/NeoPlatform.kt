package ru.unicorecms.unicoreconnect.neoforge

import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import org.apache.logging.log4j.LogManager
import ru.unicorecms.unicoreconnect.common.platform.BanAdapter
import ru.unicorecms.unicoreconnect.common.platform.Cancellable
import ru.unicorecms.unicoreconnect.common.platform.ItemBridge
import ru.unicorecms.unicoreconnect.common.platform.PermissionAdapter
import ru.unicorecms.unicoreconnect.common.platform.PlatformPlayer
import ru.unicorecms.unicoreconnect.common.platform.PlatformScheduler
import ru.unicorecms.unicoreconnect.common.platform.UnicoreLogger
import ru.unicorecms.unicoreconnect.common.platform.UnicorePlatform
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class NeoLogger : UnicoreLogger {
    private val logger = LogManager.getLogger("UnicoreConnect")

    override fun info(message: String) = logger.info(message)

    override fun warn(message: String) = logger.warn(message)

    override fun error(message: String, error: Throwable?) {
        if (error == null) logger.error(message) else logger.error(message, error)
    }
}

class NeoPlayer(private val handle: ServerPlayer) : PlatformPlayer {
    override val uuid: UUID get() = handle.uuid
    override val name: String get() = handle.gameProfile.name
    override val address: String?
        get() = handle.connection.connection.remoteAddress.toString().substringAfter('/').substringBefore(':').ifBlank { null }

    override fun sendMessage(message: String) {
        handle.sendSystemMessage(Component.literal(message))
    }

    override fun hasPermission(permission: String) = NeoPermissions.check(handle, permission)

    fun handle() = handle
}

class NeoScheduler(private val server: () -> MinecraftServer?) : PlatformScheduler {
    private val pool = Executors.newScheduledThreadPool(2) { task ->
        Thread(task, "UnicoreConnect").apply { isDaemon = true }
    }

    override fun sync(task: () -> Unit) {
        val current = server()

        if (current == null || current.isSameThread) task() else current.execute { task() }
    }

    override fun async(task: () -> Unit) {
        pool.execute(task)
    }

    override fun repeatSync(delayTicks: Long, periodTicks: Long, task: () -> Unit): Cancellable {
        val handle = pool.scheduleAtFixedRate({ sync(task) }, delayTicks * 50, periodTicks * 50, TimeUnit.MILLISECONDS)

        return object : Cancellable {
            override fun cancel() {
                handle.cancel(false)
            }
        }
    }

    fun shutdown() = pool.shutdownNow()
}

class NeoPlatformImpl(
    private val serverProvider: () -> MinecraftServer?,
    override val permissions: PermissionAdapter?,
    override val bans: BanAdapter?,
    override val items: ItemBridge?,
) : UnicorePlatform {
    override val logger: UnicoreLogger = NeoLogger()
    val neoScheduler = NeoScheduler(serverProvider)
    override val scheduler: PlatformScheduler get() = neoScheduler

    override fun player(uuid: UUID): PlatformPlayer? = serverProvider()?.playerList?.getPlayer(uuid)?.let { NeoPlayer(it) }

    override fun player(name: String): PlatformPlayer? = serverProvider()?.playerList?.getPlayerByName(name)?.let { NeoPlayer(it) }

    override fun onlinePlayers(): Collection<PlatformPlayer> =
        serverProvider()?.playerList?.players?.map { NeoPlayer(it) } ?: emptyList()

    override fun runCommand(command: String) {
        val server = serverProvider() ?: return
        val source = server.createCommandSourceStack()

        server.commands.performPrefixedCommand(source, command)
    }
}
