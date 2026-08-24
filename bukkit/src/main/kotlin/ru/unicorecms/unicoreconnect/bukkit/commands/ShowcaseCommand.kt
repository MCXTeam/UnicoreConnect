package ru.unicorecms.unicoreconnect.bukkit.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.MessageType
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Subcommand
import co.aikar.commands.annotation.Syntax
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import ru.unicorecms.unicoreconnect.bukkit.utils.ItemMagic
import ru.unicorecms.unicoreconnect.common.Permissions
import ru.unicorecms.unicoreconnect.common.UnicoreCommon
import ru.unicorecms.unicoreconnect.common.types.WarehouseItem
import ru.unicorecms.unicoreconnect.bukkit.CommandManager
import ru.unicorecms.unicoreconnect.bukkit.PluginInstance
import ru.unicorecms.unicoreconnect.common.types.GiveMethod

@CommandAlias("showcase|cart")
class ShowcaseCommand : BaseCommand() {
    private val itemMagic = ItemMagic()
    private val plugin = PluginInstance.plugin

    @Subcommand("create")
    @Syntax("[price] [name]")
    @CommandPermission(Permissions.SHOWCASE_CREATE)
    fun create(player: Player, price: Double, name: String) = Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
        val data = itemMagic.serialize(player.inventory.itemInHand, name, price)
        val req = UnicoreCommon.showcaseService.create(data)

        if (req)
            player.sendMessage(CommandManager.msg("unicoreconnect.command_showcase_create", replacements = arrayOf("{id}", data.id)))
        else
            throw Exception()
    })

    @Subcommand("all")
    @CommandPermission(Permissions.SHOWCASE_ALL)
    fun all(player: Player) = Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
        val gived = arrayListOf<WarehouseItem>()
        val req = UnicoreCommon.showcaseService.find(player.uniqueId)
        val virtualInventory = Bukkit.createInventory(null, InventoryType.PLAYER)
        virtualInventory.contents = player.inventory.contents

        if (req.isEmpty())
            return@Runnable player.sendMessage(CommandManager.msg("unicoreconnect.command_showcase_all_fail", type = MessageType.ERROR))

        for (item in req) {
            when (item.product.give_method) {
                GiveMethod.UnicoreConnect -> {
                    val itemStack = itemMagic.serialize(item)

                    if (itemStack != null) {
                        for (i in 1..item.amount) {
                            if (virtualInventory.addItem(itemStack).size == 0)
                                gived.add(item)
                        }
                    }
                }
                GiveMethod.UnicoreConnectCommand -> {
                    for (command in item.product.commands!!) {
                        val dispatchCommand = command
                            .replace("{user.username}", player.name)
                            .replace("{product.name}", item.product.name!!)
                            .replace("{product.amount}", item.amount.toString())
                            .replace("{server.id}", UnicoreCommon.server!!.id)
                            .replace("{server.name}", UnicoreCommon.server!!.name)

                        if (plugin.server.dispatchCommand(plugin.server.consoleSender, dispatchCommand))
                            gived.add(item)
                    }
                }
                else -> {}
            }

        }

        UnicoreCommon.showcaseService.gived(gived)
        player.inventory.contents = virtualInventory.contents
        player.sendMessage(CommandManager.msg("unicoreconnect.command_showcase", replacements = arrayOf("{amount}", gived.size.toString())))
    })

    @Subcommand("list")
    @CommandPermission(Permissions.SHOWCASE_LIST)
    fun list(player: Player) = Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
        val req = UnicoreCommon.showcaseService.find(player.uniqueId)

        if (req.isEmpty())
            return@Runnable player.sendMessage(CommandManager.msg("unicoreconnect.command_showcase_all_fail", type = MessageType.ERROR))

        player.sendMessage(CommandManager.msg("unicoreconnect.command_showcase_list", replacements = arrayOf("{items}", req.joinToString(
            "\n"
        ) { "${it.product.name} x${it.amount} (#${it.id})" })))
    })

    @Syntax("[id]")
    @Subcommand("give")
    @CommandPermission(Permissions.SHOWCASE_GIVE)
    fun give(player: Player, id: Int) = Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
        val gived = arrayListOf<WarehouseItem>()
        val req = UnicoreCommon.showcaseService.find(player.uniqueId).filter { it.id == id }
        val virtualInventory = Bukkit.createInventory(null, InventoryType.PLAYER)
        virtualInventory.contents = player.inventory.contents

        if (req.isEmpty())
            return@Runnable player.sendMessage(CommandManager.msg("unicoreconnect.command_showcase_give_fail", replacements = arrayOf("{id}", id.toString()), type = MessageType.ERROR))

        for (item in req) {
            when (item.product.give_method) {
                GiveMethod.UnicoreConnect -> {
                    val itemStack = itemMagic.serialize(item)

                    if (itemStack != null) {
                        for (i in 1..item.amount) {
                            if (virtualInventory.addItem(itemStack).size == 0)
                                gived.add(item)
                        }
                    }
                }
                GiveMethod.UnicoreConnectCommand -> {
                    for (command in item.product.commands!!) {
                        val dispatchCommand = command
                                .replace("{user.username}", player.name)
                                .replace("{product.name}", item.product.name!!)
                                .replace("{product.amount}", item.amount.toString())
                                .replace("{server.id}", UnicoreCommon.server!!.id)
                                .replace("{server.name}", UnicoreCommon.server!!.name)

                        if (plugin.server.dispatchCommand(plugin.server.consoleSender, dispatchCommand))
                            gived.add(item)
                    }
                }
                else -> {}
            }
        }

        UnicoreCommon.showcaseService.gived(gived)
        player.inventory.contents = virtualInventory.contents
        player.sendMessage(CommandManager.msg("unicoreconnect.command_showcase", replacements = arrayOf("{amount}", gived.size.toString())))
    })
}
