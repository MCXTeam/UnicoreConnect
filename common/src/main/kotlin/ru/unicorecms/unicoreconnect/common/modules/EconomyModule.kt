package ru.unicorecms.unicoreconnect.common.modules

import ru.unicorecms.unicoreconnect.common.UnicoreCommon
import ru.unicorecms.unicoreconnect.common.format.Formats
import ru.unicorecms.unicoreconnect.common.platform.EconomyBridge
import ru.unicorecms.unicoreconnect.common.platform.EconomyProvider
import ru.unicorecms.unicoreconnect.common.platform.UnicorePlatform
import java.util.UUID

class EconomyModule(private val platform: UnicorePlatform) : EconomyProvider {
    private val bridges = mutableListOf<EconomyBridge>()

    override fun balance(uuid: UUID): Double = UnicoreCommon.moneyService.findOne(uuid).money

    override fun has(uuid: UUID, amount: Double): Boolean = balance(uuid) >= amount

    override fun withdraw(uuid: UUID, amount: Double): Boolean {
        return try {
            UnicoreCommon.moneyService.withdraw(uuid, amount)
            true
        } catch (error: Throwable) {
            platform.logger.error("Списание $amount у $uuid не удалось", error)
            false
        }
    }

    override fun deposit(uuid: UUID, amount: Double): Boolean {
        return try {
            UnicoreCommon.moneyService.deposit(uuid, amount)
            true
        } catch (error: Throwable) {
            platform.logger.error("Начисление $amount игроку $uuid не удалось", error)
            false
        }
    }

    override fun format(amount: Double): String = Formats.money(amount)

    fun register(available: List<EconomyBridge>) {
        available.filter { it.available() }.forEach { bridge ->
            try {
                bridge.register(this)
                bridges.add(bridge)
                platform.logger.info("Экономика подключена к ${bridge.id}")
            } catch (error: Throwable) {
                platform.logger.error("Экономику не удалось подключить к ${bridge.id}", error)
            }
        }

        if (bridges.isEmpty()) platform.logger.info("Экономика работает сама по себе")
    }

    fun unregister() {
        bridges.forEach { runCatching { it.unregister() } }
        bridges.clear()
    }
}
