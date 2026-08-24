package ru.unicorecms.unicoreconnect.common.types

class User {
    var uuid: String = ""
    var username: String = ""
    var perms: List<String> = emptyList()
    var ban: Ban? = null
}