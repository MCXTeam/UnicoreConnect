package ru.unicorecms.unicoreconnect.bukkit.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.MessageType
import co.aikar.commands.annotation.*
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import ru.unicorecms.unicoreconnect.bukkit.CommandManager
import ru.unicorecms.unicoreconnect.bukkit.PluginInstance
import ru.unicorecms.unicoreconnect.common.Permissions
import ru.unicorecms.unicoreconnect.common.UnicoreCommon
import ru.unicorecms.unicoreconnect.common.format.Formats


@CommandAlias("real|rbal")
class RealCommand : BaseCommand() {
    private val plugin = PluginInstance.plugin

    @Default
    @CommandPermission(Permissions.REAL)
    fun main(player: Player) = Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
        try {
            val resp = UnicoreCommon.moneyService.findReal(player.uniqueId)

            player.sendMessage(
                CommandManager.msg(
                    "unicoreconnect.command_real",
                    replacements = arrayOf(
                        "{real}",
                        Formats.real(resp.real),
                        "{virtual}",
                        Formats.money(resp.virtual)
                    )
                )
            )
        } catch (_: Exception) {
            player.sendMessage(CommandManager.msg("unicoreconnect.command_real_fail", type = MessageType.ERROR))
        }
    })
}
