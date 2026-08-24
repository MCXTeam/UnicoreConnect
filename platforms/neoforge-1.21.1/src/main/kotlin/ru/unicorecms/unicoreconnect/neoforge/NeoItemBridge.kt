package ru.unicorecms.unicoreconnect.neoforge

import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.TagParser
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import ru.unicorecms.unicoreconnect.common.platform.DescribedItem
import ru.unicorecms.unicoreconnect.common.platform.ItemBridge
import ru.unicorecms.unicoreconnect.common.platform.PlatformPlayer
import ru.unicorecms.unicoreconnect.common.platform.UnicoreLogger

class NeoItemBridge(private val logger: UnicoreLogger) : ItemBridge {
    override fun give(player: PlatformPlayer, itemId: String, nbt: String?, amount: Int): Boolean {
        val handle = (player as? NeoPlayer)?.handle() ?: return false
        val stack = build(itemId, nbt, amount) ?: return false

        if (!handle.inventory.add(stack)) handle.drop(stack, false)

        return true
    }

    override fun describeHeldItem(player: PlatformPlayer, name: String, price: Double): DescribedItem? {
        val handle = (player as? NeoPlayer)?.handle() ?: return null
        val stack = handle.mainHandItem

        if (stack.isEmpty) return null

        val id = BuiltInRegistries.ITEM.getKey(stack.item).toString()
        val custom = stack.get(DataComponents.CUSTOM_DATA)?.copyTag()?.toString()

        return DescribedItem(id, name, custom, price)
    }

    private fun build(itemId: String, nbt: String?, amount: Int): ItemStack? {
        val id = itemId.substringBefore('@')
        val location = ResourceLocation.tryParse(if (id.contains(':')) id else "minecraft:$id") ?: return null
        val item = BuiltInRegistries.ITEM.getOptional(location).orElse(null) ?: return null
        val stack = ItemStack(item, amount)

        if (nbt.isNullOrBlank()) return stack

        return try {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(TagParser.parseTag(nbt)))
            stack
        } catch (error: Throwable) {
            logger.warn("NBT предмета '$itemId' прочитать не удалось: ${error.message}")
            stack
        }
    }
}
