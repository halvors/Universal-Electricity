package universalelectricity.core.net;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.common.MinecraftForge;
import universalelectricity.api.CompatibilityModule;
import universalelectricity.api.electricity.ElectricalEvent.EnergyUpdateEvent;
import universalelectricity.api.energy.EnergyNetworkLoader;
import universalelectricity.api.energy.IConductor;
import universalelectricity.api.energy.IEnergyNetwork;
import universalelectricity.api.net.IConnector;

/** @author Calclavia */
public class EnergyNetwork extends Network<IEnergyNetwork, IConductor, Object> implements IEnergyNetwork
{
    /** The energy to be distributed on the next update. */
    private long energyBuffer;

    /** The maximum buffer that the network can take. It is the average of all energy capacitance of
     * the conductors. */
    private long energyBufferCapacity;

    /** The total energy loss of this network. The loss is based on the loss in each conductor. */
    private long networkEnergyLoss;

    /** The total energy buffer in the last tick. */
    private long lastEnergyBuffer;

    /** The direction in which a conductor is placed relative to a specific conductor. */
    private HashMap<Object, EnumSet<ForgeDirection>> handlerDirectionMap = new HashMap<Object, EnumSet<ForgeDirection>>();

    @Override
    public void addConnector(IConductor connector)
    {
        connector.setNetwork(this);
        super.addConnector(connector);
    }

    @Override
    public void update()
    {
        EnergyUpdateEvent evt = new EnergyUpdateEvent(this);
        MinecraftForge.EVENT_BUS.post(evt);

        if (!evt.isCanceled())
        {
            int handlerSize = this.handlerSet.size();
            this.lastEnergyBuffer = this.energyBuffer;
            long totalUsableEnergy = this.energyBuffer - this.networkEnergyLoss;
            long remainingUsableEnergy = totalUsableEnergy;

            for (Entry<Object, EnumSet<ForgeDirection>> entry : handlerDirectionMap.entrySet())
            {
                if (entry.getValue() != null)
                {
                    for (ForgeDirection direction : entry.getValue())
                    {
                        if (remainingUsableEnergy >= 0)
                        {
                            remainingUsableEnergy -= CompatibilityModule.receiveEnergy(entry.getKey(), direction, (totalUsableEnergy / handlerSize) + totalUsableEnergy % handlerSize, true);
                        }
                    }
                }
            }

            this.energyBuffer = Math.max(remainingUsableEnergy, 0);
        }
    }

    @Override
    public boolean canUpdate()
    {
        return this.getConnectors().size() > 0 && this.getNodes().size() > 0 && this.energyBuffer > 0 && this.energyBufferCapacity > 0;
    }

    @Override
    public boolean continueUpdate()
    {
        return this.canUpdate();
    }

    @Override
    public long getRequest()
    {
        long energyRequest = 0;
        int handlerSize = this.handlerSet.size();

        if (handlerSize > 0)
        {
            for (Entry<Object, EnumSet<ForgeDirection>> entry : handlerDirectionMap.entrySet())
            {
                if (entry.getValue() != null)
                {
                    for (ForgeDirection direction : entry.getValue())
                        energyRequest += CompatibilityModule.receiveEnergy(entry.getKey(), direction, Long.MAX_VALUE, false);
                }
            }
        }

        return energyRequest;
    }

    @Override
    public long getEnergyLoss()
    {
        return this.networkEnergyLoss;
    }

    /** Clears all cache and reconstruct the network. */
    @Override
    public void reconstruct()
    {
        if (this.connectorSet.size() > 0)
        {
            this.handlerSet.clear();
            this.handlerDirectionMap.clear();
            Iterator<IConductor> it = this.connectorSet.iterator();

            while (it.hasNext())
            {
                IConductor conductor = it.next();
                conductor.setNetwork(this);

                for (int i = 0; i < conductor.getConnections().length; i++)
                {
                    Object obj = conductor.getConnections()[i];

                    if (obj != null && !(obj instanceof IConductor))
                    {
                        if (CompatibilityModule.isHandler(obj))
                        {
                            EnumSet<ForgeDirection> set = this.handlerDirectionMap.get(obj);
                            if (set == null)
                            {
                                set = EnumSet.noneOf(ForgeDirection.class);
                            }
                            this.handlerSet.add(obj);
                            set.add(ForgeDirection.getOrientation(i).getOpposite());
                            this.handlerDirectionMap.put(obj, set);
                        }
                    }
                }

                this.energyBufferCapacity += conductor.getEnergyCapacitance();
                this.networkEnergyLoss += conductor.getEnergyLoss();
            }

            this.energyBufferCapacity /= this.connectorSet.size();

            if (this.handlerSet.size() > 0)
            {
                NetworkTickHandler.addNetwork(this);
            }
        }
    }

    @Override
    public IEnergyNetwork merge(IEnergyNetwork network)
    {
        if (network != null && network != this)
        {
            IEnergyNetwork newNetwork = new EnergyNetwork();
            newNetwork.getConnectors().addAll(this.getConnectors());
            newNetwork.getConnectors().addAll(network.getConnectors());

            network.getConnectors().clear();
            this.connectorSet.clear();

            newNetwork.reconstruct();

            return newNetwork;
        }

        return null;
    }

    @Override
    public void split(IConductor splitPoint)
    {
        System.out.println("Splitting network");
        this.removeConnector(splitPoint);
        this.reconstruct();

        /** Loop through the connected blocks and attempt to see if there are connections between the
         * two points elsewhere. */
        Object[] connectedBlocks = splitPoint.getConnections();

        for (int i = 0; i < connectedBlocks.length; i++)
        {
            Object connectedBlockA = connectedBlocks[i];

            if (connectedBlockA instanceof IConnector)
            {
                for (int ii = 0; ii < connectedBlocks.length; ii++)
                {
                    final Object connectedBlockB = connectedBlocks[ii];

                    if (connectedBlockA != connectedBlockB && connectedBlockB instanceof IConnector)
                    {
                        ConnectionPathfinder finder = new ConnectionPathfinder((IConnector) connectedBlockB, splitPoint);
                        finder.findNodes((IConnector) connectedBlockA);

                        if (finder.results.size() <= 0)
                        {
                            try
                            {
                                /** The connections A and B are not connected anymore. Give them both
                                 * a new common network. */
                                IEnergyNetwork newNetwork = EnergyNetworkLoader.getNewNetwork();

                                for (IConnector node : finder.closedSet)
                                {
                                    if (node != splitPoint)
                                    {
                                        newNetwork.addConnector((IConductor) node);
                                    }
                                }
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }

                        }
                    }
                }
            }
        }
    }

    @Override
    public long produce(long amount)
    {
        if (amount > 0)
        {
            long prevEnergyStored = this.energyBuffer;
            this.energyBuffer = Math.min(this.energyBuffer + amount, this.energyBufferCapacity);
            NetworkTickHandler.addNetwork(this);
            return Math.max(this.energyBuffer - prevEnergyStored, 0);
        }
        return 0;
    }

    @Override
    public long getLastBuffer()
    {
        return this.lastEnergyBuffer;
    }
}
