package ru.unicorecms.unicoreconnect.common.types

import java.util.*

class UserPermission {
    var user: User = User()
    var permission: DonatePermission = DonatePermission()
    var server: Server = Server()
    var expired: Date? = null
}