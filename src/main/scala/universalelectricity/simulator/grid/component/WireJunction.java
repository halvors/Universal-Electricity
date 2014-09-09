package universalelectricity.simulator.grid.component;

import net.minecraftforge.common.util.ForgeDirection;
import universalelectricity.simulator.grid.LinkedGrid;

import java.util.HashMap;

/**
 * @author DarkCow
 */
public class WireJunction extends NetworkPart
{
    HashMap<Object, ForgeDirection> connectionMap;
    HashMap<ForgeDirection, Boolean> inputMap;
    NetworkNode node = null;

    public WireJunction(LinkedGrid sim, NetworkNode node)
    {
        super(sim);
        this.node = node;
    }

    public void add(Object object, ForgeDirection side)
    {
        connectionMap.put(object, side);
    }

    public void remove(Object object)
    {
        if(connectionMap.containsKey(object))
        {
            ForgeDirection direction = connectionMap.get(object);
            connectionMap.remove(object);
            inputMap.remove(direction);
        }
    }

    @Override
    public boolean hasInputDevices()
    {
        for(Object object : connectionMap.keySet())
        {
            if(object instanceof IComponent && ((IComponent) object).hasInputDevices())
            {
                return true;
            }else
            {
                return sim.isInputDevice(object, connectionMap.get(object));
            }
        }
        return false;
    }

}