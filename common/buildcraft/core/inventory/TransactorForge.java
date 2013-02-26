package buildcraft.core.inventory;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.inventory.ICustomInventory;

public class TransactorForge extends Transactor {

    private ICustomInventory inv;
    
    public TransactorForge(ICustomInventory inv) {
        this.inv = inv;
    }

    @Override
    public int inject(ItemStack stack, ForgeDirection orientation, boolean doAdd) {
        int initialSize = stack.stackSize;
        int leftover = inv.insert(stack, initialSize, !doAdd);
        return initialSize - leftover;
    }

}
