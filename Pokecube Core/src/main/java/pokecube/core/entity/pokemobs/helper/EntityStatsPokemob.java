/**
 *
 */
package pokecube.core.entity.pokemobs.helper;

import java.io.IOException;
import java.util.Random;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityOwnable;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.potion.Potion;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import pokecube.core.Mod_Pokecube_Helper;
import pokecube.core.PokecubeCore;
import pokecube.core.database.Database;
import pokecube.core.database.Pokedex;
import pokecube.core.database.PokedexEntry;
import pokecube.core.database.abilities.Ability;
import pokecube.core.database.abilities.AbilityManager;
import pokecube.core.entity.pokemobs.EntityPokemob;
import pokecube.core.events.KillEvent;
import pokecube.core.events.LevelUpEvent;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.Move_Base;
import pokecube.core.interfaces.Nature;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.moves.PokemobDamageSource;
import pokecube.core.network.PokecubePacketHandler;
import pokecube.core.network.pokemobs.PokemobPacketHandler.MessageServer;
import pokecube.core.utils.PokeType;
import pokecube.core.utils.PokecubeSerializer;
import pokecube.core.utils.Tools;

/** @author Manchou */
public abstract class EntityStatsPokemob extends EntityTameablePokemob implements IEntityAdditionalSpawnData
{
    protected Ability ability;

    double moveSpeed;

    byte[]           ivs      = new byte[] { 0, 0, 0, 0, 0, 0 };
    protected Nature nature   = Nature.HARDY;
    public int       oldLevel = 0;
    PokedexEntry     entry;
    String           forme    = "";

    protected Entity transformedTo;

    /** The happiness value of the pokemob */
    private int bonusHappiness = 0;

    boolean wasShadow = false;

    boolean        isAncient        = false;
    /** The higher this value, the more likely for mobs to range in colour. It
     * is very sensitive to the size of this number. */
    private double colourDiffFactor = 0.25;

    /** Used for the random colour differences */
    int[] rgba = { 255, 255, 255, 255 };

    /** Used for if there is a special texture */
    public boolean shiny = false;

    public EntityStatsPokemob(World world)
    {
        super(world);
    }

    @Override
    public void init(int nb)
    {
        super.init(nb);
        this.pokedexNb = nb;
        getPokedexEntry();

        setEVs(PokecubeSerializer.noEVs); // JAVA STUPID AND BYTE GOES FROM -128
                                          // -> 127
        setIVs(new byte[] { Tools.getRandomIV(rand), Tools.getRandomIV(rand), Tools.getRandomIV(rand),
                Tools.getRandomIV(rand), Tools.getRandomIV(rand), Tools.getRandomIV(rand) });
        if (this.isAncient())
        {
            setIVs(new byte[] { 31, 31, 31, 31, 31, 31 });
        }
        if (nb == 132 && worldObj != null && !worldObj.isRemote)
        {
            boolean glitch = Math.random() > 0.95;
            getEntityData().setBoolean("dittotag", glitch);
        }

        if (PokecubeCore.isOnClientSide()) this.setHealth(getMaxHealth());
        else this.setHealth(0);
        nature = Nature.values()[(byte) (new Random()).nextInt(25)];
        setRandomColour();
    }

    @Override
    protected void applyEntityAttributes()
    {
        super.applyEntityAttributes();
        // Max Health - default 20.0D - min 0.0D - max Double.MAX_VALUE
        this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(20);// .setAttribute(20);
        // Follow Range - default 32.0D - min 0.0D - max 2048.0D
        this.getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(32);// .setAttribute(32.0D);
        // Knockback Resistance - default 0.0D - min 0.0D - max 1.0D
        this.getEntityAttribute(SharedMonsterAttributes.knockbackResistance).setBaseValue(10);// .setAttribute(0.0D);
        // Movement Speed - default 0.699D - min 0.0D - max Double.MAX_VALUE
        moveSpeed = 0.6f;
        this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(moveSpeed);// .setAttribute(moveSpeed);
        // Attack Damage - default 2.0D - min 0.0D - max Doubt.MAX_VALUE
        // getEntityAttribute(SharedMonsterAttributes.attackDamage).setAttribute(2.0D);
    }

