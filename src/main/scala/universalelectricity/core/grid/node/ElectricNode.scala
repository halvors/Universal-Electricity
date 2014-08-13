package universalelectricity.core.grid.node

import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.ForgeDirection
import universalelectricity.api.core.grid.INodeProvider
import universalelectricity.api.core.grid.electric.{IElectricNode, IResistance}

/**
 * The node used for electrical objects.
 *
 * @author Calclavia
 */
class ElectricNode(parent: INodeProvider) extends EnergyNode(parent) with IElectricNode with IResistance
{
  final val timeMultiplier = 20D

  protected var amperage = 0D
  protected var voltage = 0D
  private var resistance = 0.01D

  override def getResistance = resistance

  def setResistance(value: Double)
  {
    resistance = value
  }

  override def getVoltage: Double =
  {
    return voltage
  }

  def setCurrent(amperage: Double)
  {
    this.amperage = amperage
  }

  override def addEnergy(wattage: Double, doAdd: Boolean) : Double =
  {
    setCurrent(getEnergy / getVoltage)
    return super.addEnergy(wattage, doAdd)
  }

  override def removeEnergy(wattage: Double, doRemove: Boolean) : Double =
  {
    setCurrent(getEnergy / getVoltage)
    return super.removeEnergy(wattage, doRemove)
  }

  override def load(nbt: NBTTagCompound)
  {
    super.load(nbt)
    voltage = nbt.getDouble("voltage")
    amperage = nbt.getDouble("amperage")
  }

  override def save(nbt: NBTTagCompound)
  {
    super.save(nbt)
    nbt.setDouble("voltage", voltage)
    nbt.setDouble("amperage", amperage)
  }

  /** Is the object a or contains an electric node */
  def isElectricNode(obj : Any, accessSide : ForgeDirection) : Boolean =
  {
    if(obj.isInstanceOf[INodeProvider])
    {
      if(obj.asInstanceOf[INodeProvider].getNode(classOf[IElectricNode], accessSide).isInstanceOf[IElectricNode])
      {
        return true;
      }
    }else if(obj.isInstanceOf[IElectricNode])
    {
      return true
    }
    return false
  }

}