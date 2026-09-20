package ru.unicorecms.unicoreconnect.forge17.commands

import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.util.ChatComponentText
import ru.unicorecms.unicoreconnect.common.Permissions
import ru.unicorecms.unicoreconnect.common.UnicoreCommon
import ru.unicorecms.unicoreconnect.common.format.Formats
import ru.unicorecms.unicoreconnect.common.types.StoreRequest
import ru.unicorecms.unicoreconnect.common.types.WarehouseItem
import ru.unicorecms.unicoreconnect.forge17.ForgePermissions
import ru.unicorecms.unicoreconnect.forge17.ForgePlatformImpl
import ru.unicorecms.unicoreconnect.forge17.ForgePlayer

abstract class UnicoreCommand(
    protected val platform: ForgePlatformImpl,
    private val name: String,
    private val usage: String,
    private val node: String,
) : CommandBase() {
    override fun getCommandName() = name

    override fun getCommandUsage(sender: ICommandSender) = usage

    override fun canCommandSenderUseCommand(sender: ICommandSender) = permitted(sender, node)

    protected fun permitted(sender: ICommandSender, node: String): Boolean {
        val handle = sender as? EntityPlayerMP ?: return sender.canCommandSenderUseCommand(2, name)

        return platform.permissions?.has(handle.uniqueID, node) ?: ForgePermissions.check(handle, node)
    }

    protected fun requires(sender: ICommandSender, node: String): Boolean {
        if (permitted(sender, node)) return true

        reply(sender, "Нет права «$node»")

        return false
    }

    protected fun playerOf(sender: ICommandSender): ForgePlayer? {
        val handle = sender as? EntityPlayerMP

        if (handle == null) {
            reply(sender, "Команда доступна только игроку")
            return null
        }

        return platform.wrap(handle)
    }

    protected fun reply(sender: ICommandSender, message: String) {
        platform.scheduler.sync { sender.addChatMessage(ChatComponentText(message)) }
    }
}

class MoneyCommand(platform: ForgePlatformImpl) :
    UnicoreCommand(platform, "money", "/money [top|pay <ник> <сумма>]", Permissions.MONEY) {
    override fun processCommand(sender: ICommandSender, args: Array<String>) {
        when (args.firstOrNull()) {
            null -> balance(sender)
            "top" -> top(sender)
            "pay" -> pay(sender, args)
            else -> reply(sender, getCommandUsage(sender))
        }
    }

    private fun balance(sender: ICommandSender) {
        val player = playerOf(sender) ?: return

        platform.scheduler.async {
            runCatching { UnicoreCommon.moneyService.findOne(player.uuid) }
                .onSuccess { player.sendMessage("Баланс на ${UnicoreCommon.server?.name}: ${Formats.money(it.money)}") }
                .onFailure { player.sendMessage("Баланс получить не удалось") }
        }
    }

    private fun top(sender: ICommandSender) {
        if (!requires(sender, Permissions.MONEY_TOP)) return

        platform.scheduler.async {
            runCatching { UnicoreCommon.moneyService.top() }
                .onSuccess { top ->
                    val rows = top.mapIndexed { index, money ->
                        "${index + 1}. ${money.user.username} — ${Formats.money(money.money)}"
                    }.joinToString("\n")

                    reply(sender, rows)
                }
                .onFailure { reply(sender, "Топ получить не удалось") }
        }
    }

    private fun pay(sender: ICommandSender, args: Array<String>) {
        if (!requires(sender, Permissions.MONEY_PAY)) return

        val sendingPlayer = playerOf(sender) ?: return

        if (args.size < 3) {
            reply(sender, getCommandUsage(sender))
            return
        }

        val target = platform.player(args[1])
        val amount = args[2].toDoubleOrNull()

        if (target == null) {
            reply(sender, "Игрок «${args[1]}» не в сети")
            return
        }

        if (amount == null || amount <= 0) {
            reply(sender, "Сумма должна быть числом больше нуля")
            return
        }

        val targetUuid = target.uuid

        platform.scheduler.async {
            runCatching {
                UnicoreCommon.moneyService.transfer(
                    sendingPlayer.uuid,
                    sendingPlayer.address ?: LOCAL_ADDRESS,
                    targetUuid,
                    amount,
                )
            }
                .onSuccess { money ->
                    sendingPlayer.sendMessage("Отправлено ${Formats.money(amount)}, остаток ${Formats.money(money.money)}")
                    platform.player(targetUuid)
                        ?.sendMessage("Игрок ${sendingPlayer.name} перевёл вам ${Formats.money(amount)}")
                }
                .onFailure { sendingPlayer.sendMessage("Перевод не прошёл") }
        }
    }

    companion object {
        private const val LOCAL_ADDRESS = "127.0.0.1"
    }
}

class RealCommand(platform: ForgePlatformImpl) :
    UnicoreCommand(platform, "real", "/real", Permissions.REAL) {
    override fun processCommand(sender: ICommandSender, args: Array<String>) {
        val player = playerOf(sender) ?: return

        platform.scheduler.async {
            runCatching { UnicoreCommon.moneyService.findReal(player.uuid) }
                .onSuccess {
                    player.sendMessage("Баланс на сайте: ${Formats.real(it.real)}, бонусы: ${Formats.money(it.virtual)}")
                }
                .onFailure { player.sendMessage("Баланс на сайте получить не удалось") }
        }
    }
}

