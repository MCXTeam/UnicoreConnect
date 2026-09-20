package ru.unicorecms.unicoreconnect.neoforge

import net.minecraft.server.MinecraftServer
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.server.ServerStartedEvent
import net.neoforged.neoforge.event.server.ServerStoppingEvent
import net.neoforged.fml.common.Mod
import net.neoforged.fml.loading.FMLPaths
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
import ru.unicorecms.unicoreconnect.neoforge.commands.UnicoreCommands
import ru.unicorecms.unicoreconnect.neoforge.tasks.PlaytimeTracker

@Mod(UnicoreConnectNeoForge.MOD_ID)
class UnicoreConnectNeoForge {
    init {
        loadConfig()
        NeoForge.EVENT_BUS.register(this)
    }

    private fun loadConfig() {
        val configFile = FMLPaths.CONFIGDIR.get().resolve("unicoreconnect.json").toFile()

        UnicoreCommon(JsonConfigLoader.load(configFile))
    }

    @net.neoforged.bus.api.SubscribeEvent
    fun onServerStarted(event: ServerStartedEvent) {
        server = event.server
        start()
    }

    @net.neoforged.bus.api.SubscribeEvent
    fun onServerStopping(event: ServerStoppingEvent) {
        stop()
        server = null
    }

    @net.neoforged.bus.api.SubscribeEvent
    fun onCommands(event: RegisterCommandsEvent) {
        UnicoreCommands.register(event.dispatcher, platform)
    }

    @net.neoforged.bus.api.SubscribeEvent
    fun onPlayerJoin(event: PlayerEvent.PlayerLoggedInEvent) {
        val player = event.entity as? ServerPlayer ?: return

        donateModule?.handleJoin(player.uuid)
        bansModule?.handleJoin(player.uuid)
        playtimeTracker?.onJoin(player.uuid)
    }

    @net.neoforged.bus.api.SubscribeEvent
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

        reconnectTask = platform.neoScheduler.repeatSync(200, 60) { socketClient?.reconnectHandler() }
    }

    private fun stop() {
        economyModule?.unregister()
        reconnectTask?.cancel()
        playtimeTracker?.stop()
        socketClient?.close()
        commandsModule?.stop()
        EventDispatcher.clear()
        platform.neoScheduler.shutdown()

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

        val platform: NeoPlatformImpl by lazy {
            val logger = NeoLogger()

            NeoPlatformImpl(
                { server },
                DelegatingPermissionAdapter { permissionAdapter },
                VanillaBanAdapter { server },
                NeoItemBridge(logger),
            )
        }
    }
}
