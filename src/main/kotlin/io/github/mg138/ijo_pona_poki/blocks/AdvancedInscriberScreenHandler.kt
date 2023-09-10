package io.github.mg138.ijo_pona_poki.blocks

import appeng.api.config.YesNo
import appeng.blockentity.misc.InscriberRecipes
import appeng.client.gui.Icon
import appeng.core.definitions.AEItems
import appeng.core.localization.Side
import appeng.core.localization.Tooltips
import appeng.menu.SlotSemantics
import appeng.menu.guisync.GuiSync
import appeng.menu.implementations.UpgradeableMenu
import appeng.menu.interfaces.IProgressProvider
import appeng.menu.slot.AppEngSlot
import appeng.menu.slot.OutputSlot
import appeng.menu.slot.RestrictedInputSlot
import appeng.menu.slot.RestrictedInputSlot.PlacableItemType
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.slot.Slot

class AdvancedInscriberScreenHandler(id: Int, playerInventory: PlayerInventory, host: AdvancedInscriberBlockEntity)
    : UpgradeableMenu<AdvancedInscriberBlockEntity>(AdvancedInscriber.SCREEN_HANDLER, id, playerInventory, host),
        IProgressProvider {
    private val top: Slot
    private val middle: Slot
    private val bottom: Slot

    @GuiSync(2)
    var maxProcessingTime = -1

    @GuiSync(3)
    var processingTime = -1

    @GuiSync(7)
    var separateSides = YesNo.NO

    init {
        val inv = host.internalInventory

        val top = RestrictedInputSlot(PlacableItemType.INSCRIBER_PLATE, inv, 0)
        top.setEmptyTooltip {
            if (separateSides == YesNo.YES) Tooltips.inputSlot(Side.TOP)
            else Tooltips.inputSlot(Side.ANY)
        }
        this.top = this.addSlot(top, SlotSemantics.INSCRIBER_PLATE_TOP)

        val bottom = AppEngSlot(inv, 1)
        bottom.icon = Icon.BACKGROUND_PLATE
        bottom.setEmptyTooltip {
            if (separateSides == YesNo.YES) Tooltips.inputSlot(Side.BOTTOM)
            else Tooltips.inputSlot(Side.ANY)
        }
        this.bottom = this.addSlot(bottom, SlotSemantics.INSCRIBER_PLATE_BOTTOM)

        val middle = AppEngSlot(inv, 2)
        middle.icon = Icon.BACKGROUND_INGOT
        middle.setEmptyTooltip {
            if (separateSides == YesNo.YES) Tooltips.inputSlot(Side.LEFT, Side.RIGHT, Side.BACK, Side.FRONT)
            else Tooltips.inputSlot(Side.ANY)
        }
        this.middle = this.addSlot(middle, SlotSemantics.MACHINE_INPUT)

        val output = OutputSlot(inv, 3, null)
        output.setEmptyTooltip {
            if (separateSides == YesNo.YES) Tooltips.outputSlot(Side.LEFT, Side.RIGHT, Side.BACK, Side.FRONT)
            else Tooltips.outputSlot(Side.ANY)
        }
        this.addSlot(output, SlotSemantics.MACHINE_OUTPUT)
    }

    override fun standardDetectAndSendChanges() {
        if (this.isServerSide) {
            this.maxProcessingTime = this.host.getMaxProcessingTime()
            this.processingTime = this.host.getProcessingTime()
        }
        super.standardDetectAndSendChanges()
    }

    override fun isValidForSlot(slot: Slot, itemStack: ItemStack): Boolean {
        val top = this.host.internalInventory.getStackInSlot(0)
        val bottom = this.host.internalInventory.getStackInSlot(1)

        if (slot == this.middle) {
            val press = AEItems.NAME_PRESS

            return if (!press.isSameAs(top) && !press.isSameAs(bottom)) {
                InscriberRecipes.findRecipe(this.host.world, itemStack, top, bottom, false) != null
            } else {
                !press.isSameAs(itemStack)
            }
        }

        if ((slot != this.top || bottom.isEmpty) && (slot != this.bottom || top.isEmpty)) {
            return true
        }

        val otherSlot = if (slot == this.top) {
            this.bottom.stack
        } else {
            this.top.stack
        }

        val namePress = AEItems.NAME_PRESS
        return if (namePress.isSameAs(otherSlot)) {
            namePress.isSameAs(itemStack)
        } else {
            InscriberRecipes.isValidOptionalIngredientCombination(
                this.host.world, itemStack, otherSlot
            )
        }
    }

    override fun getCurrentProgress() = this.processingTime
    override fun getMaxProgress() = this.maxProcessingTime
}