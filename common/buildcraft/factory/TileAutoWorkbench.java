/**
 * Copyright (c) SpaceToad, 2011
 * http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package buildcraft.factory;

import java.util.LinkedList;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.inventory.ICustomInventory;
import net.minecraftforge.inventory.IForgeInventory;
import net.minecraftforge.inventory.IForgeInventoryTile;
import net.minecraftforge.inventory.IInventorySlot;
import net.minecraftforge.inventory.ILinearInventory;
import net.minecraftforge.inventory.IStackFilter;
import net.minecraftforge.inventory.InventoryAdapters;
import buildcraft.api.core.Position;
import buildcraft.api.inventory.ISpecialInventory;
import buildcraft.core.inventory.TransactorRoundRobin;
import buildcraft.core.proxy.CoreProxy;
import buildcraft.core.utils.Utils;
import buildcraft.core.utils.CraftingHelper;

public class TileAutoWorkbench extends TileEntity implements IForgeInventoryTile, IInventory {

	private ItemStack stackList[] = new ItemStack[9];
	private IRecipe currentRecipe = null;
	
	class LocalInventoryCrafting extends InventoryCrafting {

		public LocalInventoryCrafting() {
			super(new Container() {

				@SuppressWarnings("all")
				public boolean isUsableByPlayer(EntityPlayer entityplayer) {
					return false;
				}

				@SuppressWarnings("all")
				public boolean canInteractWith(EntityPlayer entityplayer) {
					// TODO Auto-generated method stub
					return false;
				}
			}, 3, 3);
			// TODO Auto-generated constructor stub
		}

	}

	public IRecipe getCurrentRecipe() {

		return currentRecipe ;
	}

	@Override
	public int getSizeInventory() {

		return stackList.length;
	}

	@Override
	public ItemStack getStackInSlot(int i) {
		return stackList[i];
	}

	@Override
	public ItemStack decrStackSize(int slotId, int count) {
		if (stackList[slotId] == null)
			return null;
		if (stackList[slotId].stackSize > count)
			return stackList[slotId].splitStack(count);
		ItemStack stack = stackList[slotId];
		stackList[slotId] = null;
		return stack;
	}

	@Override
	public void setInventorySlotContents(int i, ItemStack itemstack) {
		stackList[i] = itemstack;
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int slot) {
		if (this.stackList[slot] == null)
			return null;

		ItemStack stackToTake = this.stackList[slot];
		this.stackList[slot] = null;
		return stackToTake;
	}

	@Override
	public String getInvName() {

		return "";
	}

	@Override
	public int getInventoryStackLimit() {

		return 64;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer entityplayer) {
		return worldObj.getBlockTileEntity(xCoord, yCoord, zCoord) == this;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound) {
		super.readFromNBT(nbttagcompound);

		Utils.readStacksFromNBT(nbttagcompound, "stackList", stackList);
	}

	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound) {
		super.writeToNBT(nbttagcompound);

		Utils.writeStacksToNBT(nbttagcompound, "stackList", stackList);
	}

	class StackPointer {

		IInventorySlot slot;
		ItemStack item;
	}

	public ItemStack findRecipe() {
		InventoryCrafting craftMatrix = new LocalInventoryCrafting();

		for (int i = 0; i < getSizeInventory(); ++i) {
			ItemStack stack = getStackInSlot(i);

			craftMatrix.setInventorySlotContents(i, stack);
		}

		if(this.currentRecipe == null || !this.currentRecipe.matches(craftMatrix, worldObj))
			currentRecipe = CraftingHelper.findMatchingRecipe(craftMatrix, worldObj);

		if(currentRecipe!=null)
			return currentRecipe.getCraftingResult(craftMatrix);
		return null;
	}
 
	public ItemStack extractItem(boolean doRemove, boolean removeRecipe) {
		InventoryCrafting craftMatrix = new LocalInventoryCrafting();

		LinkedList<StackPointer> pointerList = new LinkedList<StackPointer>();

		int itemsToLeave = (removeRecipe ? 0 : 1);
		
		ILinearInventory selfInventory = InventoryAdapters.asLinearInventory((IInventory)this, ForgeDirection.UP);

		for (int i = 0; i < getSizeInventory(); ++i) {
			ItemStack stack = getStackInSlot(i);

			if (stack != null) {
				if (stack.stackSize <= itemsToLeave) {
					StackPointer pointer = getNearbyItem(stack, doRemove);

					if (pointer == null) {
					    if(doRemove)
					        resetPointers(pointerList);

						return null;
					} else {
						pointerList.add(pointer);
					}
				} else {
					StackPointer pointer = new StackPointer();
					pointer.slot = selfInventory.getSlot(i);
					pointer.item = this.decrStackSize(i, 1);
					stack = pointer.item;

					pointerList.add(pointer);
				}
			}

			craftMatrix.setInventorySlotContents(i, stack);
		}

		if(this.currentRecipe == null || !this.currentRecipe.matches(craftMatrix, worldObj))
			currentRecipe = buildcraft.core.utils.CraftingHelper.findMatchingRecipe(craftMatrix, worldObj);

		
		ItemStack resultStack = null;
		if(currentRecipe != null) {
			resultStack = currentRecipe.getCraftingResult(craftMatrix);
		}

		if(!doRemove)
		    return resultStack;
		
		if (resultStack == null) {
			resetPointers(pointerList);
		} else {
			for (StackPointer p : pointerList) {
				// replace with the container where appropriate

				if (p.item.getItem().getContainerItem() != null) {
					ItemStack newStack = p.item.getItem().getContainerItemStack(p.item);

					if (p.item.isItemStackDamageable()) {
						if (newStack.getItemDamage() >= p.item.getMaxDamage()) {
							MinecraftForge.EVENT_BUS.post(new PlayerDestroyItemEvent(CoreProxy.proxy.getBuildCraftPlayer(worldObj, xCoord, yCoord, zCoord),
									newStack));
							this.worldObj.playSoundAtEntity(CoreProxy.proxy.getBuildCraftPlayer(worldObj, xCoord, yCoord, zCoord), "random.break", 0.8F,
									0.8F + this.worldObj.rand.nextFloat() * 0.4F);
							newStack = null;
						}
					}
					
					// If this returns false the container item is lost.
					p.slot.setStack(newStack, false);
				}
			}
		}

		return resultStack;
	}

	public void resetPointers(LinkedList<StackPointer> pointers) {
		for (StackPointer p : pointers) {
			ItemStack item = p.slot.getStack();

			if (item == null) {
			    // if this returns false the item is lost
                p.slot.setStack(p.item, false);
                
			} else {
				item = item.copy();
				item.stackSize++;
				
				// if this returns false the item is lost
				p.slot.setStack(item, false);
			}
		}
	}

	public StackPointer getNearbyItem(ItemStack stack, boolean doRemove) {
		StackPointer pointer = null;

		if (pointer == null) {
			pointer = getNearbyItemFromOrientation(stack, ForgeDirection.WEST, doRemove);
		}

		if (pointer == null) {
			pointer = getNearbyItemFromOrientation(stack, ForgeDirection.EAST, doRemove);
		}

		if (pointer == null) {
			pointer = getNearbyItemFromOrientation(stack, ForgeDirection.DOWN, doRemove);
		}

		if (pointer == null) {
			pointer = getNearbyItemFromOrientation(stack, ForgeDirection.UP, doRemove);
		}

		if (pointer == null) {
			pointer = getNearbyItemFromOrientation(stack, ForgeDirection.NORTH, doRemove);
		}

		if (pointer == null) {
			pointer = getNearbyItemFromOrientation(stack, ForgeDirection.SOUTH, doRemove);
		}

		return pointer;
	}

	public StackPointer getNearbyItemFromOrientation(ItemStack itemStack, ForgeDirection direction, boolean doRemove) {
		TileEntity tile = worldObj.getBlockTileEntity(xCoord + direction.offsetX, yCoord + direction.offsetY, zCoord + direction.offsetZ);
		
		if (tile == null)
            return null;
        
        ILinearInventory inventory = InventoryAdapters.asLinearInventory(tile, direction.getOpposite());
        if(inventory == null)
            return null;
        
        for (int j = 0; j < inventory.getNumSlots(); ++j) {
            IInventorySlot slot = inventory.getSlot(j);
            ItemStack stack = slot.getStack(); 

            if (stack != null && stack.stackSize > 0 && stack.itemID == itemStack.itemID && (stack.isItemStackDamageable() || stack.getItemDamage() == itemStack.getItemDamage()) && ItemStack.areItemStackTagsEqual(stack, itemStack)) {
                ItemStack newStack = stack.copy();
                if(newStack.stackSize == 1)
                    newStack = null;
                else
                    newStack.stackSize--;
                
                if(slot.setStack(newStack, !doRemove)) {
                    StackPointer result = new StackPointer();
                    result.slot = slot;
                    result.item = stack;
    
                    return result;
                }
            }
        }

		return null;
	}

	@Override
	public void openChest() {
	}

	@Override
	public void closeChest() {
	}

	/* FORGE INVENTORY */
	
	private class ForgeInventory implements ICustomInventory {

        @Override
        public int insert(ItemStack item, int amount, boolean simulate) {
            ItemStack stack = item.copy();
            stack.stackSize = amount;
            int added = new TransactorRoundRobin(TileAutoWorkbench.this).add(item, ForgeDirection.UP, !simulate).stackSize;
            return amount - added;
        }

        @Override
        public ItemStack extract(IStackFilter filter, int amount, boolean simulate) {
            ItemStack rv = extractItem(false, false);
            if(rv == null || !filter.matchesItem(rv) || rv.stackSize > amount)
                return null;
            if(simulate)
                return rv;
            else
                return extractItem(true, false);
        }
	}

    @Override
    public IForgeInventory getInventory(ForgeDirection side) {
        return new ForgeInventory();
    }

}
