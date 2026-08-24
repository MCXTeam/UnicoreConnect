package ru.unicorecms.unicoreconnect.bukkit

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import ru.unicorecms.unicoreconnect.bukkit.commands.MoneyCommand
import ru.unicorecms.unicoreconnect.bukkit.commands.PlaytimeCommand
import ru.unicorecms.unicoreconnect.bukkit.commands.ShowcaseCommand
import ru.unicorecms.unicoreconnect.bukkit.commands.UnicoreConnectCommand
import ru.unicorecms.unicoreconnect.bukkit.config.UnicorePluginConfig
import ru.unicorecms.unicoreconnect.bukkit.listeners.BanManagerListener
import ru.unicorecms.unicoreconnect.bukkit.listeners.JoinListener
import ru.unicorecms.unicoreconnect.adapters.bans.banmanager.BanManagerAdapter
import ru.unicorecms.unicoreconnect.adapters.economy.vault.VaultEconomyBridge
import ru.unicorecms.unicoreconnect.bukkit.platform.BukkitPlatform
import ru.unicorecms.unicoreconnect.adapters.permissions.luckperms.LuckPermsAdapter
import ru.unicorecms.unicoreconnect.bukkit.platform.VanillaBanAdapter
import ru.unicorecms.unicoreconnect.bukkit.tasks.PlaytimeTask
import ru.unicorecms.unicoreconnect.common.SocketClient
import ru.unicorecms.unicoreconnect.common.UnicoreCommon
import ru.unicorecms.unicoreconnect.common.config.PermissionsConfig
import ru.unicorecms.unicoreconnect.common.events.EventDispatcher
import ru.unicorecms.unicoreconnect.common.modules.BansModule
import ru.unicorecms.unicoreconnect.common.modules.DonateModule
import ru.unicorecms.unicoreconnect.common.modules.EconomyModule
import ru.unicorecms.unicoreconnect.common.platform.CommandPermissionAdapter
import ru.unicorecms.unicoreconnect.common.platform.PermissionAdapter
import ru.unicorecms.unicoreconnect.common.platform.UnicorePlatform

@Suppress("unused")
class UnicoreConnectBukkit : JavaPlugin() {
    private val pluginInstance = PluginInstance(this)
    private val unicoreConfig = UnicorePluginConfig().get()
    private val unicoreCommon = UnicoreCommon(unicoreConfig)
    private val commandManager = CommandManager()

    private lateinit var platform: UnicorePlatform
    private lateinit var socketClient: SocketClient

    private val playtimeTask = PlaytimeTask()

    private var donateModule: DonateModule? = null
    private var bansModule: BansModule? = null
    private var economyModule: EconomyModule? = null

    override fun onEnable() {
        platform = BukkitPlatform(this, choosePermissionAdapter(), chooseBanAdapter(), null)
        socketClient = SocketClient(platform.logger)

        logger.info("Проверяем сервер в UnicoreCMS...")

        val gameServer = UnicoreCommon.serversService.check(platform.logger)

        if (gameServer == null) {
            logger.warning("Сервер '${UnicoreCommon.config.server}' не найден в UnicoreCMS или адрес API указан неверно")
            return
        }

        UnicoreCommon.server = gameServer
        logger.info("Сервер получен: ${gameServer.name} [#${gameServer.id}]")

        commandManager.init()
        UnicoreCommon.messages = { key, value -> CommandManager.msg(key, replacements = arrayOf("{name}", value)) }

        socketClient.connect()

        if (unicoreConfig.modules.donate) {
            donateModule = DonateModule(platform).also { it.start() }
        } else {
            logger.info("Модуль донат-групп выключен в конфигурации")
        }

        if (unicoreConfig.modules.bans) {
            bansModule = BansModule(platform)

            if (Bukkit.getPluginManager().getPlugin("BanManager") != null) {
                server.pluginManager.registerEvents(BanManagerListener(bansModule!!), this)
            }
        } else {
            logger.info("Модуль банов выключен в конфигурации")
        }

        server.pluginManager.registerEvents(JoinListener(donateModule, bansModule), this)

        if (unicoreConfig.modules.money) {
            economyModule = EconomyModule(platform).also { it.register(listOf(VaultEconomyBridge(this))) }
        }
        if (unicoreConfig.modules.playtime) {
            playtimeTask.load()
            Bukkit.getScheduler().scheduleSyncRepeatingTask(this, { playtimeTask.handler() }, 0, 20 * 60)
        }

        CommandManager.manager.registerCommand(UnicoreConnectCommand())
        if (unicoreConfig.modules.money) CommandManager.manager.registerCommand(MoneyCommand())
        if (unicoreConfig.modules.playtime) CommandManager.manager.registerCommand(PlaytimeCommand())
        if (unicoreConfig.modules.showcase) CommandManager.manager.registerCommand(ShowcaseCommand())

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, { socketClient.reconnectHandler() }, 20 * 10, 20 * 3)
    }

    override fun onDisable() {
        economyModule?.unregister()
        EventDispatcher.clear()

        if (this::socketClient.isInitialized) socketClient.close()
    }

    private fun choosePermissionAdapter(): PermissionAdapter? {
        if (!unicoreConfig.modules.donate) return null

        val requested = unicoreConfig.permissions.adapter
        val luckPerms = LuckPermsAdapter()

        val adapter = when (requested) {
            PermissionsConfig.ADAPTER_AUTO -> if (luckPerms.available()) luckPerms else CommandPermissionAdapter(platformForCommands())
            luckPerms.id -> luckPerms
            else -> CommandPermissionAdapter(platformForCommands())
        }

        logger.info("Система прав: ${adapter.id}")

        return adapter
    }

    private fun chooseBanAdapter() = if (Bukkit.getPluginManager().getPlugin("BanManager") != null) {
        BanManagerAdapter()
    } else {
        VanillaBanAdapter()
    }

    private fun platformForCommands(): UnicorePlatform = BukkitPlatform(this, null, null, null)
}
