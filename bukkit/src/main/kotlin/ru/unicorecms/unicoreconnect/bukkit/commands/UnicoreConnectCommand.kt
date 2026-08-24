package ru.unicorecms.unicoreconnect.bukkit.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Subcommand
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import ru.unicorecms.unicoreconnect.bukkit.PluginInstance
import ru.unicorecms.unicoreconnect.common.Permissions
import ru.unicorecms.unicoreconnect.common.UnicoreCommon
import ru.unicorecms.unicoreconnect.common.services.donate.DonateGroupService
import ru.unicorecms.unicoreconnect.common.services.donate.DonatePermissionService

@CommandAlias("unicoreconnect|uc")
class UnicoreConnectCommand : BaseCommand() {
    private val plugin = PluginInstance.plugin

    @CommandPermission(Permissions.SYNC)
    @Subcommand("sync")
    fun sync (sender: CommandSender) = Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
        try {
            sender.sendMessage("Syncing UnicoreCMS donate-groups...")
            sender.sendMessage("Syncing UnicoreCMS donate-perms...")

            UnicoreCommon.donateGroupService.load()
            UnicoreCommon.donatePermissionService.load()

            sender.sendMessage("Loaded ${DonateGroupService.groups.size} donate-groups from API")
            sender.sendMessage("Loaded ${DonatePermissionService.permissions.size} donate-perms from API")
        } catch (e: Exception) {
            sender.sendMessage("Error getting group list! $e")
        }
    })
}