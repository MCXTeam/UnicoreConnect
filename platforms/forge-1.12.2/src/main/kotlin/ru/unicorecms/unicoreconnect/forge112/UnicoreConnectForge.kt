package ru.unicorecms.unicoreconnect.forge112

import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.server.MinecraftServer
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.event.FMLServerStartedEvent
import net.minecraftforge.fml.common.event.FMLServerStartingEvent
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.PlayerEvent
import ru.unicorecms.unicoreconnect.common.SocketClient
import ru.unicorecms.unicoreconnect.common.UnicoreCommon
import ru.unicorecms.unicoreconnect.common.config.JsonConfigLoader
import ru.unicorecms.unicoreconnect.common.config.PermissionsConfig
import ru.unicorecms.unicoreconnect.common.events.EventDispatcher
import ru.unicorecms.unicoreconnect.common.modules.BansModule
import ru.unicorecms.unicoreconnect.common.modules.DonateModule
import ru.unicorecms.unicoreconnect.common.modules.EconomyModule
import ru.unicorecms.unicoreconnect.common.platform.Cancellable
import ru.unicorecms.unicoreconnect.common.platform.CommandPermissionAdapter
import ru.unicorecms.unicoreconnect.common.platform.DelegatingPermissionAdapter
import ru.unicorecms.unicoreconnect.common.platform.PermissionAdapter
import ru.unicorecms.unicoreconnect.forge112.commands.UnicoreCommands
import ru.unicorecms.unicoreconnect.forge112.tasks.PlaytimeTracker
import java.io.File

@Mod(modid = UnicoreConnectForge.MOD_ID, useMetadata = true, acceptableRemoteVersions = "*")
class UnicoreConnectForge {
    @Mod.EventHandler
    fun preInit(event: FMLPreInitializationEvent) {
        UnicoreCommon(JsonConfigLoader.load(File(event.modConfigurationDirectory, CONFIG_NAME)))
        ForgePermissions.register()
        MinecraftForge.EVENT_BUS.register(this)
    }

    @Mod.EventHandler
    fun serverStarting(event: FMLServerStartingEvent) {
        server = event.server

        UnicoreCommands.all(platform).forEach { event.registerServerCommand(it) }
    }

    @Mod.EventHandler
    fun serverStarted(event: FMLServerStartedEvent) {
        start()
    }

    @Mod.EventHandler
    fun serverStopping(event: FMLServerStoppingEvent) {
        stop()
        server = null
    }

    @SubscribeEvent
    fun onPlayerJoin(event: PlayerEvent.PlayerLoggedInEvent) {
        val player = event.player as? EntityPlayerMP ?: return

        donateModule?.handleJoin(player.uniqueID)
        bansModule?.handleJoin(player.uniqueID)
        playtimeTracker?.onJoin(player.uniqueID)
    }

    @SubscribeEvent
    fun onPlayerQuit(event: PlayerEvent.PlayerLoggedOutEvent) {
        val player = event.player as? EntityPlayerMP ?: return

        playtimeTracker?.onQuit(player.uniqueID)
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
        if (config.modules.bans) bansModule = BansModule(platform)
        if (config.modules.playtime) playtimeTracker = PlaytimeTracker(platform).also { it.start() }

        reconnectTask = platform.forgeScheduler.repeatSync(200, 60) { socketClient?.reconnectHandler() }
    }

    private fun stop() {
        economyModule?.unregister()
        reconnectTask?.cancel()
        playtimeTracker?.stop()
        socketClient?.close()
        EventDispatcher.clear()
        platform.forgeScheduler.shutdown()

        socketClient = null
        donateModule = null
        economyModule = null
        bansModule = null
        playtimeTracker = null
    }

    private fun choosePermissionAdapter(): PermissionAdapter? {
        if (!UnicoreCommon.config.modules.donate) return null

        val requested = UnicoreCommon.config.permissions.adapter
        val forgeEssentials = ForgePermissions.forgeEssentials

        return when {
            requested == forgeEssentials.id -> forgeEssentials
            requested == PermissionsConfig.ADAPTER_COMMANDS -> CommandPermissionAdapter(platform)
            forgeEssentials.available() -> forgeEssentials
            else -> CommandPermissionAdapter(platform)
        }
    }

    companion object {
        const val MOD_ID = "unicoreconnect"

        private const val CONFIG_NAME = "unicoreconnect.json"

        private var server: MinecraftServer? = null
        private var socketClient: SocketClient? = null
        private var donateModule: DonateModule? = null
        private var economyModule: EconomyModule? = null
        private var bansModule: BansModule? = null
        private var playtimeTracker: PlaytimeTracker? = null
        private var permissionAdapter: PermissionAdapter? = null
        private var reconnectTask: Cancellable? = null

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
