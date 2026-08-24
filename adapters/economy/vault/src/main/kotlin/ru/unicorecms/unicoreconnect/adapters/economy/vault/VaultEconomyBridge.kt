package ru.unicorecms.unicoreconnect.adapters.economy.vault

import net.milkbowl.vault.economy.Economy
import net.milkbowl.vault.economy.EconomyResponse
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.ServicePriority
import ru.unicorecms.unicoreconnect.common.platform.EconomyBridge
import ru.unicorecms.unicoreconnect.common.platform.EconomyProvider

class VaultEconomyBridge(private val plugin: Plugin) : EconomyBridge {
    override val id = "vault"

    private var service: UnicoreEconomy? = null

    override fun available() = Bukkit.getPluginManager().getPlugin("Vault") != null

    override fun register(provider: EconomyProvider) {
        val economy = UnicoreEconomy(plugin, provider)

        Bukkit.getServicesManager().register(Economy::class.java, economy, plugin, ServicePriority.Highest)
        service = economy
    }

    override fun unregister() {
        service?.let { Bukkit.getServicesManager().unregister(Economy::class.java, it) }
        service = null
    }

    fun provider(): Economy? = service
}

class UnicoreEconomy(private val plugin: Plugin, private val provider: EconomyProvider) : EconomyWrapper() {
    override fun isEnabled() = plugin.isEnabled

    override fun getName() = plugin.name

    override fun fractionalDigits() = 2

    override fun format(amount: Double) = provider.format(amount)

    override fun hasAccount(player: OfflinePlayer) = true

    override fun createPlayerAccount(player: OfflinePlayer) = true

    override fun getBalance(player: OfflinePlayer) = provider.balance(player.uniqueId)

    override fun has(player: OfflinePlayer, amount: Double) = provider.has(player.uniqueId, amount)

    override fun withdrawPlayer(player: OfflinePlayer, amount: Double): EconomyResponse {
        val done = provider.withdraw(player.uniqueId, amount)
        val type = if (done) EconomyResponse.ResponseType.SUCCESS else EconomyResponse.ResponseType.FAILURE

        return EconomyResponse(amount, if (done) provider.balance(player.uniqueId) else 0.0, type, null)
    }

    override fun depositPlayer(player: OfflinePlayer, amount: Double): EconomyResponse {
        val done = provider.deposit(player.uniqueId, amount)
        val type = if (done) EconomyResponse.ResponseType.SUCCESS else EconomyResponse.ResponseType.FAILURE

        return EconomyResponse(amount, if (done) provider.balance(player.uniqueId) else 0.0, type, null)
    }
}
