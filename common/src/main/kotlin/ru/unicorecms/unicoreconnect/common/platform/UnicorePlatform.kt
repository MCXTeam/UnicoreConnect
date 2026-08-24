package ru.unicorecms.unicoreconnect.common.platform

import java.util.UUID

interface UnicoreLogger {
    fun info(message: String)
    fun warn(message: String)
    fun error(message: String, error: Throwable? = null)
}

interface PlatformPlayer {
    val uuid: UUID
    val name: String
    val address: String?

    fun sendMessage(message: String)
    fun hasPermission(permission: String): Boolean
}

interface PlatformScheduler {
    fun sync(task: () -> Unit)
    fun async(task: () -> Unit)
    fun repeatSync(delayTicks: Long, periodTicks: Long, task: () -> Unit): Cancellable
}

interface Cancellable {
    fun cancel()
}

interface PermissionAdapter {
    val id: String

    fun available(): Boolean
    fun addGroup(uuid: UUID, group: String, expiresAt: Long?)
    fun removeGroup(uuid: UUID, group: String)
    fun addPermission(uuid: UUID, permission: String, expiresAt: Long?)
    fun removePermission(uuid: UUID, permission: String)
    fun groupsOf(uuid: UUID): Set<String>
    fun permissionsOf(uuid: UUID): Set<String>
    fun has(uuid: UUID, permission: String): Boolean? = null
}

interface BanAdapter {
    val id: String

    fun available(): Boolean
    fun banned(uuid: UUID): Boolean
    fun ban(uuid: UUID, reason: String, actor: UUID?, expiresAt: Long?)
    fun unban(uuid: UUID)
}

interface EconomyProvider {
    fun balance(uuid: UUID): Double
    fun has(uuid: UUID, amount: Double): Boolean
    fun withdraw(uuid: UUID, amount: Double): Boolean
    fun deposit(uuid: UUID, amount: Double): Boolean
    fun format(amount: Double): String
}

interface EconomyBridge {
    val id: String

    fun available(): Boolean
    fun register(provider: EconomyProvider)
    fun unregister()
}

interface ItemBridge {
    fun give(player: PlatformPlayer, itemId: String, nbt: String?, amount: Int): Boolean
    fun describeHeldItem(player: PlatformPlayer, name: String, price: Double): DescribedItem?
}

data class DescribedItem(val itemId: String, val name: String, val nbt: String?, val price: Double)

interface UnicorePlatform {
    val logger: UnicoreLogger
    val scheduler: PlatformScheduler
    val permissions: PermissionAdapter?
    val bans: BanAdapter?
    val items: ItemBridge?

    fun player(uuid: UUID): PlatformPlayer?
    fun player(name: String): PlatformPlayer?
    fun onlinePlayers(): Collection<PlatformPlayer>
    fun runCommand(command: String)
}
