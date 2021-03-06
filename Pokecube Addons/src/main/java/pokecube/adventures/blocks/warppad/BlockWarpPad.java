package pokecube.adventures.blocks.warppad;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import pokecube.core.PokecubeCore;
import thut.api.maths.Vector3;

public class BlockWarpPad extends Block implements ITileEntityProvider
{

    public BlockWarpPad()
    {
        super(Material.rock);
        this.setHardness(10);
        this.setCreativeTab(PokecubeCore.creativeTabPokecube);
    }

    @Override
    /** Called whenever an entity is walking on top of this block. Args: world,
     * x, y, z, entity */
    public void onEntityCollidedWithBlock(World world, BlockPos pos, Entity entity)
    {
        Vector3 loc = Vector3.getNewVector().set(pos);
        TileEntityWarpPad pad = (TileEntityWarpPad) loc.getTileEntity(world);
        if (!world.isRemote) pad.onStepped(entity);
    }

    /** Called when the block is placed in the world. */
    @Override
    public IBlockState onBlockPlaced(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ,
            int meta, EntityLivingBase placer)
    {
        Vector3 loc = Vector3.getNewVector().set(pos);
        TileEntity te = loc.getTileEntity(world);
        if (te != null && te instanceof TileEntityWarpPad)
        {
            TileEntityWarpPad pad = (TileEntityWarpPad) te;
            pad.setPlacer(placer);
        }
        return this.getStateFromMeta(meta);
    }

    @Override
    public TileEntity createNewTileEntity(World world_, int meta)
    {
        return new TileEntityWarpPad();
    }

}
