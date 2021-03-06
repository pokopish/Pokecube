package pokecube.core.blocks.fossil;

import java.util.ArrayList;
import java.util.Collections;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import pokecube.core.PokecubeItems;

public class BlockFossilStone extends Block
{
    public BlockFossilStone()
    {
        super(Material.rock);
    }

    /** This returns a complete list of items dropped from this block.
     *
     * @param world
     *            The current world
     * @param x
     *            X Position
     * @param y
     *            Y Position
     * @param z
     *            Z Position
     * @param metadata
     *            Current metadata
     * @param fortune
     *            Breakers fortune level
     * @return A ArrayList containing all items this block drops */
    public ArrayList<ItemStack> getDrops(IBlockAccess world, BlockPos pos, IBlockState state, int fortune)
    {
        ArrayList<ItemStack> ret = new ArrayList<ItemStack>();
        if (Math.random() > 0.5)
        {
            ArrayList<ItemStack> fossils = new ArrayList<ItemStack>();
            fossils.addAll(PokecubeItems.fossils.keySet());
            Collections.shuffle(fossils);
            ret.add(fossils.get(0));
        }

        return ret;
    }
}
