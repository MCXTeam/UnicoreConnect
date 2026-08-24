package ru.unicorecms.unicoreconnect.neoforge.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import ru.unicorecms.unicoreconnect.common.Permissions
import ru.unicorecms.unicoreconnect.common.UnicoreCommon
import ru.unicorecms.unicoreconnect.common.format.Formats
import ru.unicorecms.unicoreconnect.neoforge.NeoPlatformImpl
import ru.unicorecms.unicoreconnect.neoforge.NeoPlayer

object UnicoreCommands {
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>, platform: NeoPlatformImpl) {
        val config = UnicoreCommon.config

        if (config.modules.money) registerMoney(dispatcher, platform)
        if (config.modules.playtime) registerPlaytime(dispatcher, platform)
        if (config.modules.showcase) registerShowcase(dispatcher, platform)

        registerAdmin(dispatcher, platform)
    }

    private fun registerMoney(dispatcher: CommandDispatcher<CommandSourceStack>, platform: NeoPlatformImpl) {
        dispatcher.register(
            Commands.literal("money")
                .requires { permitted(it, platform, Permissions.MONEY) }
                .executes { context ->
                    val player = playerOf(context.source, platform) ?: return@executes 0

                    platform.scheduler.async {
                        runCatching { UnicoreCommon.moneyService.findOne(player.uuid) }
                            .onSuccess { player.sendMessage("Баланс на ${UnicoreCommon.server?.name}: ${Formats.money(it.money)}") }
                            .onFailure { player.sendMessage("Баланс получить не удалось") }
                    }

                    1
                }
                .then(
                    Commands.literal("top")
                        .requires { permitted(it, platform, Permissions.MONEY_TOP) }
                        .executes { context ->
                            val source = context.source

                            platform.scheduler.async {
                                runCatching { UnicoreCommon.moneyService.top() }
                                    .onSuccess { top ->
                                        val rows = top.mapIndexed { index, money ->
                                            "${index + 1}. ${money.user.username} — ${Formats.money(money.money)}"
                                        }.joinToString("\n")

                                        platform.scheduler.sync { source.source.sendSystemMessage(Component.literal(rows)) }
                                    }
                                    .onFailure {
                                        platform.scheduler.sync { source.source.sendSystemMessage(Component.literal("Топ получить не удалось")) }
                                    }
                            }

                            1
                        }
                )
                .then(
                    Commands.literal("pay")
                        .requires { permitted(it, platform, Permissions.MONEY_PAY) }
                        .then(
                            Commands.argument("player", EntityArgument.player())
                                .then(
                                    Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                                        .executes { context ->
                                            val sender = playerOf(context.source, platform) ?: return@executes 0
                                            val target = EntityArgument.getPlayer(context, "player")
                                            val amount = DoubleArgumentType.getDouble(context, "amount")

                                            platform.scheduler.async {
                                                runCatching {
                                                    UnicoreCommon.moneyService.transfer(
                                                        sender.uuid,
                                                        sender.address ?: "127.0.0.1",
                                                        target.uuid,
                                                        amount,
                                                    )
                                                }
                                                    .onSuccess { money ->
                                                        sender.sendMessage(
                                                            "Отправлено ${Formats.money(amount)}, остаток ${Formats.money(money.money)}"
                                                        )
                                                        platform.player(target.uuid)
                                                            ?.sendMessage("Игрок ${sender.name} перевёл вам ${Formats.money(amount)}")
                                                    }
                                                    .onFailure { sender.sendMessage("Перевод не прошёл") }
                                            }

                                            1
                                        }
                                )
                        )
                )
        )
    }

    private fun registerPlaytime(dispatcher: CommandDispatcher<CommandSourceStack>, platform: NeoPlatformImpl) {
        dispatcher.register(
            Commands.literal("playtime")
                .requires { permitted(it, platform, Permissions.PLAYTIME) }
                .executes { context ->
                    val player = playerOf(context.source, platform) ?: return@executes 0

                    platform.scheduler.async {
                        runCatching { UnicoreCommon.playtimeService.findOne(player.uuid) }
                            .onSuccess { player.sendMessage("В игре на ${UnicoreCommon.server?.name}: ${Formats.duration(it.time)}") }
                            .onFailure { player.sendMessage("Время получить не удалось") }
                    }

                    1
                }
                .then(
                    Commands.literal("top")
                        .requires { permitted(it, platform, Permissions.PLAYTIME_TOP) }
                        .executes { context ->
                            val source = context.source

                            platform.scheduler.async {
                                runCatching { UnicoreCommon.playtimeService.findTop() }
                                    .onSuccess { top ->
                                        val rows = top.mapIndexed { index, playtime ->
                                            "${index + 1}. ${playtime.user.username} — ${Formats.duration(playtime.time)}"
                                        }.joinToString("\n")

                                        platform.scheduler.sync { source.source.sendSystemMessage(Component.literal(rows)) }
                                    }
                                    .onFailure {
                                        platform.scheduler.sync { source.source.sendSystemMessage(Component.literal("Топ получить не удалось")) }
                                    }
                            }

                            1
                        }
                )
        )
    }

    private fun registerShowcase(dispatcher: CommandDispatcher<CommandSourceStack>, platform: NeoPlatformImpl) {
        dispatcher.register(
            Commands.literal("cart")
                .requires { permitted(it, platform, Permissions.SHOWCASE_LIST) }
                .executes { context -> listWarehouse(context.source, platform) }
                .then(Commands.literal("list").executes { context -> listWarehouse(context.source, platform) })
                .then(
                    Commands.literal("all")
                        .requires { permitted(it, platform, Permissions.SHOWCASE_ALL) }
                        .executes { context -> giveWarehouse(context.source, platform, null) }
                )
                .then(
                    Commands.literal("give")
                        .requires { permitted(it, platform, Permissions.SHOWCASE_GIVE) }
                        .then(
                            Commands.argument("id", IntegerArgumentType.integer(1))
                                .executes { context ->
                                    giveWarehouse(context.source, platform, IntegerArgumentType.getInteger(context, "id"))
                                }
                        )
                )
                .then(
                    Commands.literal("create")
                        .requires { permitted(it, platform, Permissions.SHOWCASE_CREATE) }
                        .then(
                            Commands.argument("price", DoubleArgumentType.doubleArg(0.0))
                                .then(
                                    Commands.argument("name", StringArgumentType.greedyString())
                                        .executes { context ->
                                            val player = playerOf(context.source, platform) ?: return@executes 0
                                            val price = DoubleArgumentType.getDouble(context, "price")
                                            val name = StringArgumentType.getString(context, "name")
                                            val described = platform.items?.describeHeldItem(player, name, price)

                                            if (described == null) {
                                                player.sendMessage("Возьмите предмет в руку")
                                                return@executes 0
                                            }

                                            platform.scheduler.async {
                                                runCatching {
                                                    UnicoreCommon.showcaseService.create(
                                                        ru.unicorecms.unicoreconnect.common.types.StoreRequest(
                                                            described.itemId,
                                                            described.name,
                                                            described.nbt,
                                                            described.price,
                                                        )
                                                    )
                                                }
                                                    .onSuccess { player.sendMessage("Товар добавлен в магазин") }
                                                    .onFailure { player.sendMessage("Товар добавить не удалось") }
                                            }

                                            1
                                        }
                                )
                        )
                )
        )
    }

    private fun registerAdmin(dispatcher: CommandDispatcher<CommandSourceStack>, platform: NeoPlatformImpl) {
        dispatcher.register(
            Commands.literal("unicoreconnect")
                .requires { it.hasPermission(3) || permitted(it, platform, Permissions.SYNC) }
                .then(
                    Commands.literal("sync").executes { context ->
                        val source = context.source

                        platform.scheduler.async {
                            runCatching {
                                UnicoreCommon.donateGroupService.load()
                                UnicoreCommon.donatePermissionService.load()
                            }
                                .onSuccess {
                                    platform.scheduler.sync {
                                        source.source.sendSystemMessage(Component.literal("Донат-группы и права перезагружены"))
                                    }
                                }
                                .onFailure {
                                    platform.scheduler.sync {
                                        source.source.sendSystemMessage(Component.literal("Синхронизация не удалась"))
                                    }
                                }
                        }

                        1
                    }
                )
        )
    }

    private fun listWarehouse(source: CommandSourceStack, platform: NeoPlatformImpl): Int {
        val player = playerOf(source, platform) ?: return 0

        platform.scheduler.async {
            runCatching { UnicoreCommon.showcaseService.find(player.uuid) }
                .onSuccess { items ->
                    if (items.isEmpty()) {
                        player.sendMessage("Склад пуст")
                        return@onSuccess
                    }

                    val rows = items.joinToString("\n") { "#${it.id} ${it.product.name} x${it.amount}" }

                    player.sendMessage(rows)
                }
                .onFailure { player.sendMessage("Склад получить не удалось") }
        }

        return 1
    }

    private fun giveWarehouse(source: CommandSourceStack, platform: NeoPlatformImpl, id: Int?): Int {
        val player = playerOf(source, platform) ?: return 0
        val items = platform.items ?: return 0

        platform.scheduler.async {
            runCatching {
                val warehouse = UnicoreCommon.showcaseService.find(player.uuid)
                val selected = if (id == null) warehouse.toList() else warehouse.filter { it.id == id }

                if (selected.isEmpty()) {
                    player.sendMessage("Ничего не найдено")
                    return@runCatching
                }

                val given = arrayListOf<ru.unicorecms.unicoreconnect.common.types.WarehouseItem>()

                platform.scheduler.sync {
                    selected.forEach { item ->
                        val itemId = item.product.item_id ?: return@forEach

                        repeat(item.amount) {
                            if (items.give(player, itemId, item.product.nbt, 1)) given.add(item)
                        }
                    }

                    platform.scheduler.async {
                        runCatching { UnicoreCommon.showcaseService.gived(given) }
                            .onSuccess { player.sendMessage("Выдано предметов: ${given.size}") }
                            .onFailure { player.sendMessage("Склад обновить не удалось") }
                    }
                }
            }.onFailure { player.sendMessage("Выдача не удалась") }
        }

        return 1
    }

    private fun playerOf(source: CommandSourceStack, platform: NeoPlatformImpl): NeoPlayer? {
        val handle = source.entity as? net.minecraft.server.level.ServerPlayer

        if (handle == null) {
            source.source.sendSystemMessage(Component.literal("Команда доступна только игроку"))
            return null
        }

        return NeoPlayer(handle)
    }

    private fun permitted(source: CommandSourceStack, platform: NeoPlatformImpl, node: String): Boolean {
        val handle = source.entity as? net.minecraft.server.level.ServerPlayer ?: return source.hasPermission(2)
        val adapter = platform.permissions

        return adapter?.has(handle.uuid, node) ?: source.hasPermission(0)
    }
}
