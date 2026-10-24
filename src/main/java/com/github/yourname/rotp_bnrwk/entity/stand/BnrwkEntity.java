package com.github.yourname.rotp_bnrwk.entity.stand;

import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.entity.stand.StandEntityType;
import com.github.yourname.rotp_bnrwk.init.InitSounds;

import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;

/**
 * Boku no Rhythm wo Kiitekure - user: Oyecomova.
 * Short-range humanoid Stand with a shoebill head, stitches, two discs on the head and a tattered cloak.
 * All of its abilities live in the Action classes; the entity itself only plays its summon sound.
 */
public class BnrwkEntity extends StandEntity {

    public BnrwkEntity(StandEntityType<BnrwkEntity> type, World world) {
        super(type, world);
    }

    /** Clockwork winding up + chime when the Stand appears (server side, heard by everyone nearby). */
    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (!level.isClientSide()) {
            level.playSound(null, getX(), getY(), getZ(), InitSounds.SUMMON.get(), SoundCategory.PLAYERS, 1.0F, 1.0F);
        }
    }
}
