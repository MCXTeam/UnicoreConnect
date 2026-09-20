package ru.unicorecms.unicoreconnect.forge

import net.minecraft.server.MinecraftServer
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.event.server.ServerStartedEvent
import net.minecraftforge.event.server.ServerStoppingEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.loading.FMLPaths
import net.minecraft.server.level.ServerPlayer
import ru.unicorecms.unicoreconnect.adapters.permissions.luckperms.LuckPermsAdapter
import ru.unicorecms.unicoreconnect.common.SocketClient
import ru.unicorecms.unicoreconnect.common.UnicoreCommon
import ru.unicorecms.unicoreconnect.common.config.JsonConfigLoader
import ru.unicorecms.unicoreconnect.common.config.PermissionsConfig
import ru.unicorecms.unicoreconnect.common.events.EventDispatcher
import ru.unicorecms.unicoreconnect.common.modules.BansModule
import ru.unicorecms.unicoreconnect.common.modules.CommandsModule
import ru.unicorecms.unicoreconnect.common.modules.DonateModule
import ru.unicorecms.unicoreconnect.common.modules.EconomyModule
import ru.unicorecms.unicoreconnect.common.platform.CommandPermissionAdapter
import ru.unicorecms.unicoreconnect.common.platform.DelegatingPermissionAdapter
import ru.unicorecms.unicoreconnect.common.platform.PermissionAdapter
import ru.unicorecms.unicoreconnect.forge.commands.UnicoreCommands
import ru.unicorecms.unicoreconnect.forge.tasks.PlaytimeTracker

@Mod(UnicoreConnectForge.MOD_ID)
class UnicoreConnectForge {
    init {
        loadConfig()
        MinecraftForge.EVENT_BUS.register(this)
    }

    private fun loadConfig() {
        val configFile = FMLPaths.CONFIGDIR.get().resolve("unicoreconnect.json").toFile()

        UnicoreCommon(JsonConfigLoader.load(configFile))
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    fun onServerStarted(event: ServerStartedEvent) {
        server = event.server
        start()
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    fun onServerStopping(event: ServerStoppingEvent) {
        stop()
        server = null
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    fun onCommands(event: RegisterCommandsEvent) {
        UnicoreCommands.register(event.dispatcher, platform)
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    fun onPlayerJoin(event: PlayerEvent.PlayerLoggedInEvent) {
        val player = event.entity as? ServerPlayer ?: return

        donateModule?.handleJoin(player.uuid)
        bansModule?.handleJoin(player.uuid)
        playtimeTracker?.onJoin(player.uuid)
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    fun onPlayerQuit(event: PlayerEvent.PlayerLoggedOutEvent) {
        val player = event.entity as? ServerPlayer ?: return

        playtimeTracker?.onQuit(player.uuid)
    }

    private fun start() {
        val config = UnicoreCommon.config
        val logger = platform.logger

        logger.info("Проверяем сервер в UnicoreCMS...")

        val gameServer = try {
            UnicoreCommon.serversService.check(logger)
        } catch (error: Throwable) {
            logger.error("UnicoreCMS недоступен", error)
            null
        }

        if (gameServer == null) {
            logger.warn("Сервер '${config.server}' не найден в UnicoreCMS или адрес API указан неверно")
            return
        }

        UnicoreCommon.server = gameServer
        logger.info("Сервер получен: ${gameServer.name} [#${gameServer.id}]")

        permissionAdapter = choosePermissionAdapter()
        logger.info("Система прав: ${permissionAdapter?.id ?: "выключена"}")

        socketClient = SocketClient(logger).also { it.connect() }

        if (config.modules.money) economyModule = EconomyModule(platform).also { it.register(emptyList()) }
        if (config.modules.donate) donateModule = DonateModule(platform).also { it.start() }
        commandsModule = CommandsModule(platform).also { it.start() }
        if (config.modules.bans) bansModule = BansModule(platform)
        if (config.modules.playtime) playtimeTracker = PlaytimeTracker(platform).also { it.start() }

        reconnectTask = platform.forgeScheduler.repeatSync(200, 60) { socketClient?.reconnectHandler() }
    }

    private fun stop() {
        economyModule?.unregister()
        reconnectTask?.cancel()
        playtimeTracker?.stop()
        socketClient?.close()
        commandsModule?.stop()
        EventDispatcher.clear()
        platform.forgeScheduler.shutdown()

        socketClient = null
        donateModule = null
        economyModule = null
        bansModule = null
        commandsModule = null
        playtimeTracker = null
    }

    private fun choosePermissionAdapter(): PermissionAdapter? {
        if (!UnicoreCommon.config.modules.donate) return null

        val requested = UnicoreCommon.config.permissions.adapter
        val luckPerms = LuckPermsAdapter()

        return when {
            requested == luckPerms.id -> luckPerms
            requested == PermissionsConfig.ADAPTER_COMMANDS -> CommandPermissionAdapter(platform)
            luckPerms.available() -> luckPerms
            else -> CommandPermissionAdapter(platform)
        }
    }

    companion object {
        const val MOD_ID = "unicoreconnect"

        private var server: MinecraftServer? = null
        private var socketClient: SocketClient? = null
        private var donateModule: DonateModule? = null
        private var economyModule: EconomyModule? = null
        private var bansModule: BansModule? = null
        private var commandsModule: CommandsModule? = null
        private var playtimeTracker: PlaytimeTracker? = null
        private var permissionAdapter: PermissionAdapter? = null
        private var reconnectTask: ru.unicorecms.unicoreconnect.common.platform.Cancellable? = null

        val platform: ForgePlatformImpl by lazy {
            val logger = ForgeLogger()

            ForgePlatformImpl(
                { server },
                DelegatingPermissionAdapter { permissionAdapter },
                VanillaBanAdapter { server },
                ForgeItemBridge(logger),
            )
        }
    }
}
