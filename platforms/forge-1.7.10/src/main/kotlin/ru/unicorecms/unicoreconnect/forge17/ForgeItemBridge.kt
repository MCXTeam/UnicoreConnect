package ru.unicorecms.unicoreconnect.forge17

import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.JsonToNBT
import net.minecraft.nbt.NBTTagCompound
import ru.unicorecms.unicoreconnect.common.platform.DescribedItem
import ru.unicorecms.unicoreconnect.common.platform.ItemBridge
import ru.unicorecms.unicoreconnect.common.platform.PlatformPlayer
import ru.unicorecms.unicoreconnect.common.platform.UnicoreLogger

class ForgeItemBridge(private val logger: UnicoreLogger) : ItemBridge {
    override fun give(player: PlatformPlayer, itemId: String, nbt: String?, amount: Int): Boolean {
        val handle = (player as? ForgePlayer)?.handle() ?: return false
        val stack = build(itemId, nbt, amount) ?: return false

        if (!handle.inventory.addItemStackToInventory(stack)) handle.dropPlayerItemWithRandomChoice(stack, false)

        return true
    }

    override fun describeHeldItem(player: PlatformPlayer, name: String, price: Double): DescribedItem? {
        val handle = (player as? ForgePlayer)?.handle() ?: return null
        val stack = handle.heldItem ?: return null
        val id = Item.itemRegistry.getNameForObject(stack.item) as? String ?: return null
        val meta = stack.itemDamage
        val nbt = stack.tagCompound?.toString()

        return DescribedItem(if (meta == 0) id else "$id@$meta", name, nbt, price)
    }

    private fun build(itemId: String, nbt: String?, amount: Int): ItemStack? {
        val id = itemId.substringBefore('@')
        val meta = itemId.substringAfter('@', "0").toIntOrNull() ?: 0
        val item = Item.itemRegistry.getObject(if (id.contains(':')) id else "minecraft:$id") as? Item ?: return null
        val stack = ItemStack(item, amount, meta)

        if (nbt.isNullOrBlank()) return stack

        return try {
            stack.tagCompound = JsonToNBT.func_150315_a(nbt) as NBTTagCompound
            stack
        } catch (error: Throwable) {
            logger.warn("NBT предмета '$itemId' прочитать не удалось: ${error.message}")
            stack
        }
    }
}
