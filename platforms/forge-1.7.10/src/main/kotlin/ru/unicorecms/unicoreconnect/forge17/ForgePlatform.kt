package ru.unicorecms.unicoreconnect.forge17

import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.server.MinecraftServer
import net.minecraft.util.ChatComponentText
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
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

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
        get() = handle.playerNetServerHandler.netManager.socketAddress.toString()
            .substringAfter('/')
            .substringBefore(':')
            .ifBlank { null }

    override fun sendMessage(message: String) {
        handle.addChatMessage(ChatComponentText(message))
    }

    override fun hasPermission(permission: String) = platform.permissions?.has(uuid, permission)
        ?: ForgePermissions.check(handle, permission)

    fun handle() = handle
}

class ForgeScheduler : PlatformScheduler {
    private val pool = Executors.newFixedThreadPool(2) { task ->
        Thread(task, "UnicoreConnect").apply { isDaemon = true }
    }

    private val pending = ConcurrentLinkedQueue<() -> Unit>()
    private val repeating = CopyOnWriteArrayList<Repeating>()
    private var ticks = 0L

    override fun sync(task: () -> Unit) {
        pending.add(task)
    }

    override fun async(task: () -> Unit) {
        pool.execute(task)
    }

    override fun repeatSync(delayTicks: Long, periodTicks: Long, task: () -> Unit): Cancellable {
        val entry = Repeating(ticks + delayTicks, periodTicks, task)

        repeating.add(entry)

        return object : Cancellable {
            override fun cancel() {
                repeating.remove(entry)
            }
        }
    }

    fun tick() {
        ticks++

        while (true) {
            val task = pending.poll() ?: break

            task()
        }

        repeating.forEach { entry ->
            if (ticks < entry.nextRun) return@forEach

            entry.nextRun = ticks + entry.period
            entry.task()
        }
    }

    fun shutdown() {
        pending.clear()
        repeating.clear()
        pool.shutdownNow()
    }

    private class Repeating(var nextRun: Long, val period: Long, val task: () -> Unit)
}

class ForgePlatformImpl(
    private val serverProvider: () -> MinecraftServer?,
    override val permissions: PermissionAdapter?,
    override val bans: BanAdapter?,
    override val items: ItemBridge?,
) : UnicorePlatform {
    override val logger: UnicoreLogger = ForgeLogger()
    val forgeScheduler = ForgeScheduler()
    override val scheduler: PlatformScheduler get() = forgeScheduler

    override fun player(uuid: UUID): PlatformPlayer? = wrap(online().firstOrNull { it.uniqueID == uuid })

    override fun player(name: String): PlatformPlayer? =
        wrap(serverProvider()?.configurationManager?.func_152612_a(name))

    override fun onlinePlayers(): Collection<PlatformPlayer> = online().map { ForgePlayer(it, this) }

    override fun runCommand(command: String) {
        val server = serverProvider() ?: return

        server.commandManager.executeCommand(server, command)
    }

    fun wrap(handle: EntityPlayerMP?): ForgePlayer? = handle?.let { ForgePlayer(it, this) }

    @Suppress("UNCHECKED_CAST")
    private fun online(): List<EntityPlayerMP> =
        serverProvider()?.configurationManager?.playerEntityList as? List<EntityPlayerMP> ?: emptyList()
}
