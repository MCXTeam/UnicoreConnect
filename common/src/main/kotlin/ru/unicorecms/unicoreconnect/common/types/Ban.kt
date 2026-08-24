package ru.unicorecms.unicoreconnect.common.types

import java.util.Date

class Ban {
    var user: User = User()
    var actor: User? = null
    var reason: String = ""

    var expires: Date? = null
    var created: Date? = null
}