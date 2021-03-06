package universalelectricity.core;

import ic2.api.energy.tile.IEnergySink;
import ic2.api.energy.tile.IEnergySource;
import ic2.api.item.ISpecialElectricItem;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Logger;

import net.minecraftforge.common.Configuration;
import universalelectricity.api.CompatibilityModule;
import universalelectricity.api.CompatibilityType;
import universalelectricity.api.UniversalElectricity;
import universalelectricity.compatibility.ModuleUniversalElectricity;
import universalelectricity.core.asm.TemplateInjectionManager;
import universalelectricity.core.asm.UniversalTransformer;
import universalelectricity.core.asm.template.item.TemplateICItem;
import universalelectricity.core.asm.template.item.TemplateTEItem;
import universalelectricity.core.asm.template.tile.TemplateBCTile;
import universalelectricity.core.asm.template.tile.TemplateICTile;
import universalelectricity.core.asm.template.tile.TemplateTETile;
import universalelectricity.core.net.NetworkTickHandler;
import buildcraft.api.power.IPowerReceptor;
import cofh.api.energy.IEnergyContainerItem;
import cofh.api.energy.IEnergyHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.IFMLCallHook;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.TransformerExclusions;
import cpw.mods.fml.relauncher.Side;

@Mod(modid = UELoader.ID, version = UniversalElectricity.VERSION, name = UELoader.NAME, dependencies = "before:ForgeMultipart")
@TransformerExclusions({ "universalelectricity.core.asm", "universalelectricity.core.asm.template" })
public class UELoader implements IFMLLoadingPlugin, IFMLCallHook
{
    public static final String ID = "UniversalElectricity";
    public static final String NAME = "Universal Electricity";
    /** The Universal Electricity configuration file. */
    public static Configuration CONFIGURATION;

    @SidedProxy(clientSide = "universalelectricity.core.ClientProxy", serverSide = "universalelectricity.core.CommonProxy")
    public static CommonProxy proxy;

    @Mod.Metadata(ID)
    public static ModMetadata metadata;

    public static final Logger LOGGER = Logger.getLogger(NAME);

    @EventHandler
    public void preInit(FMLPreInitializationEvent evt)
    {
        /** Loads the configuration and sets all the values. */
        CONFIGURATION = new Configuration(new File(Loader.instance().getConfigDir(), "UniversalElectricity.cfg"));
        CONFIGURATION.load();
        CompatibilityType.THERMAL_EXPANSION.reciprocal_ratio = CONFIGURATION.get("Compatibility", "Thermal Expansion Conversion Ratio", CompatibilityType.THERMAL_EXPANSION.reciprocal_ratio).getDouble(CompatibilityType.THERMAL_EXPANSION.reciprocal_ratio);
        CompatibilityType.INDUSTRIALCRAFT.reciprocal_ratio = CONFIGURATION.get("Compatibility", "IndustrialCraft Conversion Ratio", CompatibilityType.INDUSTRIALCRAFT.reciprocal_ratio).getDouble(CompatibilityType.INDUSTRIALCRAFT.reciprocal_ratio);
        CompatibilityType.BUILDCRAFT.reciprocal_ratio = CONFIGURATION.get("Compatibility", "BuildCraft Conversion Ratio", CompatibilityType.BUILDCRAFT.reciprocal_ratio).getDouble(CompatibilityType.BUILDCRAFT.reciprocal_ratio);

        CompatibilityType.THERMAL_EXPANSION.ratio = 1d / CompatibilityType.THERMAL_EXPANSION.reciprocal_ratio;
        CompatibilityType.INDUSTRIALCRAFT.ratio = 1d / CompatibilityType.INDUSTRIALCRAFT.reciprocal_ratio;
        CompatibilityType.BUILDCRAFT.ratio = 1d / CompatibilityType.BUILDCRAFT.reciprocal_ratio;

        CompatibilityModule.register(new ModuleUniversalElectricity());

        for (CompatibilityType compatibility : CompatibilityType.values())
        {
            compatibility.isModuleEnabled = CONFIGURATION.get("Compatibility", "Load " + compatibility.moduleName + " Module", true).getBoolean(true);

            if (compatibility.isModuleEnabled)
            {
                try
                {
                    CompatibilityModule.register((CompatibilityModule) Class.forName("universalelectricity.compatibility.Module" + compatibility.moduleName).newInstance());
                }
                catch (Exception e)
                {
                    System.out.println("[Universal Electricity] Failed to load module: " + compatibility.moduleName);
                    e.printStackTrace();
                }
            }
        }

        CONFIGURATION.save();

        TickRegistry.registerTickHandler(NetworkTickHandler.INSTANCE, Side.SERVER);
        proxy.init();

        /** Metadata */
        metadata.modId = ID;
        metadata.name = NAME;
        metadata.description = "Universal Electricity is a Minecraft modding library that provides an easy, flexible energy framework and compatibility bridges between various energy systems in Minecraft.";

        metadata.url = "http://www.universalelectricity.com/";
        metadata.version = UniversalElectricity.VERSION + "." + UniversalElectricity.BUILD_VERSION;
        metadata.authorList = Arrays.asList(new String[] { "Calclavia", "DarkCow", "tgame14" });
        metadata.credits = "Please visit the website.";
        metadata.autogenerated = false;
    }

