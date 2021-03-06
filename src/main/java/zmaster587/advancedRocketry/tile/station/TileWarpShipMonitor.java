package zmaster587.advancedRocketry.tile.station;

import io.netty.buffer.ByteBuf;

import java.util.LinkedList;
import java.util.List;

import cpw.mods.fml.relauncher.Side;
import zmaster587.advancedRocketry.AdvancedRocketry;
import zmaster587.advancedRocketry.api.Configuration;
import zmaster587.advancedRocketry.api.stations.ISpaceObject;
import zmaster587.advancedRocketry.api.stations.SpaceObjectManager;
import zmaster587.advancedRocketry.inventory.GuiHandler.guiId;
import zmaster587.advancedRocketry.inventory.TextureResources;
import zmaster587.advancedRocketry.inventory.modules.IButtonInventory;
import zmaster587.advancedRocketry.inventory.modules.IModularInventory;
import zmaster587.advancedRocketry.inventory.modules.IProgressBar;
import zmaster587.advancedRocketry.inventory.modules.ISelectionNotify;
import zmaster587.advancedRocketry.inventory.modules.ModuleBase;
import zmaster587.advancedRocketry.inventory.modules.ModuleButton;
import zmaster587.advancedRocketry.inventory.modules.ModuleImage;
import zmaster587.advancedRocketry.inventory.modules.ModulePlanetSelector;
import zmaster587.advancedRocketry.inventory.modules.ModuleProgress;
import zmaster587.advancedRocketry.inventory.modules.ModuleScaledImage;
import zmaster587.advancedRocketry.inventory.modules.ModuleText;
import zmaster587.advancedRocketry.network.PacketHandler;
import zmaster587.advancedRocketry.network.PacketMachine;
import zmaster587.advancedRocketry.stations.SpaceObject;
import zmaster587.advancedRocketry.tile.multiblock.TileWarpCore;
import zmaster587.advancedRocketry.util.ITilePlanetSystemSelectable;
import zmaster587.advancedRocketry.client.render.util.IndicatorBarImage;
import zmaster587.advancedRocketry.client.render.util.ProgressBarImage;
import	zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.dimension.DimensionProperties;
import zmaster587.libVulpes.util.BlockPosition;
import zmaster587.libVulpes.util.INetworkMachine;
import zmaster587.libVulpes.util.IconResource;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.ForgeDirection;

public class TileWarpShipMonitor extends TileEntity implements IModularInventory, ISelectionNotify, INetworkMachine, IButtonInventory, IProgressBar {

	protected ModulePlanetSelector container;
	private ModuleText canWarp;
	DimensionProperties dimCache;
	private SpaceObject station;

	public TileWarpShipMonitor() {
	}


	private SpaceObject getSpaceObject() {
		if(station == null && worldObj.provider.dimensionId == Configuration.spaceDimId) {
			ISpaceObject object = SpaceObjectManager.getSpaceManager().getSpaceStationFromBlockCoords(xCoord, zCoord);
			if(object instanceof SpaceObject)
				station = (SpaceObject) object;
		}
		return station;
	}


	protected int getTravelCost() {
		DimensionProperties properties = getSpaceObject().getProperties();

		DimensionProperties destProperties = DimensionManager.getInstance().getDimensionProperties(getSpaceObject().getDestOrbitingBody());
		while(destProperties.isMoon())
			destProperties = destProperties.getParentProperties();

		//TODO: actual trig
		if(properties.getStar().getId() == destProperties.getStar().getId()){
			return Math.abs(properties.orbitalDist - destProperties.orbitalDist);
		}
		return Integer.MAX_VALUE;
	}

