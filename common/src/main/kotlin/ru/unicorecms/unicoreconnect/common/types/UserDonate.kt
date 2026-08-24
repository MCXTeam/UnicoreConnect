package ru.unicorecms.unicoreconnect.common.types

import java.util.Date

class UserDonate {
    var user: User = User()
    var group: DonateGroup = DonateGroup()
    var server: Server = Server()
    var expired: Date? = null
}