    @Override
    public void onUpdate()
    {
        super.onUpdate();
        if (getBaseStats()[0] == 0) setStats(getPokedexEntry().getStats());
        if (Math.random() > 0.999 && this.getPokemonAIState(TAMED))
        {
            HappinessType.applyHappiness(this, HappinessType.TIME);
        }
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound nbttagcompound)
    {
        super.writeEntityToNBT(nbttagcompound);
        nbttagcompound.setInteger(PokecubeSerializer.EXP, getExp());
        nbttagcompound.setString(PokecubeSerializer.NICKNAME, getPokemonNickname());
        nbttagcompound.setLong(PokecubeSerializer.EVS, PokecubeSerializer.byteArrayAsLong(getEVs()));
        nbttagcompound.setLong(PokecubeSerializer.IVS, PokecubeSerializer.byteArrayAsLong(getIVs()));
        byte[] rgbaBytes = { (byte) (rgba[0] - 128), (byte) (rgba[1] - 128), (byte) (rgba[2] - 128),
                (byte) (rgba[3] - 128) };
        nbttagcompound.setByteArray("colours", rgbaBytes);
        nbttagcompound.setBoolean("shiny", shiny);
        nbttagcompound.setByte("nature", (byte) nature.ordinal());
        nbttagcompound.setInteger("happiness", bonusHappiness);
        if (ability != null) nbttagcompound.setString("ability", ability.toString());
        nbttagcompound.setBoolean("isAncient", isAncient);
        nbttagcompound.setBoolean("wasShadow", wasShadow);
        nbttagcompound.setString("forme", forme);
        if (pokedexNb == 132) nbttagcompound.setBoolean("dittotag", getEntityData().getBoolean("dittotag"));
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound nbttagcompound)
    {
        super.readEntityFromNBT(nbttagcompound);

        setPokemonNickname(nbttagcompound.getString(PokecubeSerializer.NICKNAME));

        try
        {
            setEVs(PokecubeSerializer.longAsByteArray(nbttagcompound.getLong(PokecubeSerializer.EVS)));
            long ivs = nbttagcompound.getLong(PokecubeSerializer.IVS);

            if (ivs == 0)
            {
                ivs = PokecubeSerializer.byteArrayAsLong(
                        new byte[] { Tools.getRandomIV(rand), Tools.getRandomIV(rand), Tools.getRandomIV(rand),
                                Tools.getRandomIV(rand), Tools.getRandomIV(rand), Tools.getRandomIV(rand) });
            }

            setIVs(PokecubeSerializer.longAsByteArray(ivs));
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }

        setExp(nbttagcompound.getInteger(PokecubeSerializer.EXP), false, false);
        String movesString = nbttagcompound.getString(PokecubeSerializer.MOVES);
        // isShadow = nbttagcompound.getBoolean("isShadow");
        isAncient = nbttagcompound.getBoolean("isAncient");
        wasShadow = nbttagcompound.getBoolean("wasShadow");
        // this.setShadow(isShadow);

        byte[] rgbaBytes = new byte[4];
        // TODO remove the legacy colour support eventually.
        if (nbttagcompound.hasKey("colours", 7))
        {
            rgbaBytes = nbttagcompound.getByteArray("colours");
        }
        else
        {
            rgbaBytes[0] = nbttagcompound.getByte("red");
            rgbaBytes[1] = nbttagcompound.getByte("green");
            rgbaBytes[2] = nbttagcompound.getByte("blue");
            rgbaBytes[3] = 127;
        }
        for (int i = 0; i < 4; i++)
            rgba[i] = rgbaBytes[i] + 128;

        shiny = nbttagcompound.getBoolean("shiny");
        addHappiness(nbttagcompound.getInteger("happiness"));
        if (getAbility() != null) getAbility().destroy();
        if (nbttagcompound.hasKey("ability", 8))
            setAbility(AbilityManager.getAbility(nbttagcompound.getString("ability")));
        else if (nbttagcompound.hasKey("ability", 3))
            setAbility(getPokedexEntry().getAbility(nbttagcompound.getInteger("ability")));

        if (ability == null)
        {
            Random random = new Random();
            int abilityNumber = random.nextInt(100) % 2;
            if (getPokedexEntry().getAbility(abilityNumber) == null)
            {
                if (abilityNumber != 0) abilityNumber = 0;
                else abilityNumber = 1;
            }
            setAbility(getPokedexEntry().getAbility(abilityNumber));
        }
        if (ability != null) ability.init(this);

        nature = Nature.values()[nbttagcompound.getByte("nature")];
        forme = nbttagcompound.getString("forme");
        this.changeForme(forme);
        getEntityData().setBoolean("dittotag", nbttagcompound.getBoolean("dittotag"));
        if (movesString != null && movesString.length() > 2)
        {
            String[] moves = movesString.split(",");
            int indexMove = 0;

            for (int i = 0; i < Math.min(4, moves.length); i++)
            {
                String move = moves[i];

                if (move != null && move.length() > 1)
                {
                    setMove(indexMove, move);
                    indexMove++;
                }
            }
        }
    }

