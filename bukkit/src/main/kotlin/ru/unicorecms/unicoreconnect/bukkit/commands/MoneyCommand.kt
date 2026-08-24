package ru.unicorecms.unicoreconnect.bukkit.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.MessageType
import co.aikar.commands.annotation.*
import co.aikar.commands.bukkit.contexts.OnlinePlayer
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ru.unicorecms.unicoreconnect.bukkit.CommandManager
import ru.unicorecms.unicoreconnect.bukkit.PluginInstance
import ru.unicorecms.unicoreconnect.common.Permissions
import ru.unicorecms.unicoreconnect.common.UnicoreCommon
import ru.unicorecms.unicoreconnect.common.format.Formats


@CommandAlias("money|bal|balance")
class MoneyCommand : BaseCommand() {
    private val plugin = PluginInstance.plugin

    private fun format(amount: Double): String = Formats.money(amount)

    @Default
    @CommandPermission(Permissions.MONEY)
    fun main(player: Player) = Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
        val resp = UnicoreCommon.moneyService.findOne(player.uniqueId)

        player.sendMessage(
            CommandManager.msg(
                "unicoreconnect.command_money",
                replacements = arrayOf(
                    "{server}",
                    UnicoreCommon.server!!.name,
                    "{money}",
                    format(resp.money)
                )
            )
        )
    })

    @Subcommand("pay")
    @Syntax("[player] [amount]")
    @CommandPermission(Permissions.MONEY_PAY)
    fun pay(player: Player, target: OnlinePlayer, amount: Double) = Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
        try {
            val resp = UnicoreCommon.moneyService.transfer(
                player.uniqueId,
                player.address?.address?.hostAddress ?: "127.0.0.1",
                target.player.uniqueId,
                amount
            )
            player.sendMessage(
                CommandManager.msg(
                    "unicoreconnect.command_money_pay",
                    replacements = arrayOf(
                        "{amount}",
                        format(amount),
                        "{money}",
                        format(resp.money),
                        "{target}",
                        target.player.name
                    )
                )
            )
            target.player.sendMessage(
                CommandManager.msg(
                    "unicoreconnect.command_money_pay_target",
                    replacements = arrayOf("{amount}", format(amount), "{player}", player.name)
                )
            )
        } catch (_: Exception) {
            player.sendMessage(CommandManager.msg("unicoreconnect.command_money_pay_fail", type = MessageType.ERROR))
        }
    })

    @Subcommand("top")
    @CommandPermission(Permissions.MONEY_TOP)
    fun top(sender: CommandSender) = Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
        val resp = UnicoreCommon.moneyService.top()
        sender.sendMessage(
            CommandManager.msg(
                "unicoreconnect.command_money_top",
                replacements = arrayOf(
                    "{server}",
                    UnicoreCommon.server!!.name,
                    "{rows}",
                    resp.mapIndexed { index, money ->
                        "${index + 1}.${money.user.username} - ${format(money.money)}"
                    }.joinToString("\n")
                )
            )
        )
    })
}