package ru.unicorecms.unicoreconnect.common

object Permissions {
    const val MONEY = "unicoreconnect.command.money"
    const val MONEY_TOP = "unicoreconnect.command.money.top"
    const val MONEY_PAY = "unicoreconnect.command.money.pay"
    const val PLAYTIME = "unicoreconnect.command.playtime"
    const val PLAYTIME_TOP = "unicoreconnect.command.playtime.top"
    const val SHOWCASE_LIST = "unicoreconnect.command.showcase.list"
    const val SHOWCASE_GIVE = "unicoreconnect.command.showcase.give"
    const val SHOWCASE_ALL = "unicoreconnect.command.showcase.all"
    const val SHOWCASE_CREATE = "unicoreconnect.admin.showcase.create"
    const val SYNC = "unicoreconnect.admin.sync"

    val everyone = listOf(MONEY, MONEY_TOP, MONEY_PAY, PLAYTIME, PLAYTIME_TOP, SHOWCASE_LIST, SHOWCASE_GIVE, SHOWCASE_ALL)
    val operators = listOf(SHOWCASE_CREATE, SYNC)
}