    @Override
    public PokeType getType1()
    {
        if (transformedTo instanceof IPokemob)
        {
            IPokemob to = (IPokemob) transformedTo;
            return to.getType1();
        }
        return getPokedexEntry().getType1();
    }

    @Override
    public PokeType getType2()
    {
        if (transformedTo instanceof IPokemob)
        {
            IPokemob to = (IPokemob) transformedTo;
            return to.getType2();
        }
        return getPokedexEntry().getType2();
    }

    @Override
    public boolean isType(PokeType steel)
    {
        return this.getType1() == steel || getType2() == steel;
    }

    @Override
    public float getAttackStrength()
    {
        int ATT = getPokedexEntry().getStatATT();
        int ATTSPE = getPokedexEntry().getStatATTSPE();
        float mult = getPokemonAIState(SHADOW) ? 2 : 1;

        return mult * (Tools.getStat((ATT + ATTSPE) / 2, 0, 0, getLevel(), (getModifiers()[1] + getModifiers()[3]) / 2,
                (nature.getStatsMod()[1] + nature.getStatsMod()[3]) / 2) / 3);
    }

    private void setMaxHealth(float maxHealth)
    {
        this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(maxHealth);// .setAttribute(maxHealth);
        // dataWatcher.updateObject(28, maxHealth);
    }

    @Override
    public int getHappiness()
    {
        bonusHappiness = dataWatcher.getWatchableObjectInt(HAPPYDW);
        bonusHappiness = Math.max(bonusHappiness, -getPokedexEntry().getHappiness());
        bonusHappiness = Math.min(bonusHappiness, 255 - getPokedexEntry().getHappiness());
        return bonusHappiness + getPokedexEntry().getHappiness();
    }

    @Override
    public void addHappiness(int toAdd)
    {
        this.bonusHappiness += toAdd;
        this.dataWatcher.updateObject(HAPPYDW, Integer.valueOf(bonusHappiness));
    }

    @Override
    public int[] getBaseStats()
    {
        int[] stats = new int[6];
        String[] sta = dataWatcher.getWatchableObjectString(STATSDW).split(",");
        for (int i = 0; i < 6; i++)
        {
            stats[i] = Integer.parseInt(sta[i].trim());
        }

        if (stats[0] == 0)
        {
            setStats(getPokedexEntry().getStats());
        }
        return stats;
    }

    @Override
    public int[] getActualStats()
    {
        int[] stats = getBaseStats();
        int level = getLevel();
        byte[] evs = getEVs();
        byte[] mods = getModifiers();
        stats[0] = Tools.getHP(stats[0], ivs[0], evs[0], level);
        for (int i = 1; i < stats.length; i++)
        {
            stats[i] = Tools.getStat(stats[i], ivs[i], evs[i], level, mods[i], getNature().getStatsMod()[i]);
        }
        return stats;
    }

    @Override
    public byte[] getModifiers()
    {
        return PokecubeSerializer.intAsModifierArray(dataWatcher.getWatchableObjectInt(STATMODDW));
    }

    @Override
    public void setModifiers(byte[] modifiers)
    {
        dataWatcher.updateObject(STATMODDW, PokecubeSerializer.modifierArrayAsInt(modifiers));
    }

    @Override
    public void setEVs(byte[] evs)
    {
        int[] ints = PokecubeSerializer.byteArrayAsIntArray(evs);
        dataWatcher.updateObject(EVS1DW, ints[0]);
        dataWatcher.updateObject(EVS2DV, ints[1]);
    }

    @Override
    public byte[] getEVs()
    {
        int[] ints = new int[] { dataWatcher.getWatchableObjectInt(EVS1DW), dataWatcher.getWatchableObjectInt(EVS2DV) };
        byte[] evs = PokecubeSerializer.intArrayAsByteArray(ints);
        return evs;
    }