    /** Return a list of classes that implements the IClassTransformer interface
     * 
     * @return a list of classes that implements the IClassTransformer interface */
    @Override
    public String[] getASMTransformerClass()
    {
        return new String[] { UniversalTransformer.class.getName() };
    }

    /** Return a class name that implements "ModContainer" for injection into the mod list The
     * "getName" function should return a name that other mods can, if need be, depend on.
     * Trivially, this modcontainer will be loaded before all regular mod containers, which means it
     * will be forced to be "immutable" - not susceptible to normal sorting behaviour. All other mod
     * behaviours are available however- this container can receive and handle normal loading events */
    @Override
    public String getModContainerClass()
    {
        return null;
    }

    /** Return the class name of an implementor of "IFMLCallHook", that will be run, in the main
     * thread, to perform any additional setup this coremod may require. It will be run
     * <strong>prior</strong> to Minecraft starting, so it CANNOT operate on minecraft itself. The
     * game will deliberately crash if this code is detected to trigger a minecraft class loading
     * (TODO: implement crash ;) ) */
    @Override
    public String getSetupClass()
    {
        return UELoader.class.getName();
    }

    /** Inject coremod data into this coremod This data includes: "mcLocation" : the location of the
     * minecraft directory, "coremodList" : the list of coremods "coremodLocation" : the file this
     * coremod loaded from, */
    @Override
    public void injectData(Map<String, Object> data)
    {
    }

    public String[] getLibraryRequestClass()
    {
        return null;
    }

    @Override
    public Void call() throws Exception
    {
        String asmICTiles = System.getProperty("asmICTile");
        String asmTETiles = System.getProperty("asmTETile");
        String asmBCTiles = System.getProperty("asmBCTile");
        String diable = System.getProperty("asmUEDsiable");

        if (asmTETiles == null || asmTETiles.equalsIgnoreCase("true") || asmTETiles.equalsIgnoreCase("t"))
        {
            if (asmTETiles == null || asmTETiles.equalsIgnoreCase("true") || asmTETiles.equalsIgnoreCase("t"))
                TemplateInjectionManager.registerTileTemplate(CompatibilityType.THERMAL_EXPANSION.moduleName, TemplateTETile.class, IEnergyHandler.class);
            if (asmICTiles == null || asmICTiles.equalsIgnoreCase("true") || asmICTiles.equalsIgnoreCase("t"))
                TemplateInjectionManager.registerTileTemplate(CompatibilityType.INDUSTRIALCRAFT.moduleName, TemplateICTile.class, IEnergySink.class, IEnergySource.class);
            if (asmBCTiles == null || asmBCTiles.equalsIgnoreCase("true") || asmBCTiles.equalsIgnoreCase("t"))
                TemplateInjectionManager.registerTileTemplate(CompatibilityType.BUILDCRAFT.moduleName, TemplateBCTile.class, IPowerReceptor.class);

            TemplateInjectionManager.registerItemTemplate(CompatibilityType.THERMAL_EXPANSION.moduleName, TemplateTEItem.class, IEnergyContainerItem.class);

            TemplateInjectionManager.registerItemTemplate(CompatibilityType.INDUSTRIALCRAFT.moduleName, TemplateICItem.class, ISpecialElectricItem.class);
        }
        return null;
    }
}