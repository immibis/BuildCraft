package buildcraft.core.inventory;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.common.ISidedInventory;
import net.minecraftforge.inventory.ICustomInventory;
import net.minecraftforge.inventory.InventoryAdapters;
import buildcraft.api.inventory.ISpecialInventory;
import buildcraft.core.utils.Utils;

public abstract class Transactor implements ITransactor {

	@Override
	public ItemStack add(ItemStack stack, ForgeDirection orientation, boolean doAdd) {
		ItemStack added = stack.copy();
		added.stackSize = inject(stack, orientation, doAdd);
		return added;
	}

	public abstract int inject(ItemStack stack, ForgeDirection orientation, boolean doAdd);

	public static ITransactor getTransactorFor(Object object, ForgeDirection side) {
	    
	    if (object instanceof ISpecialInventory)
            return new TransactorSpecial((ISpecialInventory) object);
	    
	    else if (object instanceof TileEntity)
	        return new TransactorForge(InventoryAdapters.asCustomInventory((TileEntity)object, side));
	    
	    else if (object instanceof IInventory)
	        return new TransactorForge(InventoryAdapters.asCustomInventory((IInventory)object, side));
	    
		return null;
	}
}
