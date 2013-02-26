/**
 * BuildCraft is open-source. It is distributed under the terms of the
 * BuildCraft Open Source License. It grants rights to read, modify, compile
 * or run the code. It does *NOT* grant the right to redistribute this software
 * or its modifications in any form, binary or source, except if expressively
 * granted by the copyright holder.
 */

package buildcraft.transport.pipes;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.common.ISidedInventory;
import net.minecraftforge.inventory.ICustomInventory;
import net.minecraftforge.inventory.IStackFilter;
import net.minecraftforge.inventory.InventoryAdapters;
import buildcraft.api.core.Position;
import buildcraft.api.inventory.ISpecialInventory;
import buildcraft.api.power.IPowerProvider;
import buildcraft.api.power.IPowerReceptor;
import buildcraft.api.power.PowerFramework;
import buildcraft.api.transport.IPipedItem;
import buildcraft.api.transport.PipeManager;
import buildcraft.core.DefaultProps;
import buildcraft.core.EntityPassiveItem;
import buildcraft.core.RedstonePowerFramework;
import buildcraft.core.utils.Utils;
import buildcraft.transport.Pipe;
import buildcraft.transport.PipeTransportItems;

public class PipeItemsWood extends Pipe implements IPowerReceptor {

	private IPowerProvider powerProvider;

	protected int baseTexture = 1 * 16 + 0;
	protected int plainTexture = 1 * 16 + 15;

	protected PipeItemsWood(PipeTransportItems transport, PipeLogic logic, int itemID) {
		super(transport, logic, itemID);

		powerProvider = PowerFramework.currentFramework.createPowerProvider();
		powerProvider.configure(50, 1, 64, 1, 64);
		powerProvider.configurePowerPerdition(64, 1);
	}
	
	protected PipeItemsWood(int itemID, PipeTransportItems transport) {
		this(transport, new PipeLogicWood(), itemID);
	}

	public PipeItemsWood(int itemID) {
		this(itemID, new PipeTransportItems());
	}

	@Override
	public String getTextureFile() {
		return DefaultProps.TEXTURE_BLOCKS;
	}

	@Override
	public int getTextureIndex(ForgeDirection direction) {
		if (direction == ForgeDirection.UNKNOWN)
			return baseTexture;
		else {
			int metadata = worldObj.getBlockMetadata(xCoord, yCoord, zCoord);

			if (metadata == direction.ordinal())
				return plainTexture;
			else
				return baseTexture;
		}
	}

	@Override
	public void setPowerProvider(IPowerProvider provider) {
		powerProvider = provider;
	}

	@Override
	public IPowerProvider getPowerProvider() {
		return powerProvider;
	}

	@Override
	public void doWork() {
		if (powerProvider.getEnergyStored() <= 0)
			return;

		World w = worldObj;

		int meta = worldObj.getBlockMetadata(xCoord, yCoord, zCoord);

		if (meta > 5)
			return;

		Position pos = new Position(xCoord, yCoord, zCoord, ForgeDirection.getOrientation(meta));
		pos.moveForwards(1);
		TileEntity tile = w.getBlockTileEntity((int) pos.x, (int) pos.y, (int) pos.z);

		ICustomInventory forgeInventory = null;
		boolean isSpecialInventory = (tile instanceof ISpecialInventory);
		
		if(!isSpecialInventory) {
		    forgeInventory = InventoryAdapters.asCustomInventory(tile, pos.orientation.getOpposite());
		    if(forgeInventory == null)
		        return;
		}
        
        if (!PipeManager.canExtractItems(this, w, (int) pos.x, (int) pos.y, (int) pos.z))
			return;

        ItemStack[] extracted = null;
        
        if(isSpecialInventory)
            extracted = checkExtractSpecial((ISpecialInventory)tile, true, pos.orientation.getOpposite());
        else
            extracted = checkExtractForge(forgeInventory, true, pos.orientation.getOpposite());
        
        if (extracted == null)
			return;

		for (ItemStack stack : extracted) {
			if (stack == null || stack.stackSize == 0) {
				powerProvider.useEnergy(1, 1, false);
				continue;
			}

			Position entityPos = new Position(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, pos.orientation.getOpposite());

			entityPos.moveForwards(0.6);

			IPipedItem entity = new EntityPassiveItem(w, entityPos.x, entityPos.y, entityPos.z, stack);

			((PipeTransportItems) transport).entityEntering(entity, entityPos.orientation);
		}
	}
	
	public ItemStack[] checkExtractSpecial(ISpecialInventory inventory, boolean doRemove, ForgeDirection from) {
	    ItemStack[] stacks = inventory.extractItem(doRemove, from, (int) powerProvider.getEnergyStored());
        if (stacks != null && doRemove) {
            for (ItemStack stack : stacks) {
                if (stack != null) {
                    powerProvider.useEnergy(stack.stackSize, stack.stackSize, doRemove);
                }
            }
        }
        return stacks;
	}
	
	public IStackFilter getExtractFilter() {
	    return IStackFilter.MATCH_ANY;
	}

	/**
	 * Return the itemstack that can be if something can be extracted from this inventory, null if none. On certain cases, the extractable slot depends on the
	 * position of the pipe.
	 */
	public ItemStack[] checkExtractForge(ICustomInventory inventory, boolean doRemove, ForgeDirection from) {
	    
	    int maxExtracted = (int)powerProvider.getEnergyStored();
	    if(maxExtracted <= 0)
	        return null;
	    
	    ItemStack is = inventory.extract(getExtractFilter(), maxExtracted, !doRemove);
	    if(is == null)
	        return null;
	    
	    if(doRemove)
	        powerProvider.useEnergy(is.stackSize, is.stackSize, true);
	    
	    return new ItemStack[] {is};
	}

	@Override
	public int powerRequest() {
		return getPowerProvider().getMaxEnergyReceived();
	}

	@Override
	public boolean canConnectRedstone() {
		if (PowerFramework.currentFramework instanceof RedstonePowerFramework)
			return true;
		return super.canConnectRedstone();
	}
}
