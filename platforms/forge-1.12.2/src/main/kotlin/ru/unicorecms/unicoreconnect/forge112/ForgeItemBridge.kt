package ru.unicorecms.unicoreconnect.forge112

import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.JsonToNBT
import net.minecraft.util.ResourceLocation
import ru.unicorecms.unicoreconnect.common.platform.DescribedItem
import ru.unicorecms.unicoreconnect.common.platform.ItemBridge
import ru.unicorecms.unicoreconnect.common.platform.PlatformPlayer
import ru.unicorecms.unicoreconnect.common.platform.UnicoreLogger

class ForgeItemBridge(private val logger: UnicoreLogger) : ItemBridge {
    override fun give(player: PlatformPlayer, itemId: String, nbt: String?, amount: Int): Boolean {
        val handle = (player as? ForgePlayer)?.handle() ?: return false
        val stack = build(itemId, nbt, amount) ?: return false

        if (!handle.inventory.addItemStackToInventory(stack)) handle.dropItem(stack, false)

        return true
    }

    override fun describeHeldItem(player: PlatformPlayer, name: String, price: Double): DescribedItem? {
        val handle = (player as? ForgePlayer)?.handle() ?: return null
        val stack = handle.heldItemMainhand

        if (stack.isEmpty) return null

        val id = Item.REGISTRY.getNameForObject(stack.item)?.toString() ?: return null
        val meta = stack.itemDamage
        val nbt = stack.tagCompound?.toString()

        return DescribedItem(if (meta == 0) id else "$id@$meta", name, nbt, price)
    }

    private fun build(itemId: String, nbt: String?, amount: Int): ItemStack? {
        val id = itemId.substringBefore('@')
        val meta = itemId.substringAfter('@', "0").toIntOrNull() ?: 0
        val location = ResourceLocation(if (id.contains(':')) id else "minecraft:$id")
        val item = Item.REGISTRY.getObject(location) ?: return null
        val stack = ItemStack(item, amount, meta)

        if (nbt.isNullOrBlank()) return stack

        return try {
            stack.tagCompound = JsonToNBT.getTagFromJson(nbt)
            stack
        } catch (error: Throwable) {
            logger.warn("NBT предмета '$itemId' прочитать не удалось: ${error.message}")
            stack
        }
    }
}
