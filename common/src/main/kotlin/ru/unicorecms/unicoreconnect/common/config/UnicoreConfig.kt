package ru.unicorecms.unicoreconnect.common.config

class UnicoreConfig {
    lateinit var server: String
    lateinit var apiUrl: String
    lateinit var apiKey: String

    var modules = ModulesConfig()
    var permissions = PermissionsConfig()
    var messages = MessagesConfig()
}

class ModulesConfig {
    var money = true
    var playtime = true
    var showcase = true
    var donate = true
    var bans = true
    var chunkloaders = true
}

class PermissionsConfig {
    var adapter = ADAPTER_AUTO
    var commands = CommandTemplates()

    companion object {
        const val ADAPTER_AUTO = "auto"
        const val ADAPTER_COMMANDS = "commands"
    }
}

class CommandTemplates {
    var groupAdd = "lp user {user.uuid} parent add {group.ingame_id}"
    var groupAddTemp = "lp user {user.uuid} parent addtemp {group.ingame_id} {period.duration} accumulate"
    var groupRemove = "lp user {user.uuid} parent remove {group.ingame_id}"
    var permissionSet = "lp user {user.uuid} permission set {permission.node} true"
    var permissionSetTemp = "lp user {user.uuid} permission settemp {permission.node} true {period.duration} accumulate"
    var permissionUnset = "lp user {user.uuid} permission unset {permission.node}"
}

class MessagesConfig {
    var giveGroup = "Донат-группа «{name}» выдана"
    var takeGroup = "Донат-группа «{name}» снята"
    var givePermission = "Донат-право «{name}» выдано"
    var takePermission = "Донат-право «{name}» снято"

    fun of(key: String): String? = when (key) {
        KEY_GIVE_GROUP -> giveGroup
        KEY_TAKE_GROUP -> takeGroup
        KEY_GIVE_PERMISSION -> givePermission
        KEY_TAKE_PERMISSION -> takePermission
        else -> null
    }

    companion object {
        const val KEY_GIVE_GROUP = "unicoreconnect.event_give_group"
        const val KEY_TAKE_GROUP = "unicoreconnect.event_take_group"
        const val KEY_GIVE_PERMISSION = "unicoreconnect.event_give_permission"
        const val KEY_TAKE_PERMISSION = "unicoreconnect.event_take_permission"
        const val PLACEHOLDER_NAME = "{name}"
    }
}
