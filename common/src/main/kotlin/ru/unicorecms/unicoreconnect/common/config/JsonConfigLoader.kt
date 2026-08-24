package ru.unicorecms.unicoreconnect.common.config

import com.google.gson.GsonBuilder
import java.io.File

object JsonConfigLoader {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun load(file: File): UnicoreConfig {
        if (!file.exists()) {
            val defaults = UnicoreConfig().apply {
                server = "hitech"
                apiUrl = "http://127.0.0.1:5000"
                apiKey = "XXX"
            }

            file.parentFile?.mkdirs()
            file.writeText(gson.toJson(defaults))

            return defaults
        }

        val loaded = gson.fromJson(file.readText(), UnicoreConfig::class.java) ?: UnicoreConfig()

        loaded.modules = filled(loaded.modules) { ModulesConfig() }
        loaded.permissions = filled(loaded.permissions) { PermissionsConfig() }
        loaded.permissions.commands = filled(loaded.permissions.commands) { CommandTemplates() }
        loaded.messages = filled(loaded.messages) { MessagesConfig() }

        file.writeText(gson.toJson(loaded))

        return loaded
    }

    private fun <T : Any> filled(value: T?, fallback: () -> T): T = value ?: fallback()
}
