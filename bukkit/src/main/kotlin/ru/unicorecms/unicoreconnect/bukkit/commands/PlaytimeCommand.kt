package ru.unicorecms.unicoreconnect.bukkit.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Subcommand
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ru.unicorecms.unicoreconnect.common.Permissions
import ru.unicorecms.unicoreconnect.common.UnicoreCommon
import ru.unicorecms.unicoreconnect.common.format.Formats
import ru.unicorecms.unicoreconnect.bukkit.CommandManager
import ru.unicorecms.unicoreconnect.bukkit.PluginInstance

@CommandAlias("playtime|pt")
class PlaytimeCommand : BaseCommand() {
    private val plugin = PluginInstance.plugin

    @Default
    @CommandPermission(Permissions.PLAYTIME)
    fun main(player: Player) = Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
        val resp = UnicoreCommon.playtimeService.findOne(player.uniqueId)

        player.sendMessage(
            CommandManager.msg(
                "unicoreconnect.command_playtime",
                replacements = arrayOf(
                    "{server}",
                    UnicoreCommon.server!!.name,
                    "{time}",
                    Formats.duration(resp.time)
                )
            )
        )
    })

    @Subcommand("top")
    @CommandPermission(Permissions.PLAYTIME_TOP)
    fun top(sender: CommandSender) = Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
        val resp = UnicoreCommon.playtimeService.findTop()
        sender.sendMessage(
            CommandManager.msg(
                "unicoreconnect.command_playtime_top",
                replacements = arrayOf( "{server}", UnicoreCommon.server!!.name, "{rows}", resp.mapIndexed { index, playtime -> "${index + 1}.${playtime.user.username} - ${Formats.duration(playtime.time)}" }.joinToString("\n"))
            )
        )
    })
}