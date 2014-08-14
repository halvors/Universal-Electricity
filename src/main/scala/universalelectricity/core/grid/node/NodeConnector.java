package universalelectricity.core.grid.node;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import universalelectricity.api.core.grid.IConnector;
import universalelectricity.api.core.grid.INode;
import universalelectricity.api.core.grid.INodeProvider;
import universalelectricity.core.grid.Node;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A Node that is designed to connect to other nodes, tiles, or objects.
 * @author Darkguardsman
 */
public class NodeConnector extends Node implements IConnector
{
    protected byte connectionMap = Byte.parseByte("111111", 2);
    protected WeakHashMap<Object, ForgeDirection> connections = new WeakHashMap<Object, ForgeDirection>();

    public NodeConnector(INodeProvider parent)
    {
        super(parent);
    }

    @Override
    public Map<Object, ForgeDirection> getConnections(Class<? extends INode> node)
    {
        return connections;
    }

    @Override
    public boolean canConnect(ForgeDirection direction, Object object)
    {
        return object != null && isValidConnection(object) && canConnect(direction);
    }

    public boolean canConnect(ForgeDirection from)
    {
        return (connectionMap & (1 << from.ordinal())) != 0;
    }

    public boolean isValidConnection(Object object)
    {
        return object.getClass().isAssignableFrom(getClass());
    }

    @Override
    public void deconstruct()
    {
        super.deconstruct();
        connections = null;
    }

    @Override
    public void reconstruct()
    {
        super.reconstruct();
        for(ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS)
        {
            if(canConnect(direction.getOpposite()))
            {
                TileEntity tile = position().add(direction).getTileEntity();
                if(isValidConnection(tile))
                {
                    connections.put(tile, direction);
                }
            }
        }
    }
}