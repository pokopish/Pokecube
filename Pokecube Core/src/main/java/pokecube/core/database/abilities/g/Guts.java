package pokecube.core.database.abilities.g;

import pokecube.core.database.abilities.Ability;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.IPokemob.MovePacket;

public class Guts extends Ability
{
    @Override
    public void onMoveUse(IPokemob mob, MovePacket move)
    {
        if(mob == move.attacker && move.pre)
        {
            move.statMults[1] = 1.5f;
        }
    }
}