class PlaytimeCommand(platform: ForgePlatformImpl) :
    UnicoreCommand(platform, "playtime", "/playtime [top]", Permissions.PLAYTIME) {
    override fun processCommand(sender: ICommandSender, args: Array<String>) {
        when (args.firstOrNull()) {
            null -> own(sender)
            "top" -> top(sender)
            else -> reply(sender, getCommandUsage(sender))
        }
    }

    private fun own(sender: ICommandSender) {
        val player = playerOf(sender) ?: return

        platform.scheduler.async {
            runCatching { UnicoreCommon.playtimeService.findOne(player.uuid) }
                .onSuccess { player.sendMessage("В игре на ${UnicoreCommon.server?.name}: ${Formats.duration(it.time)}") }
                .onFailure { player.sendMessage("Время получить не удалось") }
        }
    }

    private fun top(sender: ICommandSender) {
        if (!requires(sender, Permissions.PLAYTIME_TOP)) return

        platform.scheduler.async {
            runCatching { UnicoreCommon.playtimeService.findTop() }
                .onSuccess { top ->
                    val rows = top.mapIndexed { index, playtime ->
                        "${index + 1}. ${playtime.user.username} — ${Formats.duration(playtime.time)}"
                    }.joinToString("\n")

                    reply(sender, rows)
                }
                .onFailure { reply(sender, "Топ получить не удалось") }
        }
    }
}

class CartCommand(platform: ForgePlatformImpl) :
    UnicoreCommand(platform, "cart", "/cart [list|all|give <номер>|create <цена> <название>]", Permissions.SHOWCASE_LIST) {
    override fun processCommand(sender: ICommandSender, args: Array<String>) {
        when (args.firstOrNull()) {
            null, "list" -> list(sender)
            "all" -> give(sender, null)
            "give" -> give(sender, args.getOrNull(1)?.toIntOrNull())
            "create" -> create(sender, args)
            else -> reply(sender, getCommandUsage(sender))
        }
    }

    private fun list(sender: ICommandSender) {
        val player = playerOf(sender) ?: return

        platform.scheduler.async {
            runCatching { UnicoreCommon.showcaseService.find(player.uuid) }
                .onSuccess { items ->
                    if (items.isEmpty()) {
                        player.sendMessage("Склад пуст")
                        return@onSuccess
                    }

                    player.sendMessage(items.joinToString("\n") { "#${it.id} ${it.product.name} x${it.amount}" })
                }
                .onFailure { player.sendMessage("Склад получить не удалось") }
        }
    }

    private fun give(sender: ICommandSender, id: Int?) {
        val node = if (id == null) Permissions.SHOWCASE_ALL else Permissions.SHOWCASE_GIVE

        if (!requires(sender, node)) return

        val player = playerOf(sender) ?: return
        val items = platform.items ?: return

        platform.scheduler.async {
            runCatching {
                val warehouse = UnicoreCommon.showcaseService.find(player.uuid)
                val selected = if (id == null) warehouse.toList() else warehouse.filter { it.id == id }

                if (selected.isEmpty()) {
                    player.sendMessage("Ничего не найдено")
                    return@runCatching
                }

                val given = arrayListOf<WarehouseItem>()

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
    }

    private fun create(sender: ICommandSender, args: Array<String>) {
        if (!requires(sender, Permissions.SHOWCASE_CREATE)) return

        val player = playerOf(sender) ?: return
        val price = args.getOrNull(1)?.toDoubleOrNull()
        val name = args.drop(2).joinToString(" ")

        if (price == null || name.isBlank()) {
            reply(sender, getCommandUsage(sender))
            return
        }

        val described = platform.items?.describeHeldItem(player, name, price)

        if (described == null) {
            player.sendMessage("Возьмите предмет в руку")
            return
        }

        platform.scheduler.async {
            runCatching {
                UnicoreCommon.showcaseService.create(
                    StoreRequest(described.itemId, described.name, described.nbt, described.price)
                )
            }
                .onSuccess { player.sendMessage("Товар добавлен в магазин") }
                .onFailure { player.sendMessage("Товар добавить не удалось") }
        }
    }
}

class AdminCommand(platform: ForgePlatformImpl) :
    UnicoreCommand(platform, "unicoreconnect", "/unicoreconnect sync", Permissions.SYNC) {
    override fun processCommand(sender: ICommandSender, args: Array<String>) {
        if (args.firstOrNull() != "sync") {
            reply(sender, getCommandUsage(sender))
            return
        }

        platform.scheduler.async {
            runCatching {
                UnicoreCommon.donateGroupService.load()
                UnicoreCommon.donatePermissionService.load()
            }
                .onSuccess { reply(sender, "Донат-группы и права перезагружены") }
                .onFailure { reply(sender, "Синхронизация не удалась") }
        }
    }
}

object UnicoreCommands {
    fun all(platform: ForgePlatformImpl): List<UnicoreCommand> {
        val config = UnicoreCommon.config
        val commands = arrayListOf<UnicoreCommand>()

        if (config.modules.money) commands.add(MoneyCommand(platform))
        if (config.modules.money) commands.add(RealCommand(platform))
        if (config.modules.playtime) commands.add(PlaytimeCommand(platform))
        if (config.modules.showcase) commands.add(CartCommand(platform))

        commands.add(AdminCommand(platform))

        return commands
    }
}