	@Override
	public List<ModuleBase> getModules(int ID, EntityPlayer player) {
		List<ModuleBase> modules = new LinkedList<ModuleBase>();


		if(ID == guiId.MODULARNOINV.ordinal()) {

			ISpaceObject station = getSpaceObject();
			boolean isOnStation = station != null;
			ResourceLocation location;
			boolean hasAtmo = true;
			String planetName;

			if(isOnStation) {
				DimensionProperties properties = DimensionManager.getInstance().getDimensionProperties(station.getOrbitingPlanetId());
				location = properties.getPlanetIcon();
				hasAtmo = properties.hasAtmosphere();
				planetName = properties.getName();
			}
			else {
				location = DimensionManager.getInstance().getDimensionProperties(worldObj.provider.dimensionId).getPlanetIcon();
				planetName = DimensionManager.getInstance().getDimensionProperties(worldObj.provider.dimensionId).getName();

				if(planetName.isEmpty())
					planetName = "???";
			}


			//Source planet
			int baseX = 10;
			int baseY = 20;
			int sizeX = 70;
			int sizeY = 70;

			if(worldObj.isRemote) {
				modules.add(new ModuleScaledImage(baseX,baseY,sizeX,sizeY, TextureResources.starryBG));
				modules.add(new ModuleScaledImage(baseX + 10,baseY + 10,sizeX - 20, sizeY - 20, location));


				if(hasAtmo)
					modules.add(new ModuleScaledImage(baseX + 10,baseY + 10,sizeX - 20, sizeY - 20,0.4f, DimensionProperties.getAtmosphereResource()));

				modules.add(new ModuleText(baseX + 4, baseY + 4, "Orbiting:", 0xFFFFFF));
				modules.add(new ModuleText(baseX + 4, baseY + 16, planetName, 0xFFFFFF));

				//Border
				modules.add(new ModuleScaledImage(baseX - 3,baseY,3,sizeY, TextureResources.verticalBar));
				modules.add(new ModuleScaledImage(baseX + sizeX, baseY, -3,sizeY, TextureResources.verticalBar));
				modules.add(new ModuleScaledImage(baseX,baseY,70,3, TextureResources.horizontalBar));
				modules.add(new ModuleScaledImage(baseX,baseY + sizeY - 3,70,-3, TextureResources.horizontalBar));
			}
			modules.add(new ModuleButton(baseX - 3, baseY + sizeY, 0, "Select Planet", this, TextureResources.buttonBuild, sizeX + 6, 16));


			//Status text
			modules.add(new ModuleText(baseX, baseY + sizeY + 20, "Core Status:", 0x1b1b1b));
			boolean flag = isOnStation && getSpaceObject().getFuelAmount() >= getTravelCost() && getSpaceObject().hasUsableWarpCore();
			canWarp = new ModuleText(baseX, baseY + sizeY + 30, (isOnStation && getSpaceObject().getOrbitingPlanetId() == getSpaceObject().getDestOrbitingBody()) ? "Nowhere to go" : flag ? "Ready!" : "Not ready", flag ? 0x1baa1b : 0xFF1b1b);
			modules.add(canWarp);
			modules.add(new ModuleProgress(baseX, baseY + sizeY + 40, 10, new IndicatorBarImage(70, 58, 53, 8, 122, 58, 5, 8, ForgeDirection.EAST, TextureResources.progressBars), this));
			modules.add(new ModuleText(baseX + 82, baseY + sizeY + 20, "Fuel Cost:", 0x1b1b1b));
			modules.add(new ModuleText(baseX + 82, baseY + sizeY + 30, flag ? String.valueOf(getTravelCost()) : "N/A", 0x1b1b1b));


			//DEST planet
			baseX = 94;
			baseY = 20;
			sizeX = 70;
			sizeY = 70;
			ModuleButton warp = new ModuleButton(baseX - 3, baseY + sizeY,1, "Warp!", this ,TextureResources.buttonBuild, sizeX + 6, 16);

			modules.add(warp);

			if(worldObj.isRemote)
				modules.add(new ModuleScaledImage(baseX,baseY,sizeX,sizeY, TextureResources.starryBG));

			if(dimCache == null && isOnStation && station.getOrbitingPlanetId() != -1 )
				dimCache = DimensionManager.getInstance().getDimensionProperties(station.getOrbitingPlanetId());

			if(dimCache != null) {


				hasAtmo = dimCache.hasAtmosphere();
				planetName = dimCache.getName();
				location = dimCache.getPlanetIcon();


				if(worldObj.isRemote ) {
					modules.add(new ModuleScaledImage(baseX + 10,baseY + 10,sizeX - 20, sizeY - 20, location));

					if(hasAtmo)
						modules.add(new ModuleScaledImage(baseX + 10,baseY + 10,sizeX - 20, sizeY - 20,0.4f, DimensionProperties.getAtmosphereResource()));

				}

				modules.add(new ModuleText(baseX + 4, baseY + 4, "Dest:", 0xFFFFFF));
				modules.add(new ModuleText(baseX + 4, baseY + 16, planetName, 0xFFFFFF));


			}
			else {
				modules.add(new ModuleText(baseX + 4, baseY + 4, "Dest:", 0xFFFFFF));
				modules.add(new ModuleText(baseX + 4, baseY + 16, "None", 0xFFFFFF));
			}

			if(worldObj.isRemote) {
				//Border
				modules.add(new ModuleScaledImage(baseX - 3,baseY,3,sizeY, TextureResources.verticalBar));
				modules.add(new ModuleScaledImage(baseX + sizeX, baseY, -3,sizeY, TextureResources.verticalBar));
				modules.add(new ModuleScaledImage(baseX,baseY,70,3, TextureResources.horizontalBar));
				modules.add(new ModuleScaledImage(baseX,baseY + sizeY - 3,70,-3, TextureResources.horizontalBar));
			}


		}
		else if (ID == guiId.MODULARFULLSCREEN.ordinal()) {
			//Open planet selector menu
			container = new ModulePlanetSelector(worldObj.provider.dimensionId, TextureResources.starryBG, this);
			container.setOffset(1000, 1000);
			modules.add(container);
		}
		return modules;
	}

