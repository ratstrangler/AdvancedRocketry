package zmaster587.advancedRocketry.tile.multiblock;

import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import zmaster587.advancedRocketry.api.AdvancedRocketryBlocks;
import zmaster587.advancedRocketry.inventory.TextureResources;
import zmaster587.advancedRocketry.inventory.modules.IModularInventory;
import zmaster587.advancedRocketry.inventory.modules.ModuleBase;
import zmaster587.advancedRocketry.inventory.modules.ModuleProgress;
import zmaster587.advancedRocketry.recipe.RecipesMachine;
import zmaster587.libVulpes.interfaces.IRecipe;

public class TileLathe extends TileMultiblockMachine implements IModularInventory {

	public static final Object[][][] structure = { 
		{{'I', AdvancedRocketryBlocks.blockMotor, Blocks.air, 'c'}},
		{{'O', AdvancedRocketryBlocks.blockStructureBlock, AdvancedRocketryBlocks.blockStructureBlock, 'P'}},
	};
	
	@Override
	public List<IRecipe> getMachineRecipeList() {
		return RecipesMachine.getInstance().getRecipes(this.getClass());
	}
	@Override
	public Object[][][] getStructure() {
		return structure;
	}

	@Override
	public boolean shouldHideBlock(World world, int x, int y, int z, Block tile) {
		return true;
	}
	
	@Override
	protected float getTimeMultiplierForRecipe(IRecipe recipe) {
		return super.getTimeMultiplierForRecipe(recipe);
	}
	
	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return AxisAlignedBB.getBoundingBox(xCoord -3,yCoord -2, zCoord -3, xCoord + 3, yCoord + 2, zCoord + 3);
	}

	@Override
	public List<ModuleBase> getModules(int ID, EntityPlayer player) {
		List<ModuleBase> modules = super.getModules(ID, player);

		modules.add(new ModuleProgress(100, 40, 0, TextureResources.latheProgressBar, this));
		return modules;
	}

	@Override
	public String getMachineName() {
		return "tile.lathe.name";
	}
}
