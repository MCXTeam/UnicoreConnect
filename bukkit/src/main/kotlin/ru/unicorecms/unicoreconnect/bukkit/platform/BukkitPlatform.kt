package ru.unicorecms.unicoreconnect.bukkit.platform

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import ru.unicorecms.unicoreconnect.common.platform.BanAdapter
import ru.unicorecms.unicoreconnect.common.platform.Cancellable
import ru.unicorecms.unicoreconnect.common.platform.ItemBridge
import ru.unicorecms.unicoreconnect.common.platform.PermissionAdapter
import ru.unicorecms.unicoreconnect.common.platform.PlatformPlayer
import ru.unicorecms.unicoreconnect.common.platform.PlatformScheduler
import ru.unicorecms.unicoreconnect.common.platform.UnicoreLogger
import ru.unicorecms.unicoreconnect.common.platform.UnicorePlatform
import java.util.UUID

class BukkitPlayer(private val handle: Player) : PlatformPlayer {
    override val uuid: UUID get() = handle.uniqueId
    override val name: String get() = handle.name
    override val address: String? get() = handle.address?.address?.hostAddress

    override fun sendMessage(message: String) = handle.sendMessage(message)

    override fun hasPermission(permission: String) = handle.hasPermission(permission)

    fun handle() = handle
}

class BukkitLogger(private val plugin: JavaPlugin) : UnicoreLogger {
    override fun info(message: String) = plugin.logger.info(message)

    override fun warn(message: String) = plugin.logger.warning(message)

    override fun error(message: String, error: Throwable?) {
        if (error == null) plugin.logger.severe(message) else plugin.logger.severe("$message: $error")
    }
}

class BukkitScheduler(private val plugin: JavaPlugin) : PlatformScheduler {
    override fun sync(task: () -> Unit) {
        if (Bukkit.isPrimaryThread()) task() else Bukkit.getScheduler().runTask(plugin, Runnable { task() })
    }

    override fun async(task: () -> Unit) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable { task() })
    }

    override fun repeatSync(delayTicks: Long, periodTicks: Long, task: () -> Unit): Cancellable {
        val handle = Bukkit.getScheduler().runTaskTimer(plugin, Runnable { task() }, delayTicks, periodTicks)

        return object : Cancellable {
            override fun cancel() = handle.cancel()
        }
    }
}

class BukkitPlatform(
    private val plugin: JavaPlugin,
    override val permissions: PermissionAdapter?,
    override val bans: BanAdapter?,
    override val items: ItemBridge?,
) : UnicorePlatform {
    override val logger: UnicoreLogger = BukkitLogger(plugin)
    override val scheduler: PlatformScheduler = BukkitScheduler(plugin)

    override fun player(uuid: UUID): PlatformPlayer? = Bukkit.getPlayer(uuid)?.let { BukkitPlayer(it) }

    @Suppress("DEPRECATION")
    override fun player(name: String): PlatformPlayer? = Bukkit.getPlayer(name)?.let { BukkitPlayer(it) }

    override fun onlinePlayers(): Collection<PlatformPlayer> = Bukkit.getOnlinePlayers().map { BukkitPlayer(it) }

    override fun runCommand(command: String) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
    }
}