	@Override
	public String getModularInventoryName() {
		return "tile.stationmonitor.name";
	}

	@Override
	public boolean canInteractWithContainer(EntityPlayer entity) {
		return true;
	}

	@Override
	public void onInventoryButtonPressed(int buttonId) {
		if(getSpaceObject() != null) {
			if(buttonId == 0)
				PacketHandler.sendToServer(new PacketMachine(this, (byte)0));
			else if(buttonId == 1) {
				PacketHandler.sendToServer(new PacketMachine(this, (byte)2));
			}
		}
	}

	@Override
	public void writeDataToNetwork(ByteBuf out, byte id) {
		if(id == 1 || id == 3)
			out.writeInt(container.getSelectedSystem());
	}

	@Override
	public void readDataFromNetwork(ByteBuf in, byte packetId,
			NBTTagCompound nbt) {
		if(packetId == 1 || packetId == 3)
			nbt.setInteger("id", in.readInt());
	}

	@Override
	public void useNetworkData(EntityPlayer player, Side side, byte id,
			NBTTagCompound nbt) {
		if(id == 0)
			player.openGui(AdvancedRocketry.instance, guiId.MODULARFULLSCREEN.ordinal(), worldObj, this.xCoord, this.yCoord, this.zCoord);
		else if(id == 1 || id == 3) {
			int dimId = nbt.getInteger("id");
			container.setSelectedSystem(dimId);
			selectSystem(dimId);

			//Update known planets
			markDirty();
			worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
			if(id == 3)
				player.openGui(AdvancedRocketry.instance, guiId.MODULARNOINV.ordinal(), worldObj, this.xCoord, this.yCoord, this.zCoord);
		}
		else if(id == 2) {
			SpaceObject station = getSpaceObject();

			if(station != null && station.useFuel(getTravelCost()) != 0 && station.hasUsableWarpCore()) {
				SpaceObjectManager.getSpaceManager().moveStationToBody(station, station.getDestOrbitingBody(), 200);
				for(BlockPosition vec : station.getWarpCoreLocations()) {
					TileEntity tile = worldObj.getTileEntity(vec.x, vec.y, vec.z);
					if(tile != null && tile instanceof TileWarpCore) {
						((TileWarpCore)tile).onInventoryUpdated();
					}
				}
			}
		}
	}

	@Override
	public void onSelectionConfirmed(Object sender) {
		//Container Cannot be null at this time
		onSelected(sender);
		PacketHandler.sendToServer(new PacketMachine(this, (byte)3));
	}

	@Override
	public void onSelected(Object sender) {
		selectSystem(container.getSelectedSystem());
	}

	private void selectSystem(int id) {

		if(getSpaceObject().getOrbitingPlanetId() == SpaceObjectManager.WARPDIMID) {
			dimCache = null;
			return;
		}

		if(id == -1)
			dimCache = null;
		else {
			dimCache = DimensionManager.getInstance().getDimensionProperties(container.getSelectedSystem());

			ISpaceObject station = SpaceObjectManager.getSpaceManager().getSpaceStationFromBlockCoords(this.xCoord, this.zCoord);
			if(station != null) {
				station.setDestOrbitingBody(id);
			}
		}
	}

	@Override
	public void onSystemFocusChanged(Object sender) {
		PacketHandler.sendToServer(new PacketMachine(this, (byte)1));
	}

	
	@Override
	public float getNormallizedProgress(int id) {
		return getProgress(id)/(float)getTotalProgress(id);
	}

	@Override
	public void setProgress(int id, int progress) {
		if(id == 10)
			if(getSpaceObject() != null)
				getSpaceObject().setFuelAmount(progress);
	}

	@Override
	public int getProgress(int id) {
		if(id == 10) {
			if(getSpaceObject() != null)
				return getSpaceObject().getFuelAmount();
		}
		return 0;
	}

	@Override
	public int getTotalProgress(int id) {
		if(id == 10) {
			if(getSpaceObject() != null)
				return getSpaceObject().getMaxFuelAmount();
		}
		return 0;
	}

	@Override
	public void setTotalProgress(int id, int progress) {
	}
}
