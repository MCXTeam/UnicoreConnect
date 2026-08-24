package ru.unicorecms.unicoreconnect.adapters.economy.luxinfine

import ml.luxinfine.helper.integrations.Economy
import ml.luxinfine.helper.integrations.IntegrationsRegistry
import ml.luxinfine.helper.utils.PlayerUtils
import ru.unicorecms.unicoreconnect.common.platform.EconomyBridge
import ru.unicorecms.unicoreconnect.common.platform.EconomyProvider
import java.util.Optional
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

class LuxinfineEconomyBridge : EconomyBridge {
    override val id = ID

    private var economy: UnicoreEconomy? = null

    override fun available(): Boolean {
        return try {
            Class.forName("ml.luxinfine.helper.integrations.IntegrationsRegistry")
            true
        } catch (error: Throwable) {
            false
        }
    }

    override fun register(provider: EconomyProvider) {
        val economy = UnicoreEconomy(provider)

        if (!IntegrationsRegistry.registerProvider(Economy::class.java, INTEGRATION, economy)) {
            throw IllegalStateException("Интеграция экономики '$INTEGRATION' уже занята другим модом")
        }

        this.economy = economy
    }

    override fun unregister() {
        economy?.disable()
        economy = null
    }

    companion object {
        const val ID = "luxinfine"
        const val INTEGRATION = "unicorecms"
    }
}

class UnicoreEconomy(private val provider: EconomyProvider) : Economy {
    private val listeners = CopyOnWriteArrayList<Economy.BalanceChangeListener>()

    private var enabled = true

    fun disable() {
        enabled = false
        listeners.clear()
    }

    override fun isDummy() = false

    override fun getBalance(uuid: UUID): Optional<Double> {
        if (!enabled) return Optional.empty()

        return Optional.of(provider.balance(uuid))
    }

    override fun setBalance(uuid: UUID, amount: Double): Boolean {
        if (!enabled) return false

        val difference = amount - provider.balance(uuid)

        return when {
            difference > 0 -> addBalance(uuid, difference)
            difference < 0 -> removeBalance(uuid, -difference)
            else -> true
        }
    }

    override fun addBalance(uuid: UUID, amount: Double): Boolean {
        if (!enabled) return false

        return provider.deposit(uuid, amount).also { done -> if (done) notify(uuid) }
    }

    override fun removeBalance(uuid: UUID, amount: Double): Boolean {
        if (!enabled) return false

        return provider.withdraw(uuid, amount).also { done -> if (done) notify(uuid) }
    }

    override fun addBalanceListener(listener: Economy.BalanceChangeListener) {
        listeners.add(listener)
    }

    override fun removeBalanceListener(listener: Economy.BalanceChangeListener) {
        listeners.remove(listener)
    }

    override fun getBalanceListeners(): Collection<Economy.BalanceChangeListener> = listeners

    private fun notify(uuid: UUID) {
        if (listeners.isEmpty()) return

        val player = PlayerUtils.getOnlinePlayer(uuid).orElse(null) ?: return
        val balance = provider.balance(uuid)

        listeners.forEach { listener -> listener.onBalanceChanged(player, balance) }
    }
}
