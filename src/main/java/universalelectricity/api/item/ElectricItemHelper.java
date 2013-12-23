package universalelectricity.api.item;

import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.ForgeDirection;
import universalelectricity.api.energy.IEnergyInterface;
import cofh.api.energy.IEnergyContainerItem;

/** Some helper functions for electric items.
 * 
 * @author Calclavia */
public class ElectricItemHelper
{

    /** Recharges an electric item.
     * 
     * @param joules - The joules being provided to the electric item
     * @return The total amount of joules provided by the provider. */
    public static long chargeItem(ItemStack itemStack, long joules, boolean doCharge)
    {
        if (itemStack != null)
        {
            if (itemStack.getItem() instanceof IElectricalItem)
            {
                return ((IElectricalItem) itemStack.getItem()).recharge(itemStack, joules, doCharge);
            }
        }

        return 0;
    }

    /** Decharges an electric item.
     * 
     * @param joules - The joules being withdrawn from the electric item
     * @return The total amount of joules the provider received. */
    public static long dischargeItem(ItemStack itemStack, long joules, boolean doDischarge)
    {
        if (itemStack != null)
        {
            if (itemStack.getItem() instanceof IElectricalItem)
            {
                return ((IElectricalItem) itemStack.getItem()).discharge(itemStack, joules, doDischarge);
            }
        }

        return 0;
    }

    /** Recharges an electric item.
     * 
     * @param joules - The joules being provided to the electric item
     * @return The total amount of joules provided by the provider. */
    @Deprecated
    public static long chargeItem(ItemStack itemStack, long joules)
    {
        return chargeItem(itemStack, joules, true);
    }

    /** Decharges an electric item.
     * 
     * @param joules - The joules being withdrawn from the electric item
     * @return The total amount of joules the provider received. */
    @Deprecated
    public static long dischargeItem(ItemStack itemStack, long joules)
    {
        return dischargeItem(itemStack, joules, true);
    }

    /** Returns an uncharged version of the electric item. Use this if you want the crafting recipe
     * to use a charged version of the electric item instead of an empty version of the electric
     * item
     * 
     * @return An electrical ItemStack with a specific charge. */
    public static ItemStack getWithCharge(ItemStack itemStack, long joules)
    {
        if (itemStack != null)
        {
            if (itemStack.getItem() instanceof IElectricalItem)
            {
                ((IElectricalItem) itemStack.getItem()).setElectricity(itemStack, joules);
                return itemStack;
            }
        }

        return itemStack;
    }

    public static ItemStack getWithCharge(Item item, long joules)
    {
        return getWithCharge(new ItemStack(item), joules);
    }

    public static ItemStack getCloneWithCharge(ItemStack itemStack, long joules)
    {
        return getWithCharge(itemStack.copy(), joules);
    }

    public static ItemStack getUncharged(ItemStack itemStack)
    {
        return getWithCharge(itemStack, 0);
    }

    public static ItemStack getUncharged(Item item)
    {
        return getUncharged(new ItemStack(item));
    }
}