    @Override
    public int[] getRGBA()
    {
        return rgba;
    }

    @Override
    public void setRGBA(int... colours)
    {
        for (int i = 0; i < colours.length && i < rgba.length; i++)
        {
            rgba[i] = colours[i];
        }
    }

    @Override
    public void addEVs(byte[] evsToAdd)
    {
        byte[] evs = getEVs();
        for (int i = 0; i < 6; i++)
        {
            if (evs[i] + 128 + evsToAdd[i] <= 255 && evs[i] + 128 + evsToAdd[i] >= 0)
            {
                evs[i] = (byte) (evs[i] + evsToAdd[i]);
            }
            else
            {
                evs[i] = (byte) 127;
            }
        }

        int sum = 0;

        for (byte ev : evs)
        {
            sum += ev + 128;
        }

        if (sum < 510)
        {
            setEVs(evs);
        }
    }

    @Override
    public void setIVs(byte[] ivs)
    {
        this.ivs = ivs;
    }

    @Override
    public byte[] getIVs()
    {
        return this.ivs;
    }

    /** Handles health update.
     * 
     * @param level */
    private void setLevel(int level)
    {
        float old = getMaxHealth();
        float maxHealth = Tools.getHP(getPokedexEntry().getStatHP(), getIVs()[0], getEVs()[0], level);
        float health = getHealth();

        // actually we don't really set the level.
        if (maxHealth > old)
        {
            float damage = old - health;
            health = maxHealth - damage;

            if (health > maxHealth)
            {
                health = maxHealth;
            }
        }

        setMaxHealth(maxHealth);
        setHealth(health);
    }

    @Override
    public int getLevel()
    {
        return Tools.xpToLevel(getExperienceMode(), getExp());
    }

    @Override
    public int getPokedexNb()
    {
        if (pokedexNb == 0)
        {

            if (getClass().getName().contains("GenericPokemob"))
            {
                String num = getClass().getSimpleName().replace("GenericPokemob", "").trim();
                PokedexEntry entry = Database.getEntry(Integer.parseInt(num));
                if (entry != null && entry.getPokedexNb() > 0)
                {
                    pokedexNb = entry.getPokedexNb();
                    // new Exception().printStackTrace();
                    init(entry.getPokedexNb());
                    return pokedexNb;
                }
            }

            System.out.println(this.getClass());
            new Exception().printStackTrace();
            this.setDead();
            return 0;
        }
        return pokedexNb;
    }

    @Override
    public PokedexEntry getPokedexEntry()
    {
        if (entry == null)
        {
            entry = Pokedex.getInstance().getEntry(getPokedexNb());
        }
        if (entry.getName().equalsIgnoreCase(Pokedex.getInstance().getEntry(getPokedexNb()).getName()))
            return Pokedex.getInstance().getEntry(getPokedexNb());
        return entry;
    }

    @Override
    public void setPokedexEntry(PokedexEntry newEntry)
    {
        if (entry == newEntry) return;
        entry = newEntry;
        this.pokedexNb = entry.getPokedexNb();
        this.setStats(getPokedexEntry().getStats());
    }

    @Override
    public String getPokemonDisplayName()
    {
        if (this.isAncient())
            return "Ancient " + Database.getEntry(getPokedexEntry().getBaseName()).getTranslatedName();
        else if (getPokemonNickname() == null || getPokemonNickname().isEmpty())
        {
            return getPokedexEntry().getTranslatedName();
        }
        else
        {
            return getPokemonNickname();
        }
    }

