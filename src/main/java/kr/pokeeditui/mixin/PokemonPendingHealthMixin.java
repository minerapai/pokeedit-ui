package kr.pokeeditui.mixin;

import com.cobblemon.mod.common.pokemon.Pokemon;
import kr.pokeeditui.PendingHealth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = Pokemon.class, remap = false)
public abstract class PokemonPendingHealthMixin implements PendingHealth {
    @Unique
    private int pokeeditui$pendingHealth = -1;

    @Override
    public int pokeeditui$pendingHealth() {
        return pokeeditui$pendingHealth;
    }

    @Override
    public void pokeeditui$setPendingHealth(int health) {
        pokeeditui$pendingHealth = health;
    }
}
