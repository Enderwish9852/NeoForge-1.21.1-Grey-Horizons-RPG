package net.enderwish.TerraForma_Subpack.mixin;

import net.enderwish.TerraForma_Subpack.core.current.CurrentField;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * EntityCurrentMixin
 *
 * NEW -- the current carries everything that is in it.
 *
 * This is hooked into the movement itself rather than into a tick
 * event, because everything that moves goes through here: the
 * player, the mobs, the dropped items, the boats. Whatever is in
 * moving water is carried by it.
 *
 * The push is added to the movement, not set as it, so that a
 * swimmer still swims: the current is added to what the entity was
 * already doing. That is what makes a fast current hard to swim
 * against -- the swimming and the current pull opposite ways, and
 * the current wins when it is strong enough.
 */
@Mixin(Entity.class)
public class EntityCurrentMixin {

    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true, index = 1)
    private Vec3 gh_terraforma$carriedByTheCurrent(Vec3 movement) {
        Entity self = (Entity) (Object) this;
        Level level = self.level();
        if (level == null) return movement;
        // a passenger is moved by whatever it is riding
        if (self.getVehicle() != null) return movement;

        Vec3 current = CurrentField.currentAt(level, self.blockPosition());
        if (current == null) return movement;

        return movement.add(current.x, 0.0, current.z);
    }
}