    @Override
    public void setPokemonNickname(String nickname)
    {
        if (PokecubeCore.isOnClientSide() && nickname != getPokemonNickname())
        {
            try
            {
                byte[] string = nickname.getBytes();
                PacketBuffer buffer = new PacketBuffer(Unpooled.buffer(6 + string.length));
                buffer.writeByte(MessageServer.NICKNAME);
                buffer.writeInt(getEntityId());
                buffer.writeByte(string.length);
                buffer.writeByteArray(string);
                MessageServer packet = new MessageServer(buffer);
                PokecubePacketHandler.sendToServer(packet);
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }
        else
        {
            if (getPokedexEntry().getTranslatedName().equals(nickname))
            {
                dataWatcher.updateObject(NICKNAMEDW, "");
            }
            else
            {
                dataWatcher.updateObject(NICKNAMEDW, nickname);
            }
        }
    }

    @Override
    public String getPokemonNickname()
    {
        return dataWatcher.getWatchableObjectString(NICKNAMEDW);
    }

    @Override
    public int getExp()
    {
        return dataWatcher.getWatchableObjectInt(EXPDW);
    }

    @Override
    public void setExp(int exp, boolean notifyLevelUp, boolean newlySpawned)
    {
        if (this.isDead) return;

        int old = dataWatcher.getWatchableObjectInt(EXPDW);
        oldLevel = this.getLevel();
        int lvl100xp = Tools.maxXPs[getExperienceMode()];
        exp = Math.min(lvl100xp, exp);

        dataWatcher.updateObject(EXPDW, exp);
        int newLvl = Tools.xpToLevel(getExperienceMode(), exp);
        int oldLvl = Tools.xpToLevel(getExperienceMode(), old);

        if (oldLvl != newLvl)
        {
            // Fire event to allow others to interfere
            LevelUpEvent lvlup = new LevelUpEvent(this, newLvl, oldLevel);
            MinecraftForge.EVENT_BUS.post(lvlup);

            if (lvlup.isCanceled()) return;

            setLevel(newLvl);

            if (notifyLevelUp)
            {
                if (notifyLevelUp) if (!this.isDead && (canEvolve(null) || canEvolve(getHeldItem())))
                {
                    levelUp(newLvl);
                    this.evolve(true, getHeldItem());
                }
                levelUp(newLvl);
            }
            setStats(getPokedexEntry().getStats());

        }
    }

    /** This method gets called when the entity kills another one. */
    @Override
    public void onKillEntity(EntityLivingBase attacked)
    {
        IPokemob attacker = this;

        if (attacked instanceof IPokemob && attacked.getHealth() <= 0)
        {
            KillEvent event = new KillEvent(attacker, (IPokemob) attacked);

            MinecraftForge.EVENT_BUS.post(event);
            if (event.isCanceled() || ((IPokemob) attacked).isShadow())
            {

            }
            else if (!(((IPokemob) attacked).getPokemonAIState(TAMED) && !Mod_Pokecube_Helper.pvpExp))
            {
                attacker.setExp(
                        attacker.getExp()
                                + Tools.getExp(1, ((IPokemob) attacked).getBaseXP(), ((IPokemob) attacked).getLevel()),
                        true, false);
                byte[] evsToAdd = Pokedex.getInstance().getEntry(((IPokemob) attacked).getPokedexNb()).getEVs();
                attacker.addEVs(evsToAdd);
            }
            Entity targetOwner = ((IPokemob) attacked).getPokemonOwner();

            if (targetOwner instanceof EntityPlayer && attacker.getPokemonOwner() != targetOwner
                    && !PokecubeMod.hardMode)
            {
                ((EntityCreature) attacker).setAttackTarget((EntityLivingBase) targetOwner);
            }
            else
            {
                ((EntityCreature) attacker).setAttackTarget((EntityLivingBase) null);
            }

            if (this.getPokedexEntry().isFood(((IPokemob) attacked).getPokedexEntry())
                    && this.getPokemonAIState(HUNTING))
            {
                ((EntityHungryPokemob) this).eat(getAttackTarget());
                ((EntityPokemob) attacked).wasEaten = true;
                this.setPokemonAIState(HUNTING, false);
                getNavigator().clearPathEntity();
            }

        }
    }

    @Override
    public void setStats(int[] stats)
    {
        String sta = stats[0] + "," + stats[1] + "," + stats[2] + "," + stats[3] + "," + stats[4] + "," + stats[5];
        dataWatcher.updateObject(STATSDW, sta);
    }

    @Override
    public int getBaseXP()
    {
        return getPokedexEntry().getBaseXP();
    }

    @Override
    public int getExperienceMode()
    {
        return getPokedexEntry().getEvolutionMode();
    }

    @Override
    public int getCatchRate()
    {
        return getPokemonAIState(SHADOW) ? 0 : isAncient() ? 0 : getPokedexEntry().getCatchRate();
    }

    /** Use this for anything that does not change or need to be updated. */
    @Override
    public void writeSpawnData(ByteBuf data)
    {
        data.writeInt(forme.getBytes().length);
        data.writeBytes(forme.getBytes());
        data.writeBoolean(shiny);
        data.writeBoolean(wasShadow);
        data.writeBoolean(isAncient);
        data.writeByte((byte) nature.ordinal());
        data.writeBytes(ivs);
        boolean noTags = getEntityData().hasNoTags();
        data.writeBoolean(!noTags);
        PacketBuffer buffer = new PacketBuffer(data);
        buffer.writeNBTTagCompoundToBuffer(getEntityData());
    }

    /** Use this for anything that does not change or need to be updated. */
    @Override
    public void readSpawnData(ByteBuf data)
    {
        int num = data.readInt();
        byte[] arr = new byte[num];
        for (int i = 0; i < num; i++)
            arr[i] = data.readByte();
        forme = new String(arr);
        shiny = data.readBoolean();
        wasShadow = data.readBoolean();
        isAncient = data.readBoolean();
        nature = Nature.values()[data.readByte()];
        this.changeForme(forme);
        for (int i = 0; i < ivs.length; i++)
        {
            ivs[i] = data.readByte();
        }

        boolean tags = data.readBoolean();
        if (tags)
        {
            PacketBuffer buffer = new PacketBuffer(data);
            try
            {
                NBTTagCompound tag = buffer.readNBTTagCompoundFromBuffer();
                for (Object o : tag.getKeySet())// .func_150296_c())
                {
                    getEntityData().setTag((String) o, tag.getTag((String) o));
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean isShadow()
    {
        boolean isShadow = getPokemonAIState(SHADOW);
        if (isShadow && !wasShadow)
        {
            wasShadow = true;
        }
        return isShadow;
    }

    @Override
    public void setShadow(boolean shadow)
    {
        setPokemonAIState(SHADOW, shadow);
        if (shadow && !wasShadow)
        {
            wasShadow = true;
        }
    }

    @Override
    public boolean isAncient()
    {
        return isAncient;
    }

    @Override
    public void setAncient(boolean ancient)
    {
        isAncient = ancient;
    }

    void setRandomColour()
    {
        Random r = new Random();
        int first = r.nextInt(3);
        byte red = 127, green = 127, blue = 127;
        if (first == 0)
        {
            int min = 0;
            red = (byte) Math.max(Math.min(((5 - Math.abs(colourDiffFactor * r.nextGaussian())) * 32), 127), min);
            min = red < 63 ? 63 : 0;

            green = (byte) Math.max(Math.min(((5 - Math.abs(colourDiffFactor * r.nextGaussian())) * 32), 127), min);
            min = green < 63 ? 63 : 0;

            blue = (byte) Math.max(Math.min(((5 - Math.abs(colourDiffFactor * r.nextGaussian())) * 32), 127), min);
        }
        if (first == 1)
        {
            int min = 0;

            green = (byte) Math.max(Math.min(((5 - Math.abs(colourDiffFactor * r.nextGaussian())) * 32), 127), min);
            min = green < 63 ? 63 : 0;

            red = (byte) Math.max(Math.min(((5 - Math.abs(colourDiffFactor * r.nextGaussian())) * 32), 127), min);
            min = red < 63 ? 63 : 0;

            blue = (byte) Math.max(Math.min(((5 - Math.abs(colourDiffFactor * r.nextGaussian())) * 32), 127), min);
        }
        if (first == 2)
        {
            int min = 0;
            blue = (byte) Math.max(Math.min(((5 - Math.abs(colourDiffFactor * r.nextGaussian())) * 32), 127), min);
            min = blue < 63 ? 63 : 0;

            red = (byte) Math.max(Math.min(((5 - Math.abs(colourDiffFactor * r.nextGaussian())) * 32), 127), min);
            min = red < 63 ? 63 : 0;

            green = (byte) Math.max(Math.min(((5 - Math.abs(colourDiffFactor * r.nextGaussian())) * 32), 127), min);

        }
        rgba[0] = red + 128;
        rgba[1] = green + 128;
        rgba[2] = blue + 128;
    }

    @Override
    public void setShiny(boolean shiny)
    {
        this.shiny = shiny;
    }

    @Override
    public boolean isShiny()
    {
        return shiny;
    }

    @Override
    public Nature getNature()
    {
        return nature;
    }

    @Override
    public void setNature(Nature nature)
    {
        this.nature = nature;
    }

    @Override
    /** Called when the entity is attacked. */
    public boolean attackEntityFrom(DamageSource source, float amount)
    {
        if (!net.minecraftforge.common.ForgeHooks.onLivingAttack(this, source, amount)) return false;
        if (this.isEntityInvulnerable(source))
        {
            return false;
        }
        else if (this.worldObj.isRemote)
        {
            return false;
        }
        else
        {
            this.entityAge = 0;

            if (this.getHealth() <= 0.0F)
            {
                return false;
            }
            else if (source.isFireDamage() && this.isPotionActive(Potion.fireResistance))
            {
                return false;
            }
            else
            {
                if ((source == DamageSource.anvil || source == DamageSource.fallingBlock)
                        && this.getEquipmentInSlot(4) != null)
                {
                    this.getEquipmentInSlot(4).damageItem((int) (amount * 4.0F + this.rand.nextFloat() * amount * 2.0F),
                            this);
                    amount *= 0.75F;
                }

                this.limbSwingAmount = 1.5F;
                boolean flag = true;

                if ((float) this.hurtResistantTime > (float) this.maxHurtResistantTime / 2.0F)
                {
                    if (amount <= this.lastDamage) { return false; }

                    this.damageEntity(source, amount - this.lastDamage);
                    this.lastDamage = amount;
                    flag = false;
                }
                else
                {
                    this.lastDamage = amount;
                    this.hurtResistantTime = this.maxHurtResistantTime;
                    this.damageEntity(source, amount);
                    this.hurtTime = this.maxHurtTime = 10;
                }

                this.attackedAtYaw = 0.0F;
                Entity entity = source.getEntity();

                if (entity != null)
                {
                    if (entity instanceof EntityLivingBase)
                    {
                        this.setRevengeTarget((EntityLivingBase) entity);
                    }

                    if (entity instanceof EntityPlayer)
                    {
                        this.recentlyHit = 100;
                        this.attackingPlayer = (EntityPlayer) entity;
                    }
                    else if (entity instanceof IEntityOwnable)
                    {
                        IEntityOwnable entitywolf = (IEntityOwnable) entity;

                        if (entitywolf.getOwner() != null)
                        {
                            this.recentlyHit = 100;
                            this.attackingPlayer = null;
                        }
                    }
                }

                if (flag)
                {
                    this.worldObj.setEntityState(this, (byte) 2);

                    if (source != DamageSource.drown)
                    {
                        this.setBeenAttacked();
                    }

                    if (entity != null)
                    {
                        double d1 = entity.posX - this.posX;
                        double d0;

                        for (d0 = entity.posZ - this.posZ; d1 * d1
                                + d0 * d0 < 1.0E-4D; d0 = (Math.random() - Math.random()) * 0.01D)
                        {
                            d1 = (Math.random() - Math.random()) * 0.01D;
                        }

                        this.attackedAtYaw = (float) (MathHelper.atan2(d0, d1) * 180.0D / Math.PI
                                - (double) this.rotationYaw);
                        // Reduces knockback from distanced moves
                        if (source instanceof PokemobDamageSource)
                        {
                            if (!source.isProjectile())
                            {
                                this.knockBack(entity, amount, d1, d0);
                            }
                        }
                        else
                        {
                            this.knockBack(entity, amount, d1, d0);
                        }
                    }
                    else
                    {
                        this.attackedAtYaw = (float) ((int) (Math.random() * 2.0D) * 180);
                    }
                }

                if (this.getHealth() <= 0.0F)
                {
                    String s = this.getDeathSound();

                    if (flag && s != null)
                    {
                        this.playSound(s, this.getSoundVolume(), this.getSoundPitch());
                    }

                    this.onDeath(source);
                }
                else
                {
                    String s1 = this.getHurtSound();

                    if (flag && s1 != null)
                    {
                        this.playSound(s1, this.getSoundVolume(), this.getSoundPitch());
                    }
                }

                return true;
            }
        }
    }

    @Override
    public boolean isEntityInvulnerable(DamageSource source)
    {
        if (source instanceof PokemobDamageSource)
        {
            Move_Base move = ((PokemobDamageSource) source).move;
            return PokeType.getAttackEfficiency(move.getType(), getType1(), getType2()) <= 0;
        }

        return super.isEntityInvulnerable(source);
    }

    @Override
    public Ability getAbility()
    {
        if (getPokemonAIState(MEGAFORME)) return getPokedexEntry().getAbility(0);
        return ability;
    }

    @Override
    public void setAbility(Ability ability)
    {
        this.ability = ability;
    }
}
