package kr.pokeeditui.mixin;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.util.codec.internal.ClientPokemonP1;
import com.cobblemon.mod.common.util.codec.internal.PokemonP1;
import kr.pokeeditui.PendingHealth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Loading a pokemon (save data on the server, sync packets on the client) sets its current HP before its persistent
 * data, so the HP is clamped to the max HP without the base stat change. Remember the saved value for
 * LoadPersistentDataMixin to set again once the base stats are known.
 */
@Mixin(value = {PokemonP1.class, ClientPokemonP1.class}, remap = false)
public abstract class LoadHealthMixin {
    @Redirect(method = "into(Lcom/cobblemon/mod/common/pokemon/Pokemon;)Lcom/cobblemon/mod/common/pokemon/Pokemon;",
        at = @At(value = "INVOKE", target = "Lcom/cobblemon/mod/common/pokemon/Pokemon;setCurrentHealth(I)V"))
    private void pokeeditui$rememberHealth(Pokemon pokemon, int health) {
        ((PendingHealth) pokemon).pokeeditui$setPendingHealth(health);
        pokemon.setCurrentHealth(health);
    }
}
