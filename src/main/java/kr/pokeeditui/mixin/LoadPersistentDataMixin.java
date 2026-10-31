package kr.pokeeditui.mixin;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.util.codec.internal.ClientPokemonP2;
import com.cobblemon.mod.common.util.codec.internal.PokemonP2;
import kr.pokeeditui.BaseStats;
import kr.pokeeditui.PendingHealth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Sets the HP remembered by LoadHealthMixin again, now that the max HP includes the base stat change. */
@Mixin(value = {PokemonP2.class, ClientPokemonP2.class}, remap = false)
public abstract class LoadPersistentDataMixin {
    @Inject(method = "into(Lcom/cobblemon/mod/common/pokemon/Pokemon;)Lcom/cobblemon/mod/common/pokemon/Pokemon;",
        at = @At("RETURN"))
    private void pokeeditui$restoreHealth(Pokemon pokemon, CallbackInfoReturnable<Pokemon> cir) {
        PendingHealth pending = (PendingHealth) pokemon;
        int health = pending.pokeeditui$pendingHealth();
        pending.pokeeditui$setPendingHealth(-1);

        if (health >= 0 && health != pokemon.getCurrentHealth() && BaseStats.delta(pokemon) != null) {
            pokemon.setCurrentHealth(health);
        }
    }
}
