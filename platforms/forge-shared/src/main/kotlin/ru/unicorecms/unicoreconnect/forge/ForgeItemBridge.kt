package ru.unicorecms.unicoreconnect.forge

import net.minecraft.nbt.TagParser
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraftforge.registries.ForgeRegistries
import ru.unicorecms.unicoreconnect.common.platform.DescribedItem
import ru.unicorecms.unicoreconnect.common.platform.ItemBridge
import ru.unicorecms.unicoreconnect.common.platform.PlatformPlayer
import ru.unicorecms.unicoreconnect.common.platform.UnicoreLogger

class ForgeItemBridge(private val logger: UnicoreLogger) : ItemBridge {
    override fun give(player: PlatformPlayer, itemId: String, nbt: String?, amount: Int): Boolean {
        val handle = (player as? ForgePlayer)?.handle() ?: return false
        val stack = build(itemId, nbt, amount) ?: return false

        if (!handle.inventory.add(stack)) handle.drop(stack, false)

        return true
    }

    override fun describeHeldItem(player: PlatformPlayer, name: String, price: Double): DescribedItem? {
        val handle = (player as? ForgePlayer)?.handle() ?: return null
        val stack = handle.mainHandItem

        if (stack.isEmpty) return null

        val id = ForgeRegistries.ITEMS.getKey(stack.item)?.toString() ?: return null

        return DescribedItem(id, name, stack.tag?.toString(), price)
    }

    private fun build(itemId: String, nbt: String?, amount: Int): ItemStack? {
        val id = itemId.substringBefore('@')
        val location = ResourceLocation.tryParse(if (id.contains(':')) id else "minecraft:$id") ?: return null
        val item = ForgeRegistries.ITEMS.getValue(location) ?: return null
        val stack = ItemStack(item, amount)

        if (nbt.isNullOrBlank()) return stack

        return try {
            stack.tag = TagParser.parseTag(nbt)
            stack
        } catch (error: Throwable) {
            logger.warn("NBT предмета '$itemId' прочитать не удалось: ${error.message}")
            stack
        }
    }
}
