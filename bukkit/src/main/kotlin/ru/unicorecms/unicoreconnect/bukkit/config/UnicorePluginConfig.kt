package ru.unicorecms.unicoreconnect.bukkit.config

import org.bukkit.configuration.file.FileConfiguration
import ru.unicorecms.unicoreconnect.bukkit.PluginInstance
import ru.unicorecms.unicoreconnect.common.UnicoreCommon
import ru.unicorecms.unicoreconnect.common.config.PermissionsConfig
import ru.unicorecms.unicoreconnect.common.config.UnicoreConfig

class UnicorePluginConfig {
    private val plugin = PluginInstance.plugin
    private val fileConfig: FileConfiguration = plugin.config
    private val config = UnicoreConfig()

    private fun setDefaultValues() {
        fileConfig.options().header("UnicoreConnect - конфигурация плагина")
        fileConfig.addDefault("server", "hitech")
        fileConfig.addDefault("api.url", "http://127.0.0.1:5000")
        fileConfig.addDefault("api.key", "XXX")
        fileConfig.addDefault("items_mapping", UnicoreCommon.itemsMapDefault)

        fileConfig.addDefault("modules.money", config.modules.money)
        fileConfig.addDefault("modules.playtime", config.modules.playtime)
        fileConfig.addDefault("modules.showcase", config.modules.showcase)
        fileConfig.addDefault("modules.donate", config.modules.donate)
        fileConfig.addDefault("modules.bans", config.modules.bans)
        fileConfig.addDefault("modules.chunkloaders", config.modules.chunkloaders)

        fileConfig.addDefault("permissions.adapter", config.permissions.adapter)
        fileConfig.addDefault("permissions.commands.group_add", config.permissions.commands.groupAdd)
        fileConfig.addDefault("permissions.commands.group_add_temp", config.permissions.commands.groupAddTemp)
        fileConfig.addDefault("permissions.commands.group_remove", config.permissions.commands.groupRemove)
        fileConfig.addDefault("permissions.commands.permission_set", config.permissions.commands.permissionSet)
        fileConfig.addDefault("permissions.commands.permission_set_temp", config.permissions.commands.permissionSetTemp)
        fileConfig.addDefault("permissions.commands.permission_unset", config.permissions.commands.permissionUnset)

        save()
    }

    private fun save() {
        fileConfig.options().copyDefaults(true)
        plugin.saveConfig()
    }

    init {
        setDefaultValues()

        config.server = fileConfig.getString("server").orEmpty()
        config.apiUrl = fileConfig.getString("api.url").orEmpty()
        config.apiKey = fileConfig.getString("api.key").orEmpty()

        config.modules.money = fileConfig.getBoolean("modules.money", config.modules.money)
        config.modules.playtime = fileConfig.getBoolean("modules.playtime", config.modules.playtime)
        config.modules.showcase = fileConfig.getBoolean("modules.showcase", config.modules.showcase)
        config.modules.donate = fileConfig.getBoolean("modules.donate", config.modules.donate)
        config.modules.bans = fileConfig.getBoolean("modules.bans", config.modules.bans)
        config.modules.chunkloaders = fileConfig.getBoolean("modules.chunkloaders", config.modules.chunkloaders)

        config.permissions.adapter = fileConfig.getString("permissions.adapter") ?: PermissionsConfig.ADAPTER_AUTO
        config.permissions.commands.groupAdd = template("group_add", config.permissions.commands.groupAdd)
        config.permissions.commands.groupAddTemp = template("group_add_temp", config.permissions.commands.groupAddTemp)
        config.permissions.commands.groupRemove = template("group_remove", config.permissions.commands.groupRemove)
        config.permissions.commands.permissionSet = template("permission_set", config.permissions.commands.permissionSet)
        config.permissions.commands.permissionSetTemp = template("permission_set_temp", config.permissions.commands.permissionSetTemp)
        config.permissions.commands.permissionUnset = template("permission_unset", config.permissions.commands.permissionUnset)

        val mapItem = fileConfig.getConfigurationSection("items_mapping")

        mapItem?.getKeys(false)?.forEach { key -> UnicoreCommon.itemsMap[key] = mapItem[key].toString() }
    }

    private fun template(key: String, fallback: String): String = fileConfig.getString("permissions.commands.$key") ?: fallback

    fun get(): UnicoreConfig = config
}
