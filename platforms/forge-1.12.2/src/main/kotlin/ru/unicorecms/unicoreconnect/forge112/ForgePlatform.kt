package ru.unicorecms.unicoreconnect.forge112

import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.server.MinecraftServer
import net.minecraft.util.text.TextComponentString
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

class ForgeLogger : UnicoreLogger {
    private val logger = LogManager.getLogger("UnicoreConnect")

    override fun info(message: String) = logger.info(message)

    override fun warn(message: String) = logger.warn(message)

    override fun error(message: String, error: Throwable?) {
        if (error == null) logger.error(message) else logger.error(message, error)
    }
}

class ForgePlayer(private val handle: EntityPlayerMP, private val platform: ForgePlatformImpl) : PlatformPlayer {
    override val uuid: UUID get() = handle.uniqueID
    override val name: String get() = handle.gameProfile.name
    override val address: String?
        get() = handle.connection.netManager.remoteAddress.toString()
            .substringAfter('/')
            .substringBefore(':')
            .ifBlank { null }

    override fun sendMessage(message: String) {
        handle.sendMessage(TextComponentString(message))
    }

    override fun hasPermission(permission: String) = platform.permissions?.has(uuid, permission)
        ?: ForgePermissions.check(handle, permission)

    fun handle() = handle
}

class ForgeScheduler(private val server: () -> MinecraftServer?) : PlatformScheduler {
    private val pool = Executors.newScheduledThreadPool(2) { task ->
        Thread(task, "UnicoreConnect").apply { isDaemon = true }
    }

    override fun sync(task: () -> Unit) {
        val current = server()

        if (current == null || current.isCallingFromMinecraftThread) task() else current.addScheduledTask(task)
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

class ForgePlatformImpl(
    private val serverProvider: () -> MinecraftServer?,
    override val permissions: PermissionAdapter?,
    override val bans: BanAdapter?,
    override val items: ItemBridge?,
) : UnicorePlatform {
    override val logger: UnicoreLogger = ForgeLogger()
    val forgeScheduler = ForgeScheduler(serverProvider)
    override val scheduler: PlatformScheduler get() = forgeScheduler

    override fun player(uuid: UUID): PlatformPlayer? = wrap(serverProvider()?.playerList?.getPlayerByUUID(uuid))

    override fun player(name: String): PlatformPlayer? = wrap(serverProvider()?.playerList?.getPlayerByUsername(name))

    override fun onlinePlayers(): Collection<PlatformPlayer> =
        serverProvider()?.playerList?.players?.map { ForgePlayer(it, this) } ?: emptyList()

    override fun runCommand(command: String) {
        val server = serverProvider() ?: return

        server.commandManager.executeCommand(server, command)
    }

    fun wrap(handle: EntityPlayerMP?): ForgePlayer? = handle?.let { ForgePlayer(it, this) }
}
